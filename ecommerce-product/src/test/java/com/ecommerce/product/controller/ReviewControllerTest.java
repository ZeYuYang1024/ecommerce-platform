package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.entity.Review;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.mapper.ReviewMapper;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ReviewController controller;

    @Test
    void merchantListShouldOnlyQueryCurrentMerchantSpuReviews() {
        Page<Review> page = new Page<>(1, 10);
        Review review = new Review();
        review.setId(9001L);
        review.setSpuId(101L);
        page.setRecords(List.of(review));
        when(productService.getSpuIdsByMerchant(2001L)).thenReturn(List.of(101L, 102L));
        when(reviewMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Result<Page<Review>> result = controller.listMerchant(2001L, 1, 10);

        assertThat(result.getData().getRecords()).hasSize(1);
        verify(productService).getSpuIdsByMerchant(2001L);
        verify(reviewMapper).selectPage(any(Page.class), any());
    }

    @Test
    void merchantDeleteShouldRejectCrossTenantReview() {
        Review review = new Review();
        review.setId(9001L);
        review.setSpuId(101L);
        when(reviewMapper.selectById(9001L)).thenReturn(review);

        Spu spu = new Spu();
        spu.setId(101L);
        spu.setMerchantId(3002L);
        when(productService.getSpuById(101L)).thenReturn(spu);

        assertThatThrownBy(() -> controller.deleteMerchant(9001L, 2001L))
                .isInstanceOf(BusinessException.class);

        verify(reviewMapper, never()).deleteById(9001L);
    }
}
