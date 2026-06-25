package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.constant.WarehouseStockMode;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseBinType;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.common.WarehouseType;
import com.ecommerce.warehouse.common.WarehouseZoneType;
import com.ecommerce.warehouse.dto.request.CreateWarehouseRequest;
import com.ecommerce.warehouse.dto.request.UpdateWarehouseRequest;
import com.ecommerce.warehouse.dto.response.WarehouseBinVO;
import com.ecommerce.warehouse.dto.response.WarehouseVO;
import com.ecommerce.warehouse.dto.response.WarehouseZoneVO;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.entity.WarehouseBin;
import com.ecommerce.warehouse.entity.WarehouseZone;
import com.ecommerce.warehouse.mapper.WarehouseBinMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.mapper.WarehouseZoneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseZoneMapper warehouseZoneMapper;

    @Mock
    private WarehouseBinMapper warehouseBinMapper;

    @InjectMocks
    private WarehouseServiceImpl service;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setWarehouseName("广州主仓");
        warehouse.setWarehouseCode("GZ001");
        warehouse.setWarehouseType(WarehouseType.MERCHANT);
        warehouse.setStockMode(WarehouseStockMode.LIGHT);
        warehouse.setMerchantId(100L);
        warehouse.setProvince("广东省");
        warehouse.setCity("广州市");
        warehouse.setDistrict("白云区");
        warehouse.setAddress("白云大道100号");
        warehouse.setContactName("张三");
        warehouse.setContactPhone("13800138000");
        warehouse.setStatus(1);
    }

    @Nested
    class ListWarehousesTests {

        @Test
        void shouldListWarehousesWithPagination() {
            Page<Warehouse> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(warehouse));
            when(warehouseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            IPage<WarehouseVO> result = service.listWarehouses(1, 10, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getWarehouseCode()).isEqualTo("GZ001");
        }

        @Test
        void shouldListWarehousesWithMerchantFilter() {
            Page<Warehouse> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(warehouse));
            when(warehouseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            IPage<WarehouseVO> result = service.listWarehouses(1, 10, 100L);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getMerchantId()).isEqualTo(100L);
        }
    }

    @Nested
    class GetWarehouseTests {

        @Test
        void shouldGetWarehouseById() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

            WarehouseVO result = service.getWarehouse(1L);

            assertThat(result.getWarehouseType()).isEqualTo(WarehouseType.MERCHANT);
            assertThat(result.getWarehouseTypeText()).isEqualTo("商家仓");
            assertThat(result.getStockMode()).isEqualTo(WarehouseStockMode.LIGHT);
            assertThat(result.getStockModeText()).isEqualTo("轻仓");
            assertThat(result.getStatusText()).isEqualTo("启用");
        }

        @Test
        void shouldThrowWhenWarehouseNotFound() {
            when(warehouseMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.getWarehouse(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());
        }
    }

    @Nested
    class CreateWarehouseTests {

        @Test
        void shouldCreateMerchantLightWarehouse() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseMapper.insert(any(Warehouse.class))).thenReturn(1);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("GZ002");
            req.setWarehouseName("广州二号仓");
            req.setWarehouseType(WarehouseType.MERCHANT);
            req.setStockMode(WarehouseStockMode.LIGHT);
            req.setMerchantId(100L);

            WarehouseVO result = service.createWarehouse(req);

            assertThat(result.getWarehouseCode()).isEqualTo("GZ002");
            assertThat(result.getWarehouseType()).isEqualTo(WarehouseType.MERCHANT);
            assertThat(result.getStockMode()).isEqualTo(WarehouseStockMode.LIGHT);
            verify(warehouseMapper).insert(any(Warehouse.class));
        }

        @Test
        void shouldCreatePlatformManagedWarehouse() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseMapper.insert(any(Warehouse.class))).thenReturn(1);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("PT001");
            req.setWarehouseName("平台托管仓");
            req.setWarehouseType(WarehouseType.PLATFORM);
            req.setStockMode(WarehouseStockMode.MANAGED);
            req.setMerchantId(null);

            WarehouseVO result = service.createWarehouse(req);

            assertThat(result.getWarehouseType()).isEqualTo(WarehouseType.PLATFORM);
            assertThat(result.getStockMode()).isEqualTo(WarehouseStockMode.MANAGED);
            assertThat(result.getMerchantId()).isNull();
        }

        @Test
        void shouldRejectManagedMerchantWarehouse() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("M001");
            req.setWarehouseName("商家托管仓");
            req.setWarehouseType(WarehouseType.MERCHANT);
            req.setStockMode(WarehouseStockMode.MANAGED);
            req.setMerchantId(100L);

            assertThatThrownBy(() -> service.createWarehouse(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode());
        }

        @Test
        void shouldRejectPlatformWarehouseWithMerchantId() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("PT002");
            req.setWarehouseName("错误平台仓");
            req.setWarehouseType(WarehouseType.PLATFORM);
            req.setStockMode(WarehouseStockMode.MANAGED);
            req.setMerchantId(100L);

            assertThatThrownBy(() -> service.createWarehouse(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("platform warehouse must not bind merchantId");
        }
    }

    @Nested
    class UpdateWarehouseTests {

        @Test
        void shouldUpdateWarehouse() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseMapper.updateById(any(Warehouse.class))).thenReturn(1);

            UpdateWarehouseRequest req = new UpdateWarehouseRequest();
            req.setWarehouseName("广州主仓(已更新)");
            req.setContactPhone("13900139000");

            WarehouseVO result = service.updateWarehouse(1L, req);

            assertThat(result.getWarehouseName()).isEqualTo("广州主仓(已更新)");
            verify(warehouseMapper).updateById(any(Warehouse.class));
        }
    }

    @Nested
    class ZoneCrudTests {

        @Test
        void shouldListZonesByWarehouse() {
            WarehouseZone zone = new WarehouseZone();
            zone.setId(10L);
            zone.setWarehouseId(1L);
            zone.setZoneCode("Z-STO");
            zone.setZoneName("存储区");
            zone.setZoneType(WarehouseZoneType.STORAGE);
            when(warehouseZoneMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(zone));

            List<WarehouseZoneVO> result = service.listZonesByWarehouse(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getZoneType()).isEqualTo(WarehouseZoneType.STORAGE);
            assertThat(result.get(0).getZoneTypeText()).isEqualTo("存储区");
        }

        @Test
        void shouldCreateZone() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseZoneMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseZoneMapper.insert(any(WarehouseZone.class))).thenReturn(1);

            WarehouseZoneVO vo = new WarehouseZoneVO();
            vo.setWarehouseId(1L);
            vo.setZoneCode("Z-PICK");
            vo.setZoneName("拣货区");
            vo.setZoneType(WarehouseZoneType.PICKING);

            WarehouseZoneVO result = service.createZone(vo);

            assertThat(result.getZoneTypeText()).isEqualTo("拣货区");
        }
    }

    @Nested
    class BinCrudTests {

        @Test
        void shouldListBinsByWarehouse() {
            WarehouseBin bin = new WarehouseBin();
            bin.setId(20L);
            bin.setZoneId(10L);
            bin.setWarehouseId(1L);
            bin.setBinCode("B-A01");
            bin.setBinType(WarehouseBinType.STANDARD);
            when(warehouseBinMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(bin));

            List<WarehouseBinVO> result = service.listBinsByWarehouse(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBinType()).isEqualTo(WarehouseBinType.STANDARD);
            assertThat(result.get(0).getBinTypeText()).isEqualTo("普通货位");
        }

        @Test
        void shouldCreateBin() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            WarehouseZone zone = new WarehouseZone();
            zone.setId(10L);
            when(warehouseZoneMapper.selectById(10L)).thenReturn(zone);
            when(warehouseBinMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseBinMapper.insert(any(WarehouseBin.class))).thenReturn(1);

            WarehouseBinVO vo = new WarehouseBinVO();
            vo.setZoneId(10L);
            vo.setWarehouseId(1L);
            vo.setBinCode("B-A01");
            vo.setBinType(WarehouseBinType.COLD);

            WarehouseBinVO result = service.createBin(vo);

            assertThat(result.getBinTypeText()).isEqualTo("冷藏货位");
        }
    }

    @Test
    void shouldThrowWhenDeletingNonexistentWarehouse() {
        when(warehouseMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteWarehouse(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());

        verify(warehouseMapper, never()).deleteById(anyLong());
    }
}
