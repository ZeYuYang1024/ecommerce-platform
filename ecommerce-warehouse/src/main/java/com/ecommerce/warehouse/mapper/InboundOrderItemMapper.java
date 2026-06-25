package com.ecommerce.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.warehouse.entity.InboundOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InboundOrderItemMapper extends BaseMapper<InboundOrderItem> {
}
