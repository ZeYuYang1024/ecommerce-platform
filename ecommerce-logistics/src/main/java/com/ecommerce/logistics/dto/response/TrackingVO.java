package com.ecommerce.logistics.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrackingVO {
    private String shippingNo;
    private String providerName;
    private String providerCode;
    private String trackingNo;
    private Integer shippingStatus;
    private String shippingStatusText;
    private List<TraceNode> tracks;

    @Data
    public static class TraceNode {
        private LocalDateTime time;
        private String desc;
        private String location;
        private String eventType;
    }
}
