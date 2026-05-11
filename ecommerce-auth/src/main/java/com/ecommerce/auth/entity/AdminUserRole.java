package com.ecommerce.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user_role")
public class AdminUserRole extends BaseEntity {
    private Long adminUserId;
    private Long roleId;
}
