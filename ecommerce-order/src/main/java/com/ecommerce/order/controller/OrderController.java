package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final ProductSpuClient productSpuClient;

    public OrderController(OrderService orderService, OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                           ProductSpuClient productSpuClient) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productSpuClient = productSpuClient;
    }

    @PostMapping("/orders")
    public Result<OrderVO> create(@RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.createOrder(userId, request));
    }

    @GetMapping("/orders")
    public Result<Page<OrderVO>> listByUser(@RequestHeader("X-User-Id") Long userId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listByUser(userId, page, size));
    }

    @GetMapping("/orders/summaries")
    public Result<List<OrderSummaryVO>> listSummariesByUser(@RequestHeader("X-User-Id") Long userId,
                                                            @RequestParam(defaultValue = "5") int limit) {
        return Result.ok(orderService.listSummariesByUser(userId, limit));
    }

    @GetMapping("/internal/orders/no/{orderNo}")
    public Result<OrderInternalVO> internalGetByOrderNo(@PathVariable String orderNo, @RequestParam("userId") Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId));
        if (order == null) {
            return Result.fail(404, "order not found");
        }
        return Result.ok(toInternalVO(order, false));
    }

    @GetMapping("/internal/orders/{id}/shipping-snapshot")
    public Result<OrderInternalVO> internalGetShippingSnapshot(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(404, "order not found");
        }
        return Result.ok(toInternalVO(order, true));
    }

    @GetMapping("/internal/orders/no/{orderNo}/member")
    public Result<OrderMemberVO> internalGetByOrderNoForMember(@PathVariable String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return Result.fail(404, "order not found");
        }
        OrderMemberVO vo = new OrderMemberVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setPointsUsed(order.getPointsUsed());
        vo.setPointsDeductionAmount(order.getPointsDeductionAmount());
        vo.setPointsDeductionRatio(order.getPointsDeductionRatio());
        vo.setStatus(order.getStatus());
        return Result.ok(vo);
    }

    @GetMapping("/internal/orders/merchant/order-nos")
    public Result<List<String>> internalListOrderNosByMerchant(@RequestParam("merchantId") Long merchantId) {
        return Result.ok(orderService.listOrderNosByMerchant(merchantId));
    }

    @GetMapping("/orders/no/{orderNo}")
    public Result<OrderVO> detailByOrderNo(@RequestHeader("X-User-Id") Long userId, @PathVariable String orderNo) {
        return Result.ok(orderService.getOrderByOrderNo(userId, orderNo));
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return Result.ok(orderService.getOrder(userId, id));
    }

    @PutMapping("/orders/{id}/cancel")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        orderService.cancelOrder(userId, id);
        return Result.ok();
    }

    private OrderInternalVO toInternalVO(Order order, boolean includeItems) {
        OrderInternalVO vo = new OrderInternalVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setPointsUsed(order.getPointsUsed());
        vo.setPointsDeductionAmount(order.getPointsDeductionAmount());
        vo.setPointsDeductionRatio(order.getPointsDeductionRatio());
        vo.setStatus(order.getStatus());
        if (includeItems) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            Map<Long, Long> merchantMap = loadMerchantMap(items);
            vo.setMerchantId(resolveOrderMerchantId(merchantMap));
            vo.setItems(items.stream().map(item -> {
                OrderInternalVO.OrderItemSnapshot snapshot = new OrderInternalVO.OrderItemSnapshot();
                snapshot.setOrderItemId(item.getId());
                snapshot.setSkuId(item.getSkuId());
                snapshot.setMerchantId(merchantMap.get(item.getSkuId()));
                snapshot.setQuantity(item.getQuantity());
                return snapshot;
            }).toList());
        }
        return vo;
    }

    private Map<Long, Long> loadMerchantMap(List<OrderItem> items) {
        List<Long> skuIds = items.stream()
                .map(OrderItem::getSkuId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        Result<List<com.ecommerce.common.dto.SkuBatchVO>> response = productSpuClient.batchQuerySkus(skuIds);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Map.of();
        }
        return response.getData().stream()
                .filter(sku -> sku != null && sku.getSkuId() != null)
                .collect(Collectors.toMap(
                        com.ecommerce.common.dto.SkuBatchVO::getSkuId,
                        com.ecommerce.common.dto.SkuBatchVO::getMerchantId,
                        (left, right) -> left));
    }

    private Long resolveOrderMerchantId(Map<Long, Long> merchantMap) {
        List<Long> merchantIds = merchantMap.values().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return merchantIds.size() == 1 ? merchantIds.getFirst() : null;
    }
}
