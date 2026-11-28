package com.ecommerce.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.logistics.entity.ShippingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShippingOrderMapper extends BaseMapper<ShippingOrder> {
    @Select("SELECT * FROM shipping_order WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    ShippingOrder lockById(Long id);
}
