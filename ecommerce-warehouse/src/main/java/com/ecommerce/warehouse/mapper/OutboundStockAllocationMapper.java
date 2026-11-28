package com.ecommerce.warehouse.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OutboundStockAllocationMapper {
    record Allocation(Long stockId, Long binId, int quantity) {}

    @Insert("""
            INSERT INTO outbound_stock_allocation (outbound_item_id, stock_id, quantity)
            VALUES (#{itemId}, #{stockId}, #{quantity})
            """)
    void insert(@Param("itemId") Long itemId, @Param("stockId") Long stockId, @Param("quantity") int quantity);

    @Select("""
            SELECT a.stock_id AS stockId, s.bin_id AS binId, a.quantity
            FROM outbound_stock_allocation a JOIN physical_stock s ON s.id = a.stock_id
            WHERE a.outbound_item_id = #{itemId} ORDER BY a.stock_id
            """)
    List<Allocation> findByItem(Long itemId);
}
