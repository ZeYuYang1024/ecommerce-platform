package com.ecommerce.knowledge.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void shouldDeserializePlatformDateTimeFormatForFeignDtos() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ReflectionTestUtils.setField(config, "dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        ReflectionTestUtils.setField(config, "dateFormat", "yyyy-MM-dd");
        ReflectionTestUtils.setField(config, "timeFormat", "HH:mm:ss");
        ReflectionTestUtils.setField(config, "timeZone", "Asia/Shanghai");

        JsonMapper mapper = config.jsonMapper();

        Result<Page<OrderVO>> restored = mapper.readValue("""
                {
                  "code": 0,
                  "message": "ok",
                  "data": {
                    "records": [
                      {
                        "id": "1",
                        "orderNo": "ORD202605110001",
                        "createdAt": "2026-05-11 22:25:33"
                      }
                    ]
                  }
                }
                """, new TypeReference<>() {
        });

        assertThat(restored.getData().getRecords()).hasSize(1);
        assertThat(restored.getData().getRecords().getFirst().getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 5, 11, 22, 25, 33));
    }
}
