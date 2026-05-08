package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.inventory.common.InventoryErrorCode;
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
        return stockMapper.selectList(
                new LambdaQueryWrapper<Stock>().in(Stock::getSkuId, skuIds));
    }

    @Transactional
    @Override
    public void deduct(Long skuId, int quantity) {
        Stock stock = getBySkuId(skuId);
        if (stock.getAvailableStock() < quantity) {
            throw new BusinessException(InventoryErrorCode.STOCK_INSUFFICIENT);
        }

        int updated = stockMapper.update(null,
                new LambdaUpdateWrapper<Stock>()
                        .eq(Stock::getSkuId, skuId)
                        .eq(Stock::getVersion, stock.getVersion())
                        .setSql("locked_stock = locked_stock + " + quantity)
                        .setSql("available_stock = available_stock - " + quantity)
                        .setSql("version = version + 1"));

        if (updated == 0) {
            throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
        }
    }

    @Transactional
    @Override
    public void release(Long skuId, int quantity) {
        Stock stock = getBySkuId(skuId);
        int updated = stockMapper.update(null,
                new LambdaUpdateWrapper<Stock>()
                        .eq(Stock::getSkuId, skuId)
                        .eq(Stock::getVersion, stock.getVersion())
                        .setSql("locked_stock = locked_stock - " + quantity)
                        .setSql("available_stock = available_stock + " + quantity)
                        .setSql("version = version + 1"));

        if (updated == 0) {
            throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
        }
    }

    @Override
    public void setStock(Long skuId, int totalStock) {
        Stock existing = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
        if (existing != null) {
            int diff = totalStock - existing.getTotalStock();
            stockMapper.update(null,
                    new LambdaUpdateWrapper<Stock>()
                            .eq(Stock::getSkuId, skuId)
                            .setSql("total_stock = " + totalStock)
                            .setSql("available_stock = available_stock + " + diff));
        } else {
            Stock stock = new Stock();
            stock.setId(SnowflakeUtils.nextId());
            stock.setSkuId(skuId);
            stock.setTotalStock(totalStock);
            stock.setLockedStock(0);
            stock.setAvailableStock(totalStock);
            stock.setVersion(0);
            stockMapper.insert(stock);
        }
    }
}
