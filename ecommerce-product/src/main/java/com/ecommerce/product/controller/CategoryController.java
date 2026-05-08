package com.ecommerce.product.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.product.dto.response.CategoryVO;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {

    private final ProductService productService;

    public CategoryController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> tree() {
        List<Category> all = productService.categoryTree();
        List<CategoryVO> vos = all.stream().map(this::toVO).collect(Collectors.toList());
        return Result.ok(buildTree(vos));
    }

    @GetMapping("/admin/categories")
    public Result<List<CategoryVO>> all() {
        List<Category> all = productService.categoryTree();
        List<CategoryVO> vos = all.stream().map(this::toVO).collect(Collectors.toList());
        return Result.ok(vos);
    }

    @PostMapping("/admin/categories")
    public Result<CategoryVO> create(@RequestBody Category category) {
        if (category.getParentId() != null && category.getParentId() > 0) {
            category.setLevel(2);
        }
        return Result.ok(toVO(productService.createCategory(category)));
    }

    @PutMapping("/admin/categories/{id}")
    public Result<CategoryVO> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        return Result.ok(toVO(productService.updateCategory(category)));
    }

    @DeleteMapping("/admin/categories/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteCategory(id);
        return Result.ok();
    }

    private CategoryVO toVO(Category c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setParentId(c.getParentId());
        vo.setLevel(c.getLevel());
        vo.setSort(c.getSort());
        vo.setIcon(c.getIcon());
        return vo;
    }

    private List<CategoryVO> buildTree(List<CategoryVO> all) {
        Map<Long, CategoryVO> map = new HashMap<>();
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : all) {
            map.put(vo.getId(), vo);
        }
        for (CategoryVO vo : all) {
            Long pid = vo.getParentId();
            if (pid != null && pid > 0 && map.containsKey(pid)) {
                map.get(pid).addChild(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }
}
