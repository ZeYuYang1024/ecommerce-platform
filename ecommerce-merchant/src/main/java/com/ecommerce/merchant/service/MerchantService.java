package com.ecommerce.merchant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
import com.ecommerce.merchant.dto.request.MerchantRegisterRequest;
import com.ecommerce.merchant.dto.request.MerchantUpdateRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;

import java.util.List;

public interface MerchantService {
    MerchantVO register(MerchantRegisterRequest request);
    MerchantVO getById(Long id);
    MerchantVO update(Long id, MerchantUpdateRequest request);
    Page<MerchantVO> list(Integer status, int page, int size);
    List<MerchantVO> listAll(Integer status);
    MerchantVO audit(Long id, MerchantAuditRequest request, Long auditorId);
}
