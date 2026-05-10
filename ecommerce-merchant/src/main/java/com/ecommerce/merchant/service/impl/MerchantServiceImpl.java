package com.ecommerce.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.merchant.common.MerchantErrorCode;
import com.ecommerce.merchant.dto.request.MerchantAuditRequest;
import com.ecommerce.merchant.dto.request.MerchantRegisterRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;
import com.ecommerce.merchant.entity.Merchant;
import com.ecommerce.merchant.entity.MerchantAudit;
import com.ecommerce.merchant.mapper.MerchantAuditMapper;
import com.ecommerce.merchant.mapper.MerchantMapper;
import com.ecommerce.merchant.client.AuthClient;
import com.ecommerce.merchant.service.MerchantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final MerchantAuditMapper auditMapper;
    private final AuthClient authClient;

    public MerchantServiceImpl(MerchantMapper merchantMapper, MerchantAuditMapper auditMapper,
                                AuthClient authClient) {
        this.merchantMapper = merchantMapper;
        this.auditMapper = auditMapper;
        this.authClient = authClient;
    }

    @Override
    @Transactional
    public MerchantVO register(MerchantRegisterRequest request) {
        // 检查店铺名称是否已存在
        Long count = merchantMapper.selectCount(
                new LambdaQueryWrapper<Merchant>().eq(Merchant::getName, request.getName()));
        if (count > 0) {
            throw new BusinessException(MerchantErrorCode.MERCHANT_NAME_EXISTS);
        }

        Merchant merchant = new Merchant();
        merchant.setId(SnowflakeUtils.nextId());
        merchant.setName(request.getName());
        merchant.setLogo(request.getLogo());
        merchant.setContactName(request.getContactName());
        merchant.setContactPhone(request.getContactPhone());
        merchant.setBusinessLicense(request.getBusinessLicense());
        merchant.setStatus(0); // 待审核
        merchantMapper.insert(merchant);

        return toVO(merchant);
    }

    @Override
    public MerchantVO getById(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException(MerchantErrorCode.MERCHANT_NOT_FOUND);
        }
        return toVO(merchant);
    }

    @Override
    public List<MerchantVO> list(Integer status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Merchant::getStatus, status);
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);
        return merchantMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MerchantVO audit(Long id, MerchantAuditRequest request, Long auditorId) {
        if (request.getAction() < 1 || request.getAction() > 3) {
            throw new BusinessException(MerchantErrorCode.INVALID_AUDIT_ACTION);
        }

        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException(MerchantErrorCode.MERCHANT_NOT_FOUND);
        }
        if (merchant.getStatus() != 0) {
            throw new BusinessException(MerchantErrorCode.MERCHANT_NOT_PENDING);
        }

        // 更新商家状态
        merchant.setStatus(request.getAction());
        merchant.setReason(request.getComment());
        merchantMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Merchant>()
                        .eq("id", id)
                        .set("status", request.getAction())
                        .set("reason", request.getComment()));

        // 记录审核日志
        MerchantAudit audit = new MerchantAudit();
        audit.setId(SnowflakeUtils.nextId());
        audit.setMerchantId(id);
        audit.setAuditorId(auditorId);
        audit.setAction(request.getAction());
        audit.setComment(request.getComment());
        auditMapper.insert(audit);

        // 审核通过时自动创建商家管理员账号
        if (request.getAction() == 1) {
            try {
                com.ecommerce.common.dto.CreateMerchantAccountRequest req = new com.ecommerce.common.dto.CreateMerchantAccountRequest();
                req.setMerchantId(id);
                req.setMerchantName(merchant.getName());
                authClient.createMerchantAccount(req);
            } catch (Exception ignored) {}
        }

        return toVO(merchant);
    }

    private MerchantVO toVO(Merchant m) {
        MerchantVO vo = new MerchantVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setLogo(m.getLogo());
        vo.setContactName(m.getContactName());
        vo.setContactPhone(m.getContactPhone());
        vo.setBusinessLicense(m.getBusinessLicense());
        vo.setStatus(m.getStatus());
        vo.setStatusText(statusText(m.getStatus()));
        vo.setReason(m.getReason());
        vo.setCreatedAt(m.getCreatedAt());
        return vo;
    }

    private String statusText(Integer status) {
        if (status == null) return "未知";
        if (status == 0) return "待审核";
        if (status == 1) return "已通过";
        if (status == 2) return "已驳回";
        if (status == 3) return "已关停";
        return "未知";
    }
}
