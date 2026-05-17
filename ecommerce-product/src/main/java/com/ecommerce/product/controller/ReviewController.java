package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.mapper.ReviewMapper;
import com.ecommerce.product.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewMapper reviewMapper;
    private final ProductService productService;

    public ReviewController(ReviewMapper reviewMapper, ProductService productService) {
        this.reviewMapper = reviewMapper;
        this.productService = productService;
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

    @GetMapping("/admin/merchant/reviews")
    public Result<Page<Review>> listMerchant(@RequestHeader("X-Merchant-Id") Long merchantId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        List<Long> spuIds = productService.getSpuIdsByMerchant(merchantId);
        Page<Review> pageReq = new Page<>(page, size);
        if (spuIds.isEmpty()) {
            pageReq.setRecords(List.of());
            pageReq.setTotal(0);
            return Result.ok(pageReq);
        }
        return Result.ok(reviewMapper.selectPage(pageReq, new LambdaQueryWrapper<Review>()
                .in(Review::getSpuId, spuIds)
                .orderByDesc(Review::getCreatedAt)));
    }

    @DeleteMapping("/admin/reviews/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reviewMapper.deleteById(id);
        return Result.ok();
    }

    @DeleteMapping("/admin/merchant/reviews/{id}")
    public Result<Void> deleteMerchant(@PathVariable Long id,
                                       @RequestHeader("X-Merchant-Id") Long merchantId) {
        Review review = reviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException(ProductErrorCode.REVIEW_NOT_FOUND);
        }
        Spu spu = productService.getSpuById(review.getSpuId());
        if (!merchantId.equals(spu.getMerchantId())) {
            throw new BusinessException(ProductErrorCode.REVIEW_FORBIDDEN);
        }
        reviewMapper.deleteById(id);
        return Result.ok();
    }
}
