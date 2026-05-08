package com.ecommerce.inventory.dto.response;

public class StockVO {
    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
    public Integer getLockedStock() { return lockedStock; }
    public void setLockedStock(Integer lockedStock) { this.lockedStock = lockedStock; }
    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }
}
