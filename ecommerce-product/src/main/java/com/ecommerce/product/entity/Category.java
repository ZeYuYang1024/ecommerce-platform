package com.ecommerce.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@TableName("category")
public class Category extends BaseEntity {
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
