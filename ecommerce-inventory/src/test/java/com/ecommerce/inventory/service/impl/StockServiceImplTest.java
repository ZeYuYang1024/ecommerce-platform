package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.inventory.common.InventoryErrorCode;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.entity.Stock;
import com.ecommerce.inventory.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private StockMapper stockMapper;
    @InjectMocks private StockServiceImpl stockService;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = new Stock();
        stock.setId(1L);
        stock.setSkuId(100L);
        stock.setTotalStock(500);
        stock.setLockedStock(50);
        stock.setAvailableStock(450);
        stock.setVersion(0);
    }

    @Nested
    class QueryTests {

        @Test
        void getBySkuId_shouldReturnStock_whenFound() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);

            Stock result = stockService.getBySkuId(100L);

            assertThat(result.getSkuId()).isEqualTo(100L);
            assertThat(result.getAvailableStock()).isEqualTo(450);
        }

        @Test
        void getBySkuId_shouldThrow_whenNotFound() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> stockService.getBySkuId(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.STOCK_NOT_FOUND.getCode());
        }

        @Test
        void batchQuery_shouldReturnStocks() {
            Stock stock2 = new Stock();
            stock2.setId(2L); stock2.setSkuId(200L);
            when(stockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(stock, stock2));

            List<Stock> result = stockService.batchQuery(Arrays.asList(100L, 200L));

            assertThat(result).hasSize(2);
        }

        @Test
        void batchQuery_shouldReturnEmpty_whenEmptyList() {
            // empty list returns early without hitting the mapper
            List<Stock> result = stockService.batchQuery(Collections.emptyList());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class DeductTests {

        @Test
        void deduct_shouldDecreaseAvailableStock() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            stockService.deduct(100L, 10);

            verify(stockMapper).update(isNull(), any());
        }

        @Test
        void deduct_shouldThrow_whenInsufficient() {
            stock = new Stock();
            stock.setSkuId(100L);
            stock.setTotalStock(10);
            stock.setLockedStock(0);
            stock.setAvailableStock(5);
            stock.setVersion(0);
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);

            assertThatThrownBy(() -> stockService.deduct(100L, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.STOCK_INSUFFICIENT.getCode());
        }
    }

    @Nested
    class ReleaseTests {

        @Test
        void release_shouldReturnStock() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            stockService.release(100L, 10);

            verify(stockMapper).update(isNull(), any());
        }
    }

    @Nested
    class SetStockTests {

        @Test
        void setStock_shouldCreateNew_whenNotExists() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(stockMapper.insert(any(Stock.class))).thenReturn(1);

            stockService.setStock(300L, 100);

            verify(stockMapper).insert(any(Stock.class));
        }

        @Test
        void setStock_shouldUpdateExisting() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            stockService.setStock(100L, 600);

            verify(stockMapper).update(isNull(), any());
        }
    }

    @Nested
    class ToVOTests {

        @Test
        void toVO_shouldMapAllFields() {
            StockVO vo = stockService.toVO(stock);

            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getSkuId()).isEqualTo(100L);
            assertThat(vo.getTotalStock()).isEqualTo(500);
            assertThat(vo.getLockedStock()).isEqualTo(50);
            assertThat(vo.getAvailableStock()).isEqualTo(450);
        }
    }

    @Nested
    class BoundaryTests {

        @Test
        void deduct_shouldSucceed_whenQuantityEqualsAvailableStock() {
            stock.setAvailableStock(10);
            stock.setLockedStock(0);
            stock.setTotalStock(10);
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            // deduct exactly all available stock — should succeed
            assertThatCode(() -> stockService.deduct(100L, 10))
                    .doesNotThrowAnyException();
        }

        @Test
        void deduct_shouldThrow_whenOneOverAvailable() {
            stock.setAvailableStock(5);
            stock.setTotalStock(5);
            stock.setLockedStock(0);
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);

            // 6 > 5, just over the boundary
            assertThatThrownBy(() -> stockService.deduct(100L, 6))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.STOCK_INSUFFICIENT.getCode());
        }

        @Test
        void deduct_shouldHandleMinimumQuantity() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            assertThatCode(() -> stockService.deduct(100L, 1))
                    .doesNotThrowAnyException();
        }

        @Test
        void release_shouldRejectZeroQuantity() {
            // quantity <= 0 should throw INVALID_QUANTITY
            assertThatThrownBy(() -> stockService.release(100L, 0))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.INVALID_QUANTITY.getCode());
        }

        @Test
        void release_shouldRejectNegativeQuantity() {
            assertThatThrownBy(() -> stockService.release(100L, -1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.INVALID_QUANTITY.getCode());
        }

        @Test
        void setStock_shouldHandleZero() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            assertThatCode(() -> stockService.setStock(100L, 0))
                    .doesNotThrowAnyException();
        }

        @Test
        void setStock_shouldHandleLargeValue() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(stockMapper.insert(any(Stock.class))).thenReturn(1);

            assertThatCode(() -> stockService.setStock(300L, Integer.MAX_VALUE))
                    .doesNotThrowAnyException();
        }

        @Test
        void batchQuery_shouldHandleSingleElementList() {
            when(stockMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.singletonList(stock));

            List<Stock> result = stockService.batchQuery(Collections.singletonList(100L));

            assertThat(result).hasSize(1);
        }

        @Test
        void batchQuery_shouldHandleNullList() {
            // null input returns empty list without hitting the mapper
            List<Stock> result = stockService.batchQuery(null);
            assertThat(result).isEmpty();
        }

        @Test
        void batchQuery_shouldHandleEmptyList() {
            List<Stock> result = stockService.batchQuery(Collections.emptyList());
            assertThat(result).isEmpty();
        }

        @Test
        void toVO_shouldHandleZeroValues() {
            Stock zeroStock = new Stock();
            zeroStock.setId(2L);
            zeroStock.setSkuId(200L);
            zeroStock.setTotalStock(0);
            zeroStock.setLockedStock(0);
            zeroStock.setAvailableStock(0);

            StockVO vo = stockService.toVO(zeroStock);

            assertThat(vo.getTotalStock()).isEqualTo(0);
            assertThat(vo.getLockedStock()).isEqualTo(0);
            assertThat(vo.getAvailableStock()).isEqualTo(0);
        }

        @Test
        void getBySkuId_shouldHandleMaxLongValue() {
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> stockService.getBySkuId(Long.MAX_VALUE))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(InventoryErrorCode.STOCK_NOT_FOUND.getCode());
        }

        @Test
        void setStock_shouldHandleDecrease() {
            // set stock lower than current — diff should be negative
            stock.setTotalStock(100);
            stock.setAvailableStock(80);
            when(stockMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(stock);
            when(stockMapper.update(eq(null), any())).thenReturn(1);

            assertThatCode(() -> stockService.setStock(100L, 50))
                    .doesNotThrowAnyException();
        }
    }
}
