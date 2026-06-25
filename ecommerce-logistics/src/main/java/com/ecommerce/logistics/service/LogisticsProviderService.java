package com.ecommerce.logistics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;

import java.util.List;

public interface LogisticsProviderService {
    IPage<LogisticsProviderVO> listProviders(int page, int size);
    List<LogisticsProviderVO> listAllEnabled();
    LogisticsProviderVO getProvider(Long id);
    LogisticsProviderVO createProvider(LogisticsProviderVO vo);
    LogisticsProviderVO updateProvider(Long id, LogisticsProviderVO vo);
    void deleteProvider(Long id);
    void toggleStatus(Long id, Integer status);
}
