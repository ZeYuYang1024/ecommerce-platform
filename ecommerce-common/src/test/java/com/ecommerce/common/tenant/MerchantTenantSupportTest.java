package com.ecommerce.common.tenant;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class MerchantTenantSupportTest {

    @Test
    void shouldAllowPlatformAdminToBypassMerchantScopeCheck() {
        assertThatCode(() -> MerchantTenantSupport.requireMerchantScope("super_admin", null, 3001L, TestErrorCode.DENIED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowMerchantToAccessOwnScope() {
        assertThatCode(() -> MerchantTenantSupport.requireMerchantScope("merchant", 2001L, 2001L, TestErrorCode.DENIED))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMerchantWhenTargetScopeDiffers() {
        assertThatThrownBy(() -> MerchantTenantSupport.requireMerchantScope("merchant", 2001L, 3001L, TestErrorCode.DENIED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TestErrorCode.DENIED.getMessage());
    }

    @Test
    void shouldRejectMissingMerchantIdForMerchantUser() {
        assertThatThrownBy(() -> MerchantTenantSupport.requireMerchantId(null, TestErrorCode.DENIED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TestErrorCode.DENIED.getMessage());
    }

    @Test
    void shouldResolveScopedMerchantIdFromHeaderForMerchantUser() {
        Long merchantId = MerchantTenantSupport.resolveScopedMerchantId(
                "merchant", 2001L, 3001L, TestErrorCode.DENIED);

        org.assertj.core.api.Assertions.assertThat(merchantId).isEqualTo(2001L);
    }

    @Test
    void shouldResolveScopedMerchantIdFromRequestForPlatformAdmin() {
        Long merchantId = MerchantTenantSupport.resolveScopedMerchantId(
                "super_admin", null, 3001L, TestErrorCode.DENIED);

        org.assertj.core.api.Assertions.assertThat(merchantId).isEqualTo(3001L);
    }

    @Test
    void shouldRejectMissingHeaderWhenResolvingMerchantRequestScope() {
        assertThatThrownBy(() -> MerchantTenantSupport.resolveRequestMerchantId(
                "merchant", null, TestErrorCode.DENIED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TestErrorCode.DENIED.getMessage());
    }

    @Test
    void shouldReturnHeaderWhenResolvingPlatformRequestScope() {
        Long merchantId = MerchantTenantSupport.resolveRequestMerchantId(
                "super_admin", 4001L, TestErrorCode.DENIED);

        org.assertj.core.api.Assertions.assertThat(merchantId).isEqualTo(4001L);
    }

    @Test
    void shouldAllowOwnerAccessWhenCurrentMerchantScopeMissing() {
        assertThatNoException().isThrownBy(() ->
                MerchantTenantSupport.requireOwnerAccess(null, 3001L, TestErrorCode.DENIED));
    }

    @Test
    void shouldAllowOwnerAccessWhenMerchantMatches() {
        assertThatNoException().isThrownBy(() ->
                MerchantTenantSupport.requireOwnerAccess(2001L, 2001L, TestErrorCode.DENIED));
    }

    @Test
    void shouldRejectOwnerAccessWhenMerchantDiffers() {
        assertThatThrownBy(() ->
                MerchantTenantSupport.requireOwnerAccess(2001L, 3001L, TestErrorCode.DENIED))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TestErrorCode.DENIED.getMessage());
    }

    private enum TestErrorCode implements ErrorCode {
        DENIED(99900001, "merchant permission denied");

        private final int code;
        private final String message;

        TestErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
