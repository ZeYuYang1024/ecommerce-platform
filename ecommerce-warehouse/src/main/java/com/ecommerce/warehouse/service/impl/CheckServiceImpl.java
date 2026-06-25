package com.ecommerce.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.warehouse.common.CheckStatus;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateCheckRequest;
import com.ecommerce.warehouse.dto.response.StockCheckVO;
import com.ecommerce.warehouse.entity.PhysicalStock;
import com.ecommerce.warehouse.entity.StockCheck;
import com.ecommerce.warehouse.entity.StockCheckItem;
import com.ecommerce.warehouse.entity.Warehouse;
import com.ecommerce.warehouse.mapper.PhysicalStockMapper;
import com.ecommerce.warehouse.mapper.StockCheckItemMapper;
import com.ecommerce.warehouse.mapper.StockCheckMapper;
import com.ecommerce.warehouse.mapper.WarehouseMapper;
import com.ecommerce.warehouse.service.CheckService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CheckServiceImpl implements CheckService {

    private final StockCheckMapper stockCheckMapper;
    private final StockCheckItemMapper stockCheckItemMapper;
    private final PhysicalStockMapper physicalStockMapper;
    private final WarehouseMapper warehouseMapper;

    public CheckServiceImpl(StockCheckMapper stockCheckMapper,
                            StockCheckItemMapper stockCheckItemMapper,
                            PhysicalStockMapper physicalStockMapper,
                            WarehouseMapper warehouseMapper) {
        this.stockCheckMapper = stockCheckMapper;
        this.stockCheckItemMapper = stockCheckItemMapper;
        this.physicalStockMapper = physicalStockMapper;
        this.warehouseMapper = warehouseMapper;
    }

    // ======================== Query ========================

    @Override
    public IPage<StockCheckVO> listChecks(int page, int size, Long warehouseId, Long merchantId) {
        Page<StockCheck> p = new Page<>(page, size);
        LambdaQueryWrapper<StockCheck> wrapper = new LambdaQueryWrapper<StockCheck>()
                .orderByDesc(StockCheck::getCreatedAt);
        if (warehouseId != null) {
            wrapper.eq(StockCheck::getWarehouseId, warehouseId);
        }
        if (merchantId != null) {
            wrapper.eq(StockCheck::getMerchantId, merchantId);
        }
        IPage<StockCheck> result = stockCheckMapper.selectPage(p, wrapper);
        return result.convert(this::toVO);
    }

    @Override
    public StockCheckVO getCheck(Long id) {
        StockCheck entity = stockCheckMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(WarehouseErrorCode.CHECK_NOT_FOUND);
        }
        return toVO(entity);
    }

    // ======================== Mutations ========================

    @Override
    @Transactional
    public StockCheckVO createCheck(CreateCheckRequest req) {
        // Validate warehouse exists and is enabled
        Warehouse warehouse = warehouseMapper.selectById(req.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_NOT_FOUND);
        }
        if (warehouse.getStatus() != null && warehouse.getStatus() != 1) {
            throw new BusinessException(WarehouseErrorCode.WAREHOUSE_DISABLED);
        }

        // Create check order
        StockCheck check = new StockCheck();
        check.setCheckNo(generateCheckNo());
        check.setWarehouseId(req.getWarehouseId());
        check.setStatus(CheckStatus.IN_PROGRESS);
        check.setMerchantId(req.getMerchantId());
        stockCheckMapper.insert(check);

        // Populate check items from current physical stock
        List<PhysicalStock> stocks = physicalStockMapper.selectList(
                new LambdaQueryWrapper<PhysicalStock>()
                        .eq(PhysicalStock::getWarehouseId, req.getWarehouseId()));
        if (stocks != null) {
            for (PhysicalStock stock : stocks) {
                StockCheckItem item = new StockCheckItem();
                item.setCheckId(check.getId());
                item.setSkuId(stock.getSkuId());
                item.setBinId(stock.getBinId());
                item.setSystemQty(stock.getQuantity());
                // actualQty and diffQty remain null until recorded
                stockCheckItemMapper.insert(item);
            }
        }

        return toVO(check);
    }

    @Override
    @Transactional
    public void recordCheckItem(Long checkId, Long itemId, int actualQty) {
        StockCheck check = stockCheckMapper.selectById(checkId);
        if (check == null) {
            throw new BusinessException(WarehouseErrorCode.CHECK_NOT_FOUND);
        }
        if (check.getStatus() != CheckStatus.IN_PROGRESS) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }

        StockCheckItem item = stockCheckItemMapper.selectById(itemId);
        if (item == null || !item.getCheckId().equals(checkId)) {
            throw new BusinessException(WarehouseErrorCode.STOCK_NOT_FOUND);
        }

        item.setActualQty(actualQty);
        item.setDiffQty(actualQty - item.getSystemQty());
        stockCheckItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void completeCheck(Long id) {
        StockCheck check = stockCheckMapper.selectById(id);
        if (check == null) {
            throw new BusinessException(WarehouseErrorCode.CHECK_NOT_FOUND);
        }
        if (check.getStatus() != CheckStatus.IN_PROGRESS) {
            throw new BusinessException(WarehouseErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Check if any items have non-zero diffs
        List<StockCheckItem> items = stockCheckItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItem>()
                        .eq(StockCheckItem::getCheckId, id));
        boolean hasDiff = false;
        if (items != null) {
            for (StockCheckItem item : items) {
                if (item.getDiffQty() != null && item.getDiffQty() != 0) {
                    hasDiff = true;
                    break;
                }
            }
        }

        if (hasDiff) {
            check.setStatus(CheckStatus.DIFF_PENDING);
        } else {
            check.setStatus(CheckStatus.COMPLETED);
        }
        stockCheckMapper.updateById(check);
    }

    // ======================== Private helpers ========================

    private String generateCheckNo() {
        return "CK" + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private StockCheckVO toVO(StockCheck entity) {
        StockCheckVO vo = new StockCheckVO();
        vo.setId(entity.getId());
        vo.setCheckNo(entity.getCheckNo());
        vo.setWarehouseId(entity.getWarehouseId());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(CheckStatus.text(entity.getStatus()));
        vo.setMerchantId(entity.getMerchantId());
        vo.setCreatedAt(entity.getCreatedAt());

        // Load items
        List<StockCheckItem> items = stockCheckItemMapper.selectList(
                new LambdaQueryWrapper<StockCheckItem>()
                        .eq(StockCheckItem::getCheckId, entity.getId()));
        List<StockCheckVO.Item> voItems = new ArrayList<>();
        if (items != null) {
            for (StockCheckItem item : items) {
                StockCheckVO.Item voItem = new StockCheckVO.Item();
                voItem.setId(item.getId());
                voItem.setSkuId(item.getSkuId());
                voItem.setSystemQty(item.getSystemQty());
                voItem.setActualQty(item.getActualQty());
                voItem.setDiffQty(item.getDiffQty());
                voItems.add(voItem);
            }
        }
        vo.setItems(voItems);
        return vo;
    }
}
