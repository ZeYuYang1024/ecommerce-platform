package com.ecommerce.member.controller.internal;

import com.ecommerce.member.dto.response.internal.PointsReserveResponse;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.service.PointsReservationService;
import com.ecommerce.member.service.RefundCompensationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberInternalControllerTest {

    @Mock
    private PointsReservationService pointsReservationService;

    @Mock
    private RefundCompensationService refundCompensationService;

    @InjectMocks
    private MemberInternalController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReservePoints() throws Exception {
        PointsReserveResponse response = new PointsReserveResponse();
        response.setReservationNo("PR1");
        response.setReservedPoints(120);
        response.setStatus("RESERVED");
        when(pointsReservationService.reserve(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/internal/member/points/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":10001,"orderNo":"ORD-1","sceneType":"ORDER_DEDUCTION","points":120,"idempotencyKey":"reserve:ORD-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationNo").value("PR1"))
                .andExpect(jsonPath("$.data.reservedPoints").value(120));
    }

    @Test
    void shouldReplayRefundCompensation() throws Exception {
        RefundCompensationResult response = new RefundCompensationResult();
        response.setDuplicate(false);
        response.setRestoredPoints(120);
        response.setReversedGrowth(199);
        when(refundCompensationService.compensate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/internal/member/refund-compensations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refundNo":"RF1","orderNo":"ORD-1","userId":10001,"refundAmount":199.00,"refundType":"FULL","idempotencyKey":"refund:ORD-1:RF1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restoredPoints").value(120));
    }
}
