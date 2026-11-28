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
        template.setRemainingCount(validTotal(template.getTotalCount()));
        templateMapper.insert(template);
        return template;
    }

    @Override
    public CouponTemplate createTemplate(CouponTemplate template, Long merchantId) {
        template.setMerchantId(MerchantTenantSupport.requireMerchantId(merchantId, CouponErrorCode.COUPON_FORBIDDEN));
        template.setRemainingCount(validTotal(template.getTotalCount()));
        templateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional
    public CouponTemplate updateTemplate(CouponTemplate template) {
        return updateTemplateLocked(template, null);
    }

    @Override
    @Transactional
    public CouponTemplate updateTemplate(CouponTemplate template, Long merchantId) {
        return updateTemplateLocked(template, MerchantTenantSupport.requireMerchantId(
                merchantId, CouponErrorCode.COUPON_FORBIDDEN));
    }

    private CouponTemplate updateTemplateLocked(CouponTemplate template, Long merchantId) {
        CouponTemplate existing = lockTemplate(template.getId());
        if (merchantId != null) {
            MerchantTenantSupport.requireMerchantScope("merchant", merchantId,
                    existing.getMerchantId(), CouponErrorCode.COUPON_FORBIDDEN);
        }
        int target = validTotal(template.getTotalCount() == null ? existing.getTotalCount() : template.getTotalCount());
        long issued = issuedCount(existing.getId());
        if (target < issued) throw new BusinessException(CouponErrorCode.COUPON_TOTAL_BELOW_ISSUED);
        template.setMerchantId(existing.getMerchantId());
        template.setTotalCount(target);
        template.setRemainingCount(target - (int) issued);
        templateMapper.updateById(template);
        return template;
    }

    @Override
    @Transactional
    public void changeStatus(Long id, int status, Long merchantId) {
        if (status != 0 && status != 1) throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        CouponTemplate existing = lockTemplate(id);
        if (merchantId != null) {
            MerchantTenantSupport.requireMerchantScope("merchant", merchantId,
                    existing.getMerchantId(), CouponErrorCode.COUPON_FORBIDDEN);
        }
        templateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id).setSql("status = " + status));
    }

    private CouponTemplate lockTemplate(Long id) {
        CouponTemplate existing = templateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id).last("FOR UPDATE"));
        if (existing == null) throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        return existing;
    }

    private long issuedCount(Long id) {
        return userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getTemplateId, id));
    }

    private int validTotal(Integer total) {
        if (total == null || total < 0) throw new BusinessException(CouponErrorCode.COUPON_TOTAL_BELOW_ISSUED);
        return total;
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

    @Override
    @Transactional
    public void claim(Long userId, Long templateId) {
        CouponTemplate template = lockTemplate(templateId);
        if (!Integer.valueOf(1).equals(template.getStatus())) {
            throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        }
        if (template.getTotalCount() == null || issuedCount(templateId) >= template.getTotalCount()
                || template.getRemainingCount() == null || template.getRemainingCount() <= 0) {
            throw new BusinessException(CouponErrorCode.COUPON_EXHAUSTED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStartTime() != null && template.getStartTime().isAfter(now)) {
            throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (template.getEndTime() != null && !template.getEndTime().isAfter(now)) {
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
        List<CouponTemplate> templates = templateMapper.selectList(availableTemplates());
        List<CouponVO> vos = new ArrayList<>();
        for (CouponTemplate template : templates) {
            vos.add(toVO(template));
        }
        return vos;
    }

    @Override
    public Page<CouponVO> listAvailableCoupons(int page, int size) {
        IPage<CouponTemplate> pageResult = templateMapper.selectPage(new Page<>(page, size), availableTemplates());
        Page<CouponVO> result = new Page<>(page, size);
        result.setTotal(pageResult.getTotal());
        result.setRecords(pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    private LambdaQueryWrapper<CouponTemplate> availableTemplates() {
        LocalDateTime now = LocalDateTime.now();
        return new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1).gt(CouponTemplate::getRemainingCount, 0)
                .and(start -> start.isNull(CouponTemplate::getStartTime).or().le(CouponTemplate::getStartTime, now))
                .and(end -> end.isNull(CouponTemplate::getEndTime).or().gt(CouponTemplate::getEndTime, now))
                .orderByDesc(CouponTemplate::getCreatedAt, CouponTemplate::getId);
    }

    @Override
    public List<CouponVO> listCurrentUserCouponSummaries(Long userId) {
        return listUserCoupons(userId, null);
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
        LocalDateTime now = LocalDateTime.now();
        return buildUserCouponVOs(userCouponMapper.selectList(userCoupons(userId, status, now)), now);
    }

    @Override
    public Page<CouponVO> listUserCoupons(Long userId, Integer status, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        IPage<UserCoupon> rows = userCouponMapper.selectPage(new Page<>(page, size), userCoupons(userId, status, now));
        return new Page<CouponVO>(rows.getCurrent(), rows.getSize(), rows.getTotal())
                .setRecords(buildUserCouponVOs(rows.getRecords(), now));
    }

    private LambdaQueryWrapper<UserCoupon> userCoupons(Long userId, Integer status, LocalDateTime now) {
        LambdaQueryWrapper<UserCoupon> query = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) {
            query.apply("""
                    CASE WHEN status <> 0 THEN status ELSE COALESCE((
                        SELECT CASE
                            WHEN ct.status <> 1 OR ct.status IS NULL OR ct.start_time > {0} THEN 3
                            WHEN ct.end_time <= {0} THEN 2
                            ELSE 0 END
                        FROM coupon_template ct WHERE ct.id = user_coupon.template_id AND ct.deleted = 0
                    ), 3) END = {1}
                    """, now, status);
        }
        return query.orderByDesc(UserCoupon::getCreatedAt, UserCoupon::getId);
    }

    private List<CouponVO> buildUserCouponVOs(List<UserCoupon> userCoupons, LocalDateTime now) {
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
            vo.setStatus(userCoupon.getStatus() == 0 ? templateAvailability(template, now) : userCoupon.getStatus());
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
        requireUsableTemplate(template, LocalDateTime.now());

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
        requireUsableTemplate(templateMapper.selectById(userCoupon.getTemplateId()), LocalDateTime.now());
        userCoupon.setStatus(1);
        userCoupon.setOrderNo(orderNo);
        userCoupon.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(userCoupon);
    }

    private void requireUsableTemplate(CouponTemplate template, LocalDateTime now) {
        switch (templateAvailability(template, now)) {
            case 2 -> throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);
            case 3 -> throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
            default -> { }
        }
    }

    private int templateAvailability(CouponTemplate template, LocalDateTime now) {
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())
                || (template.getStartTime() != null && template.getStartTime().isAfter(now))) return 3;
        if (template.getEndTime() != null && !template.getEndTime().isAfter(now)) return 2;
        return 0;
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
