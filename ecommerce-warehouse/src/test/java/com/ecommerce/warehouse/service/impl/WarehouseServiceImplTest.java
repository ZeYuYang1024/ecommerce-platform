package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateWarehouseRequest;
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
        warehouse.setWarehouseType("MAIN");
        warehouse.setStockMode("MANAGED");
        warehouse.setMerchantId(100L);
        warehouse.setProvince("广东省");
        warehouse.setCity("广州市");
        warehouse.setDistrict("白云区");
        warehouse.setAddress("白云大道100号");
        warehouse.setContactName("张三");
        warehouse.setContactPhone("13800138000");
        warehouse.setStatus(1);
    }

    // ======================== Warehouse CRUD ========================

    @Nested
    class ListWarehousesTests {

        @Test
        void shouldListWarehousesWithPagination() {
            Page<Warehouse> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(warehouse));
            when(warehouseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<WarehouseVO> result = service.listWarehouses(1, 10, null);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords().get(0).getWarehouseCode()).isEqualTo("GZ001");
            assertThat(result.getRecords().get(0).getWarehouseName()).isEqualTo("广州主仓");
        }

        @Test
        void shouldListWarehousesWithMerchantFilter() {
            Page<Warehouse> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(Collections.singletonList(warehouse));
            when(warehouseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<WarehouseVO> result = service.listWarehouses(1, 10, 100L);

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getRecords().get(0).getMerchantId()).isEqualTo(100L);
        }

        @Test
        void shouldReturnEmptyPage() {
            Page<Warehouse> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(Collections.emptyList());
            when(warehouseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            IPage<WarehouseVO> result = service.listWarehouses(1, 10, null);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    class ListAllEnabledTests {

        @Test
        void shouldListAllEnabledWarehouses() {
            Warehouse w2 = new Warehouse();
            w2.setId(2L);
            w2.setWarehouseCode("SH001");
            w2.setWarehouseName("上海分仓");
            w2.setStatus(1);
            when(warehouseMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(warehouse, w2));

            List<WarehouseVO> result = service.listAllEnabled(null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getWarehouseCode()).isEqualTo("GZ001");
            assertThat(result.get(1).getWarehouseCode()).isEqualTo("SH001");
        }

        @Test
        void shouldReturnEmptyListWhenNoEnabledWarehouses() {
            when(warehouseMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<WarehouseVO> result = service.listAllEnabled(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetWarehouseTests {

        @Test
        void shouldGetWarehouseById() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);

            WarehouseVO result = service.getWarehouse(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getWarehouseCode()).isEqualTo("GZ001");
            assertThat(result.getWarehouseName()).isEqualTo("广州主仓");
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
        void shouldCreateWarehouse() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseMapper.insert(any(Warehouse.class))).thenReturn(1);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("GZ002");
            req.setWarehouseName("广州二号仓");
            req.setWarehouseType("MAIN");
            req.setStockMode("MANAGED");
            req.setMerchantId(100L);

            WarehouseVO result = service.createWarehouse(req);

            assertThat(result.getWarehouseCode()).isEqualTo("GZ002");
            assertThat(result.getWarehouseName()).isEqualTo("广州二号仓");
            assertThat(result.getStatus()).isEqualTo(1);
            verify(warehouseMapper).insert(any(Warehouse.class));
        }

        @Test
        void shouldRejectDuplicateWarehouseCode() {
            when(warehouseMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseCode("GZ001");

            assertThatThrownBy(() -> service.createWarehouse(req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_CODE_EXISTS.getCode());

            verify(warehouseMapper, never()).insert(any(Warehouse.class));
        }
    }

    @Nested
    class UpdateWarehouseTests {

        @Test
        void shouldUpdateWarehouse() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseMapper.updateById(any(Warehouse.class))).thenReturn(1);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseName("广州主仓(已更新)");
            req.setContactPhone("13900139000");

            WarehouseVO result = service.updateWarehouse(1L, req);

            assertThat(result.getWarehouseName()).isEqualTo("广州主仓(已更新)");
            verify(warehouseMapper).updateById(any(Warehouse.class));
        }

        @Test
        void shouldRejectUpdateForNonexistentWarehouse() {
            when(warehouseMapper.selectById(999L)).thenReturn(null);

            CreateWarehouseRequest req = new CreateWarehouseRequest();
            req.setWarehouseName("不存在");

            assertThatThrownBy(() -> service.updateWarehouse(999L, req))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());

            verify(warehouseMapper, never()).updateById(any(Warehouse.class));
        }
    }

    @Nested
    class DeleteWarehouseTests {

        @Test
        void shouldDeleteWarehouse() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseMapper.deleteById(1L)).thenReturn(1);

            service.deleteWarehouse(1L);

            verify(warehouseMapper).deleteById(1L);
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

    @Nested
    class ToggleStatusTests {

        @Test
        void shouldEnableWarehouse() {
            warehouse.setStatus(0);
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseMapper.updateById(any(Warehouse.class))).thenReturn(1);

            service.toggleStatus(1L, 1);

            assertThat(warehouse.getStatus()).isEqualTo(1);
            verify(warehouseMapper).updateById(warehouse);
        }

        @Test
        void shouldDisableWarehouse() {
            warehouse.setStatus(1);
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseMapper.updateById(any(Warehouse.class))).thenReturn(1);

            service.toggleStatus(1L, 0);

            assertThat(warehouse.getStatus()).isEqualTo(0);
            verify(warehouseMapper).updateById(warehouse);
        }

        @Test
        void shouldThrowWhenTogglingNonexistentWarehouse() {
            when(warehouseMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> service.toggleStatus(999L, 1))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode().getCode())
                    .isEqualTo(WarehouseErrorCode.WAREHOUSE_NOT_FOUND.getCode());

            verify(warehouseMapper, never()).updateById(any(Warehouse.class));
        }
    }

    // ======================== Zone CRUD ========================

    @Nested
    class ZoneCrudTests {

        @Test
        void shouldListZonesByWarehouse() {
            WarehouseZone zone = new WarehouseZone();
            zone.setId(10L);
            zone.setWarehouseId(1L);
            zone.setZoneCode("Z-RCV");
            zone.setZoneName("收货区");
            zone.setZoneType("RECEIVING");
            when(warehouseZoneMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(zone));

            List<WarehouseZoneVO> result = service.listZonesByWarehouse(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getZoneCode()).isEqualTo("Z-RCV");
            assertThat(result.get(0).getZoneTypeText()).isEqualTo("收货区");
        }

        @Test
        void shouldCreateZone() {
            when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
            when(warehouseZoneMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);
            when(warehouseZoneMapper.insert(any(WarehouseZone.class))).thenReturn(1);

            WarehouseZoneVO vo = new WarehouseZoneVO();
            vo.setWarehouseId(1L);
            vo.setZoneCode("Z-RCV");
            vo.setZoneName("收货区");
            vo.setZoneType("RECEIVING");

            WarehouseZoneVO result = service.createZone(vo);

            assertThat(result.getZoneCode()).isEqualTo("Z-RCV");
            assertThat(result.getZoneTypeText()).isEqualTo("收货区");
            verify(warehouseZoneMapper).insert(any(WarehouseZone.class));
        }

        @Test
        void shouldUpdateZone() {
            WarehouseZone zone = new WarehouseZone();
            zone.setId(10L);
            zone.setWarehouseId(1L);
            zone.setZoneCode("Z-RCV");
            zone.setZoneName("收货区");
            zone.setZoneType("RECEIVING");
            when(warehouseZoneMapper.selectById(10L)).thenReturn(zone);
            when(warehouseZoneMapper.updateById(any(WarehouseZone.class))).thenReturn(1);

            WarehouseZoneVO vo = new WarehouseZoneVO();
            vo.setZoneName("新收货区");

            WarehouseZoneVO result = service.updateZone(10L, vo);

            assertThat(result.getZoneName()).isEqualTo("新收货区");
            verify(warehouseZoneMapper).updateById(any(WarehouseZone.class));
        }

        @Test
        void shouldDeleteZone() {
            WarehouseZone zone = new WarehouseZone();
            zone.setId(10L);
            when(warehouseZoneMapper.selectById(10L)).thenReturn(zone);
            when(warehouseZoneMapper.deleteById(10L)).thenReturn(1);

            service.deleteZone(10L);

            verify(warehouseZoneMapper).deleteById(10L);
        }
    }

    // ======================== Bin CRUD ========================

    @Nested
    class BinCrudTests {

        @Test
        void shouldListBinsByWarehouse() {
            WarehouseBin bin = new WarehouseBin();
            bin.setId(20L);
            bin.setZoneId(10L);
            bin.setWarehouseId(1L);
            bin.setBinCode("B-A01");
            bin.setBinType("STANDARD");
            when(warehouseBinMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(bin));

            List<WarehouseBinVO> result = service.listBinsByWarehouse(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBinCode()).isEqualTo("B-A01");
            assertThat(result.get(0).getBinTypeText()).isEqualTo("标准货位");
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
            vo.setBinType("STANDARD");

            WarehouseBinVO result = service.createBin(vo);

            assertThat(result.getBinCode()).isEqualTo("B-A01");
            assertThat(result.getBinTypeText()).isEqualTo("标准货位");
            verify(warehouseBinMapper).insert(any(WarehouseBin.class));
        }

        @Test
        void shouldDeleteBin() {
            WarehouseBin bin = new WarehouseBin();
            bin.setId(20L);
            when(warehouseBinMapper.selectById(20L)).thenReturn(bin);
            when(warehouseBinMapper.deleteById(20L)).thenReturn(1);

            service.deleteBin(20L);

            verify(warehouseBinMapper).deleteById(20L);
        }
    }
}
