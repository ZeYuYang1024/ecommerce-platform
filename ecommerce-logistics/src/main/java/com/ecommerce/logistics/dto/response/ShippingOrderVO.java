package com.ecommerce.logistics.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShippingOrderVO {
    private Long id;
    private String shippingNo;
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private Long providerId;
    private String providerCode;
    private String providerName;
    private String trackingNo;
    private Integer shippingStatus;
    private String shippingStatusText;
    private Integer dispatchType;
    private Integer sourceType;
    private BigDecimal shippingFee;
    private Integer packageWeight;
    private String packageSize;
    private String waybillUrl;
    private LocalDateTime lastTraceTime;
    private String lastTraceDesc;
    private LocalDateTime shippedAt;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private List<ShippingItemVO> items;

    @Data
    public static class ShippingItemVO {
        private Long id;
        private Long orderItemId;
        private Long skuId;
        private Integer quantity;
    }
}
