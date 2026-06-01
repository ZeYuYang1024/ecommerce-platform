package com.ecommerce.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.member.dto.response.PointsTransactionVO;
import java.time.LocalDateTime;

public interface PointsService {

    /**
     * 发放积分 (幂等，基于 bizKey)
     * @param userId      用户ID
     * @param amount      积分数
     * @param sourceType  来源类型 ORDER/CHECKIN/REVIEW/CAMPAIGN
     * @param sourceId    来源业务ID
     * @param bizKey      幂等业务键
     * @param remark      备注
     */
    void earn(Long userId, Integer amount, String sourceType, String sourceId, String bizKey, String remark);

    /**
     * 积分流水列表（分页）
     */
    IPage<PointsTransactionVO> getTransactions(Long userId, int page, int size);
}
