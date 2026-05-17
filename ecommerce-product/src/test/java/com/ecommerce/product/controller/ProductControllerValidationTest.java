package com.ecommerce.product.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.product.service.BrandService;
import com.ecommerce.product.service.ProductService;
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
class ProductControllerValidationTest {

    @Mock
    private ProductService productService;

    @Mock
    private BrandService brandService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService, brandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantCreateShouldRejectMissingCategoryId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", "2001")
                        .content("""
                                {
                                  "spu": {
                                    "name": "merchant-product"
                                  },
                                  "skus": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("categoryId")));

        verifyNoInteractions(productService);
    }

    @Test
    void merchantUpdateShouldRejectMissingName() throws Exception {
        mockMvc.perform(put("/api/v1/admin/merchant/products/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", "2001")
                        .content("""
                                {
                                  "categoryId": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verifyNoInteractions(productService);
        verifyNoInteractions(brandService);
    }

    @Test
    void merchantUpdateShouldRejectMissingCategoryId() throws Exception {
        mockMvc.perform(put("/api/v1/admin/merchant/products/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", "2001")
                        .content("""
                                {
                                  "name": "merchant-product"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("categoryId")));

        verifyNoInteractions(productService);
        verifyNoInteractions(brandService);
    }
}
