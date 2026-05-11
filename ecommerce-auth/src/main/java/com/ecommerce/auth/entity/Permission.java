package com.ecommerce.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {
    private String name;
    private String code;
    private String type;
    private Long parentId;
    private String path;
    private String icon;
    private Integer sort;

    @TableField(exist = false)
    private List<Permission> children;
}
