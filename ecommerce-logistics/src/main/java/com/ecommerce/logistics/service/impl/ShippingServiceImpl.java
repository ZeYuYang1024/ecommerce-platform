package com.ecommerce.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.OutboundType;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.common.dto.ShippingExceptionMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.logistics.client.OrderClient;
import com.ecommerce.logistics.client.WarehouseClient;
import com.ecommerce.logistics.client.dto.CreateOutboundRequest;
import com.ecommerce.logistics.client.dto.OutboundOrderVO;
import com.ecommerce.logistics.client.dto.WarehouseInfoVO;
import com.ecommerce.logistics.common.FulfillmentStatus;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.common.ShippingStatus;
import com.ecommerce.logistics.dto.request.CreateShippingRequest;
import com.ecommerce.logistics.dto.response.FulfillmentSummaryVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.TrackingVO;
import com.ecommerce.logistics.entity.LogisticsProvider;
import com.ecommerce.logistics.entity.ShippingOrder;
import com.ecommerce.logistics.entity.ShippingOrderItem;
import com.ecommerce.logistics.entity.TrackingRecord;
import com.ecommerce.logistics.mapper.LogisticsProviderMapper;
import com.ecommerce.logistics.mapper.ShippingOrderItemMapper;
import com.ecommerce.logistics.mapper.ShippingOrderMapper;
import com.ecommerce.logistics.mapper.TrackingRecordMapper;
import com.ecommerce.logistics.provider.AggregationProvider;
import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.ecommerce.logistics.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final ShippingOrderMapper shippingOrderMapper;
    private final ShippingOrderItemMapper shippingOrderItemMapper;
    private final LogisticsProviderMapper providerMapper;
    private final TrackingRecordMapper trackingRecordMapper;
    private final OutboxService outboxService;
    private final AggregationProvider aggregationProvider;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;
    private final JsonMapper jsonMapper;

    @Value("${logistics.tracking.cache-minutes:30}")
    private int trackingCacheMinutes;

    private static final DateTimeFormatter NO_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger shippingNoCounter = new AtomicInteger(
            (int) (System.currentTimeMillis() % 100000));

    @Override
    @Transactional
    public ShippingOrderVO createShipping(CreateShippingRequest request, String userType, Long merchantId) {
        // idempotency check
        ShippingOrder existing = shippingOrderMapper.selectOne(
                new LambdaQueryWrapper<ShippingOrder>()
                        .eq(ShippingOrder::getOrderId, request.getOrderId())
                        .eq(ShippingOrder::getClientRequestId, request.getClientRequestId()));
        if (existing != null) {
            return toVO(existing);
        }

        LogisticsProvider provider = providerMapper.selectById(request.getProviderId());
        if (provider == null || provider.getStatus() != 1) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_NOT_FOUND);
        }

        // duplicate trackingNo check
        if (shippingOrderMapper.exists(new LambdaQueryWrapper<ShippingOrder>()
                .eq(ShippingOrder::getProviderId, request.getProviderId())
                .eq(ShippingOrder::getTrackingNo, request.getTrackingNo()))) {
            throw new BusinessException(LogisticsErrorCode.SHIPPING_DUPLICATE);
        }

        // prevent duplicate order items from being shipped
        List<ShippingOrderItem> allShipped = shippingOrderItemMapper.selectList(
                new LambdaQueryWrapper<ShippingOrderItem>()
                        .in(ShippingOrderItem::getOrderItemId,
                                request.getItems().stream().map(CreateShippingRequest.ShippingItemRequest::getOrderItemId).toList()));
        if (!allShipped.isEmpty()) {
            throw new BusinessException(LogisticsErrorCode.QUANTITY_EXCEEDS_ORDER);
        }

        OrderInternalVO orderSnapshot = loadOrderSnapshot(request.getOrderId());
        validateOrderForShipping(orderSnapshot);

        String shippingNo = "SH" + LocalDateTime.now().format(NO_SUFFIX) + String.format("%05d", shippingNoCounter.getAndUpdate(n -> (n + 1) % 100000));

        WarehouseInfoVO warehouse = loadWarehouse(request.getWarehouseId());
        if (warehouse != null) {
            MerchantTenantSupport.requireOwnerAccess(
                    merchantId, warehouse.getMerchantId(), LogisticsErrorCode.WAREHOUSE_FORBIDDEN);
        }
        boolean managedWarehouse = isManagedWarehouse(warehouse);
        Long resolvedMerchantId = resolveShippingMerchantId(orderSnapshot, warehouse, merchantId);
        MerchantTenantSupport.requireOwnerAccess(
                merchantId, resolvedMerchantId, LogisticsErrorCode.SHIPPING_FORBIDDEN);

        ShippingOrder order = new ShippingOrder();
        order.setShippingNo(shippingNo);
        order.setClientRequestId(request.getClientRequestId());
        order.setOrderId(request.getOrderId());
        order.setOrderNo(orderSnapshot.getOrderNo());
        order.setWarehouseId(request.getWarehouseId());
        order.setProviderId(request.getProviderId());
        order.setProviderCode(provider.getProviderCode());
        order.setTrackingNo(request.getTrackingNo());
        order.setDispatchType(0);
        order.setSourceType(request.getSourceType() != null ? request.getSourceType() : 0);
        order.setPackageWeight(request.getPackageWeight() != null ? request.getPackageWeight() : 0);
        order.setPackageSize(request.getPackageSize());
        order.setMerchantId(resolvedMerchantId);
        order.setVersion(0);

        // Managed warehouse: create outbound and wait for outbound-shipped event.
        // Light warehouse: dispatch immediately.
        if (managedWarehouse) {
            order.setShippingStatus(ShippingStatus.PENDING);
        } else {
            order.setShippingStatus(ShippingStatus.DISPATCHED);
            order.setShippedAt(LocalDateTime.now());
        }
        shippingOrderMapper.insert(order);

        for (CreateShippingRequest.ShippingItemRequest itemReq : request.getItems()) {
            ShippingOrderItem item = new ShippingOrderItem();
            item.setShippingId(order.getId());
            item.setOrderItemId(itemReq.getOrderItemId());
            item.setSkuId(itemReq.getSkuId());
            item.setQuantity(itemReq.getQuantity());
            shippingOrderItemMapper.insert(item);
        }

        if (managedWarehouse) {
            CreateOutboundRequest outboundReq = new CreateOutboundRequest();
            outboundReq.setWarehouseId(request.getWarehouseId());
            outboundReq.setOutboundType(OutboundType.SALES);
            outboundReq.setShippingId(order.getId());
            outboundReq.setMerchantId(resolvedMerchantId);
            outboundReq.setItems(request.getItems().stream().map(i -> {
                CreateOutboundRequest.OutboundItemRequest oi = new CreateOutboundRequest.OutboundItemRequest();
                oi.setSkuId(i.getSkuId());
                oi.setQuantity(i.getQuantity());
                return oi;
            }).toList());

            try {
                Result<OutboundOrderVO> result = warehouseClient.createOutbound(outboundReq);
                if (result == null || result.getCode() != 200) {
                    throw new BusinessException(LogisticsErrorCode.WAREHOUSE_OUTBOUND_FAILED);
                }
                // Managed warehouse: outbound created, stock locked — wait for outbound-shipped event
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to create outbound order for shipping {}", shippingNo, e);
                throw new BusinessException(LogisticsErrorCode.WAREHOUSE_OUTBOUND_FAILED);
            }
        } else {
            // Light warehouse mode: enqueue shipping-dispatched event immediately
            ShippingDispatchedMessage msg = new ShippingDispatchedMessage();
            msg.setShippingId(order.getId());
            msg.setOrderId(order.getOrderId());
            msg.setOrderNo(order.getOrderNo());
            msg.setTrackingNo(order.getTrackingNo());
            msg.setShippingStatus(order.getShippingStatus());
            msg.setUserId(orderSnapshot.getUserId());
            msg.setMerchantId(resolvedMerchantId);
            msg.setTransactionId("logistics-dispatch-" + shippingNo);
            msg.setIdempotencyKey("shipping-dispatched:" + shippingNo);
            msg.setOccurredAt(LocalDateTime.now());
            outboxService.enqueue("shipping", shippingNo, "shipping-dispatched", msg);
        }

        log.info("Shipping order created: shippingNo={}, orderId={}, trackingNo={}, warehouseManaged={}",
                shippingNo, request.getOrderId(), request.getTrackingNo(), managedWarehouse);
        return toVO(order);
    }

    @Override
    public IPage<ShippingOrderVO> listShipping(int page, int size, String orderNo, Integer shippingStatus, Long merchantId) {
        LambdaQueryWrapper<ShippingOrder> wrapper = new LambdaQueryWrapper<>();
        if (orderNo != null && !orderNo.isEmpty()) wrapper.eq(ShippingOrder::getOrderNo, orderNo);
        if (shippingStatus != null) wrapper.eq(ShippingOrder::getShippingStatus, shippingStatus);
        if (merchantId != null) wrapper.eq(ShippingOrder::getMerchantId, merchantId);
        wrapper.orderByDesc(ShippingOrder::getCreatedAt);

        Page<ShippingOrder> p = new Page<>(page, size);
        IPage<ShippingOrder> result = shippingOrderMapper.selectPage(p, wrapper);

        // Batch-fetch items and providers to avoid N+1 queries in toVO
        if (!result.getRecords().isEmpty()) {
            List<Long> shippingIds = result.getRecords().stream()
                    .map(ShippingOrder::getId).collect(Collectors.toList());
            List<ShippingOrderItem> allItems = shippingOrderItemMapper.selectList(
                    new LambdaQueryWrapper<ShippingOrderItem>().in(ShippingOrderItem::getShippingId, shippingIds));
            Map<Long, List<ShippingOrderItem>> itemsByShippingId = allItems.stream()
                    .collect(Collectors.groupingBy(ShippingOrderItem::getShippingId));

            Set<Long> providerIds = result.getRecords().stream()
                    .map(ShippingOrder::getProviderId).collect(Collectors.toSet());
            Map<Long, LogisticsProvider> providerMap = providerMapper.selectBatchIds(providerIds).stream()
                    .collect(Collectors.toMap(LogisticsProvider::getId, p2 -> p2));

            return result.convert(order -> toVO(order, itemsByShippingId, providerMap));
        }

        return result.convert(this::toVO);
    }

    @Override
    public ShippingOrderVO getShipping(Long id, String userType, Long merchantId) {
        ShippingOrder order = shippingOrderMapper.selectById(id);
        if (order == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        MerchantTenantSupport.requireOwnerAccess(
                merchantId, order.getMerchantId(), LogisticsErrorCode.SHIPPING_FORBIDDEN);
        return toVO(order);
    }

    @Override
    public ShippingOrderVO getShippingByOrderId(Long orderId) {
        ShippingOrder order = shippingOrderMapper.selectOne(
                new LambdaQueryWrapper<ShippingOrder>().eq(ShippingOrder::getOrderId, orderId)
                        .orderByDesc(ShippingOrder::getCreatedAt).last("LIMIT 1"));
        if (order == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        return toVO(order);
    }

    @Override
    public TrackingVO getTracking(Long shippingId, Long merchantId) {
        ShippingOrder order = shippingOrderMapper.selectById(shippingId);
        if (order == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        MerchantTenantSupport.requireOwnerAccess(
                merchantId, order.getMerchantId(), LogisticsErrorCode.SHIPPING_FORBIDDEN);

        List<TrackingRecord> localTracks = trackingRecordMapper.selectList(
                new LambdaQueryWrapper<TrackingRecord>()
                        .eq(TrackingRecord::getShippingId, shippingId)
                        .orderByDesc(TrackingRecord::getTraceTime));

        // refresh from remote if local tracks are empty or stale
        boolean needRefresh = localTracks.isEmpty();
        if (!localTracks.isEmpty()) {
            LocalDateTime latest = localTracks.get(0).getTraceTime();
            needRefresh = latest.plusMinutes(trackingCacheMinutes).isBefore(LocalDateTime.now());
        }

        if (needRefresh) {
            try {
                TrackingQueryResponse resp = aggregationProvider.queryTracking(
                        order.getTrackingNo(), resolveAggregationTrackingCode(order));
                if (resp != null && resp.isSuccess() && resp.getTraces() != null) {
                    for (TrackingQueryResponse.TraceItem t : resp.getTraces()) {
                        String traceHash = DigestUtils.md5DigestAsHex(
                                (order.getTrackingNo() + t.getTime().toString() + t.getDesc()).getBytes(StandardCharsets.UTF_8));
                        if (trackingRecordMapper.exists(new LambdaQueryWrapper<TrackingRecord>()
                                .eq(TrackingRecord::getShippingId, shippingId)
                                .eq(TrackingRecord::getTraceHash, traceHash))) {
                            continue;
                        }
                        TrackingRecord record = new TrackingRecord();
                        record.setShippingId(shippingId);
                        record.setProviderCode(order.getProviderCode());
                        record.setTrackingNo(order.getTrackingNo());
                        record.setTraceHash(traceHash);
                        record.setTraceTime(t.getTime());
                        record.setTraceDesc(t.getDesc());
                        record.setTraceStatus(t.getStatus());
                        record.setLocation(t.getLocation());
                        trackingRecordMapper.insert(record);
                    }
                    // reload local tracks after refresh
                    localTracks = trackingRecordMapper.selectList(
                            new LambdaQueryWrapper<TrackingRecord>()
                                    .eq(TrackingRecord::getShippingId, shippingId)
                                    .orderByDesc(TrackingRecord::getTraceTime));
                }
            } catch (Exception e) {
                log.warn("Failed to refresh tracking from provider: shippingId={}", shippingId, e);
            }
        }

        return buildTrackingVO(order, localTracks);
    }

    @Override
    public TrackingVO getTrackingByTrackingNo(String trackingNo, String providerCode, Long merchantId) {
        ShippingOrder order = shippingOrderMapper.selectOne(
                new LambdaQueryWrapper<ShippingOrder>()
                        .eq(ShippingOrder::getTrackingNo, trackingNo)
                        .eq(ShippingOrder::getProviderCode, providerCode)
                        .orderByDesc(ShippingOrder::getCreatedAt).last("LIMIT 1"));
        if (order == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        MerchantTenantSupport.requireOwnerAccess(
                merchantId, order.getMerchantId(), LogisticsErrorCode.SHIPPING_FORBIDDEN);
        return getTracking(order.getId(), merchantId);
    }

    @Override
    public List<FulfillmentSummaryVO> getFulfillmentSummary(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyList();

        List<ShippingOrder> orders = shippingOrderMapper.selectList(
                new LambdaQueryWrapper<ShippingOrder>().in(ShippingOrder::getOrderId, orderIds));

        Map<Long, List<ShippingOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(ShippingOrder::getOrderId));

        return orderIds.stream().map(orderId -> {
            FulfillmentSummaryVO vo = new FulfillmentSummaryVO();
            vo.setOrderId(orderId);
            List<ShippingOrder> shippings = grouped.getOrDefault(orderId, Collections.emptyList());
            if (shippings.isEmpty()) {
                vo.setFulfillmentStatus(FulfillmentStatus.WAITING_SHIP);
            } else {
                boolean hasException = shippings.stream().anyMatch(s -> s.getShippingStatus() == ShippingStatus.EXCEPTION);
                boolean allSigned = shippings.stream().allMatch(s -> s.getShippingStatus() == ShippingStatus.SIGNED);
                boolean allDispatched = shippings.stream().allMatch(s -> s.getShippingStatus() >= ShippingStatus.DISPATCHED);

                if (hasException) vo.setFulfillmentStatus(FulfillmentStatus.EXCEPTION);
                else if (allSigned) vo.setFulfillmentStatus(FulfillmentStatus.DELIVERED);
                else if (allDispatched) vo.setFulfillmentStatus(FulfillmentStatus.DISPATCHED);
                else vo.setFulfillmentStatus(FulfillmentStatus.PARTIALLY_DISPATCHED);
            }
            vo.setFulfillmentStatusText(FulfillmentStatus.text(vo.getFulfillmentStatus()));
            vo.setShippingCount(shippings.size());
            vo.setDeliveredCount((int) shippings.stream().filter(s -> s.getShippingStatus() == ShippingStatus.SIGNED).count());

            ShippingOrder latestShipping = shippings.stream()
                    .filter(s -> s.getLastTraceDesc() != null)
                    .max(Comparator.comparing(ShippingOrder::getLastTraceTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            if (latestShipping != null) {
                vo.setLatestTraceDesc(latestShipping.getLastTraceDesc());
                vo.setLatestTraceTime(latestShipping.getLastTraceTime());
            }
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public void processCallback(String aggregationProviderCode, String rawBody, String signature) {
        log.info("Logistics callback received: aggregationProvider={}, body={}, signature={}",
                aggregationProviderCode, rawBody, signature);
        if (!aggregationProvider.verifyCallbackSignature(aggregationProviderCode, rawBody, signature)) {
            throw new BusinessException(LogisticsErrorCode.CALLBACK_SIGNATURE_INVALID);
        }
        CallbackPayload payload = parseCallbackPayload(rawBody);
        if (payload == null || payload.getTrackingNo() == null || payload.getStatus() == null) {
            return;
        }
        String carrierCode = resolveCarrierCode(payload, aggregationProviderCode);
        if (carrierCode == null) {
            log.warn("Ignore callback without carrier code: aggregationProvider={}, trackingNo={}",
                    aggregationProviderCode, payload.getTrackingNo());
            return;
        }

        ShippingOrder order = shippingOrderMapper.selectOne(
                new LambdaQueryWrapper<ShippingOrder>()
                        .eq(ShippingOrder::getTrackingNo, payload.getTrackingNo())
                        .eq(ShippingOrder::getProviderCode, carrierCode)
                        .orderByDesc(ShippingOrder::getCreatedAt)
                        .last("LIMIT 1"));
        if (order == null) {
            log.warn("Ignore callback for unknown shipping: aggregationProvider={}, carrierCode={}, trackingNo={}",
                    aggregationProviderCode, carrierCode, payload.getTrackingNo());
            return;
        }

        persistTrackingRecord(order, carrierCode, rawBody, payload);
        updateShippingStatusFromCallback(order, payload);
    }

    @Override
    public String generateWaybill(Long shippingId, Long merchantId) {
        ShippingOrder order = shippingOrderMapper.selectById(shippingId);
        if (order == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        MerchantTenantSupport.requireOwnerAccess(
                merchantId, order.getMerchantId(), LogisticsErrorCode.SHIPPING_FORBIDDEN);

        // Call aggregation provider to get waybill
        // Phase 3 stub: generate a mock waybill URL
        String waybillUrl = "https://waybill.example.com/" + order.getTrackingNo() + ".pdf";
        order.setWaybillUrl(waybillUrl);
        shippingOrderMapper.updateById(order);
        log.info("Waybill generated: shippingId={}, url={}", shippingId, waybillUrl);
        return waybillUrl;
    }

    @Override
    @Transactional
    public List<ShippingOrderVO> batchShip(com.ecommerce.logistics.dto.request.BatchShipRequest request, String userType, Long merchantId) {
        List<ShippingOrderVO> results = new java.util.ArrayList<>();
        for (com.ecommerce.logistics.dto.request.BatchShipRequest.BatchShipItem item : request.getItems()) {
            OrderInternalVO orderSnapshot;
            try {
                orderSnapshot = loadOrderSnapshot(item.getOrderId());
                validateOrderForShipping(orderSnapshot);
            } catch (BusinessException e) {
                log.warn("Batch ship failed for orderId={}: {}", item.getOrderId(), e.getMessage());
                ShippingOrderVO errVo = new ShippingOrderVO();
                errVo.setOrderId(item.getOrderId());
                results.add(errVo);
                continue;
            }

            CreateShippingRequest shipReq = new CreateShippingRequest();
            shipReq.setOrderId(item.getOrderId());
            shipReq.setProviderId(item.getProviderId());
            shipReq.setTrackingNo(item.getTrackingNo());
            shipReq.setPackageWeight(item.getPackageWeight());
            shipReq.setClientRequestId("batch-" + item.getOrderId() + "-" + System.currentTimeMillis());
            shipReq.setItems(orderSnapshot.getItems() == null ? Collections.emptyList() : orderSnapshot.getItems().stream().map(orderItem -> {
                CreateShippingRequest.ShippingItemRequest shippingItem = new CreateShippingRequest.ShippingItemRequest();
                shippingItem.setOrderItemId(orderItem.getOrderItemId());
                shippingItem.setSkuId(orderItem.getSkuId());
                shippingItem.setQuantity(orderItem.getQuantity());
                return shippingItem;
            }).toList());

            try {
                ShippingOrderVO vo = createShipping(shipReq, userType, merchantId);
                results.add(vo);
            } catch (BusinessException e) {
                log.warn("Batch ship failed for orderId={}: {}", item.getOrderId(), e.getMessage());
                ShippingOrderVO errVo = new ShippingOrderVO();
                errVo.setOrderId(item.getOrderId());
                results.add(errVo);
            }
        }
        return results;
    }

    private ShippingOrderVO toVO(ShippingOrder order) {
        ShippingOrderVO vo = new ShippingOrderVO();
        vo.setId(order.getId());
        vo.setShippingNo(order.getShippingNo());
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setProviderId(order.getProviderId());
        vo.setProviderCode(order.getProviderCode());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setShippingStatus(order.getShippingStatus());
        vo.setShippingStatusText(ShippingStatus.text(order.getShippingStatus()));
        vo.setDispatchType(order.getDispatchType());
        vo.setSourceType(order.getSourceType());
        vo.setShippingFee(order.getShippingFee());
        vo.setPackageWeight(order.getPackageWeight());
        vo.setPackageSize(order.getPackageSize());
        vo.setWaybillUrl(order.getWaybillUrl());
        vo.setLastTraceTime(order.getLastTraceTime());
        vo.setLastTraceDesc(order.getLastTraceDesc());
        vo.setShippedAt(order.getShippedAt());
        vo.setSignedAt(order.getSignedAt());
        vo.setCreatedAt(order.getCreatedAt());

        List<ShippingOrderItem> items = shippingOrderItemMapper.selectList(
                new LambdaQueryWrapper<ShippingOrderItem>().eq(ShippingOrderItem::getShippingId, order.getId()));
        vo.setItems(items.stream().map(i -> {
            ShippingOrderVO.ShippingItemVO iv = new ShippingOrderVO.ShippingItemVO();
            iv.setId(i.getId());
            iv.setOrderItemId(i.getOrderItemId());
            iv.setSkuId(i.getSkuId());
            iv.setQuantity(i.getQuantity());
            return iv;
        }).toList());

        LogisticsProvider provider = providerMapper.selectById(order.getProviderId());
        if (provider != null) vo.setProviderName(provider.getProviderName());

        return vo;
    }

    private ShippingOrderVO toVO(ShippingOrder order, Map<Long, List<ShippingOrderItem>> itemsByShippingId, Map<Long, LogisticsProvider> providerMap) {
        ShippingOrderVO vo = new ShippingOrderVO();
        vo.setId(order.getId());
        vo.setShippingNo(order.getShippingNo());
        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setProviderId(order.getProviderId());
        vo.setProviderCode(order.getProviderCode());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setShippingStatus(order.getShippingStatus());
        vo.setShippingStatusText(ShippingStatus.text(order.getShippingStatus()));
        vo.setDispatchType(order.getDispatchType());
        vo.setSourceType(order.getSourceType());
        vo.setShippingFee(order.getShippingFee());
        vo.setPackageWeight(order.getPackageWeight());
        vo.setPackageSize(order.getPackageSize());
        vo.setWaybillUrl(order.getWaybillUrl());
        vo.setLastTraceTime(order.getLastTraceTime());
        vo.setLastTraceDesc(order.getLastTraceDesc());
        vo.setShippedAt(order.getShippedAt());
        vo.setSignedAt(order.getSignedAt());
        vo.setCreatedAt(order.getCreatedAt());

        List<ShippingOrderItem> items = itemsByShippingId.getOrDefault(order.getId(), Collections.emptyList());
        vo.setItems(items.stream().map(i -> {
            ShippingOrderVO.ShippingItemVO iv = new ShippingOrderVO.ShippingItemVO();
            iv.setId(i.getId());
            iv.setOrderItemId(i.getOrderItemId());
            iv.setSkuId(i.getSkuId());
            iv.setQuantity(i.getQuantity());
            return iv;
        }).toList());

        LogisticsProvider provider = providerMap.get(order.getProviderId());
        if (provider != null) vo.setProviderName(provider.getProviderName());

        return vo;
    }

    private TrackingVO buildTrackingVO(ShippingOrder order, List<TrackingRecord> tracks) {
        TrackingVO vo = new TrackingVO();
        vo.setShippingNo(order.getShippingNo());
        vo.setProviderCode(order.getProviderCode());
        vo.setTrackingNo(order.getTrackingNo());
        vo.setShippingStatus(order.getShippingStatus());
        vo.setShippingStatusText(ShippingStatus.text(order.getShippingStatus()));

        LogisticsProvider provider = providerMapper.selectById(order.getProviderId());
        if (provider != null) vo.setProviderName(provider.getProviderName());

        vo.setTracks(tracks.stream().map(t -> {
            TrackingVO.TraceNode node = new TrackingVO.TraceNode();
            node.setTime(t.getTraceTime());
            node.setDesc(t.getTraceDesc());
            node.setLocation(t.getLocation());
            node.setEventType(t.getEventType());
            return node;
        }).toList());
        return vo;
    }

    private WarehouseInfoVO loadWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        try {
            Result<WarehouseInfoVO> result = warehouseClient.getWarehouse(warehouseId);
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                throw new BusinessException(LogisticsErrorCode.WAREHOUSE_OUTBOUND_FAILED);
            }
            return result.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query warehouse info: warehouseId={}", warehouseId, e);
            throw new BusinessException(LogisticsErrorCode.WAREHOUSE_OUTBOUND_FAILED);
        }
    }

    private boolean isManagedWarehouse(WarehouseInfoVO warehouse) {
        return warehouse != null && java.util.Objects.equals(warehouse.getStockMode(), WarehouseStockMode.MANAGED);
    }

    private OrderInternalVO loadOrderSnapshot(Long orderId) {
        try {
            Result<OrderInternalVO> result = orderClient.getShippingSnapshot(orderId);
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                throw new BusinessException(LogisticsErrorCode.ORDER_NOT_FOUND);
            }
            return result.getData();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query order snapshot: orderId={}", orderId, e);
            throw new BusinessException(LogisticsErrorCode.ORDER_NOT_FOUND);
        }
    }

    private void validateOrderForShipping(OrderInternalVO orderSnapshot) {
        if (orderSnapshot.getStatus() == null) {
            throw new BusinessException(LogisticsErrorCode.ORDER_NOT_FOUND);
        }
        if (orderSnapshot.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(LogisticsErrorCode.ORDER_NOT_PAID);
        }
    }

    private Long resolveShippingMerchantId(OrderInternalVO orderSnapshot, WarehouseInfoVO warehouse, Long requestMerchantId) {
        if (orderSnapshot != null && orderSnapshot.getMerchantId() != null) {
            return orderSnapshot.getMerchantId();
        }
        if (orderSnapshot != null && orderSnapshot.getItems() != null) {
            List<Long> merchantIds = orderSnapshot.getItems().stream()
                    .map(OrderInternalVO.OrderItemSnapshot::getMerchantId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (merchantIds.size() == 1) {
                return merchantIds.getFirst();
            }
        }
        if (warehouse != null && warehouse.getMerchantId() != null) {
            return warehouse.getMerchantId();
        }
        return requestMerchantId;
    }

    private CallbackPayload parseCallbackPayload(String rawBody) {
        try {
            return jsonMapper.readValue(rawBody, CallbackPayload.class);
        } catch (Exception e) {
            log.warn("Ignore callback with unparsable payload: body={}", rawBody, e);
            return null;
        }
    }

    private String resolveAggregationTrackingCode(ShippingOrder order) {
        LogisticsProvider provider = providerMapper.selectById(order.getProviderId());
        if (provider != null && provider.getAggregationCode() != null && !provider.getAggregationCode().isBlank()) {
            return provider.getAggregationCode();
        }
        return order.getProviderCode();
    }

    private String resolveCarrierCode(CallbackPayload payload, String fallbackProviderCode) {
        if (payload.getExpressCode() != null && !payload.getExpressCode().isBlank()) {
            return payload.getExpressCode();
        }
        if (payload.getProviderCode() != null && !payload.getProviderCode().isBlank()) {
            return payload.getProviderCode();
        }
        if (fallbackProviderCode != null && !fallbackProviderCode.isBlank()) {
            return fallbackProviderCode;
        }
        return null;
    }

    private void persistTrackingRecord(ShippingOrder order, String providerCode, String rawBody, CallbackPayload payload) {
        LocalDateTime traceTime = payload.resolveTime();
        String traceDesc = payload.getDesc() != null ? payload.getDesc() : payload.getStatus();
        String traceHash = DigestUtils.md5DigestAsHex(
                (order.getTrackingNo() + traceTime + traceDesc).getBytes(StandardCharsets.UTF_8));
        if (trackingRecordMapper.exists(new LambdaQueryWrapper<TrackingRecord>()
                .eq(TrackingRecord::getShippingId, order.getId())
                .eq(TrackingRecord::getTraceHash, traceHash))) {
            return;
        }

        TrackingRecord record = new TrackingRecord();
        record.setShippingId(order.getId());
        record.setProviderCode(providerCode);
        record.setTrackingNo(order.getTrackingNo());
        record.setTraceHash(traceHash);
        record.setTraceTime(traceTime);
        record.setTraceDesc(traceDesc);
        record.setTraceStatus(payload.getStatus());
        record.setEventType(payload.getStatus());
        record.setLocation(payload.getLocation());
        record.setRawData(rawBody);
        trackingRecordMapper.insert(record);
    }

    private void updateShippingStatusFromCallback(ShippingOrder order, CallbackPayload payload) {
        LocalDateTime traceTime = payload.resolveTime();
        String traceDesc = payload.getDesc() != null ? payload.getDesc() : payload.getStatus();
        order.setLastTraceTime(traceTime);
        order.setLastTraceDesc(traceDesc);

        switch (payload.getStatus()) {
            case "SIGNED" -> {
                order.setShippingStatus(ShippingStatus.SIGNED);
                order.setSignedAt(traceTime);
                shippingOrderMapper.updateById(order);
                publishOrderDeliveredIfAllSigned(order, traceTime);
            }
            case "EXCEPTION" -> {
                order.setShippingStatus(ShippingStatus.EXCEPTION);
                shippingOrderMapper.updateById(order);
                publishShippingException(order, traceDesc);
            }
            case "RETURNED" -> {
                order.setShippingStatus(ShippingStatus.RETURNED);
                shippingOrderMapper.updateById(order);
            }
            default -> shippingOrderMapper.updateById(order);
        }
    }

    private void publishOrderDeliveredIfAllSigned(ShippingOrder order, LocalDateTime signedAt) {
        List<ShippingOrder> shipments = shippingOrderMapper.selectList(
                new LambdaQueryWrapper<ShippingOrder>().eq(ShippingOrder::getOrderId, order.getOrderId()));
        boolean allSigned = !shipments.isEmpty() && shipments.stream()
                .allMatch(shipping -> shipping.getShippingStatus() == ShippingStatus.SIGNED);
        if (!allSigned) {
            return;
        }
        OrderInternalVO orderSnapshot = loadOrderSnapshot(order.getOrderId());

        OrderDeliveredMessage message = new OrderDeliveredMessage();
        message.setShippingId(order.getId());
        message.setOrderId(order.getOrderId());
        message.setOrderNo(order.getOrderNo());
        message.setUserId(orderSnapshot.getUserId());
        message.setTransactionId("logistics-delivered-" + order.getOrderNo());
        message.setIdempotencyKey("order-delivered:" + order.getOrderNo());
        message.setSignedAt(signedAt);
        message.setOccurredAt(signedAt);
        outboxService.enqueue("shipping", order.getOrderNo(), "order-delivered", message);
    }

    private void publishShippingException(ShippingOrder order, String exceptionDesc) {
        ShippingExceptionMessage message = new ShippingExceptionMessage();
        message.setShippingId(order.getId());
        message.setOrderId(order.getOrderId());
        message.setOrderNo(order.getOrderNo());
        message.setTrackingNo(order.getTrackingNo());
        message.setExceptionDesc(exceptionDesc);
        message.setTransactionId("shipping-exception-" + order.getShippingNo());
        message.setIdempotencyKey("shipping-exception:" + order.getShippingNo());
        message.setOccurredAt(LocalDateTime.now());
        outboxService.enqueue("shipping", order.getShippingNo(), "shipping-exception", message);
    }

    @lombok.Data
    private static class CallbackPayload {
        private static final DateTimeFormatter CALLBACK_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private String trackingNo;
        @JsonAlias("shipperCode")
        private String expressCode;
        @JsonAlias("carrierCode")
        private String providerCode;
        private String status;
        private String time;
        private String desc;
        private String location;

        LocalDateTime resolveTime() {
            if (time == null || time.isBlank()) {
                return LocalDateTime.now();
            }
            try {
                return LocalDateTime.parse(time, CALLBACK_TIME);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
    }

}
