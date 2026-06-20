package com.ecommerce.warehouse.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateInboundRequest {
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotNull(message = "入库类型不能为空")
    private String inboundType;

    private String sourceOrderNo;

    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    private String remark;

    @NotEmpty(message = "入库明细不能为空")
    private List<InboundItem> items;

    @Data
    public static class InboundItem {
        @NotNull(message = "SKU ID不能为空")
        private Long skuId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;
    }
}
