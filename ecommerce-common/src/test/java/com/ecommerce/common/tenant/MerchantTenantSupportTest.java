package com.ecommerce.common.tenant;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
