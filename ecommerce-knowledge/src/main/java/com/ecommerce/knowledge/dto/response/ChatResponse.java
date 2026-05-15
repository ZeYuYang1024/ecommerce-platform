package com.ecommerce.knowledge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String sessionId;
    private List<Source> sources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String documentTitle;
        private String chunkText;
        private Double score;
    }
}
