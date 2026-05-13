package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.mapper.ReviewMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewMapper reviewMapper;

    public ReviewController(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    @GetMapping("/products/{spuId}/reviews")
    public Result<List<Review>> listBySpu(@PathVariable Long spuId) {
        return Result.ok(reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getSpuId, spuId)
                        .orderByDesc(Review::getCreatedAt)));
    }

    @PostMapping("/products/{spuId}/reviews")
    public Result<Review> create(@PathVariable Long spuId,
                                  @RequestHeader("X-User-Id") Long userId,
                                  @RequestBody Review review) {
        review.setId(SnowflakeUtils.nextId());
        review.setSpuId(spuId);
        review.setUserId(userId);
        reviewMapper.insert(review);
        return Result.ok(review);
    }

    @GetMapping("/admin/reviews")
    public Result<Page<Review>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Review> pageReq = new Page<>(page, size);
        return Result.ok(reviewMapper.selectPage(pageReq,
                new LambdaQueryWrapper<Review>().orderByDesc(Review::getCreatedAt)));
    }

    @DeleteMapping("/admin/reviews/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewMapper.deleteById(id);
        return Result.ok();
    }
}
