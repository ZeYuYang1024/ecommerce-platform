package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.client.ReviewOrderClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.mapper.ReviewMapper;
import com.ecommerce.product.service.ProductService;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewMapper reviewMapper;
    private final ProductService productService;

    private final ReviewOrderClient orders;

    public ReviewController(ReviewMapper reviewMapper, ProductService productService, ReviewOrderClient orders) {
        this.reviewMapper = reviewMapper;
        this.productService = productService;
        this.orders = orders;
    }

    @GetMapping("/products/{spuId}/reviews")
    public Result<List<Review>> listBySpu(@PathVariable Long spuId) {
        return Result.ok(reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getSpuId, spuId)
                        .orderByDesc(Review::getCreatedAt)));
    }

    record ReviewRequest(@NotNull Long orderId, @NotNull @Min(1) @Max(5) Integer rating,
                         @Size(max = 1024) String content, @Size(max = 8192) String images) {}

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PostMapping("/products/{spuId}/reviews")
    public Result<Review> create(@PathVariable Long spuId,
                                  @RequestHeader("X-User-Id") Long userId,
                                  @Valid @RequestBody ReviewRequest request) {
        var eligibility = orders.reviewEligible(request.orderId(), userId, spuId);
        if (eligibility == null || eligibility.getCode() != 200 || eligibility.getData() == null) {
            return Result.fail(503, "暂时无法核实订单，请稍后重试");
        }
        if (!eligibility.getData()) throw new BusinessException(ProductErrorCode.REVIEW_ORDER_REQUIRED);
        if (reviewMapper.lockProduct(spuId) == null) throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        Review review = new Review();
        review.setId(SnowflakeUtils.nextId());
        review.setOrderId(request.orderId());
        review.setVerifiedOrderId(request.orderId());
        review.setRating(request.rating());
        review.setContent(request.content());
        review.setImages(request.images());
        review.setSpuId(spuId);
        review.setUserId(userId);
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException duplicate) {
            throw new BusinessException(ProductErrorCode.REVIEW_ALREADY_SUBMITTED);
        }
        reviewMapper.refreshRating(spuId);
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @DeleteMapping("/admin/reviews/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Review review = reviewMapper.selectById(id);
        if (review != null) deleteReview(review);
        return Result.ok();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
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
        deleteReview(review);
        return Result.ok();
    }
    private void deleteReview(Review review) {
        Long spuId = review.getSpuId();
        reviewMapper.lockProduct(spuId);
        if (reviewMapper.deleteById(review.getId()) > 0) reviewMapper.refreshRating(spuId);
    }
}
