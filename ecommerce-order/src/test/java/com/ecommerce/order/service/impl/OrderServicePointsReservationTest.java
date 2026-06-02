package com.ecommerce.order.service.impl;

import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.MemberClient;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.client.dto.MemberPointsReservationReleaseRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveResponse;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServicePointsReservationTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper itemMapper;

    @Mock
    private ProductSpuClient productSpuClient;

    @Mock
    private CartClient cartClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private MemberClient memberClient;

    @InjectMocks
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pointsDeductionEnabled", true);
        ReflectionTestUtils.setField(service, "pointsPerYuan", 100);
    }

    @Test
    void createOrderShouldReservePointsBeforePersistingOrder() {
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));
        when(memberClient.reservePoints(any())).thenReturn(Result.ok(buildReserveResponse("PR1", 200, "RESERVED")));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "99.00")));
        req.setUsePoints(true);
        req.setPointsToUse(200);
        req.setClientRequestId("submit-ord-1");

        OrderVO vo = service.createOrder(1L, req);

        assertThat(vo.getOrderNo()).isNotBlank();
        assertThat(vo.getTotalAmount()).isEqualByComparingTo("196.00");
        verify(memberClient).reservePoints(argThat((MemberPointsReserveRequest request) ->
                request != null
                        && Long.valueOf(1L).equals(request.getUserId())
                        && "ORDER_DEDUCTION".equals(request.getSceneType())
                        && Integer.valueOf(200).equals(request.getPoints())));
        verify(orderMapper).insert(any(Order.class));
    }

    @Test
    void createOrderShouldPersistConfiguredPointsSnapshotOnOrder() {
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(itemMapper.insert(any(OrderItem.class))).thenReturn(1);
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));
        when(memberClient.reservePoints(any())).thenReturn(Result.ok(buildReserveResponse("PR1", 200, "RESERVED")));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "99.00")));
        req.setUsePoints(true);
        req.setPointsToUse(200);
        req.setClientRequestId("submit-ord-2");

        service.createOrder(1L, req);

        verify(orderMapper).insert(argThat((Order order) ->
                order != null
                        && order.getOriginalAmount().compareTo(new BigDecimal("198.00")) == 0
                        && order.getTotalAmount().compareTo(new BigDecimal("196.00")) == 0
                        && Integer.valueOf(200).equals(order.getPointsUsed())
                        && order.getPointsDeductionAmount().compareTo(new BigDecimal("2.00")) == 0
                        && Integer.valueOf(100).equals(order.getPointsDeductionRatio())));
    }

    @Test
    void createOrderShouldRejectPointsThatDoNotMatchConfiguredRatio() {
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 1, "99.00")));
        req.setUsePoints(true);
        req.setPointsToUse(120);

        assertThatThrownBy(() -> service.createOrder(1L, req))
                .isInstanceOf(BusinessException.class);

        verify(memberClient, never()).reservePoints(any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void createOrderShouldRejectPointsDeductionAboveOrderAmount() {
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "1.00"))));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 1, "1.00")));
        req.setUsePoints(true);
        req.setPointsToUse(200);

        assertThatThrownBy(() -> service.createOrder(1L, req))
                .isInstanceOf(BusinessException.class);

        verify(memberClient, never()).reservePoints(any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void createOrderShouldAbortWhenReservationFails() {
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));
        when(memberClient.reservePoints(any())).thenReturn(Result.fail(10020010, "points insufficient"));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "99.00")));
        req.setUsePoints(true);
        req.setPointsToUse(200);
        req.setClientRequestId("submit-ord-1");

        assertThatThrownBy(() -> service.createOrder(1L, req))
                .isInstanceOf(BusinessException.class);
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void createOrderShouldReleaseReservedPointsWhenItemPersistenceFails() {
        when(orderMapper.insert(any(Order.class))).thenReturn(1);
        when(itemMapper.insert(any(OrderItem.class))).thenThrow(new RuntimeException("item insert failed"));
        when(productSpuClient.batchQuerySkus(anyList()))
                .thenReturn(Result.ok(List.of(buildSku(1L, 1L, "Product", "img-1", "99.00"))));
        when(memberClient.reservePoints(any())).thenReturn(Result.ok(buildReserveResponse("PR1", 200, "RESERVED")));

        CreateOrderRequest req = buildRequest(List.of(buildRequestItem(1L, 2, "99.00")));
        req.setUsePoints(true);
        req.setPointsToUse(200);
        req.setClientRequestId("submit-ord-1");

        assertThatThrownBy(() -> service.createOrder(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("item insert failed");

        verify(memberClient).releasePoints(argThat((String reservationNo) -> "PR1".equals(reservationNo)),
                argThat((MemberPointsReservationReleaseRequest release) ->
                        release != null
                                && "ORDER_CREATE_FAILED".equals(release.getReason())));
    }

    private CreateOrderRequest buildRequest(List<CreateOrderRequest.OrderItemRequest> items) {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setReceiverName("receiver");
        req.setReceiverPhone("13800001111");
        req.setReceiverAddress("address");
        req.setItems(items);
        return req;
    }

    private CreateOrderRequest.OrderItemRequest buildRequestItem(Long skuId, int quantity, String price) {
        CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
        item.setSkuId(skuId);
        item.setSpuId(1L);
        item.setName("request-item");
        item.setPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    private SkuBatchVO buildSku(Long skuId, Long spuId, String name, String image, String price) {
        SkuBatchVO sku = new SkuBatchVO();
        sku.setSkuId(skuId);
        sku.setSpuId(spuId);
        sku.setSkuName(name);
        sku.setImage(image);
        sku.setPrice(new BigDecimal(price));
        return sku;
    }

    private MemberPointsReserveResponse buildReserveResponse(String reservationNo, Integer reservedPoints, String status) {
        MemberPointsReserveResponse response = new MemberPointsReserveResponse();
        response.setReservationNo(reservationNo);
        response.setReservedPoints(reservedPoints);
        response.setStatus(status);
        return response;
    }
}
