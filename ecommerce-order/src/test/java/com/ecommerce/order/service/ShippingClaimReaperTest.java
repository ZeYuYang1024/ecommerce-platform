package com.ecommerce.order.service;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.LogisticsClient;
import com.ecommerce.order.client.WarehouseShipmentClient;
import com.ecommerce.order.client.dto.FulfillmentSummaryVO;
import com.ecommerce.order.mapper.ShippingClaimMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingClaimReaperTest {
    @Mock private ShippingClaimMapper claims;
    @Mock private LogisticsClient logistics;
    @Mock private WarehouseShipmentClient warehouse;
    @InjectMocks private ShippingClaimReaper reaper;

    @Test
    void deletesExpiredClaimOnlyWhenNoFulfillmentExists() {
        var claim = new ShippingClaimMapper.Claim(1L, "SHIP-1", 15L, false);
        when(claims.findExpiredForOrder(1L)).thenReturn(List.of(claim));
        when(logistics.getFulfillmentSummary(List.of(1L))).thenReturn(Result.ok(List.of(summary(0))));
        when(claims.releaseExpired(1L, "SHIP-1")).thenReturn(1);

        assertThat(reaper.releaseExpired(1L)).isEqualTo(1);
    }

    @Test
    void preservesAnOrphanThatWarehouseAlreadyShipped() {
        var claim = new ShippingClaimMapper.Claim(1L, "SHIP-1", 15L, true);
        when(claims.findExpiredForOrder(1L)).thenReturn(List.of(claim));
        when(logistics.getFulfillmentSummary(List.of(1L))).thenReturn(Result.ok(List.of(summary(0))));
        when(warehouse.cancelUnshipped(15L)).thenReturn(Result.ok(false));

        assertThat(reaper.hasShipping(1L)).isTrue();
        verify(claims).confirmExpired(1L, "SHIP-1");
        verify(claims, never()).releaseExpired(1L, "SHIP-1");
    }

    @Test
    void releasesConfirmedOrphanAfterWarehouseCancelsItsUnshippedOutbound() {
        var claim = new ShippingClaimMapper.Claim(1L, "SHIP-1", 15L, true);
        when(claims.findConfirmedManagedForOrder(1L)).thenReturn(List.of(claim));
        when(logistics.getFulfillmentSummary(List.of(1L))).thenReturn(Result.ok(List.of(summary(0))));
        when(warehouse.cancelUnshipped(15L)).thenReturn(Result.ok(true));
        when(claims.releaseManaged(1L, "SHIP-1")).thenReturn(1);

        assertThat(reaper.releaseExpired(1L)).isEqualTo(1);
    }

    @Test
    void failedEvidenceLookupDoesNotReleaseClaim() {
        var claim = new ShippingClaimMapper.Claim(1L, "SHIP-1", 15L, true);
        when(claims.findExpiredForOrder(1L)).thenReturn(List.of(claim));
        when(logistics.getFulfillmentSummary(List.of(1L))).thenReturn(Result.fail(503, "unavailable"));

        assertThatThrownBy(() -> reaper.releaseExpired(1L)).isInstanceOf(IllegalStateException.class);
        verify(claims, never()).releaseExpired(1L, "SHIP-1");
    }

    private FulfillmentSummaryVO summary(int shippingCount) {
        FulfillmentSummaryVO row = new FulfillmentSummaryVO();
        row.setOrderId(1L);
        row.setShippingCount(shippingCount);
        return row;
    }
}
