package com.ecommerce.logistics.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.logistics.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CallbackControllerTest {

    @Mock
    private ShippingService shippingService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CallbackController(shippingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldPassAggregationProviderCodeToShippingService() throws Exception {
        mockMvc.perform(post("/api/v1/callback/logistics/stub")
                        .header("X-Signature", "signature-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"delivered\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("success"));

        verify(shippingService).processCallback("stub", "{\"status\":\"delivered\"}", "signature-abc");
    }
}
