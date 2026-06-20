package com.ecommerce.logistics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;

import java.math.BigDecimal;

public interface ShippingTemplateService {
    IPage<ShippingTemplateVO> listTemplates(int page, int size, Long merchantId);
    ShippingTemplateVO getTemplate(Long id);
    ShippingTemplateVO createTemplate(CreateShippingTemplateRequest req);
    ShippingTemplateVO updateTemplate(Long id, CreateShippingTemplateRequest req);
    void deleteTemplate(Long id);
    BigDecimal calculateFee(Long templateId, int quantity, int weight, int volume, String provinceCode);
}
