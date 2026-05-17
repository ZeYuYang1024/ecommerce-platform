package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.GlobalExceptionHandler;
import com.ecommerce.knowledge.service.KbDocumentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantKbDocumentControllerValidationTest {

    @Mock
    private KbDocumentService documentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantKbDocumentController(documentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_shouldRejectMissingCategoryIdForMerchant() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/knowledge/documents")
                        .header("X-Merchant-Id", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDocumentPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(96010));

        verifyNoInteractions(documentService);
    }

    @Test
    void update_shouldRejectInvalidStatusForMerchant() throws Exception {
        mockMvc.perform(put("/api/v1/admin/merchant/knowledge/documents/1001")
                        .header("X-Merchant-Id", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "invalid-status"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(96010))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(documentService);
    }

    private static final class CreateDocumentPayload {
        public String title = "merchant doc";
        public String content = "content";
    }
}
