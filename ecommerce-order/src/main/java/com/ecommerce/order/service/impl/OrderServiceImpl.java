package com.ecommerce.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxQuery;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.ErrorCode;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.transaction.DistributedTransactionContext;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.LogisticsClient;
import com.ecommerce.order.client.MemberClient;
import com.ecommerce.order.client.ProductSpuClient;
import com.ecommerce.order.client.dto.FulfillmentSummaryVO;
import com.ecommerce.order.client.dto.MemberPointsReservationReleaseRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveResponse;
import com.ecommerce.order.common.OrderErrorCode;
import com.ecommerce.order.dto.request.CreateOrderRequest;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.dto.response.OrderVO;
import com.ecommerce.order.dto.response.OutboxMessageVO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.transaction.OrderTransactionCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final CartClient cartClient;
    private final ProductSpuClient productSpuClient;
    private final MemberClient memberClient;
    private final LogisticsClient logisticsClient;
    private final OutboxService outboxService;

    @Value("${member.points.deduction.enabled:true}")
    private boolean pointsDeductionEnabled;

    @Value("${member.points.deduction.points-per-yuan:100}")
    private int pointsPerYuan;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper itemMapper, ProductSpuClient productSpuClient,
                            CartClient cartClient, MemberClient memberClient, LogisticsClient logisticsClient,
                            OutboxService outboxService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.cartClient = cartClient;
        this.productSpuClient = productSpuClient;
        this.memberClient = memberClient;
        this.logisticsClient = logisticsClient;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }

        Map<Long, SkuBatchVO> skuMap = loadSkuSnapshots(request);

        BigDecimal originalTotal = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            SkuBatchVO sku = skuMap.get(item.getSkuId());
            BigDecimal price = sku.getPrice();
            originalTotal = originalTotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        PointsDeductionSnapshot deduction = calculatePointsDeduction(request, originalTotal);

        Order order = new Order();
        order.setId(SnowflakeUtils.nextId());
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOriginalAmount(originalTotal);
        order.setTotalAmount(originalTotal.subtract(deduction.deductionAmount()));
        order.setPointsUsed(deduction.pointsUsed());
        order.setPointsDeductionAmount(deduction.deductionAmount());
        order.setPointsDeductionRatio(deduction.pointsDeductionRatio());
        order.setStatus(OrderStatus.PENDING);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());

        String reservationNo = reservePointsIfNeeded(userId, request, order.getOrderNo(), deduction);
        order.setPointsReservationNo(reservationNo);

        try {
            orderMapper.insert(order);

            List<OrderItemMessage> lockItems = new ArrayList<>();
            for (CreateOrderRequest.OrderItemRequest i : request.getItems()) {
                SkuBatchVO sku = skuMap.get(i.getSkuId());
                OrderItem item = new OrderItem();
                item.setId(SnowflakeUtils.nextId());
                item.setOrderId(order.getId());
                item.setOrderNo(order.getOrderNo());
                item.setSkuId(i.getSkuId());
                item.setSpuId(sku.getSpuId());
                item.setName(sku.getSkuName());
                item.setImage(sku.getImage());
                BigDecimal price = sku.getPrice();
                item.setPrice(price);
                item.setQuantity(i.getQuantity());
                item.setTotalPrice(price.multiply(BigDecimal.valueOf(i.getQuantity())));
                itemMapper.insert(item);

                lockItems.add(new OrderItemMessage(i.getSkuId(), i.getQuantity()));
            }

            DistributedTransactionContext transaction = OrderTransactionCoordinator.startOrderCreated(order.getOrderNo());
            outboxService.enqueue("order", order.getOrderNo(), "order-created",
                    OrderTransactionCoordinator.buildInventoryMessage(transaction, lockItems));

            try {
                cartClient.getCart(userId);
            } catch (Exception e) {
                log.warn("cart refresh failed for userId={}", userId, e);
            }

            return toVO(order, Collections.emptyMap(), Collections.emptyMap());
        } catch (RuntimeException ex) {
            releaseReservedPointsQuietly(order.getPointsReservationNo(), order.getOrderNo(), "ORDER_CREATE_FAILED");
            throw ex;
        }
    }

    @Override
    public OrderVO getOrderByOrderNo(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo)
                        .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        return getOrder(userId, order.getId());
    }

    @Override
    public OrderVO getOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(Collections.singletonList(order));
        return toVO(order, itemsMap, loadFulfillmentSummaryMap(Collections.singletonList(order)));
    }

    @Override
    public Page<OrderVO> listByUser(Long userId, int page, int size) {
        Page<Order> pageReq = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageReq,
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        Map<Long, FulfillmentSummaryVO> fulfillmentMap = loadFulfillmentSummaryMap(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> toVO(o, itemsMap, fulfillmentMap)).collect(Collectors.toList());
        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<OrderSummaryVO> listSummariesByUser(Long userId, int limit) {
        int size = normalizeSummaryLimit(limit);
        Page<Order> result = orderMapper.selectPage(new Page<>(1, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        if (result.getRecords().isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        return result.getRecords().stream()
                .map(order -> toSummaryVO(order, itemsMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_PENDING);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<OrderItemMessage> releaseItems = items.stream()
                .map(i -> new OrderItemMessage(i.getSkuId(), i.getQuantity()))
                .collect(Collectors.toList());
        outboxService.enqueue("order", order.getOrderNo(), "order-cancelled",
                new OrderInventoryMessage(order.getOrderNo(), releaseItems));
        releaseReservedPointsQuietly(order.getPointsReservationNo(), order.getOrderNo(), "USER_CANCELLED");
    }

    private void updateStatusByOrderNo(String orderNo, Integer status) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        validateStatusTransition(order.getStatus(), status);
        if (Objects.equals(order.getStatus(), status)) {
            return;
        }
        order.setStatus(status);
        orderMapper.updateById(order);
    }

    @Override
    public Page<OrderVO> listAll(int page, int size, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        Page<Order> pageReq = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageReq, wrapper);
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        Map<Long, FulfillmentSummaryVO> fulfillmentMap = loadFulfillmentSummaryMap(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> toVO(o, itemsMap, fulfillmentMap)).collect(Collectors.toList());
        return new Page<OrderVO>(result.getCurrent(), result.getSize(), result.getTotal()).setRecords(vos);
    }

    @Override
    public Page<OrderVO> listByMerchant(Long merchantId, int page, int size, Integer status) {
        List<Long> merchantSpuIds = loadMerchantSpuIds(merchantId);
        if (merchantSpuIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        List<Long> orderIds = loadMerchantOrderIds(merchantSpuIds);
        if (orderIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> pageReq = new Page<>(page, size);
        Page<Order> result = orderMapper.selectPage(pageReq, wrapper);
        Map<Long, List<OrderItem>> itemsMap = loadItemsForOrders(result.getRecords());
        Map<Long, FulfillmentSummaryVO> fulfillmentMap = loadFulfillmentSummaryMap(result.getRecords());
        List<OrderVO> vos = result.getRecords().stream().map(o -> {
            ensureMerchantOwnsOrder(o, "merchant", merchantId);
            return toVO(o, itemsMap, fulfillmentMap);
        }).collect(Collectors.toList());
        return new Page<OrderVO>(result.getCurrent(), result.getSize(), result.getTotal()).setRecords(vos);
    }

    @Override
    public List<String> listOrderNosByMerchant(Long merchantId) {
        List<Long> merchantSpuIds = loadMerchantSpuIds(merchantId);
        if (merchantSpuIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderIds = loadMerchantOrderIds(merchantSpuIds);
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return orderMapper.selectList(new LambdaQueryWrapper<Order>().in(Order::getId, orderIds)).stream()
                .map(Order::getOrderNo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> listForRecon(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .between(start != null && end != null, Order::getCreatedAt, start, end)
                        .orderByAsc(Order::getCreatedAt));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status, String userType, Long merchantId) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        ensureMerchantOwnsOrder(order, userType, merchantId);
        Integer currentStatus = order.getStatus();
        validateStatusTransition(currentStatus, status);
        if (Objects.equals(currentStatus, status)) {
            return;
        }
        order.setStatus(status);
        orderMapper.updateById(order);
        releaseReservedPointsIfPendingCancellation(order, currentStatus, status, "STATUS_UPDATE_CANCELLED");
    }

    @Override
    @Transactional
    public void applyInventoryCompensation(OrderPaidMessage message) {
        if (message == null || message.getOrderNo() == null || message.getStatus() == null) {
            return;
        }
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, message.getOrderNo()));
        if (order == null) {
            log.warn("Ignore inventory compensation for missing order: {}", message.getOrderNo());
            return;
        }
        if (!OrderTransactionCoordinator.shouldApplyInventoryCompensation(order.getStatus(), message.getStatus())) {
            log.warn("Ignore inventory compensation: orderNo={}, currentStatus={}, incomingStatus={}",
                    message.getOrderNo(), order.getStatus(), message.getStatus());
            return;
        }
        Integer currentStatus = order.getStatus();
        order.setStatus(message.getStatus());
        orderMapper.updateById(order);
        releaseReservedPointsIfPendingCancellation(order, currentStatus, message.getStatus(), "INVENTORY_COMPENSATION");
    }

    @Override
    public Page<OutboxMessageVO> listOutbox(OutboxQuery query, int page, int size) {
        Page<OutboxMessage> result = outboxService.queryMessages(query, page, size);
        Page<OutboxMessageVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toOutboxVO).toList());
        return voPage;
    }

    @Override
    public OutboxSummary getOutboxSummary(OutboxQuery query) {
        return outboxService.summarize(query);
    }

    @Override
    public int retryOutboxMessage(Long messageId) {
        return outboxService.retryMessage(messageId) ? 1 : 0;
    }

    @Override
    public int retryOutboxBatch(OutboxQuery query, int limit) {
        return outboxService.retryBatch(query, limit);
    }

    private void ensureMerchantOwnsOrder(Order order, String userType, Long merchantId) {
        if (!"merchant".equals(userType)) {
            return;
        }
        if (merchantId == null) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        List<Long> merchantSpuIds = loadMerchantSpuIds(merchantId);
        if (merchantSpuIds.isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        final List<Long> ownedSpuIds = merchantSpuIds;
        boolean owned = !items.isEmpty() && items.stream().allMatch(item -> ownedSpuIds.contains(item.getSpuId()));
        if (!owned) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
    }

    private OrderVO toVO(Order order, Map<Long, List<OrderItem>> itemsMap, Map<Long, FulfillmentSummaryVO> fulfillmentMap) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setPointsUsed(order.getPointsUsed());
        vo.setPointsDeductionAmount(order.getPointsDeductionAmount());
        vo.setPointsDeductionRatio(order.getPointsDeductionRatio());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        FulfillmentSummaryVO fulfillment = fulfillmentMap.get(order.getId());
        if (fulfillment != null) {
            vo.setFulfillmentStatus(fulfillment.getFulfillmentStatus());
            vo.setFulfillmentStatusText(fulfillment.getFulfillmentStatusText());
        }
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCreatedAt(order.getCreatedAt());

        List<OrderItem> items = itemsMap.getOrDefault(order.getId(), Collections.emptyList());
        List<OrderVO.OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderVO.OrderItemVO iv = new OrderVO.OrderItemVO();
            iv.setId(item.getId());
            iv.setSkuId(item.getSkuId());
            iv.setSpuId(item.getSpuId());
            iv.setName(item.getName());
            iv.setImage(item.getImage());
            iv.setPrice(item.getPrice());
            iv.setQuantity(item.getQuantity());
            iv.setTotalPrice(item.getTotalPrice());
            itemVOs.add(iv);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private OrderSummaryVO toSummaryVO(Order order, List<OrderItem> items) {
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setOrderNo(order.getOrderNo());
        summary.setTotalAmount(order.getTotalAmount());
        summary.setStatus(order.getStatus());
        summary.setStatusText(statusText(order.getStatus()));
        summary.setCreatedAt(order.getCreatedAt());
        if (!items.isEmpty()) {
            OrderItem firstItem = items.getFirst();
            int itemCount = items.stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            String firstItemName = firstItem.getName();
            summary.setFirstItemName(firstItemName);
            summary.setItemCount(itemCount);
            summary.setItemSummary(buildItemSummary(firstItemName, itemCount));
        }
        return summary;
    }

    private List<Long> loadMerchantSpuIds(Long merchantId) {
        try {
            Result<List<Long>> response = productSpuClient.getSpuIdsByMerchant(merchantId);
            return response.getData() != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> loadMerchantOrderIds(List<Long> spuIds) {
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getSpuId, spuIds));
        return items.stream()
                .map(OrderItem::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<Long, List<OrderItem>> loadItemsForOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds));
        return items.stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private Map<Long, FulfillmentSummaryVO> loadFulfillmentSummaryMap(List<Order> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).filter(Objects::nonNull).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<List<FulfillmentSummaryVO>> response = logisticsClient.getFulfillmentSummary(orderIds);
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                return Collections.emptyMap();
            }
            return response.getData().stream()
                    .filter(Objects::nonNull)
                    .filter(summary -> summary.getOrderId() != null)
                    .collect(Collectors.toMap(FulfillmentSummaryVO::getOrderId, summary -> summary, (left, right) -> left));
        } catch (Exception e) {
            log.warn("load fulfillment summary failed: orderIds={}", orderIds, e);
            return Collections.emptyMap();
        }
    }

    private Map<Long, SkuBatchVO> loadSkuSnapshots(CreateOrderRequest request) {
        List<Long> skuIds = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getSkuId)
                .toList();
        if (skuIds.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }

        List<SkuBatchVO> skuSnapshots;
        try {
            Result<List<SkuBatchVO>> response = productSpuClient.batchQuerySkus(skuIds);
            skuSnapshots = response.getData();
        } catch (Exception e) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }
        if (skuSnapshots == null || skuSnapshots.size() != skuIds.size()) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }

        Map<Long, SkuBatchVO> skuMap = new HashMap<>();
        for (SkuBatchVO snapshot : skuSnapshots) {
            if (snapshot == null || snapshot.getSkuId() == null || snapshot.getSpuId() == null || snapshot.getPrice() == null) {
                throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
            }
            skuMap.put(snapshot.getSkuId(), snapshot);
        }
        if (!skuMap.keySet().containsAll(skuIds)) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEMS_EMPTY);
        }
        return skuMap;
    }

    private void validateStatusTransition(Integer currentStatus, Integer targetStatus) {
        if (Objects.equals(currentStatus, targetStatus)) {
            return;
        }
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        if (currentStatus == OrderStatus.PENDING && targetStatus == OrderStatus.CANCELLED) {
            return;
        }
        throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
    }

    private int normalizeSummaryLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private String reservePointsIfNeeded(Long userId, CreateOrderRequest request, String orderNo,
                                         PointsDeductionSnapshot deduction) {
        if (!Boolean.TRUE.equals(request.getUsePoints())) {
            return null;
        }
        if (deduction.pointsUsed() == null || deduction.pointsUsed() <= 0) {
            return null;
        }

        Result<MemberPointsReserveResponse> response = memberClient.reservePoints(new MemberPointsReserveRequest(
                userId,
                orderNo,
                "ORDER_DEDUCTION",
                deduction.pointsUsed(),
                buildReservationIdempotencyKey(orderNo, request.getClientRequestId())));
        if (response == null || response.getCode() != 200 || response.getData() == null
                || response.getData().getReservationNo() == null || response.getData().getReservationNo().isBlank()) {
            throw remoteBusinessException(response, OrderErrorCode.ORDER_FORBIDDEN.getCode(), "points reservation failed");
        }
        return response.getData().getReservationNo();
    }

    private PointsDeductionSnapshot calculatePointsDeduction(CreateOrderRequest request, BigDecimal originalTotal) {
        if (!Boolean.TRUE.equals(request.getUsePoints()) || request.getPointsToUse() == null || request.getPointsToUse() <= 0) {
            return new PointsDeductionSnapshot(0, BigDecimal.ZERO.setScale(2, RoundingMode.DOWN), pointsPerYuan);
        }
        if (!pointsDeductionEnabled || pointsPerYuan <= 0) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        if (request.getPointsToUse() % pointsPerYuan != 0) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        BigDecimal deductionAmount = BigDecimal.valueOf(request.getPointsToUse())
                .divide(BigDecimal.valueOf(pointsPerYuan), 2, RoundingMode.DOWN);
        if (deductionAmount.compareTo(BigDecimal.ZERO) <= 0 || deductionAmount.compareTo(originalTotal) > 0) {
            throw new BusinessException(OrderErrorCode.ORDER_FORBIDDEN);
        }
        return new PointsDeductionSnapshot(request.getPointsToUse(), deductionAmount, pointsPerYuan);
    }

    private String buildReservationIdempotencyKey(String orderNo, String clientRequestId) {
        if (clientRequestId != null && !clientRequestId.isBlank()) {
            return "reserve:" + orderNo + ":" + clientRequestId;
        }
        return "reserve:" + orderNo;
    }

    private void releaseReservedPointsQuietly(String reservationNo, String orderNo, String reason) {
        if (reservationNo == null || reservationNo.isBlank()) {
            return;
        }
        try {
            memberClient.releasePoints(reservationNo, new MemberPointsReservationReleaseRequest(
                    reservationNo,
                    reason,
                    "release:" + orderNo + ":" + reason));
        } catch (Exception ex) {
            log.warn("release reserved points failed: reservationNo={}, orderNo={}, reason={}",
                    reservationNo, orderNo, reason, ex);
        }
    }

    private void releaseReservedPointsIfPendingCancellation(Order order, Integer currentStatus, Integer targetStatus,
                                                            String reason) {
        if (order == null) {
            return;
        }
        if (!Objects.equals(currentStatus, OrderStatus.PENDING)
                || !Objects.equals(targetStatus, OrderStatus.CANCELLED)) {
            return;
        }
        releaseReservedPointsQuietly(order.getPointsReservationNo(), order.getOrderNo(), reason);
    }

    private BusinessException remoteBusinessException(Result<?> response, int defaultCode, String defaultMessage) {
        int code = response != null ? response.getCode() : defaultCode;
        String message = response != null && response.getMessage() != null ? response.getMessage() : defaultMessage;
        return new BusinessException(new ErrorCode() {
            @Override
            public int getCode() {
                return code;
            }

            @Override
            public String getMessage() {
                return message;
            }
        });
    }

    private String buildItemSummary(String firstItemName, int itemCount) {
        if (firstItemName == null || firstItemName.isBlank()) {
            return null;
        }
        if (itemCount <= 1) {
            return firstItemName;
        }
        return firstItemName + " and " + itemCount + " items";
    }

    private String statusText(Integer status) {
        return OrderStatus.text(status);
    }

    private OutboxMessageVO toOutboxVO(OutboxMessage message) {
        OutboxMessageVO vo = new OutboxMessageVO();
        vo.setId(message.getId());
        vo.setAggregateId(message.getAggregateId());
        vo.setTopic(message.getTopic());
        vo.setStatus(message.getStatus());
        vo.setRetryCount(message.getRetryCount());
        vo.setNextRetryAt(message.getNextRetryAt());
        vo.setLastError(message.getLastError());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                String.format("%06d", Math.abs((int) (System.nanoTime() % 1_000_000)));
    }

    private record PointsDeductionSnapshot(Integer pointsUsed, BigDecimal deductionAmount, Integer pointsDeductionRatio) {
    }
}
