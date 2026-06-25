package com.ecommerce.warehouse.controller.internal;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.response.WarehouseVO;
import com.ecommerce.warehouse.service.CheckService;
import com.ecommerce.warehouse.service.InboundService;
import com.ecommerce.warehouse.service.OutboundService;
import com.ecommerce.warehouse.service.StockService;
import com.ecommerce.warehouse.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalWarehouseControllerTest {

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private InboundService inboundService;

    @Mock
    private OutboundService outboundService;

    @Mock
    private StockService stockService;

    @Mock
    private CheckService checkService;

    @InjectMocks
    private InternalWarehouseController controller;

    @Test
    void shouldGetWarehouseInfoById() {
        WarehouseVO warehouse = new WarehouseVO();
        warehouse.setId(9L);
        warehouse.setStockMode(WarehouseStockMode.LIGHT);
        when(warehouseService.getWarehouse(9L)).thenReturn(warehouse);

        WarehouseVO result = controller.getWarehouse(9L).getData();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getStockMode()).isEqualTo(WarehouseStockMode.LIGHT);
        verify(warehouseService).getWarehouse(9L);
    }

    @Test
    void shouldPropagateWarehouseNotFound() {
        when(warehouseService.getWarehouse(99L))
                .thenThrow(new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND));

        assertThatThrownBy(() -> controller.getWarehouse(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());
    }
}
