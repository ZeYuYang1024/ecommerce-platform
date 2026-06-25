package com.ecommerce.logistics.provider;

import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "logistics.provider.active", havingValue = "stub", matchIfMissing = true)
public class StubAggregationProvider implements AggregationProvider {

    @Override
    public String getProviderCode() {
        return "stub";
    }

    @Override
    public TrackingQueryResponse queryTracking(String trackingNo, String expressCode) {
        log.info("Stub tracking query: trackingNo={}, expressCode={}", trackingNo, expressCode);
        return TrackingQueryResponse.builder()
                .success(true)
                .trackingNo(trackingNo)
                .expressCode(expressCode)
                .traces(List.of(
                        TrackingQueryResponse.TraceItem.builder()
                                .time(LocalDateTime.now().minusHours(2))
                                .desc("【Stub】包裹已发出")
                                .status("DISPATCHED")
                                .location("发货地")
                                .build()
                ))
                .build();
    }

    @Override
    public boolean subscribeTracking(String trackingNo, String expressCode, String callbackUrl) {
        log.info("Stub tracking subscribe skipped: trackingNo={}", trackingNo);
        return false;
    }

    @Override
    public boolean verifyCallbackSignature(String aggregationProviderCode, String rawBody, String signature) {
        return true;
    }
}
