package com.ecommerce.logistics.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubAggregationProviderTest {

    private final StubAggregationProvider provider = new StubAggregationProvider();

    @Test
    void shouldAlwaysAcceptCallbackSignature() {
        assertThat(provider.verifyCallbackSignature("stub", "{\"trackingNo\":\"SF123\"}", "any-signature"))
                .isTrue();
    }
}
