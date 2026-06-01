package com.ecommerce.member.service;

import com.ecommerce.member.dto.response.CheckInStatusVO;

public interface CheckInService {

    /**
     * 每日签到，返回签到结果
     */
    CheckInStatusVO checkIn(Long userId);

    /**
     * 查询今日签到状态
     */
    CheckInStatusVO getStatus(Long userId);
}
