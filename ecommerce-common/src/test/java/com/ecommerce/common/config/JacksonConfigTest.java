package com.ecommerce.common.config;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void shouldSerializeAndDeserializeJavaTimeTypesWithConfiguredFormats() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ReflectionTestUtils.setField(config, "dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        ReflectionTestUtils.setField(config, "dateFormat", "yyyy-MM-dd");
        ReflectionTestUtils.setField(config, "timeFormat", "HH:mm:ss");
        ReflectionTestUtils.setField(config, "timeZone", "Asia/Shanghai");

        JsonMapper mapper = config.jsonMapper();
        TimePayload payload = new TimePayload();
        payload.id = 9007199254740992L;
        payload.createdAt = LocalDateTime.of(2026, 5, 17, 14, 30, 45);
        payload.bizDate = LocalDate.of(2026, 5, 17);
        payload.bizTime = LocalTime.of(9, 8, 7);

        String json = mapper.writeValueAsString(payload);

        assertThat(json).contains("\"id\":\"9007199254740992\"");
        assertThat(json).contains("\"createdAt\":\"2026-05-17 14:30:45\"");
        assertThat(json).contains("\"bizDate\":\"2026-05-17\"");
        assertThat(json).contains("\"bizTime\":\"09:08:07\"");

        TimePayload restored = mapper.readValue("""
                {
                  "id":"9007199254740992",
                  "createdAt":"2026-05-17 14:30:45",
                  "bizDate":"2026-05-17",
                  "bizTime":"09:08:07"
                }
                """, TimePayload.class);

        assertThat(restored.createdAt).isEqualTo(LocalDateTime.of(2026, 5, 17, 14, 30, 45));
        assertThat(restored.bizDate).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(restored.bizTime).isEqualTo(LocalTime.of(9, 8, 7));
    }

    static class TimePayload {
        public Long id;
        public LocalDateTime createdAt;
        public LocalDate bizDate;
        public LocalTime bizTime;
    }
}
