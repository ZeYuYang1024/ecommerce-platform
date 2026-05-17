package com.ecommerce.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateStatusRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.dto.response.SkuVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.service.BrandService;
import com.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock private ProductService productService;
    @Mock private BrandService brandService;
    @InjectMocks private ProductController controller;

    private SpuVO spuVO;
    private ProductDetailVO detailVO;

    @BeforeEach
    void setUp() {
        spuVO = new SpuVO();
        spuVO.setId(1L);
        spuVO.setName("测试商品");
        spuVO.setCategoryId(10L);
        spuVO.setStatus(1);
        spuVO.setDescription("描述");
        spuVO.setMainImage("img.jpg");
        spuVO.setAvgRating(new BigDecimal("4.5"));
        spuVO.setReviewCount(10);
        spuVO.setCreatedAt(LocalDateTime.now());

        SkuVO skuVO = new SkuVO();
        skuVO.setId(100L);
        skuVO.setSpuId(1L);
        skuVO.setName("SKU1");
        skuVO.setPrice(new BigDecimal("99.00"));

        detailVO = new ProductDetailVO();
        detailVO.setSpu(spuVO);
        detailVO.setSkus(Collections.singletonList(skuVO));
    }

    @Nested
    class PublicEndpoints {

        @Test
        void list_shouldReturnPagedProducts() {
            Page<com.ecommerce.product.entity.Spu> spuPage = new Page<>(1, 10);
            Spu spu = new Spu();
            spu.setId(1L);
            spu.setName("测试商品");
            spuPage.setRecords(Collections.singletonList(spu));
            spuPage.setTotal(1);

            when(productService.spuPage(eq(1), eq(10), isNull(), eq(1), isNull())).thenReturn(spuPage);
            when(productService.toSpuVO(any(Spu.class))).thenReturn(spuVO);

            Result<Page<SpuVO>> result = controller.list(1, 10, null, null);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getTotal()).isEqualTo(1);
            assertThat(result.getData().getRecords().get(0).getName()).isEqualTo("测试商品");
        }

        @Test
        void list_shouldFilterByCategoryAndKeyword() {
            Page<Spu> spuPage = new Page<>(1, 5);
            spuPage.setRecords(Collections.emptyList());
            when(productService.spuPage(eq(1), eq(5), eq(10L), eq(1), eq("手机")))
                    .thenReturn(spuPage);

            Result<Page<SpuVO>> result = controller.list(1, 5, 10L, "手机");

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getRecords()).isEmpty();
        }

        @Test
        void detail_shouldReturnProductDetail() {
            when(productService.getProductDetail(1L)).thenReturn(detailVO);

            Result<ProductDetailVO> result = controller.detail(1L);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getSpu().getName()).isEqualTo("测试商品");
            assertThat(result.getData().getSkus()).hasSize(1);
        }
    }

    @Nested
    class AdminEndpoints {

        @Test
        void adminList_shouldReturnPagedProducts() {
            Page<Spu> spuPage = new Page<>(1, 10);
            Spu spu = new Spu();
            spu.setId(2L);
            spu.setName("管理端商品");
            spuPage.setRecords(Collections.singletonList(spu));
            spuPage.setTotal(1);

            when(productService.spuPage(eq(1), eq(10), isNull(), isNull(), isNull())).thenReturn(spuPage);
            SpuVO adminSpuVO = new SpuVO();
            adminSpuVO.setId(2L);
            adminSpuVO.setName("管理端商品");
            when(productService.toSpuVO(any(Spu.class))).thenReturn(adminSpuVO);

            Result<Page<SpuVO>> result = controller.adminList(1, 10, null, null, null, "super_admin", null);

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getRecords().get(0).getName()).isEqualTo("管理端商品");
        }

        @Test
        void adminList_shouldFilterByStatus() {
            Page<Spu> spuPage = new Page<>(1, 10);
            spuPage.setRecords(Collections.emptyList());
            when(productService.spuPage(eq(1), eq(10), isNull(), eq(0), isNull())).thenReturn(spuPage);

            Result<Page<SpuVO>> result = controller.adminList(1, 10, null, 0, null, "super_admin", null);

            assertThat(result.getCode()).isEqualTo(200);
        }

        @Test
        void merchantList_shouldDelegateToMerchantScopedService() {
            Page<Spu> spuPage = new Page<>(1, 10);
            spuPage.setRecords(Collections.emptyList());
            when(productService.spuPageByMerchant(eq(1), eq(10), isNull(), isNull(), isNull(), eq(88L)))
                    .thenReturn(spuPage);

            Result<Page<SpuVO>> result = controller.merchantList(1, 10, null, null, null, 88L);

            assertThat(result.getCode()).isEqualTo(200);
            verify(productService).spuPageByMerchant(1, 10, null, null, null, 88L);
            verify(productService, never()).spuPage(eq(1), eq(10), isNull(), isNull(), isNull());
        }

        @Test
        void merchantDetail_shouldRejectCrossTenantAccess() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(3002L);
            when(productService.getSpuById(1L)).thenReturn(existing);

            assertThatThrownBy(() -> controller.merchantDetail(1L, 2001L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void create_shouldCreateProduct() {
            CreateProductRequest req = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("新品");
            req.setSpu(spuReq);

            Spu created = new Spu();
            created.setId(99L);
            created.setName("新品");
            when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(created);

            Result<Spu> result = controller.create(req, null, "super_admin");

            assertThat(result.getCode()).isEqualTo(200);
            assertThat(result.getData().getName()).isEqualTo("新品");
        }

        @Test
        void updateStatus_shouldSucceed() {
            doNothing().when(productService).updateStatus(eq(1L), eq(0));

            Result<Void> result = controller.updateStatus(1L,
                    new UpdateStatusRequest() {{ setStatus(0); }}, "super_admin", null);

            assertThat(result.getCode()).isEqualTo(200);
            verify(productService).updateStatus(1L, 0);
        }

        @Test
        void delete_shouldSucceed() {
            doNothing().when(productService).deleteSpu(1L);

            Result<Void> result = controller.delete(1L, "super_admin", null);

            assertThat(result.getCode()).isEqualTo(200);
            verify(productService).deleteSpu(1L);
        }

        @Test
        void create_shouldAssignMerchantIdForMerchantAdmin() {
            CreateProductRequest req = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("新品");
            req.setSpu(spuReq);

            Spu created = new Spu();
            created.setId(99L);
            created.setName("鏂板搧");
            when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(created);
            when(productService.updateSpu(any(Spu.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Result<Spu> result = controller.create(req, 88L, "merchant");

            assertThat(result.getData().getMerchantId()).isEqualTo(88L);
            verify(productService).updateSpu(argThat(spu -> Long.valueOf(88L).equals(spu.getMerchantId())));
        }

        @Test
        void create_shouldValidateMerchantBrandBeforeCreate() {
            CreateProductRequest req = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("品牌商品");
            spuReq.setBrandId(3001L);
            req.setSpu(spuReq);

            Spu created = new Spu();
            created.setId(101L);
            when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(created);
            when(productService.updateSpu(any(Spu.class))).thenAnswer(invocation -> invocation.getArgument(0));

            controller.create(req, 88L, "merchant");

            verify(brandService).validateMerchantBrandSelectable(88L, 3001L);
        }

        @Test
        void create_shouldRejectPendingMerchantBrand() {
            CreateProductRequest req = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("待审品牌商品");
            spuReq.setBrandId(3001L);
            req.setSpu(spuReq);

            doThrow(new BusinessException(ProductErrorCode.PRODUCT_FORBIDDEN))
                    .when(brandService).validateMerchantBrandSelectable(88L, 3001L);

            assertThatThrownBy(() -> controller.create(req, 88L, "merchant"))
                    .isInstanceOf(BusinessException.class);

            verify(productService, never()).createProduct(any(CreateProductRequest.class));
        }

        @Test
        void update_shouldRejectMerchantCrossTenantModification() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(99L);
            when(productService.getSpuById(1L)).thenReturn(existing);

            Spu incoming = new Spu();
            incoming.setName("hack");

            assertThatThrownBy(() -> controller.update(1L, incoming, "merchant", 88L))
                    .isInstanceOf(BusinessException.class);

            verify(productService, never()).updateSpu(any(Spu.class));
        }

        @Test
        void update_shouldPreserveMerchantOwnershipForMerchantAdmin() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(88L);
            when(productService.getSpuById(1L)).thenReturn(existing);
            when(productService.updateSpu(any(Spu.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Spu incoming = new Spu();
            incoming.setMerchantId(999L);
            incoming.setName("updated");

            Result<Spu> result = controller.update(1L, incoming, "merchant", 88L);

            assertThat(result.getData().getMerchantId()).isEqualTo(88L);
            verify(productService).updateSpu(argThat(spu -> Long.valueOf(88L).equals(spu.getMerchantId())));
        }

        @Test
        void update_shouldValidateMerchantBrandSelectionForMerchantAdmin() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(88L);
            when(productService.getSpuById(1L)).thenReturn(existing);

            Spu incoming = new Spu();
            incoming.setName("updated");
            incoming.setBrandId(3001L);

            doThrow(new BusinessException(ProductErrorCode.BRAND_FORBIDDEN))
                    .when(brandService).validateMerchantBrandSelectable(88L, 3001L);

            assertThatThrownBy(() -> controller.update(1L, incoming, "merchant", 88L))
                    .isInstanceOf(BusinessException.class);

            verify(productService, never()).updateSpu(any(Spu.class));
        }

        @Test
        void updateStatus_shouldRejectMerchantCrossTenantModification() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(99L);
            when(productService.getSpuById(1L)).thenReturn(existing);

            assertThatThrownBy(() -> controller.updateStatus(1L,
                    new UpdateStatusRequest() {{ setStatus(0); }}, "merchant", 88L))
                    .isInstanceOf(BusinessException.class);

            verify(productService, never()).updateStatus(anyLong(), anyInt());
        }

        @Test
        void delete_shouldRejectMerchantCrossTenantModification() {
            Spu existing = new Spu();
            existing.setId(1L);
            existing.setMerchantId(99L);
            when(productService.getSpuById(1L)).thenReturn(existing);

            assertThatThrownBy(() -> controller.delete(1L, "merchant", 88L))
                    .isInstanceOf(BusinessException.class);

            verify(productService, never()).deleteSpu(anyLong());
        }
    }
}
