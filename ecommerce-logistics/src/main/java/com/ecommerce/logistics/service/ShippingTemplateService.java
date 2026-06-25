package com.ecommerce.logistics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.request.UpdateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;

import java.math.BigDecimal;

public interface ShippingTemplateService {
    IPage<ShippingTemplateVO> listTemplates(int page, int size, Long merchantId);
    ShippingTemplateVO getTemplate(Long id, Long merchantId);
    ShippingTemplateVO createTemplate(CreateShippingTemplateRequest req);
    ShippingTemplateVO updateTemplate(Long id, UpdateShippingTemplateRequest req, Long merchantId);
    void deleteTemplate(Long id, Long merchantId);
    BigDecimal calculateFee(Long templateId, int quantity, int weight, int volume, String provinceCode);
}
