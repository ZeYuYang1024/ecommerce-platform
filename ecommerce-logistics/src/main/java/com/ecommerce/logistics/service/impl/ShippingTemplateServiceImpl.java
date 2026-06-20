package com.ecommerce.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;
import com.ecommerce.logistics.entity.ShippingTemplate;
import com.ecommerce.logistics.mapper.ShippingTemplateMapper;
import com.ecommerce.logistics.service.ShippingTemplateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingTemplateServiceImpl implements ShippingTemplateService {

    private final ShippingTemplateMapper shippingTemplateMapper;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public IPage<ShippingTemplateVO> listTemplates(int page, int size, Long merchantId) {
        Page<ShippingTemplate> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ShippingTemplate> wrapper = new LambdaQueryWrapper<>();
        if (merchantId != null) {
            wrapper.eq(ShippingTemplate::getMerchantId, merchantId);
        }
        wrapper.orderByDesc(ShippingTemplate::getCreatedAt);
        Page<ShippingTemplate> result = shippingTemplateMapper.selectPage(pageParam, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public ShippingTemplateVO getTemplate(Long id) {
        ShippingTemplate template = shippingTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(LogisticsErrorCode.TEMPLATE_NOT_FOUND);
        }
        return toVO(template);
    }

    @Override
    @Transactional
    public ShippingTemplateVO createTemplate(CreateShippingTemplateRequest req) {
        ShippingTemplate template = new ShippingTemplate();
        template.setTemplateName(req.getTemplateName());
        template.setMerchantId(req.getMerchantId());
        template.setCalcType(req.getCalcType() != null ? req.getCalcType() : 0);
        template.setFirstUnit(req.getFirstUnit() != null ? req.getFirstUnit() : 0);
        template.setFirstFee(req.getFirstFee() != null ? req.getFirstFee() : BigDecimal.ZERO);
        template.setContinueUnit(req.getContinueUnit() != null ? req.getContinueUnit() : 0);
        template.setContinueFee(req.getContinueFee() != null ? req.getContinueFee() : BigDecimal.ZERO);
        template.setFreeCondition(req.getFreeCondition());
        template.setRegionRules(req.getRegionRules());
        shippingTemplateMapper.insert(template);
        return toVO(template);
    }

    @Override
    @Transactional
    public ShippingTemplateVO updateTemplate(Long id, CreateShippingTemplateRequest req) {
        ShippingTemplate template = shippingTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(LogisticsErrorCode.TEMPLATE_NOT_FOUND);
        }
        template.setTemplateName(req.getTemplateName());
        template.setMerchantId(req.getMerchantId());
        template.setCalcType(req.getCalcType() != null ? req.getCalcType() : template.getCalcType());
        template.setFirstUnit(req.getFirstUnit() != null ? req.getFirstUnit() : template.getFirstUnit());
        template.setFirstFee(req.getFirstFee() != null ? req.getFirstFee() : template.getFirstFee());
        template.setContinueUnit(req.getContinueUnit() != null ? req.getContinueUnit() : template.getContinueUnit());
        template.setContinueFee(req.getContinueFee() != null ? req.getContinueFee() : template.getContinueFee());
        template.setFreeCondition(req.getFreeCondition() != null ? req.getFreeCondition() : template.getFreeCondition());
        template.setRegionRules(req.getRegionRules() != null ? req.getRegionRules() : template.getRegionRules());
        shippingTemplateMapper.updateById(template);
        return toVO(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        ShippingTemplate template = shippingTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(LogisticsErrorCode.TEMPLATE_NOT_FOUND);
        }
        shippingTemplateMapper.deleteById(id);
    }

    @Override
    public BigDecimal calculateFee(Long templateId, int quantity, int weight, int volume, String provinceCode) {
        ShippingTemplate template = shippingTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(LogisticsErrorCode.TEMPLATE_NOT_FOUND);
        }

        // 1. Check free_condition for free shipping
        try {
            if (StringUtils.hasText(template.getFreeCondition())) {
                Map<String, Object> freeCondition = OBJECT_MAPPER.readValue(
                        template.getFreeCondition(), new TypeReference<Map<String, Object>>() {});
                String type = (String) freeCondition.get("type");
                Object thresholdObj = freeCondition.get("threshold");
                if (type != null && thresholdObj != null) {
                    double threshold = ((Number) thresholdObj).doubleValue();
                    if ("amount".equals(type)) {
                        // orderAmount not available in this method; amount-based free
                        // shipping should be checked by the caller before invoking calculateFee
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse free_condition for template {}: {}", templateId, e.getMessage());
        }

        int firstUnit = template.getFirstUnit();
        BigDecimal firstFee = template.getFirstFee();
        int continueUnit = template.getContinueUnit();
        BigDecimal continueFee = template.getContinueFee();

        // 2. Check region_rules for province-specific rates
        try {
            if (StringUtils.hasText(template.getRegionRules()) && StringUtils.hasText(provinceCode)) {
                Map<String, Map<String, Object>> regionRules = OBJECT_MAPPER.readValue(
                        template.getRegionRules(), new TypeReference<Map<String, Map<String, Object>>>() {});
                Map<String, Object> provinceRule = regionRules.get(provinceCode);
                if (provinceRule != null) {
                    if (provinceRule.get("firstUnit") != null) {
                        firstUnit = ((Number) provinceRule.get("firstUnit")).intValue();
                    }
                    if (provinceRule.get("firstFee") != null) {
                        firstFee = BigDecimal.valueOf(((Number) provinceRule.get("firstFee")).doubleValue());
                    }
                    if (provinceRule.get("continueUnit") != null) {
                        continueUnit = ((Number) provinceRule.get("continueUnit")).intValue();
                    }
                    if (provinceRule.get("continueFee") != null) {
                        continueFee = BigDecimal.valueOf(((Number) provinceRule.get("continueFee")).doubleValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse region_rules for template {}: {}", templateId, e.getMessage());
        }

        // 3. Determine total units based on calcType
        int calcType = template.getCalcType() != null ? template.getCalcType() : 0;
        int totalUnits;
        switch (calcType) {
            case 1:
                totalUnits = weight;
                break;
            case 2:
                totalUnits = volume;
                break;
            case 0:
            default:
                totalUnits = quantity;
                break;
        }

        // 4. Calculate fee: firstFee + ceil((total - firstUnit) / continueUnit) * continueFee
        if (totalUnits <= firstUnit || continueUnit <= 0) {
            return firstFee;
        }

        int excess = totalUnits - firstUnit;
        int additionalUnits = (int) Math.ceil((double) excess / continueUnit);
        BigDecimal additionalFee = continueFee.multiply(BigDecimal.valueOf(additionalUnits));
        return firstFee.add(additionalFee).setScale(2, RoundingMode.HALF_UP);
    }

    private ShippingTemplateVO toVO(ShippingTemplate entity) {
        ShippingTemplateVO vo = new ShippingTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setMerchantId(entity.getMerchantId());
        vo.setCalcType(entity.getCalcType());
        vo.setCalcTypeText(calcTypeText(entity.getCalcType()));
        vo.setFirstUnit(entity.getFirstUnit());
        vo.setFirstFee(entity.getFirstFee());
        vo.setContinueUnit(entity.getContinueUnit());
        vo.setContinueFee(entity.getContinueFee());
        vo.setFreeCondition(entity.getFreeCondition());
        vo.setRegionRules(entity.getRegionRules());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String calcTypeText(Integer calcType) {
        if (calcType == null) {
            return "按件";
        }
        switch (calcType) {
            case 1:
                return "按重量";
            case 2:
                return "按体积";
            case 0:
            default:
                return "按件";
        }
    }
}
