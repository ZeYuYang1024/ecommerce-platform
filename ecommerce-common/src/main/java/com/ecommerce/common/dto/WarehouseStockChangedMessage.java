package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockChangedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Long warehouseId;
    private Integer physicalQty;
    private Integer availableQty;
    private Integer lockedQty;
    private String changeType;
    private Integer changeQty;
    private String transactionId;
    private String idempotencyKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
}
