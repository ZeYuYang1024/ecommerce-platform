package com.ecommerce.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.payment.entity.DailySettlement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface DailySettlementMapper extends BaseMapper<DailySettlement> {
    @Insert("""
            INSERT INTO daily_settlement (id, settlement_date) VALUES (#{id}, #{date})
            ON DUPLICATE KEY UPDATE id = daily_settlement.id
            """)
    void ensureDay(@Param("id") Long id, @Param("date") LocalDate date);
}
