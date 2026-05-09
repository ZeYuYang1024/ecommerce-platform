package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {
    @NotEmpty
    private List<OrderItemRequest> items;
    @NotBlank
    private String receiverName;
    @NotBlank
    private String receiverPhone;
    @NotBlank
    private String receiverAddress;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public void setReceiverAddress(String receiverAddress) { this.receiverAddress = receiverAddress; }

    public static class OrderItemRequest {
        private Long skuId;
        private Long spuId;
        private String name;
        private String image;
        private String price;
        private Integer quantity;

        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public Long getSpuId() { return spuId; }
        public void setSpuId(Long spuId) { this.spuId = spuId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
