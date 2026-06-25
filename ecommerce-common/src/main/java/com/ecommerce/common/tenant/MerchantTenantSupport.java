package com.ecommerce.common.tenant;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.ErrorCode;

public final class MerchantTenantSupport {

    private MerchantTenantSupport() {
    }

    public static boolean isMerchantUser(String userType) {
        return "merchant".equals(userType);
    }

    public static Long requireMerchantId(Long merchantId, ErrorCode errorCode) {
        if (merchantId == null) {
            throw new BusinessException(errorCode);
        }
        return merchantId;
    }

    public static void requireMerchantScope(String userType, Long merchantId, Long targetMerchantId, ErrorCode errorCode) {
        if (!isMerchantUser(userType)) {
            return;
        }
        Long requiredMerchantId = requireMerchantId(merchantId, errorCode);
        if (!requiredMerchantId.equals(targetMerchantId)) {
            throw new BusinessException(errorCode);
        }
    }

    public static Long resolveScopedMerchantId(String userType, Long headerMerchantId, Long requestMerchantId,
                                               ErrorCode errorCode) {
        if (isMerchantUser(userType)) {
            return requireMerchantId(headerMerchantId, errorCode);
        }
        return headerMerchantId != null ? headerMerchantId : requestMerchantId;
    }

    public static Long resolveRequestMerchantId(String userType, Long headerMerchantId, ErrorCode errorCode) {
        if (isMerchantUser(userType)) {
            return requireMerchantId(headerMerchantId, errorCode);
        }
        return headerMerchantId;
    }

    public static void requireOwnerAccess(Long currentMerchantId, Long ownerMerchantId, ErrorCode errorCode) {
        if (currentMerchantId == null) {
            return;
        }
        if (ownerMerchantId == null || !currentMerchantId.equals(ownerMerchantId)) {
            throw new BusinessException(errorCode);
        }
    }
}
