package com.ecommerce.member.service;

import com.ecommerce.member.dto.response.MemberLevelVO;
import com.ecommerce.member.entity.MemberLevel;
import java.util.List;

public interface LevelService {

    /**
     * 检查并执行升级（成长值增加后调用）
     */
    void checkUpgrade(Long userId, Long currentGrowthValue);

    /**
     * 获取用户当前等级
     */
    MemberLevel getCurrentLevel(Long userId);

    /**
     * 获取所有等级列表（按 sortOrder 排序）
     */
    List<MemberLevelVO> getAllLevels();

    /**
     * 获取默认等级 (REGULAR)
     */
    MemberLevel getDefaultLevel();

    /**
     * 更新等级配置
     */
    void updateLevel(Long levelId, MemberLevelVO vo);
}
