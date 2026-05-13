package com.ecommerce.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper templateMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public CouponTemplate createTemplate(CouponTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    @Override
    public CouponTemplate updateTemplate(CouponTemplate template) {
        CouponTemplate exist = templateMapper.selectById(template.getId());
        if (exist == null) throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        templateMapper.updateById(template);
        return template;
    }

    @Override
    public Page<CouponTemplate> listTemplates(Integer status, int page, int size) {
        LambdaQueryWrapper<CouponTemplate> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(CouponTemplate::getStatus, status);
        q.orderByDesc(CouponTemplate::getCreatedAt);
        Page<CouponTemplate> pageReq = new Page<>(page, size);
        return templateMapper.selectPage(pageReq, q);
    }

    private List<CouponTemplate> listAllTemplates(Integer status) {
        LambdaQueryWrapper<CouponTemplate> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(CouponTemplate::getStatus, status);
        q.orderByDesc(CouponTemplate::getCreatedAt);
        return templateMapper.selectList(q);
    }

    @Override
    @Transactional
    public void claim(Long userId, Long templateId) {
        CouponTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getStatus() == 0) throw new BusinessException(CouponErrorCode.TEMPLATE_NOT_FOUND);
        if (template.getRemainingCount() != null && template.getRemainingCount() <= 0) throw new BusinessException(CouponErrorCode.COUPON_EXHAUSTED);
        if (template.getEndTime() != null && template.getEndTime().isBefore(LocalDateTime.now())) throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);

        // per-user limit
        int perLimit = template.getPerUserLimit() != null ? template.getPerUserLimit() : 1;
        LambdaQueryWrapper<UserCoupon> ucq = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId);
        long userCount = userCouponMapper.selectCount(ucq);
        if (userCount >= perLimit) throw new BusinessException(CouponErrorCode.USER_LIMIT_REACHED);

        // 原子扣减库存
        int rows = templateMapper.update(null,
                new LambdaUpdateWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getId, templateId)
                        .gt(CouponTemplate::getRemainingCount, 0)
                        .setSql("remaining_count = remaining_count - 1"));
        if (rows == 0) throw new BusinessException(CouponErrorCode.COUPON_EXHAUSTED);

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setTemplateId(templateId);
        uc.setStatus(0);
        userCouponMapper.insert(uc);
    }

    @Override
    public List<CouponVO> listAvailableCoupons() {
        List<CouponTemplate> templates = listAllTemplates(1);
        List<CouponVO> vos = new ArrayList<>();
        for (CouponTemplate t : templates) {
            CouponVO vo = toVO(t);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public Page<CouponVO> listAvailableCoupons(int page, int size) {
        LambdaQueryWrapper<CouponTemplate> q = new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .orderByDesc(CouponTemplate::getCreatedAt);
        IPage<CouponTemplate> ipage = templateMapper.selectPage(new Page<>(page, size), q);
        Page<CouponVO> result = new Page<>(page, size);
        result.setTotal(ipage.getTotal());
        result.setRecords(ipage.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return result;
    }

    private CouponVO toVO(CouponTemplate t) {
        CouponVO vo = new CouponVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setMinAmount(t.getMinAmount());
        vo.setDiscountAmount(t.getDiscountAmount());
        vo.setDiscountRate(t.getDiscountRate());
        vo.setStatus(0);
        vo.setStartTime(t.getStartTime());
        vo.setEndTime(t.getEndTime());
        return vo;
    }

    @Override
    public List<CouponVO> listUserCoupons(Long userId, Integer status) {
        LambdaQueryWrapper<UserCoupon> q = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) q.eq(UserCoupon::getStatus, status);
        q.orderByDesc(UserCoupon::getCreatedAt);
        List<UserCoupon> ucs = userCouponMapper.selectList(q);
        if (ucs.isEmpty()) return Collections.emptyList();
        return buildUserCouponVOs(ucs);
    }

    @Override
    public Page<CouponVO> listUserCoupons(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<UserCoupon> q = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId);
        if (status != null) q.eq(UserCoupon::getStatus, status);
        q.orderByDesc(UserCoupon::getCreatedAt);
        IPage<UserCoupon> ipage = userCouponMapper.selectPage(new Page<>(page, size), q);
        Page<CouponVO> result = new Page<>(page, size);
        result.setTotal(ipage.getTotal());
        result.setRecords(ipage.getRecords().isEmpty()
                ? Collections.emptyList()
                : buildUserCouponVOs(ipage.getRecords()));
        return result;
    }

    private List<CouponVO> buildUserCouponVOs(List<UserCoupon> ucs) {
        Map<Long, CouponTemplate> templateMap = new HashMap<>();
        List<CouponVO> vos = new ArrayList<>();
        for (UserCoupon uc : ucs) {
            CouponTemplate t = templateMap.computeIfAbsent(uc.getTemplateId(), id -> templateMapper.selectById(id));
            CouponVO vo = new CouponVO();
            vo.setId(uc.getTemplateId());
            vo.setUserCouponId(uc.getId());
            vo.setName(t != null ? t.getName() : "");
            vo.setType(t != null ? t.getType() : "");
            vo.setMinAmount(t != null ? t.getMinAmount() : null);
            vo.setDiscountAmount(t != null ? t.getDiscountAmount() : null);
            vo.setDiscountRate(t != null ? t.getDiscountRate() : null);
            vo.setStatus(uc.getStatus());
            vo.setStartTime(t != null ? t.getStartTime() : null);
            vo.setEndTime(t != null ? t.getEndTime() : null);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public Map<String, Object> verify(Long userCouponId, Long userId, BigDecimal orderAmount) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        if (uc.getStatus() != 0) throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);

        CouponTemplate t = templateMapper.selectById(uc.getTemplateId());
        if (t == null || t.getStatus() == 0) throw new BusinessException(CouponErrorCode.COUPON_NOT_AVAILABLE);
        if (t.getEndTime() != null && t.getEndTime().isBefore(LocalDateTime.now())) throw new BusinessException(CouponErrorCode.COUPON_EXPIRED);

        BigDecimal minAmount = t.getMinAmount() != null ? t.getMinAmount() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minAmount) < 0) throw new BusinessException(CouponErrorCode.MIN_AMOUNT_NOT_MET);

        BigDecimal discount = calculateDiscount(t, orderAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("discount", discount);
        result.put("couponName", t.getName());
        result.put("templateId", t.getId());
        return result;
    }

    @Override
    @Transactional
    public void use(Long userCouponId, String orderNo) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || uc.getStatus() != 0) throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
        uc.setStatus(1);
        uc.setOrderNo(orderNo);
        uc.setUsedAt(LocalDateTime.now());
        userCouponMapper.updateById(uc);
    }

    private BigDecimal calculateDiscount(CouponTemplate t, BigDecimal orderAmount) {
        switch (t.getType()) {
            case "FLAT":
                return t.getDiscountAmount() != null ? t.getDiscountAmount() : BigDecimal.ZERO;
            case "DISCOUNT":
                BigDecimal rate = t.getDiscountRate() != null ? t.getDiscountRate() : BigDecimal.ONE;
                return orderAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            case "FULL_REDUCTION":
                return t.getDiscountAmount() != null ? t.getDiscountAmount() : BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }
}
