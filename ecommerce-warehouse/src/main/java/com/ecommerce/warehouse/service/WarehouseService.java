package com.ecommerce.warehouse.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ecommerce.warehouse.dto.request.CreateWarehouseRequest;
import com.ecommerce.warehouse.dto.request.UpdateWarehouseRequest;
import com.ecommerce.warehouse.dto.response.WarehouseBinVO;
import com.ecommerce.warehouse.dto.response.WarehouseVO;
import com.ecommerce.warehouse.dto.response.WarehouseZoneVO;

import java.util.List;

public interface WarehouseService {

    IPage<WarehouseVO> listWarehouses(int page, int size, Long merchantId);

    List<WarehouseVO> listAllEnabled(Long merchantId);

    WarehouseVO getWarehouse(Long id);

    WarehouseVO createWarehouse(CreateWarehouseRequest req);

    WarehouseVO updateWarehouse(Long id, UpdateWarehouseRequest req);

    void deleteWarehouse(Long id);

    void toggleStatus(Long id, Integer status);

    // Zone CRUD
    List<WarehouseZoneVO> listZonesByWarehouse(Long warehouseId);

    WarehouseZoneVO createZone(WarehouseZoneVO vo);

    WarehouseZoneVO updateZone(Long id, WarehouseZoneVO vo);

    void deleteZone(Long id);

    // Bin CRUD
    List<WarehouseBinVO> listBinsByWarehouse(Long warehouseId);

    List<WarehouseBinVO> listBinsByZone(Long zoneId);

    WarehouseBinVO createBin(WarehouseBinVO vo);

    WarehouseBinVO updateBin(Long id, WarehouseBinVO vo);

    void deleteBin(Long id);
}
