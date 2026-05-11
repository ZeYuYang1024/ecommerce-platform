package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.inventory.common.InventoryErrorCode;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.entity.Stock;
import com.ecommerce.inventory.mapper.StockMapper;
import com.ecommerce.inventory.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;

    public StockServiceImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public Stock getBySkuId(Long skuId) {
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
        if (stock == null) {
            throw new BusinessException(InventoryErrorCode.STOCK_NOT_FOUND);
        }
        return stock;
    }

    @Override
    public List<Stock> batchQuery(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return stockMapper.selectList(
                new LambdaQueryWrapper<Stock>().in(Stock::getSkuId, skuIds));
    }

    private static final int MAX_RETRIES = 3;

    @Transactional
    @Override
    public void deduct(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(InventoryErrorCode.INVALID_QUANTITY);
        }
        for (int i = 0; i < MAX_RETRIES; i++) {
            Stock stock = getBySkuId(skuId);
            if (stock.getAvailableStock() < quantity) {
                throw new BusinessException(InventoryErrorCode.STOCK_INSUFFICIENT);
            }
            int updated = stockMapper.update(null,
                    new LambdaUpdateWrapper<Stock>()
                            .eq(Stock::getSkuId, skuId)
                            .eq(Stock::getVersion, stock.getVersion())
                            .apply("locked_stock = locked_stock + {0}", quantity)
                            .apply("available_stock = available_stock - {0}", quantity)
                            .setSql("version = version + 1"));
            if (updated > 0) {
                return;
            }
        }
        throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
    }

    @Transactional
    @Override
    public void release(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(InventoryErrorCode.INVALID_QUANTITY);
        }
        for (int i = 0; i < MAX_RETRIES; i++) {
            Stock stock = getBySkuId(skuId);
            if (stock.getLockedStock() < quantity) {
                throw new BusinessException(InventoryErrorCode.LOCKED_STOCK_INSUFFICIENT);
            }
            int updated = stockMapper.update(null,
                    new LambdaUpdateWrapper<Stock>()
                            .eq(Stock::getSkuId, skuId)
                            .eq(Stock::getVersion, stock.getVersion())
                            .apply("locked_stock = locked_stock - {0}", quantity)
                            .apply("available_stock = available_stock + {0}", quantity)
                            .setSql("version = version + 1"));
            if (updated > 0) {
                return;
            }
        }
        throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
    }

    @Transactional
    @Override
    public void setStock(Long skuId, int totalStock) {
        if (totalStock < 0) {
            throw new BusinessException(InventoryErrorCode.INVALID_QUANTITY);
        }
        for (int i = 0; i < MAX_RETRIES; i++) {
            Stock existing = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
            if (existing != null) {
                int diff = totalStock - existing.getTotalStock();
                int updated = stockMapper.update(null,
                        new LambdaUpdateWrapper<Stock>()
                                .eq(Stock::getSkuId, skuId)
                                .eq(Stock::getVersion, existing.getVersion())
                                .apply("total_stock = {0}", totalStock)
                                .apply("available_stock = available_stock + {0}", diff)
                                .setSql("version = version + 1"));
                if (updated > 0) {
                    return;
                }
                continue;
            }
            Stock stock = new Stock();
            stock.setId(SnowflakeUtils.nextId());
            stock.setSkuId(skuId);
            stock.setTotalStock(totalStock);
            stock.setLockedStock(0);
            stock.setAvailableStock(totalStock);
            stock.setVersion(0);
            stockMapper.insert(stock);
            return;
        }
        throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
    }

    @Override
    public StockVO toVO(Stock s) {
        StockVO vo = new StockVO();
        vo.setId(s.getId());
        vo.setSkuId(s.getSkuId());
        vo.setTotalStock(s.getTotalStock());
        vo.setLockedStock(s.getLockedStock());
        vo.setAvailableStock(s.getAvailableStock());
        return vo;
    }
}
