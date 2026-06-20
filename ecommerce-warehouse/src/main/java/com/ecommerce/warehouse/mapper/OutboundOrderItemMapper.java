package com.ecommerce.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.warehouse.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboundOrderItemMapper extends BaseMapper<OutboundOrderItem> {
}
