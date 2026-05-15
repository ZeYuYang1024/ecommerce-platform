package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.CreateCategoryRequest;
import com.ecommerce.knowledge.dto.response.CategoryVO;
import com.ecommerce.knowledge.service.KbCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/knowledge/categories")
@RequiredArgsConstructor
public class KbCategoryController {

    private final KbCategoryService categoryService;

    @PostMapping
    public Result<CategoryVO> create(@RequestBody CreateCategoryRequest request) {
        return Result.ok(categoryService.create(request));
    }

    @GetMapping
    public Result<List<CategoryVO>> listAll() {
        return Result.ok(categoryService.listAll());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
