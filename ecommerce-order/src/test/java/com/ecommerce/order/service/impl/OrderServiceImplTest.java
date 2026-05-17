package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.ProductSpuClient;
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
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderMapper orderMapper;
    @Mock private RocketMQTemplate rocketMQTemplate;
    @Mock private OrderItemMapper itemMapper;
    @Mock private ProductSpuClient productSpuClient;
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
            assertThatThrownBy(() -> service.getOrder(999L)).isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldListUserOrdersWithPagination() {
            Page<Order> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            Page<OrderVO> result = service.listByUser(1L, 1, 10);
            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
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
        void shouldMarkShippedWhenPaid() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            service.markShipped(1L, "super_admin", null);
            verify(orderMapper).updateById(any(Order.class));
        }

        @Test
        void shouldRejectShipWhenNotPaid() {
            order.setStatus(0);
            when(orderMapper.selectById(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.markShipped(1L, "super_admin", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(OrderErrorCode.ORDER_NOT_PAID.getCode());
        }

        @Test
        void shouldRejectShipWhenAlreadyCancelled() {
            order.setStatus(4);
            when(orderMapper.selectById(1L)).thenReturn(order);
            assertThatThrownBy(() -> service.markShipped(1L, "super_admin", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldThrowWhenShipNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.markShipped(999L, "super_admin", null)).isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectMerchantShipForOrderOutsideMerchantScope() {
            order.setStatus(1);
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(productSpuClient.getSpuIdsByMerchant(100L)).thenReturn(Result.ok(List.of(10L)));
            OrderItem foreignItem = new OrderItem();
            foreignItem.setOrderId(1L);
            foreignItem.setSpuId(99L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(foreignItem));

            assertThatThrownBy(() -> service.markShipped(1L, "merchant", 100L))
                    .isInstanceOf(BusinessException.class);

            verify(orderMapper, never()).updateById(any(Order.class));
        }
    }

    @Nested
    class AdminListTests {
        @Test
        void shouldListAllAdminOrdersWithPagination() {
            Page<Order> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            Page<OrderVO> result = service.listAll(1, 10, 0);
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        void shouldListAllAdminOrdersWithoutStatusFilter() {
            Page<Order> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(order));
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            Page<OrderVO> result = service.listAll(1, 10, null);
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        void shouldListForRecon() {
            order.setCreatedAt(LocalDateTime.now());
            when(orderMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(order));
            List<Order> orders = service.listForRecon(null, null);
            assertThat(orders).hasSize(1);
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
            CreateOrderRequest.OrderItemRequest i1 = new CreateOrderRequest.OrderItemRequest();
            i1.setSkuId(1L); i1.setSpuId(1L); i1.setName("A"); i1.setPrice("10.00"); i1.setQuantity(1);
            CreateOrderRequest.OrderItemRequest i2 = new CreateOrderRequest.OrderItemRequest();
            i2.setSkuId(2L); i2.setSpuId(1L); i2.setName("B"); i2.setPrice("20.00"); i2.setQuantity(3);
            CreateOrderRequest.OrderItemRequest i3 = new CreateOrderRequest.OrderItemRequest();
            i3.setSkuId(3L); i3.setSpuId(2L); i3.setName("C"); i3.setPrice("0.01"); i3.setQuantity(100);
            req.setItems(java.util.Arrays.asList(i1, i2, i3));
            OrderVO vo = service.createOrder(1L, req);
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
            assertThat(service.createOrder(1L, req).getTotalAmount()).isEqualByComparingTo("9999.00");
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
            assertThat(service.createOrder(1L, req).getTotalAmount()).isEqualByComparingTo("999999.99");
        }

        @Test
        void shouldCreateOrderWithZeroPrice() {
            when(orderMapper.insert(any(Order.class))).thenReturn(1);
            when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
            CreateOrderRequest req = new CreateOrderRequest();
            req.setReceiverName("x"); req.setReceiverPhone("x"); req.setReceiverAddress("x");
            CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
            item.setSkuId(1L); item.setSpuId(1L); item.setName("赠品"); item.setPrice("0.00"); item.setQuantity(1);
            req.setItems(Collections.singletonList(item));
            assertThat(service.createOrder(1L, req).getTotalAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        void shouldShowCorrectStatusTextForAllStatuses() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            assertThat(service.getOrder(1L).getStatusText()).isEqualTo("待支付");
            order.setStatus(1); assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已支付");
            order.setStatus(2); assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已发货");
            order.setStatus(3); assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已完成");
            order.setStatus(4); assertThat(service.getOrder(1L).getStatusText()).isEqualTo("已取消");
        }

        @Test
        void shouldCancelOrderWithBoundaryStatus() {
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

        @Test
        void shouldUpdateStatusToAllValidTransitions() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.updateById(any(Order.class))).thenReturn(1);
            service.updateStatus(1L, 3, "super_admin", null); // completed
            assertThat(order.getStatus()).isEqualTo(3);
        }

        @Test
        void shouldListAllWithPage2Empty() {
            Page<Order> mockPage = new Page<>(2, 10, 15);
            mockPage.setRecords(Collections.emptyList());
            when(orderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);
            Page<OrderVO> result = service.listAll(2, 10, null);
            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(15);
        }

        @Test
        void shouldUpdateStatusThrowsWhenOrderNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> service.updateStatus(999L, 1, "super_admin", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void shouldRejectMerchantStatusUpdateForOrderOutsideMerchantScope() {
            when(orderMapper.selectById(1L)).thenReturn(order);
            when(productSpuClient.getSpuIdsByMerchant(100L)).thenReturn(Result.ok(List.of(10L)));
            OrderItem foreignItem = new OrderItem();
            foreignItem.setOrderId(1L);
            foreignItem.setSpuId(99L);
            when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(foreignItem));

            assertThatThrownBy(() -> service.updateStatus(1L, 3, "merchant", 100L))
                    .isInstanceOf(BusinessException.class);

            verify(orderMapper, never()).updateById(any(Order.class));
        }
    }
}
