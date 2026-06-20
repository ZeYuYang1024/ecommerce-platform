package com.ecommerce.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.logistics.entity.ShippingOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShippingOrderItemMapper extends BaseMapper<ShippingOrderItem> {
}
