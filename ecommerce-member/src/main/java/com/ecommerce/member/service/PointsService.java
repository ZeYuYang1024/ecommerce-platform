package com.ecommerce.member.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.member.dto.response.PointsTransactionVO;

public interface PointsService {

    /**
     * 发放积分，兼容一期发积分场景。
     */
    void earn(Long userId, Integer amount, String sourceType, String sourceId, String bizKey, String remark);

    /**
     * 发放积分，并记录关联的预占单或冲正来源。
     */
    void earn(Long userId, Integer amount, String sourceType, String sourceId, String bizKey,
              String remark, String relatedReservationNo, Long reversalOfTxId);

    /**
     * 消费积分。
     */
    void spend(Long userId, Integer amount, String sourceType, String sourceId, String bizKey,
               String remark, String relatedReservationNo);

    /**
     * 冲正已消费积分。
     */
    void reverseSpend(Long userId, Integer amount, String sourceType, String sourceId, String bizKey,
                      String remark, String relatedReservationNo, Long reversalOfTxId);

    /**
     * 积分流水列表。
     */
    IPage<PointsTransactionVO> getTransactions(Long userId, int page, int size);
}
