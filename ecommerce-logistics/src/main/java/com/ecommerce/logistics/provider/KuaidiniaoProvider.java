package com.ecommerce.logistics.provider;

import com.ecommerce.logistics.provider.dto.TrackingQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 快递鸟 (Kuaidiniao) logistics tracking provider.
 * <p>
 * Calls the 快递鸟 instant query and subscription APIs via HTTP POST with JSON body.
 * Request signing uses MD5(requestData + apiKey).toUpperCase().
 * Activated when {@code logistics.provider.active=kuaidiniao}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "logistics.provider.active", havingValue = "kuaidiniao")
public class KuaidiniaoProvider implements AggregationProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiSecret;
    private final String baseUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public KuaidiniaoProvider(
            @Value("${logistics.provider.kuaidiniao.api-key}") String apiKey,
            @Value("${logistics.provider.kuaidiniao.api-secret}") String apiSecret,
            @Value("${logistics.provider.kuaidiniao.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProviderCode() {
        return "kuaidiniao";
    }

    @Override
    public TrackingQueryResponse queryTracking(String trackingNo, String expressCode) {
        try {
            String requestData = objectMapper.writeValueAsString(Map.of(
                    "ShipperCode", expressCode,
                    "LogisticCode", trackingNo
            ));

            String dataSign = md5(requestData + apiKey).toUpperCase();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("DataSign", dataSign);

            HttpEntity<String> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/dist", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseTrackingResponse(trackingNo, expressCode, response.getBody());
            }

            return TrackingQueryResponse.builder()
                    .success(false)
                    .trackingNo(trackingNo)
                    .expressCode(expressCode)
                    .errorMessage("API returned status: " + response.getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error("Kuaidiniao queryTracking failed: trackingNo={}, expressCode={}", trackingNo, expressCode, e);
            return TrackingQueryResponse.builder()
                    .success(false)
                    .trackingNo(trackingNo)
                    .expressCode(expressCode)
                    .errorMessage("Query failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean subscribeTracking(String trackingNo, String expressCode, String callbackUrl) {
        try {
            String requestData = objectMapper.writeValueAsString(Map.of(
                    "ShipperCode", expressCode,
                    "LogisticCode", trackingNo,
                    "RequestType", "1002",
                    "CallbackUrl", callbackUrl
            ));

            String dataSign = md5(requestData + apiKey).toUpperCase();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("DataSign", dataSign);

            HttpEntity<String> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/dist", entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                boolean success = json.has("Success") && json.get("Success").asBoolean();
                log.info("Kuaidiniao subscribe result: trackingNo={}, success={}", trackingNo, success);
                return success;
            }

            return false;
        } catch (Exception e) {
            log.error("Kuaidiniao subscribeTracking failed: trackingNo={}", trackingNo, e);
            return false;
        }
    }

    /**
     * Parse the 快递鸟 JSON response into {@link TrackingQueryResponse}.
     * <p>
     * Expected response format:
     * <pre>
     * {
     *   "Success": true,
     *   "State": "3",
     *   "Traces": [
     *     { "AcceptTime": "2024-01-01 12:00:00", "AcceptStation": "已签收" }
     *   ]
     * }
     * </pre>
     */
    private TrackingQueryResponse parseTrackingResponse(String trackingNo, String expressCode, String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            boolean success = json.has("Success") && json.get("Success").asBoolean();

            if (!success) {
                String reason = json.has("Reason") ? json.get("Reason").asText() : "Unknown error";
                return TrackingQueryResponse.builder()
                        .success(false)
                        .trackingNo(trackingNo)
                        .expressCode(expressCode)
                        .errorMessage(reason)
                        .build();
            }

            String state = json.has("State") ? json.get("State").asText() : "";
            List<TrackingQueryResponse.TraceItem> traces = new ArrayList<>();

            if (json.has("Traces") && json.get("Traces").isArray()) {
                for (JsonNode trace : json.get("Traces")) {
                    String acceptTime = trace.has("AcceptTime") ? trace.get("AcceptTime").asText() : "";
                    String acceptStation = trace.has("AcceptStation") ? trace.get("AcceptStation").asText() : "";

                    LocalDateTime time;
                    try {
                        time = LocalDateTime.parse(acceptTime, DATE_FORMATTER);
                    } catch (Exception e) {
                        log.warn("Failed to parse trace time: {}", acceptTime);
                        time = LocalDateTime.now();
                    }

                    traces.add(TrackingQueryResponse.TraceItem.builder()
                            .time(time)
                            .desc(acceptStation)
                            .status(mapStateToStatus(state))
                            .location(extractLocation(acceptStation))
                            .build());
                }
            }

            return TrackingQueryResponse.builder()
                    .success(true)
                    .trackingNo(trackingNo)
                    .expressCode(expressCode)
                    .traces(traces)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse kuaidiniao response: {}", responseBody, e);
            return TrackingQueryResponse.builder()
                    .success(false)
                    .trackingNo(trackingNo)
                    .expressCode(expressCode)
                    .errorMessage("Parse error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Map 快递鸟 State field to a readable status code.
     * 0=no trace, 1=picked up, 2=in transit, 3=delivered, 4=exception
     */
    private String mapStateToStatus(String state) {
        return switch (state) {
            case "1" -> "PICKED_UP";
            case "2" -> "IN_TRANSIT";
            case "3" -> "DELIVERED";
            case "4" -> "EXCEPTION";
            default -> state;
        };
    }

    /**
     * Extract a location hint from the trace station description.
     * 快递鸟 station text often contains location in brackets: 【上海市】
     */
    private String extractLocation(String station) {
        if (station == null || station.isEmpty()) {
            return "";
        }
        int idx = station.indexOf("【");
        if (idx >= 0) {
            int end = station.indexOf("】", idx);
            if (end > idx) {
                return station.substring(idx + 1, end);
            }
        }
        return station.length() > 20 ? station.substring(0, 20) : station;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 digest failed", e);
        }
    }
}
