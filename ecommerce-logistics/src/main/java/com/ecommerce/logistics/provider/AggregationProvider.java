package com.ecommerce.logistics.provider;

import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;

public interface AggregationProvider {
    String getProviderCode();

    /** 实时查询物流轨迹 */
    TrackingQueryResponse queryTracking(String trackingNo, String expressCode);

    /** 订阅物流轨迹推送（Phase 1 stub返回false） */
    boolean subscribeTracking(String trackingNo, String expressCode, String callbackUrl);
}
