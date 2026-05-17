package com.ecommerce.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.dto.SkuOwnerVO;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.inventory.client.ProductClient;
import com.ecommerce.inventory.common.InventoryErrorCode;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.entity.Stock;
import com.ecommerce.inventory.mapper.StockMapper;
import com.ecommerce.inventory.service.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    private static final int MAX_RETRIES = 3;

    private final StockMapper stockMapper;
    private final ProductClient productClient;

    public StockServiceImpl(StockMapper stockMapper, ProductClient productClient) {
        this.stockMapper = stockMapper;
        this.productClient = productClient;
    }

    @Override
    public Stock getBySkuId(Long skuId) {
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
        if (stock == null) {
            throw new BusinessException(InventoryErrorCode.STOCK_NOT_FOUND);
        }
        return stock;
    }

    @Override
    public List<Stock> batchQuery(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        return stockMapper.selectList(new LambdaQueryWrapper<Stock>().in(Stock::getSkuId, skuIds));
    }

    @Override
    public Page<StockVO> list(Long skuId, Integer stockStatus, int page, int size) {
        Page<Stock> result = stockMapper.selectPage(new Page<>(page, size), buildListWrapper(skuId, stockStatus, null));
        return enrichPage(result);
    }

    @Override
    public Page<StockVO> listForMerchant(Long merchantId, Long skuId, Integer stockStatus, int page, int size) {
        List<Long> merchantSkuIds = productClient.listSkuIdsByMerchant(merchantId).getData();
        if (merchantSkuIds == null || merchantSkuIds.isEmpty()) {
            return new Page<>(page, size, 0);
        }
        Page<Stock> result = stockMapper.selectPage(new Page<>(page, size), buildListWrapper(skuId, stockStatus, merchantSkuIds));
        return enrichPage(result);
    }

    private LambdaQueryWrapper<Stock> buildListWrapper(Long skuId, Integer stockStatus, List<Long> skuIds) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        if (skuIds != null) {
            wrapper.in(Stock::getSkuId, skuIds);
        }
        if (skuId != null) {
            wrapper.eq(Stock::getSkuId, skuId);
        }
        if (stockStatus != null) {
            if (stockStatus == 0) {
                wrapper.eq(Stock::getAvailableStock, 0);
            } else if (stockStatus == 1) {
                wrapper.lt(Stock::getAvailableStock, 10).gt(Stock::getAvailableStock, 0);
            } else if (stockStatus == 2) {
                wrapper.ge(Stock::getAvailableStock, 10);
            }
        }
        wrapper.orderByDesc(Stock::getUpdatedAt);
        return wrapper;
    }

    private Page<StockVO> enrichPage(Page<Stock> result) {
        List<StockVO> vos = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        if (!vos.isEmpty()) {
            List<Long> skuIds = vos.stream().map(StockVO::getSkuId).distinct().toList();
            try {
                List<SkuBatchVO> skuInfos = productClient.batchQuerySkus(skuIds).getData();
                if (skuInfos != null && !skuInfos.isEmpty()) {
                    Map<Long, SkuBatchVO> skuMap = skuInfos.stream()
                            .collect(Collectors.toMap(SkuBatchVO::getSkuId, Function.identity(), (a, b) -> a));
                    for (StockVO vo : vos) {
                        SkuBatchVO info = skuMap.get(vo.getSkuId());
                        if (info != null) {
                            vo.setSkuName(info.getSkuName());
                            vo.setSpuName(info.getSpuName());
                            vo.setPrice(info.getPrice());
                        }
                    }
                }
            } catch (Exception ignored) {
                // best effort enrich only
            }
        }
        Page<StockVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

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
            Stock existing = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
            if (existing != null) {
                if (totalStock < existing.getLockedStock()) {
                    throw new BusinessException(InventoryErrorCode.TOTAL_STOCK_BELOW_LOCKED);
                }
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

    @Transactional
    @Override
    public void setStockForMerchant(Long merchantId, Long skuId, int totalStock) {
        ensureMerchantOwnsSku(merchantId, skuId);
        setStock(skuId, totalStock);
    }

    private void ensureMerchantOwnsSku(Long merchantId, Long skuId) {
        SkuOwnerVO owner = productClient.querySkuOwner(skuId).getData();
        if (owner == null || owner.getMerchantId() == null || !merchantId.equals(owner.getMerchantId())) {
            throw new BusinessException(InventoryErrorCode.STOCK_FORBIDDEN);
        }
    }

    @Override
    public StockVO toVO(Stock stock) {
        StockVO vo = new StockVO();
        vo.setId(stock.getId());
        vo.setSkuId(stock.getSkuId());
        vo.setTotalStock(stock.getTotalStock());
        vo.setLockedStock(stock.getLockedStock());
        vo.setAvailableStock(stock.getAvailableStock());
        return vo;
    }
}
