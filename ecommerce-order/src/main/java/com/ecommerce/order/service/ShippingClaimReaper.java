package com.ecommerce.order.service;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.LogisticsClient;
import com.ecommerce.order.client.WarehouseShipmentClient;
import com.ecommerce.order.client.dto.FulfillmentSummaryVO;
import com.ecommerce.order.mapper.ShippingClaimMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingClaimReaper {
    private final ShippingClaimMapper claims;
    private final LogisticsClient logistics;
    private final WarehouseShipmentClient warehouse;

    public boolean hasShipping(Long orderId) {
        if (claims.existsForOrder(orderId)) return true;
        for (ShippingClaimMapper.Claim claim : claims.findExpiredForOrder(orderId)) {
            if (hasEvidence(claim)) {
                claims.confirmExpired(orderId, claim.shippingNo());
                return true;
            }
            claims.releaseExpired(orderId, claim.shippingNo());
        }
        return claims.existsForOrder(orderId);
    }

    public boolean hasOtherShipping(Long orderId, String shippingNo) {
        if (claims.existsOtherForOrder(orderId, shippingNo)) return true;
        for (ShippingClaimMapper.Claim claim : claims.findExpiredForOrder(orderId)) {
            if (shippingNo.equals(claim.shippingNo())) continue;
            reconcile(claim);
        }
        return claims.existsOtherForOrder(orderId, shippingNo);
    }

    public int releaseExpired(Long orderId) {
        return claims.findExpiredForOrder(orderId).stream().mapToInt(this::reconcile).sum();
    }

    @Scheduled(fixedDelayString = "${after-sale.shipping-claim-cleanup-ms:60000}")
    public void cleanup() {
        for (ShippingClaimMapper.Claim claim : claims.findExpired()) {
            try {
                reconcile(claim);
            } catch (RuntimeException unavailable) {
                log.warn("Could not resolve expired shipping claim: orderId={}, shippingNo={}",
                        claim.orderId(), claim.shippingNo(), unavailable);
            }
        }
    }

    private int reconcile(ShippingClaimMapper.Claim claim) {
        if (hasEvidence(claim)) {
            claims.confirmExpired(claim.orderId(), claim.shippingNo());
            return 0;
        }
        return claims.releaseExpired(claim.orderId(), claim.shippingNo());
    }

    private boolean hasEvidence(ShippingClaimMapper.Claim claim) {
        List<FulfillmentSummaryVO> rows = requireData(logistics.getFulfillmentSummary(List.of(claim.orderId())));
        FulfillmentSummaryVO fulfillment = rows.stream().filter(row -> claim.orderId().equals(row.getOrderId()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Shipping summary is missing"));
        if (fulfillment.getShippingCount() > 0) return true;
        if (!claim.managed()) return false;
        if (claim.shippingId() == null) throw new IllegalStateException("Managed claim has no shipment ID");
        return requireData(warehouse.hasOutbound(claim.shippingId()));
    }

    private static <T> T requireData(Result<T> result) {
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new IllegalStateException("Could not verify shipment evidence");
        }
        return result.getData();
    }
}
