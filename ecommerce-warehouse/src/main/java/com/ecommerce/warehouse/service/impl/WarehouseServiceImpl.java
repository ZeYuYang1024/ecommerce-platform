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
import com.ecommerce.warehouse.service.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseZoneMapper warehouseZoneMapper;
    private final WarehouseBinMapper warehouseBinMapper;

    public WarehouseServiceImpl(WarehouseMapper warehouseMapper,
                                WarehouseZoneMapper warehouseZoneMapper,
                                WarehouseBinMapper warehouseBinMapper) {
        this.warehouseMapper = warehouseMapper;
        this.warehouseZoneMapper = warehouseZoneMapper;
        this.warehouseBinMapper = warehouseBinMapper;
    }

    @Override
    public IPage<WarehouseVO> listWarehouses(int page, int size, Long merchantId) {
        Page<Warehouse> p = new Page<>(page, size);
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
                .orderByAsc(Warehouse::getWarehouseCode);
        if (merchantId != null) {
            wrapper.eq(Warehouse::getMerchantId, merchantId);
        }
        IPage<Warehouse> result = warehouseMapper.selectPage(p, wrapper);
        return result.convert(this::toWarehouseVO);
    }

    @Override
    public List<WarehouseVO> listAllEnabled(Long merchantId) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1)
                .orderByAsc(Warehouse::getWarehouseCode);
        if (merchantId != null) {
            wrapper.eq(Warehouse::getMerchantId, merchantId);
        }
        return warehouseMapper.selectList(wrapper).stream().map(this::toWarehouseVO).toList();
    }

    @Override
    public WarehouseVO getWarehouse(Long id) {
        Warehouse entity = warehouseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        return toWarehouseVO(entity);
    }

    @Override
    @Transactional
    public WarehouseVO createWarehouse(CreateWarehouseRequest req) {
        if (warehouseMapper.exists(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getWarehouseCode, req.getWarehouseCode()))) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_CODE_EXISTS);
        }

        validateWarehouseOwnership(req.getWarehouseType(), req.getStockMode(), req.getMerchantId());

        Warehouse entity = new Warehouse();
        entity.setWarehouseName(req.getWarehouseName());
        entity.setWarehouseCode(req.getWarehouseCode());
        entity.setWarehouseType(req.getWarehouseType());
        entity.setStockMode(req.getStockMode());
        entity.setMerchantId(req.getMerchantId());
        entity.setProvince(req.getProvince());
        entity.setCity(req.getCity());
        entity.setDistrict(req.getDistrict());
        entity.setAddress(req.getAddress());
        entity.setContactName(req.getContactName());
        entity.setContactPhone(req.getContactPhone());
        entity.setStatus(1);
        warehouseMapper.insert(entity);
        return toWarehouseVO(entity);
    }

    @Override
    @Transactional
    public WarehouseVO updateWarehouse(Long id, UpdateWarehouseRequest req) {
        Warehouse entity = warehouseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }

        Integer warehouseType = req.getWarehouseType() != null ? req.getWarehouseType() : entity.getWarehouseType();
        Integer stockMode = req.getStockMode() != null ? req.getStockMode() : entity.getStockMode();
        Long merchantId = req.getMerchantId() != null ? req.getMerchantId() : entity.getMerchantId();
        validateWarehouseOwnership(warehouseType, stockMode, merchantId);

        if (req.getWarehouseName() != null) {
            entity.setWarehouseName(req.getWarehouseName());
        }
        if (req.getWarehouseType() != null) {
            entity.setWarehouseType(req.getWarehouseType());
        }
        if (req.getStockMode() != null) {
            entity.setStockMode(req.getStockMode());
        }
        entity.setMerchantId(merchantId);
        if (req.getProvince() != null) {
            entity.setProvince(req.getProvince());
        }
        if (req.getCity() != null) {
            entity.setCity(req.getCity());
        }
        if (req.getDistrict() != null) {
            entity.setDistrict(req.getDistrict());
        }
        if (req.getAddress() != null) {
            entity.setAddress(req.getAddress());
        }
        if (req.getContactName() != null) {
            entity.setContactName(req.getContactName());
        }
        if (req.getContactPhone() != null) {
            entity.setContactPhone(req.getContactPhone());
        }
        warehouseMapper.updateById(entity);
        return toWarehouseVO(entity);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long id) {
        if (warehouseMapper.selectById(id) == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        warehouseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        Warehouse entity = warehouseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        entity.setStatus(status);
        warehouseMapper.updateById(entity);
    }

    @Override
    public List<WarehouseZoneVO> listZonesByWarehouse(Long warehouseId) {
        return warehouseZoneMapper.selectList(
                new LambdaQueryWrapper<WarehouseZone>()
                        .eq(WarehouseZone::getWarehouseId, warehouseId)
                        .orderByAsc(WarehouseZone::getZoneCode))
                .stream()
                .map(this::toZoneVO)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseZoneVO createZone(WarehouseZoneVO vo) {
        if (warehouseMapper.selectById(vo.getWarehouseId()) == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouseZoneMapper.exists(new LambdaQueryWrapper<WarehouseZone>()
                .eq(WarehouseZone::getWarehouseId, vo.getWarehouseId())
                .eq(WarehouseZone::getZoneCode, vo.getZoneCode()))) {
            throw new BusinessException(WarehouseErrorCode.ZONE_CODE_EXISTS);
        }
        WarehouseZone entity = new WarehouseZone();
        entity.setWarehouseId(vo.getWarehouseId());
        entity.setZoneName(vo.getZoneName());
        entity.setZoneCode(vo.getZoneCode());
        entity.setZoneType(vo.getZoneType());
        warehouseZoneMapper.insert(entity);
        return toZoneVO(entity);
    }

    @Override
    @Transactional
    public WarehouseZoneVO updateZone(Long id, WarehouseZoneVO vo) {
        WarehouseZone entity = warehouseZoneMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.ZONE_NOT_FOUND);
        }
        if (vo.getZoneName() != null) {
            entity.setZoneName(vo.getZoneName());
        }
        if (vo.getZoneCode() != null) {
            entity.setZoneCode(vo.getZoneCode());
        }
        if (vo.getZoneType() != null) {
            entity.setZoneType(vo.getZoneType());
        }
        warehouseZoneMapper.updateById(entity);
        return toZoneVO(entity);
    }

    @Override
    @Transactional
    public void deleteZone(Long id) {
        if (warehouseZoneMapper.selectById(id) == null) {
            throw new BusinessException(WarehouseErrorCode.ZONE_NOT_FOUND);
        }
        warehouseZoneMapper.deleteById(id);
    }

    @Override
    public List<WarehouseBinVO> listBinsByWarehouse(Long warehouseId) {
        return warehouseBinMapper.selectList(
                new LambdaQueryWrapper<WarehouseBin>()
                        .eq(WarehouseBin::getWarehouseId, warehouseId)
                        .orderByAsc(WarehouseBin::getBinCode))
                .stream()
                .map(this::toBinVO)
                .toList();
    }

    @Override
    public List<WarehouseBinVO> listBinsByZone(Long zoneId) {
        return warehouseBinMapper.selectList(
                new LambdaQueryWrapper<WarehouseBin>()
                        .eq(WarehouseBin::getZoneId, zoneId)
                        .orderByAsc(WarehouseBin::getBinCode))
                .stream()
                .map(this::toBinVO)
                .toList();
    }

    @Override
    @Transactional
    public WarehouseBinVO createBin(WarehouseBinVO vo) {
        if (warehouseMapper.selectById(vo.getWarehouseId()) == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouseZoneMapper.selectById(vo.getZoneId()) == null) {
            throw new BusinessException(WarehouseErrorCode.ZONE_NOT_FOUND);
        }
        if (warehouseBinMapper.exists(new LambdaQueryWrapper<WarehouseBin>()
                .eq(WarehouseBin::getWarehouseId, vo.getWarehouseId())
                .eq(WarehouseBin::getBinCode, vo.getBinCode()))) {
            throw new BusinessException(WarehouseErrorCode.BIN_CODE_EXISTS);
        }
        WarehouseBin entity = new WarehouseBin();
        entity.setZoneId(vo.getZoneId());
        entity.setWarehouseId(vo.getWarehouseId());
        entity.setBinCode(vo.getBinCode());
        entity.setBinType(vo.getBinType());
        warehouseBinMapper.insert(entity);
        return toBinVO(entity);
    }

    @Override
    @Transactional
    public WarehouseBinVO updateBin(Long id, WarehouseBinVO vo) {
        WarehouseBin entity = warehouseBinMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.BIN_NOT_FOUND);
        }
        if (vo.getBinCode() != null) {
            entity.setBinCode(vo.getBinCode());
        }
        if (vo.getBinType() != null) {
            entity.setBinType(vo.getBinType());
        }
        warehouseBinMapper.updateById(entity);
        return toBinVO(entity);
    }

    @Override
    @Transactional
    public void deleteBin(Long id) {
        if (warehouseBinMapper.selectById(id) == null) {
            throw new BusinessException(WarehouseErrorCode.BIN_NOT_FOUND);
        }
        warehouseBinMapper.deleteById(id);
    }

    private WarehouseVO toWarehouseVO(Warehouse entity) {
        WarehouseVO vo = new WarehouseVO();
        vo.setId(entity.getId());
        vo.setWarehouseName(entity.getWarehouseName());
        vo.setWarehouseCode(entity.getWarehouseCode());
        vo.setWarehouseType(entity.getWarehouseType());
        vo.setWarehouseTypeText(WarehouseType.text(entity.getWarehouseType()));
        vo.setStockMode(entity.getStockMode());
        vo.setStockModeText(WarehouseStockMode.text(entity.getStockMode()));
        vo.setMerchantId(entity.getMerchantId());
        vo.setProvince(entity.getProvince());
        vo.setCity(entity.getCity());
        vo.setDistrict(entity.getDistrict());
        vo.setAddress(entity.getAddress());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(entity.getStatus() != null && entity.getStatus() == 1 ? "启用" : "停用");
        return vo;
    }

    private WarehouseZoneVO toZoneVO(WarehouseZone entity) {
        WarehouseZoneVO vo = new WarehouseZoneVO();
        vo.setId(entity.getId());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setZoneName(entity.getZoneName());
        vo.setZoneCode(entity.getZoneCode());
        vo.setZoneType(entity.getZoneType());
        vo.setZoneTypeText(WarehouseZoneType.text(entity.getZoneType()));
        return vo;
    }

    private WarehouseBinVO toBinVO(WarehouseBin entity) {
        WarehouseBinVO vo = new WarehouseBinVO();
        vo.setId(entity.getId());
        vo.setZoneId(entity.getZoneId());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setBinCode(entity.getBinCode());
        vo.setBinType(entity.getBinType());
        vo.setBinTypeText(WarehouseBinType.text(entity.getBinType()));
        return vo;
    }

    private void validateWarehouseOwnership(Integer warehouseType, Integer stockMode, Long merchantId) {
        if (warehouseType == null || stockMode == null) {
            return;
        }
        if (warehouseType == WarehouseType.PLATFORM) {
            if (merchantId != null) {
                throw new IllegalArgumentException("platform warehouse must not bind merchantId");
            }
            return;
        }
        if (warehouseType == WarehouseType.MERCHANT && merchantId == null) {
            throw new IllegalArgumentException("merchant warehouse must bind merchantId");
        }
        if (warehouseType == WarehouseType.MERCHANT && stockMode == WarehouseStockMode.MANAGED) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_FORBIDDEN);
        }
    }
}
