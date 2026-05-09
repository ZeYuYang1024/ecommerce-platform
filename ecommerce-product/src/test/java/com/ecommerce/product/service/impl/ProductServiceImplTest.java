package com.ecommerce.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.response.ProductDetailVO;
import com.ecommerce.product.dto.response.SpuVO;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Sku;
import com.ecommerce.product.entity.Spu;
import com.ecommerce.product.mapper.CategoryMapper;
import com.ecommerce.product.mapper.SkuMapper;
import com.ecommerce.product.mapper.SpuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock private SpuMapper spuMapper;
    @Mock private SkuMapper skuMapper;
    @Mock private CategoryMapper categoryMapper;
    @InjectMocks private ProductServiceImpl productService;

    private Spu spu;
    private Sku sku;

    @BeforeEach
    void setUp() {
        spu = new Spu();
        spu.setId(1L);
        spu.setName("测试商品");
        spu.setCategoryId(10L);
        spu.setBrandId(20L);
        spu.setDescription("商品描述");
        spu.setMainImage("main.jpg");
        spu.setImages("[\"img1.jpg\",\"img2.jpg\"]");
        spu.setDetail("<p>详情</p>");
        spu.setStatus(1);
        spu.setAvgRating(new BigDecimal("4.5"));
        spu.setReviewCount(100);
        spu.setCreatedAt(java.time.LocalDateTime.now());

        sku = new Sku();
        sku.setId(100L);
        sku.setSpuId(1L);
        sku.setName("测试SKU");
        sku.setSpec("{\"color\":\"red\"}");
        sku.setPrice(new BigDecimal("99.00"));
        sku.setOriginalPrice(new BigDecimal("129.00"));
        sku.setImage("sku.jpg");
    }

    @Nested
    class SpuTests {

        @Test
        void spuPage_shouldReturnPagedResults() {
            Page<Spu> page = new Page<>(1, 10);
            page.setRecords(Collections.singletonList(spu));
            page.setTotal(1);
            when(spuMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Page<Spu> result = productService.spuPage(1, 10, null, null, null);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
            verify(spuMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        void spuPage_shouldFilterByCategoryAndStatus() {
            when(spuMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            productService.spuPage(1, 10, 10L, 1, null);

            verify(spuMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        void getSpuById_shouldReturnSpu_whenFound() {
            when(spuMapper.selectById(1L)).thenReturn(spu);

            Spu result = productService.getSpuById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("测试商品");
        }

        @Test
        void getSpuById_shouldThrow_whenNotFound() {
            when(spuMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> productService.getSpuById(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.getCode());
        }

        @Test
        void getProductDetail_shouldReturnDetailWithSkus() {
            when(spuMapper.selectById(1L)).thenReturn(spu);
            when(skuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(sku));

            ProductDetailVO result = productService.getProductDetail(1L);

            assertThat(result.getSpu()).isNotNull();
            assertThat(result.getSpu().getName()).isEqualTo("测试商品");
            assertThat(result.getSkus()).hasSize(1);
            assertThat(result.getSkus().get(0).getName()).isEqualTo("测试SKU");
        }

        @Test
        void createProduct_shouldInsertSpuAndSkus() {
            CreateProductRequest request = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("新品");
            spuReq.setCategoryId(1L);
            request.setSpu(spuReq);

            CreateProductRequest.SkuRequest skuReq = new CreateProductRequest.SkuRequest();
            skuReq.setName("新SKU");
            skuReq.setPrice("50.00");
            request.setSkus(Collections.singletonList(skuReq));

            when(spuMapper.insert(any(Spu.class))).thenReturn(1);
            when(skuMapper.insert(any(Sku.class))).thenReturn(1);

            Spu result = productService.createProduct(request);

            assertThat(result.getName()).isEqualTo("新品");
            verify(spuMapper).insert(any(Spu.class));
            verify(skuMapper).insert(any(Sku.class));
        }

        @Test
        void createProduct_shouldWorkWithoutSkus() {
            CreateProductRequest request = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("无SKU商品");
            request.setSpu(spuReq);

            when(spuMapper.insert(any(Spu.class))).thenReturn(1);

            Spu result = productService.createProduct(request);

            assertThat(result.getName()).isEqualTo("无SKU商品");
            verify(skuMapper, never()).insert(any(Sku.class));
        }

        @Test
        void updateSpu_shouldUpdate() {
            when(spuMapper.updateById(any(Spu.class))).thenReturn(1);

            Spu result = productService.updateSpu(spu);

            assertThat(result.getId()).isEqualTo(1L);
            verify(spuMapper).updateById(spu);
        }

        @Test
        void updateStatus_shouldUpdateStatus() {
            when(spuMapper.selectById(1L)).thenReturn(spu);
            when(spuMapper.updateById(any(Spu.class))).thenReturn(1);

            productService.updateStatus(1L, 0);

            ArgumentCaptor<Spu> captor = ArgumentCaptor.forClass(Spu.class);
            verify(spuMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(0);
        }

        @Test
        void updateStatus_shouldThrow_whenSpuNotFound() {
            when(spuMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> productService.updateStatus(999L, 0))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void deleteSpu_shouldDelete() {
            when(spuMapper.deleteById(1L)).thenReturn(1);

            productService.deleteSpu(1L);

            verify(spuMapper).deleteById(1L);
        }

        @Test
        void toSpuVO_shouldMapAllFields() {
            SpuVO vo = productService.toSpuVO(spu);

            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getName()).isEqualTo("测试商品");
            assertThat(vo.getCategoryId()).isEqualTo(10L);
            assertThat(vo.getBrandId()).isEqualTo(20L);
            assertThat(vo.getDescription()).isEqualTo("商品描述");
            assertThat(vo.getMainImage()).isEqualTo("main.jpg");
            assertThat(vo.getImages()).isEqualTo("[\"img1.jpg\",\"img2.jpg\"]");
            assertThat(vo.getDetail()).isEqualTo("<p>详情</p>");
            assertThat(vo.getStatus()).isEqualTo(1);
            assertThat(vo.getAvgRating()).isEqualTo(new BigDecimal("4.5"));
            assertThat(vo.getReviewCount()).isEqualTo(100);
        }
    }

    @Nested
    class CategoryTests {

        @Test
        void categoryTree_shouldReturnRootsOnly() {
            Category root1 = new Category();
            root1.setId(1L); root1.setName("服装"); root1.setParentId(0L); root1.setSort(1);
            Category root2 = new Category();
            root2.setId(2L); root2.setName("电子"); root2.setParentId(0L); root2.setSort(2);
            Category child = new Category();
            child.setId(3L); child.setName("手机"); child.setParentId(2L); child.setSort(1);

            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(root1, root2, child));

            List<Category> result = productService.categoryTree();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(Category::getName).containsExactly("服装", "电子");
        }

        @Test
        void categoryTree_shouldReturnEmpty_whenNoData() {
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<Category> result = productService.categoryTree();

            assertThat(result).isEmpty();
        }

        @Test
        void createCategory_shouldInsert() {
            Category cat = new Category();
            cat.setName("新分类");
            when(categoryMapper.insert(any(Category.class))).thenReturn(1);

            Category result = productService.createCategory(cat);

            assertThat(result.getName()).isEqualTo("新分类");
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getSort()).isEqualTo(0);
            verify(categoryMapper).insert(cat);
        }

        @Test
        void updateCategory_shouldUpdate() {
            Category cat = new Category();
            cat.setId(1L); cat.setName("更新分类");
            when(categoryMapper.updateById(cat)).thenReturn(1);

            Category result = productService.updateCategory(cat);

            assertThat(result.getName()).isEqualTo("更新分类");
        }

        @Test
        void deleteCategory_shouldDelete() {
            when(categoryMapper.deleteById(1L)).thenReturn(1);

            productService.deleteCategory(1L);

            verify(categoryMapper).deleteById(1L);
        }
    }

    @Nested
    class SkuTests {

        @Test
        void getSkusBySpuId_shouldReturnSkus() {
            when(skuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(sku));

            List<Sku> result = productService.getSkusBySpuId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSpuId()).isEqualTo(1L);
        }

        @Test
        void getSkusBySpuId_shouldReturnEmpty_whenNoSkus() {
            when(skuMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<Sku> result = productService.getSkusBySpuId(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class BoundaryTests {

        @Test
        void spuPage_shouldHandleEmptyKeyword() {
            when(spuMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            // empty string should not cause issues
            assertThatCode(() -> productService.spuPage(1, 10, null, 1, ""))
                    .doesNotThrowAnyException();
        }

        @Test
        void spuPage_shouldHandleSpecialCharsInKeyword() {
            when(spuMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 10));

            assertThatCode(() -> productService.spuPage(1, 10, null, 1, "测试%_\\"))
                    .doesNotThrowAnyException();
        }

        @Test
        void spuPage_shouldHandleBoundaryPagination() {
            when(spuMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(new Page<>(1, 100));

            // large page size boundary
            Page<Spu> result = productService.spuPage(1, 100, null, null, null);

            assertThat(result).isNotNull();
        }

        @Test
        void createProduct_shouldHandleEmptySkuList() {
            CreateProductRequest request = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("商品");
            request.setSpu(spuReq);
            request.setSkus(Collections.emptyList());

            when(spuMapper.insert(any(Spu.class))).thenReturn(1);

            Spu result = productService.createProduct(request);

            assertThat(result.getName()).isEqualTo("商品");
            verify(skuMapper, never()).insert(any(Sku.class));
        }

        @Test
        void createProduct_shouldHandleSkuWithNullPrice() {
            CreateProductRequest request = new CreateProductRequest();
            CreateProductRequest.SpuRequest spuReq = new CreateProductRequest.SpuRequest();
            spuReq.setName("商品");
            request.setSpu(spuReq);

            CreateProductRequest.SkuRequest skuReq = new CreateProductRequest.SkuRequest();
            skuReq.setName("免费SKU");
            skuReq.setPrice(null);
            request.setSkus(Collections.singletonList(skuReq));

            when(spuMapper.insert(any(Spu.class))).thenReturn(1);
            when(skuMapper.insert(any(Sku.class))).thenReturn(1);

            assertThatCode(() -> productService.createProduct(request)).doesNotThrowAnyException();
        }

        @Test
        void toSpuVO_shouldHandleNullFields() {
            Spu minimal = new Spu();
            minimal.setId(1L);
            minimal.setName(null);
            minimal.setCategoryId(null);
            minimal.setAvgRating(null);
            minimal.setReviewCount(null);
            minimal.setImages(null);
            minimal.setDetail(null);

            SpuVO vo = productService.toSpuVO(minimal);

            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getName()).isNull();
            assertThat(vo.getCategoryId()).isNull();
            assertThat(vo.getAvgRating()).isNull();
            assertThat(vo.getReviewCount()).isNull();
            assertThat(vo.getImages()).isNull();
            assertThat(vo.getDetail()).isNull();
        }

        @Test
        void toSpuVO_shouldNotLeakDeletedField() {
            SpuVO vo = productService.toSpuVO(spu);

            // verify SpuVO does NOT expose the BaseEntity.deleted field
            assertThat(vo.getClass().getDeclaredFields())
                    .noneMatch(f -> f.getName().equals("deleted"));
        }

        @Test
        void categoryTree_shouldHandleSingleRootNoChildren() {
            Category root = new Category();
            root.setId(1L); root.setName("唯一分类"); root.setParentId(0L); root.setSort(1);
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(root));

            List<Category> result = productService.categoryTree();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("唯一分类");
        }

        @Test
        void categoryTree_shouldHandleNullParentId() {
            Category root = new Category();
            root.setId(1L); root.setName("分类"); root.setParentId(null); root.setSort(1);
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(root));

            List<Category> result = productService.categoryTree();

            assertThat(result).hasSize(1);
        }

        @Test
        void createCategory_shouldDefaultLevelAndSort_whenNull() {
            Category cat = new Category();
            cat.setName("新分类");
            cat.setLevel(null);
            cat.setSort(null);
            when(categoryMapper.insert(any(Category.class))).thenReturn(1);

            Category result = productService.createCategory(cat);

            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getSort()).isEqualTo(0);
        }

        @Test
        void createCategory_shouldKeepExistingLevelAndSort() {
            Category cat = new Category();
            cat.setName("二级分类");
            cat.setLevel(2);
            cat.setSort(5);
            when(categoryMapper.insert(any(Category.class))).thenReturn(1);

            Category result = productService.createCategory(cat);

            assertThat(result.getLevel()).isEqualTo(2);
            assertThat(result.getSort()).isEqualTo(5);
        }

        @Test
        void updateStatus_shouldWork_whenAlreadySameStatus() {
            spu.setStatus(0);
            when(spuMapper.selectById(1L)).thenReturn(spu);
            when(spuMapper.updateById(any(Spu.class))).thenReturn(1);

            assertThatCode(() -> productService.updateStatus(1L, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        void updateStatus_shouldHandleBoundaryStatusValues() {
            when(spuMapper.selectById(1L)).thenReturn(spu);
            when(spuMapper.updateById(any(Spu.class))).thenReturn(1);

            // status=-1 (edge case)
            assertThatCode(() -> productService.updateStatus(1L, -1))
                    .doesNotThrowAnyException();
        }

        @Test
        void getProductDetail_shouldHandleNoSkus() {
            when(spuMapper.selectById(1L)).thenReturn(spu);
            when(skuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            ProductDetailVO result = productService.getProductDetail(1L);

            assertThat(result.getSpu()).isNotNull();
            assertThat(result.getSkus()).isEmpty();
        }
    }
}
