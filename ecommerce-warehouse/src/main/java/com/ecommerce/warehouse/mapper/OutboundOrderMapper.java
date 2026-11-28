package com.ecommerce.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.warehouse.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {
    @Select("SELECT * FROM outbound_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    OutboundOrder lockById(Long id);

    @Select("SELECT * FROM outbound_order WHERE shipping_id = #{shippingId} AND deleted = 0 FOR UPDATE")
    OutboundOrder lockByShippingId(Long shippingId);
}
