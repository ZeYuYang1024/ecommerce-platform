package com.ecommerce.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {}
