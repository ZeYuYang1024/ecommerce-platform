package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.GlobalExceptionHandler;
import com.ecommerce.knowledge.service.KbCategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantKbCategoryControllerValidationTest {

    @Mock
    private KbCategoryService categoryService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantKbCategoryController(categoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldRejectMissingNameForMerchant() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/knowledge/categories")
                        .header("X-Merchant-Id", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(96010));

        verifyNoInteractions(categoryService);
    }

    private static final class CreateCategoryPayload {
        public String code = "merchant-cat";
    }
}
