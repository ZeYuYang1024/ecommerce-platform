package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import com.ecommerce.inventory.client.ProductClient;
import com.ecommerce.inventory.entity.Stock;
import com.ecommerce.inventory.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockMerchantServiceTest {

    @Mock
    private StockMapper stockMapper;

    private RecordingProductClientHandler productClientHandler;
    private StockServiceImpl stockService;

    @BeforeEach
    void setUp() {
        productClientHandler = new RecordingProductClientHandler();
        ProductClient productClient = (ProductClient) Proxy.newProxyInstance(
                ProductClient.class.getClassLoader(),
                new Class<?>[]{ProductClient.class},
                productClientHandler);
        stockService = new StockServiceImpl(stockMapper, productClient);
    }

    @Test
    void listForMerchant_shouldUseMerchantSkuScope() throws Exception {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSkuId(100L);
        stock.setTotalStock(20);
        stock.setLockedStock(2);
        stock.setAvailableStock(18);

        Page<Stock> page = new Page<>(1, 10);
        page.setRecords(List.of(stock));
        page.setTotal(1);
        when(stockMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Method method = StockServiceImpl.class.getMethod("listForMerchant", Long.class, Long.class, Integer.class, int.class, int.class);
        Object result = method.invoke(stockService, 2001L, null, null, 1, 10);

        assertThat(result).isInstanceOf(Page.class);
        assertThat(((Page<?>) result).getRecords()).hasSize(1);
        assertThat(productClientHandler.calledMethods).contains("listSkuIdsByMerchant");
        assertThat(productClientHandler.merchantLookupArgs).containsExactly(2001L);
        verify(stockMapper).selectPage(any(Page.class), any());
    }

    @Test
    void setStockForMerchant_shouldRejectCrossTenantSku() throws Exception {
        Method method = StockServiceImpl.class.getMethod("setStockForMerchant", Long.class, Long.class, int.class);

        assertThatThrownBy(() -> invoke(method, stockService, 2001L, 100L, 50))
                .isInstanceOf(BusinessException.class);
        assertThat(productClientHandler.lastMethodName).isEqualTo("querySkuOwner");
        assertThat(productClientHandler.lastArgs).containsExactly(100L);
    }

    private static Object invoke(Method method, Object target, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }

    private static final class RecordingProductClientHandler implements InvocationHandler {

        private String lastMethodName;
        private Object[] lastArgs = new Object[0];
        private Object[] merchantLookupArgs = new Object[0];
        private final List<String> calledMethods = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            lastMethodName = method.getName();
            calledMethods.add(method.getName());
            lastArgs = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
            if ("listSkuIdsByMerchant".equals(method.getName())) {
                merchantLookupArgs = lastArgs;
            }
            return switch (method.getName()) {
                case "listSkuIdsByMerchant" -> Result.ok(List.of(100L, 101L));
                case "batchQuerySkus" -> Result.ok(List.of());
                case "querySkuOwner" -> Result.ok(createOwnerDto(100L, 3002L));
                default -> Result.ok(null);
            };
        }

        private Object createOwnerDto(Long skuId, Long merchantId) throws Exception {
            Class<?> dtoClass = Class.forName("com.ecommerce.common.dto.SkuOwnerVO");
            Object dto = dtoClass.getDeclaredConstructor().newInstance();
            dtoClass.getMethod("setSkuId", Long.class).invoke(dto, skuId);
            dtoClass.getMethod("setMerchantId", Long.class).invoke(dto, merchantId);
            return dto;
        }
    }
}
