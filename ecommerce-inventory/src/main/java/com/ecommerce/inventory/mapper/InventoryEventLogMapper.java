package com.ecommerce.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.inventory.entity.InventoryEventLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryEventLogMapper extends BaseMapper<InventoryEventLog> {

    @Insert("""
            INSERT INTO inventory_order_lock (order_no) VALUES (#{orderNo})
            ON DUPLICATE KEY UPDATE order_no = inventory_order_lock.order_no
            """)
    void lockOrder(@Param("orderNo") String orderNo);

    @Select("""
            SELECT COUNT(*) > 0 FROM inventory_event_log
            WHERE order_no = #{orderNo} AND topic = #{topic} AND status = 1 AND deleted = 0
            """)
    boolean wasProcessed(@Param("orderNo") String orderNo, @Param("topic") String topic);

    @Update("""
            UPDATE inventory_event_log
            SET status = 1
            WHERE id = #{id}
            """)
    int markProcessed(@Param("id") Long id);

    @Update("""
            UPDATE inventory_event_log
            SET status = 2
            WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id);
}
