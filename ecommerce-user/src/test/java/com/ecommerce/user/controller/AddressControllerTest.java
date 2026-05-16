package com.ecommerce.user.controller;

import com.ecommerce.user.dto.request.AddressRequest;
import com.ecommerce.user.dto.response.AddressVO;
import com.ecommerce.user.service.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressController(addressService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldListCurrentUserAddressesFromHeader() throws Exception {
        AddressVO address = new AddressVO();
        address.setId(10L);
        address.setUserId(1L);
        address.setReceiverName("Alice");
        address.setDetail("Road 100");
        when(addressService.listByUserId(1L)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/v1/users/addresses/current").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].userId").value(1))
                .andExpect(jsonPath("$.data[0].receiverName").value("Alice"))
                .andExpect(jsonPath("$.data[0].detail").value("Road 100"));

        verify(addressService).listByUserId(1L);
    }

    @Test
    void shouldRequireUserIdHeaderForCurrentAddresses() throws Exception {
        mockMvc.perform(get("/api/v1/users/addresses/current"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addressService);
    }

    @Test
    void shouldRequireAuthorizationHeaderForUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/users/addresses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addressService);
    }

    @Test
    void shouldPassAuthorizationHeaderForUpdate() throws Exception {
        AddressVO address = new AddressVO();
        address.setId(1L);
        address.setUserId(1L);
        when(addressService.update(eq("Bearer token"), eq(1L), any(AddressRequest.class))).thenReturn(address);

        mockMvc.perform(put("/api/v1/users/addresses/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));

        verify(addressService).update(eq("Bearer token"), eq(1L), any(AddressRequest.class));
    }

    @Test
    void shouldRequireAuthorizationHeaderForDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/users/addresses/1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addressService);
    }

    @Test
    void shouldPassAuthorizationHeaderForDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/users/addresses/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(addressService).delete("Bearer token", 1L);
    }

    @Test
    void shouldRequireAuthorizationHeaderForSetDefault() throws Exception {
        mockMvc.perform(put("/api/v1/users/addresses/1/default"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addressService);
    }

    @Test
    void shouldPassAuthorizationHeaderForSetDefault() throws Exception {
        mockMvc.perform(put("/api/v1/users/addresses/1/default")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(addressService).setDefault("Bearer token", 1L);
    }

    private AddressRequest validRequest() {
        AddressRequest request = new AddressRequest();
        request.setReceiverName("Alice");
        request.setReceiverPhone("13800001111");
        request.setProvince("Province");
        request.setCity("City");
        request.setDistrict("District");
        request.setDetail("Road 100");
        return request;
    }
}
