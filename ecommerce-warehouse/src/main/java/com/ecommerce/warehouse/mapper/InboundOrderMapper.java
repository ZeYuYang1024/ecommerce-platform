package com.ecommerce.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.warehouse.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {
}
