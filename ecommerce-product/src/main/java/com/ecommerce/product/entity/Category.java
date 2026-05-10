package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Category> children;
}
