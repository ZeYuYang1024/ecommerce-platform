package com.ecommerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.inventory.entity.InventoryEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryEventLogMapper extends BaseMapper<InventoryEventLog> {

    @Update("""
            UPDATE inventory_event_log
            SET status = 1
            WHERE id = #{id}
            """)
    int markProcessed(@Param("id") Long id);
}
