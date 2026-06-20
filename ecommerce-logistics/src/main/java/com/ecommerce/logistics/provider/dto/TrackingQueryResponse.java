package com.ecommerce.logistics.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingQueryResponse {
    private boolean success;
    private String errorMessage;
    private String trackingNo;
    private String expressCode;
    private List<TraceItem> traces;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceItem {
        private LocalDateTime time;
        private String desc;
        private String status;
        private String location;
    }
}
