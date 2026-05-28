package com.ecommerce.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxMessage> {
}
