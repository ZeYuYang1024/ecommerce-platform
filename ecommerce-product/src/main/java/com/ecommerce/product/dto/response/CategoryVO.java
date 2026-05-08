package com.ecommerce.product.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CategoryVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;
    private List<CategoryVO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public List<CategoryVO> getChildren() { return children; }
    public void setChildren(List<CategoryVO> children) { this.children = children; }

    public void addChild(CategoryVO child) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(child);
    }
}
