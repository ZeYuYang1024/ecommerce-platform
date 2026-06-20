package com.ecommerce.warehouse.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateOutboundRequest {
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotNull(message = "出库类型不能为空")
    private String outboundType;

    private Long shippingId;

    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    private String remark;

    @NotEmpty(message = "出库明细不能为空")
    private List<OutboundItem> items;

    @Data
    public static class OutboundItem {
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

        private Long binId;
    }
}
