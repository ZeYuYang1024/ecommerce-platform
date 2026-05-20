package com.ecommerce.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.CouponVerifyVO;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.tenant.MerchantTenantSupport;
import com.ecommerce.coupon.common.CouponErrorCode;
import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.entity.CouponTemplate;
import com.ecommerce.coupon.entity.UserCoupon;
import com.ecommerce.coupon.mapper.CouponTemplateMapper;
import com.ecommerce.coupon.mapper.UserCouponMapper;
import com.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public CouponTemplate createTemplate(CouponTemplate template) {
        template.setMerchantId(null);
        templateMapper.insert(template);
        return template;
    }

    @Override
    public CouponTemplate createTemplate(CouponTemplate template, Long merchantId) {
        template.setMerchantId(MerchantTenantSupport.requireMerchantId(merchantId, CouponErrorCode.COUPON_FORBIDDEN));
        templateMapper.insert(template);
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(CouponTemplate template) {
        CouponTemplate exist = templateMapper.selectById(template.getId());
        if (exist == null) {
            throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        }
        template.setMerchantId(exist.getMerchantId());
        templateMapper.updateById(template);
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(CouponTemplate template, Long merchantId) {
        CouponTemplate exist = templateMapper.selectById(template.getId());
        if (exist == null) {
            throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        }
        MerchantTenantSupport.requireMerchantScope("merchant", merchantId, exist.getMerchantId(), CouponErrorCode.COUPON_FORBIDDEN);
        template.setMerchantId(exist.getMerchantId());
        templateMapper.updateById(template);
        return template;
    }

    @Override
    public Page<CouponTemplate> listTemplates(Integer status, int page, int size) {
        LambdaQueryWrapper<CouponTemplate> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(CouponTemplate::getStatus, status);
        }
        query.orderByDesc(CouponTemplate::getCreatedAt);
        return templateMapper.selectPage(new Page<>(page, size), query);
    }

    @Override
    public Page<CouponTemplate> listTemplates(Integer status, int page, int size, Long merchantId) {
        Long scopedMerchantId = MerchantTenantSupport.requireMerchantId(merchantId, CouponErrorCode.COUPON_FORBIDDEN);
        LambdaQueryWrapper<CouponTemplate> query = new LambdaQueryWrapper<>();
        query.eq(CouponTemplate::getMerchantId, scopedMerchantId);
        if (status != null) {
            query.eq(CouponTemplate::getStatus, status);
        }
        query.orderByDesc(CouponTemplate::getCreatedAt);
        return templateMapper.selectPage(new Page<>(page, size), query);
    }

    private List<CouponTemplate> listAllTemplates(Integer status) {
        LambdaQueryWrapper<CouponTemplate> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(CouponTemplate::getStatus, status);
        }
        query.orderByDesc(CouponTemplate::getCreatedAt);
        return templateMapper.selectList(query);
    }

    @Override
    @Transactional
    public void claim(Long userId, Long templateId) {
        CouponTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getStatus() == 0) {
            throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        }
        if (template.getRemainingCount() != null && template.getRemainingCount() <= 0) {
            throw new BusinessException(CouponErrorCode.COUPON_EXHAUSTED);
        }
        if (template.getEndTime() != null && template.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }

        int perLimit = template.getPerUserLimit() != null ? template.getPerUserLimit() : 1;
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId);
        long userCount = userCouponMapper.selectCount(query);
        if (userCount >= perLimit) {
            throw new BusinessException(CouponErrorCode.USER_LIMIT_REACHED);
        }

        int rows = templateMapper.update(null,
                new LambdaUpdateWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getId, templateId)
                        .gt(CouponTemplate::getRemainingCount, 0)
                        .setSql("remaining_count = remaining_count - 1"));
        if (rows == 0) {
            throw new BusinessException(CouponErrorCode.COUPON_EXHAUSTED);
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setTemplateId(templateId);
        userCoupon.setStatus(0);
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public List<CouponVO> listAvailableCoupons() {
        List<CouponTemplate> templates = listAllTemplates(1);
        List<CouponVO> vos = new ArrayList<>();
        for (CouponTemplate template : templates) {
            vos.add(toVO(template));
        }
        return vos;
    }

    @Override
    public Page<CouponVO> listAvailableCoupons(int page, int size) {
        LambdaQueryWrapper<CouponTemplate> query = new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .orderByDesc(CouponTemplate::getCreatedAt);
        IPage<CouponTemplate> pageResult = templateMapper.selectPage(new Page<>(page, size), query);
        Page<CouponVO> result = new Page<>(page, size);
        result.setTotal(pageResult.getTotal());
        result.setRecords(pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<CouponVO> listCurrentUserCouponSummaries(Long userId) {
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreatedAt);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(query);
        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }
        return buildUserCouponVOs(userCoupons);
    }

    private CouponVO toVO(CouponTemplate template) {
        CouponVO vo = new CouponVO();
        vo.setId(template.getId());
        vo.setUserCouponId(null);
        vo.setName(template.getName());
        vo.setType(template.getType());
        vo.setMinAmount(template.getMinAmount());
        vo.setDiscountAmount(template.getDiscountAmount());
        vo.setDiscountRate(template.getDiscountRate());
        vo.setStatus(0);
        vo.setStartTime(template.getStartTime());
        vo.setEndTime(template.getEndTime());
        return vo;
    }

    @Override
    public List<CouponVO> listUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            query.eq(UserCoupon::getStatus, status);
        }
        query.orderByDesc(UserCoupon::getCreatedAt);
        List<UserCoupon> userCoupons = userCouponMapper.selectList(query);
        if (userCoupons.isEmpty()) {
            return Collections.emptyList();
        }
        return buildUserCouponVOs(userCoupons);
    }

    @Override
    public Page<CouponVO> listUserCoupons(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            query.eq(UserCoupon::getStatus, status);
        }
        query.orderByDesc(UserCoupon::getCreatedAt);
        IPage<UserCoupon> pageResult = userCouponMapper.selectPage(new Page<>(page, size), query);
        Page<CouponVO> result = new Page<>(page, size);
        result.setTotal(pageResult.getTotal());
        result.setRecords(pageResult.getRecords().isEmpty()
                ? Collections.emptyList()
                : buildUserCouponVOs(pageResult.getRecords()));
        return result;
    }

    private List<CouponVO> buildUserCouponVOs(List<UserCoupon> userCoupons) {
        Map<Long, CouponTemplate> templateMap = new HashMap<>();
        List<CouponVO> vos = new ArrayList<>();
        for (UserCoupon userCoupon : userCoupons) {
            CouponTemplate template = templateMap.computeIfAbsent(userCoupon.getTemplateId(), templateMapper::selectById);
            CouponVO vo = new CouponVO();
            vo.setId(userCoupon.getTemplateId());
            vo.setUserCouponId(userCoupon.getId());
            vo.setName(template != null ? template.getName() : "");
            vo.setType(template != null ? template.getType() : "");
            vo.setMinAmount(template != null ? template.getMinAmount() : null);
            vo.setDiscountAmount(template != null ? template.getDiscountAmount() : null);
            vo.setDiscountRate(template != null ? template.getDiscountRate() : null);
            vo.setStatus(userCoupon.getStatus());
            vo.setStartTime(template != null ? template.getStartTime() : null);
            vo.setEndTime(template != null ? template.getEndTime() : null);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public CouponVerifyVO verify(Long userCouponId, Long userId, BigDecimal orderAmount) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userCoupon.getUserId().equals(userId)) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (userCoupon.getStatus() != 0) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
        }

        CouponTemplate template = templateMapper.selectById(userCoupon.getTemplateId());
        if (template == null || template.getStatus() == 0) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (template.getEndTime() != null && template.getEndTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
        }

        BigDecimal minAmount = template.getMinAmount() != null ? template.getMinAmount() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minAmount) < 0) {
            throw new BusinessException(CouponErrorCode.MIN_AMOUNT_NOT_MET);
        }

        BigDecimal discount = calculateDiscount(template, orderAmount);

        CouponVerifyVO vo = new CouponVerifyVO();
        vo.setValid(true);
        vo.setDiscount(discount);
        vo.setCouponName(template.getName());
        vo.setTemplateId(template.getId());
        return vo;
    }

    @Override
    @Transactional
    public void use(Long userCouponId, String orderNo) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || userCoupon.getStatus() != 0) {
            throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
        }
        userCoupon.setStatus(1);
        userCoupon.setOrderNo(orderNo);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
    }

    private BigDecimal calculateDiscount(CouponTemplate template, BigDecimal orderAmount) {
        return switch (template.getType()) {
            case "FLAT" -> template.getDiscountAmount() != null ? template.getDiscountAmount() : BigDecimal.ZERO;
            case "DISCOUNT" -> {
                BigDecimal rate = template.getDiscountRate() != null ? template.getDiscountRate() : BigDecimal.ONE;
                yield orderAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            }
            case "FULL_REDUCTION" -> template.getDiscountAmount() != null ? template.getDiscountAmount() : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };
    }
}
