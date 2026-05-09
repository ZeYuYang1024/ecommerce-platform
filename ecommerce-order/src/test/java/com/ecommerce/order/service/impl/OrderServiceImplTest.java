package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper itemMapper;
    @InjectMocks private OrderServiceImpl service;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNo("202605091200000001");
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("6999.00"));
        order.setStatus(0);
        order.setReceiverName("收货人");
        order.setReceiverPhone("13800001111");
        order.setReceiverAddress("北京市");
    }

    @Nested
    class CreateTests {
        @Test
        void shouldCreateOrder() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("收货人"); req.setReceiverPhone("138"); req.setReceiverAddress("地址");
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setSkuId(1L); item.setSpuId(1L); item.setName("商品"); item.setPrice("99.00"); item.setQuantity(2);
            req.setItems(Collections.singletonList(item));

            OrderVO vo = service.createOrder(1L, req);
            assertThat(vo.getOrderNo()).isNotNull();
            assertThat(vo.getStatus()).isEqualTo(0);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("198.00");
        }

        @Test
        void shouldRejectEmptyItems() {
            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("x"); req.setReceiverPhone("x"); req.setReceiverAddress("x");
            req.setItems(Collections.emptyList());

            assertThatThrownBy(() -> service.createOrder(1L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(OrderErrorCode.ORDER_ITEMS_EMPTY.getCode());
        }
    }

    @Nested
    class QueryTests {
        @Test
        void shouldGetOrderWithItems() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            OrderVO vo = service.getOrder(1L);
            assertThat(vo.getOrderNo()).isEqualTo("202605091200000001");
            assertThat(vo.getStatusText()).isEqualTo("待支付");
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.getOrder(999L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldListUserOrders() {
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(order));
            List<OrderVO> list = service.listByUser(1L);
            assertThat(list).hasSize(1);
        }
    }

    @Nested
    class CancelTests {
        @Test
        void shouldCancelPendingOrder() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            service.cancelOrder(1L, 1L);
            verify(orderMapper).updateById(any(Order.class));
        }

        @Test
        void shouldRejectCancelNonPending() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(OrderErrorCode.ORDER_NOT_PENDING);
        }
    }

    @Nested
    class ShipTests {
        @Test
        void shouldMarkShipped() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            service.markShipped(1L);
        }

        @Test
        void shouldThrowWhenShipNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.markShipped(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    class BoundaryTests {
        @Test
        void shouldCreateOrderWithMultipleItems() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);

            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("收货人"); req.setReceiverPhone("138"); req.setReceiverAddress("地址");
            CreateOrderRequest.OrderItemRequest item1 = new CreateOrderRequest.OrderItemRequest();
            item1.setSkuId(1L); item1.setSpuId(1L); item1.setName("A"); item1.setPrice("10.00"); item1.setQuantity(1);
            CreateOrderRequest.OrderItemRequest item2 = new CreateOrderRequest.OrderItemRequest();
            item2.setSkuId(2L); item2.setSpuId(1L); item2.setName("B"); item2.setPrice("20.00"); item2.setQuantity(3);
            CreateOrderRequest.OrderItemRequest item3 = new CreateOrderRequest.OrderItemRequest();
            item3.setSkuId(3L); item3.setSpuId(2L); item3.setName("C"); item3.setPrice("0.01"); item3.setQuantity(100);
            req.setItems(java.util.Arrays.asList(item1, item2, item3));

            OrderVO vo = service.createOrder(1L, req);
            // 10*1 + 20*3 + 0.01*100 = 10 + 60 + 1 = 71
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("71.00");
        }

        @Test
        void shouldCreateOrderWithSingleLargeQuantity() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("x"); req.setReceiverPhone("x"); req.setReceiverAddress("x");
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setSkuId(1L); item.setSpuId(1L); item.setName("批量"); item.setPrice("1.00"); item.setQuantity(9999);
            req.setItems(Collections.singletonList(item));
            OrderVO vo = service.createOrder(1L, req);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("9999.00");
        }

        @Test
        void shouldCreateOrderWithHighPrice() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("x"); req.setReceiverPhone("x"); req.setReceiverAddress("x");
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setSkuId(1L); item.setSpuId(1L); item.setName("奢侈品"); item.setPrice("999999.99"); item.setQuantity(1);
            req.setItems(Collections.singletonList(item));
            OrderVO vo = service.createOrder(1L, req);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("999999.99");
        }

        @Test
        void shouldCreateOrderWithZeroPriceItem() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("x"); req.setReceiverPhone("x"); req.setReceiverAddress("x");
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setSkuId(1L); item.setSpuId(1L); item.setName("赠品"); item.setPrice("0.00"); item.setQuantity(1);
            req.setItems(Collections.singletonList(item));
            OrderVO vo = service.createOrder(1L, req);
            assertThat(vo.getTotalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldListAllAdminOrders() {
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(order));
            List<OrderVO> list = service.listAll(0);
            assertThat(list).hasSize(1);
        }

        @Test
        void shouldListAllAdminOrdersWithoutStatus() {
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(order));
            List<OrderVO> list = service.listAll(null);
            assertThat(list).hasSize(1);
        }

        @Test
        void shouldShowCorrectStatusTextForAllStatuses() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            assertThat(service.getOrder(1L).getStatusText()).isEqualTo("待支付");

            order.setStatus(1);
            assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已支付");

            order.setStatus(4);
            assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已取消");
        }

        @Test
        void shouldCancelOrderWithBoundaryStatus() {
            // status=4 (already cancelled) should not be cancellable
            order.setStatus(4);
            when(orderMapper.selectById(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.cancelOrder(1L, 1L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldGetNonexistentOrderWithMaxLong() {
            when(orderMapper.selectById(Long.MAX_VALUE)).thenReturn(null);
            assertThatThrownBy(() -> service.getOrder(Long.MAX_VALUE))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
