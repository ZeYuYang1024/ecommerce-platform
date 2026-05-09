package com.ecommerce.merchant.service;

import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
import com.ecommerce.merchant.dto.request.MerchantRegisterRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;

import java.util.List;

public interface MerchantService {
    MerchantVO register(MerchantRegisterRequest request);
    MerchantVO getById(Long id);
    List<MerchantVO> list(Integer status);
    MerchantVO audit(Long id, MerchantAuditRequest request, Long auditorId);
}
