package com.ecommerce.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;
import com.ecommerce.logistics.entity.LogisticsProvider;
import com.ecommerce.logistics.mapper.LogisticsProviderMapper;
import com.ecommerce.logistics.service.LogisticsProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogisticsProviderServiceImpl implements LogisticsProviderService {

    private final LogisticsProviderMapper providerMapper;

    @Override
    public IPage<LogisticsProviderVO> listProviders(int page, int size) {
        Page<LogisticsProvider> p = new Page<>(page, size);
        IPage<LogisticsProvider> result = providerMapper.selectPage(p,
                new LambdaQueryWrapper<LogisticsProvider>().orderByAsc(LogisticsProvider::getPriority));
        return result.convert(this::toVO);
    }

    @Override
    public List<LogisticsProviderVO> listAllEnabled() {
        List<LogisticsProvider> list = providerMapper.selectList(
                new LambdaQueryWrapper<LogisticsProvider>()
                        .eq(LogisticsProvider::getStatus, 1)
                        .orderByAsc(LogisticsProvider::getPriority));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public LogisticsProviderVO getProvider(Long id) {
        LogisticsProvider entity = providerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public LogisticsProviderVO createProvider(LogisticsProviderVO vo) {
        if (providerMapper.exists(new LambdaQueryWrapper<LogisticsProvider>()
                .eq(LogisticsProvider::getProviderCode, vo.getProviderCode()))) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_CODE_EXISTS);
        }
        LogisticsProvider entity = new LogisticsProvider();
        entity.setProviderCode(vo.getProviderCode());
        entity.setProviderName(vo.getProviderName());
        entity.setProviderLogo(vo.getProviderLogo());
        entity.setCustomerAccount(vo.getCustomerAccount());
        entity.setSupportWaybill(vo.getSupportWaybill() != null ? vo.getSupportWaybill() : 0);
        entity.setStatus(vo.getStatus() != null ? vo.getStatus() : 1);
        entity.setPriority(vo.getPriority() != null ? vo.getPriority() : 99);
        providerMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public LogisticsProviderVO updateProvider(Long id, LogisticsProviderVO vo) {
        LogisticsProvider entity = providerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_NOT_FOUND);
        }
        if (vo.getProviderName() != null) entity.setProviderName(vo.getProviderName());
        if (vo.getProviderLogo() != null) entity.setProviderLogo(vo.getProviderLogo());
        if (vo.getCustomerAccount() != null) entity.setCustomerAccount(vo.getCustomerAccount());
        if (vo.getSupportWaybill() != null) entity.setSupportWaybill(vo.getSupportWaybill());
        if (vo.getPriority() != null) entity.setPriority(vo.getPriority());
        providerMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void deleteProvider(Long id) {
        if (providerMapper.selectById(id) == null) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_NOT_FOUND);
        }
        providerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        LogisticsProvider entity = providerMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(LogisticsErrorCode.PROVIDER_NOT_FOUND);
        }
        entity.setStatus(status);
        providerMapper.updateById(entity);
    }

    private LogisticsProviderVO toVO(LogisticsProvider entity) {
        LogisticsProviderVO vo = new LogisticsProviderVO();
        vo.setId(entity.getId());
        vo.setProviderCode(entity.getProviderCode());
        vo.setProviderName(entity.getProviderName());
        vo.setProviderLogo(entity.getProviderLogo());
        vo.setSupportWaybill(entity.getSupportWaybill());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(entity.getStatus() == 1 ? "启用" : "停用");
        vo.setPriority(entity.getPriority());
        return vo;
    }
}
