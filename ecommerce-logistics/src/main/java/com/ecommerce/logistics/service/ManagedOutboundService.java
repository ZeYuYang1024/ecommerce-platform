package com.ecommerce.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.constant.OutboundType;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.logistics.client.OrderClient;
import com.ecommerce.logistics.client.WarehouseClient;
import com.ecommerce.logistics.client.dto.CreateOutboundRequest;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.common.ManagedOutboundState;
import com.ecommerce.logistics.common.ShippingStatus;
import com.ecommerce.logistics.entity.ShippingOrder;
import com.ecommerce.logistics.entity.ShippingOrderItem;
import com.ecommerce.logistics.mapper.ShippingOrderItemMapper;
import com.ecommerce.logistics.mapper.ShippingOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagedOutboundService {
    private final ShippingOrderMapper shipments;
    private final ShippingOrderItemMapper items;
    private final WarehouseClient warehouse;
    private final OrderClient order;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelayString = "${logistics.managed-outbound.poll-ms:5000}")
    public void processPending() {
        LocalDateTime now = LocalDateTime.now();
        var pending = shipments.selectList(new LambdaQueryWrapper<ShippingOrder>()
                .select(ShippingOrder::getId)
                .in(ShippingOrder::getOutboundState, ManagedOutboundState.REQUESTED,
                        ManagedOutboundState.CANCELLING, ManagedOutboundState.RELEASING_CLAIM)
                .and(query -> query.isNull(ShippingOrder::getOutboundRetryAt).or().le(ShippingOrder::getOutboundRetryAt, now))
                .orderByAsc(ShippingOrder::getOutboundRetryAt, ShippingOrder::getId)
                .last("LIMIT 50"));
        for (ShippingOrder shipment : pending) {
            try {
                transactions.executeWithoutResult(status -> synchronize(shipment.getId(), now));
            } catch (RuntimeException e) {
                log.error("Could not persist warehouse synchronization for shipment {}", shipment.getId(), e);
            }
        }
    }

    private void synchronize(Long shippingId, LocalDateTime now) {
        ShippingOrder shipment = shipments.lockById(shippingId);
        if (shipment == null || (shipment.getOutboundRetryAt() != null && shipment.getOutboundRetryAt().isAfter(now))) return;
        ManagedOutboundState state = shipment.getOutboundState();
        if (state != ManagedOutboundState.REQUESTED && state != ManagedOutboundState.CANCELLING
                && state != ManagedOutboundState.RELEASING_CLAIM) return;

        ManagedOutboundState nextState = state;
        String error = null;
        LocalDateTime retryAt = null;
        boolean cancelled = false;
        try {
            switch (state) {
                case REQUESTED -> {
                    var result = warehouse.createOutbound(outboundRequest(shipment));
                    requireSuccess(result);
                    if (result.getData() == null) throw new IllegalStateException("仓库未返回出库单");
                    nextState = ManagedOutboundState.CREATED;
                }
                case CANCELLING -> {
                    var result = warehouse.cancelUnshipped(shippingId);
                    requireSuccess(result);
                    if (result.getData() == null) throw new IllegalStateException("仓库未返回取消结果");
                    cancelled = result.getData();
                    nextState = cancelled ? ManagedOutboundState.RELEASING_CLAIM : ManagedOutboundState.CREATED;
                    if (!cancelled) error = "仓库已出库，无法取消";
                }
                case RELEASING_CLAIM -> {
                    requireSuccess(order.releaseCancelledShipping(shipment.getOrderId(), shipment.getShippingNo()));
                    nextState = ManagedOutboundState.CANCELLED;
                }
                default -> throw new IllegalStateException("Unexpected outbound state " + state);
            }
        } catch (RuntimeException e) {
            error = e.getMessage() != null ? e.getMessage() : "仓配服务暂不可用";
            log.warn("Managed shipment {} synchronization will retry: {}", shippingId, error);
            error = error.substring(0, Math.min(255, error.length()));
            retryAt = LocalDateTime.now().plusSeconds(30);
        }
        shipments.update(null, new LambdaUpdateWrapper<ShippingOrder>()
                .eq(ShippingOrder::getId, shippingId)
                .set(ShippingOrder::getOutboundState, nextState)
                .set(ShippingOrder::getOutboundError, error)
                .set(ShippingOrder::getOutboundRetryAt, retryAt)
                .set(cancelled, ShippingOrder::getShippingStatus, ShippingStatus.CANCELLED)
                .setSql(cancelled, "version = version + 1"));
    }

    @Transactional
    public void cancel(Long shippingId, Long merchantId) {
        ShippingOrder shipment = shipments.lockById(shippingId);
        if (shipment == null) throw new BusinessException(LogisticsErrorCode.SHIPPING_NOT_FOUND);
        MerchantTenantSupport.requireOwnerAccess(merchantId, shipment.getMerchantId(), LogisticsErrorCode.SHIPPING_FORBIDDEN);
        if (shipment.getShippingStatus() == ShippingStatus.CANCELLED || shipment.getOutboundState() == ManagedOutboundState.CANCELLING) return;
        if (shipment.getWarehouseId() == null || shipment.getShippingStatus() != ShippingStatus.PENDING) {
            throw new BusinessException(LogisticsErrorCode.INVALID_STATUS_TRANSITION);
        }

        shipments.update(null, new LambdaUpdateWrapper<ShippingOrder>()
                .eq(ShippingOrder::getId, shippingId)
                .set(ShippingOrder::getOutboundState, ManagedOutboundState.CANCELLING)
                .set(ShippingOrder::getOutboundError, null)
                .set(ShippingOrder::getOutboundRetryAt, null));
    }

    private CreateOutboundRequest outboundRequest(ShippingOrder shipment) {
        CreateOutboundRequest request = new CreateOutboundRequest();
        request.setWarehouseId(shipment.getWarehouseId());
        request.setOutboundType(OutboundType.SALES);
        request.setShippingId(shipment.getId());
        request.setMerchantId(shipment.getMerchantId());
        request.setItems(items.selectList(new LambdaQueryWrapper<ShippingOrderItem>()
                .eq(ShippingOrderItem::getShippingId, shipment.getId())).stream().map(item -> {
            CreateOutboundRequest.OutboundItemRequest row = new CreateOutboundRequest.OutboundItemRequest();
            row.setSkuId(item.getSkuId());
            row.setQuantity(item.getQuantity());
            return row;
        }).toList());
        return request;
    }

    private void requireSuccess(Result<?> reply) {
        if (reply == null || reply.getCode() != 200) {
            throw new IllegalStateException(reply == null ? "仓配服务暂不可用" : reply.getMessage());
        }
    }
}
