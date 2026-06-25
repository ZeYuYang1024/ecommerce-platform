package com.ecommerce.logistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.logistics.entity.ShippingOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShippingOrderMapper extends BaseMapper<ShippingOrder> {
}
