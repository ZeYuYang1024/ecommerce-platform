package com.ecommerce.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
