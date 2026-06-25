package com.ecommerce.logistics.provider;

import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;

public interface AggregationProvider {
    String getProviderCode();

    TrackingQueryResponse queryTracking(String trackingNo, String expressCode);

    boolean subscribeTracking(String trackingNo, String expressCode, String callbackUrl);

    default boolean verifyCallbackSignature(String aggregationProviderCode, String rawBody, String signature) {
        return true;
    }
}
