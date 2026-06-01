package com.ecommerce.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.member.dto.response.GrowthTransactionVO;

public interface GrowthService {

    /**
     * 增加成长值 (幂等，基于 bizKey)
     */
    void add(Long userId, Integer amount, String sourceType, String sourceId, String bizKey, String remark);

    /**
     * 成长值流水列表（分页）
     */
    IPage<GrowthTransactionVO> getTransactions(Long userId, int page, int size);
}
