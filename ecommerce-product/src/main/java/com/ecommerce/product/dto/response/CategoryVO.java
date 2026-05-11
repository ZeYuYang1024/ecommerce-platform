package com.ecommerce.product.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private String icon;
    private List<CategoryVO> children;

    public void addChild(CategoryVO child) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(child);
    }
}
