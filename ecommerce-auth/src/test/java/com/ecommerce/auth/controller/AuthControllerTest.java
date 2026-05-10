package com.ecommerce.auth.controller;

import com.ecommerce.auth.common.AuthErrorCode;
import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController controller;

    @Test
    void register_shouldReturnToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("123456");

        LoginResponse resp = LoginResponse.of("jwt-token", 1L, "newuser", "user");
        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        Result<LoginResponse> result = controller.register(req);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getToken()).isEqualTo("jwt-token");
        assertThat(result.getData().getUsername()).isEqualTo("newuser");
    }

    @Test
    void login_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("123456");

        LoginResponse resp = LoginResponse.of("jwt-token", 1L, "testuser", "user");
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        Result<LoginResponse> result = controller.login(req);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getToken()).isEqualTo("jwt-token");
    }

    @Test
    void adminLogin_shouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("admin123");

        LoginResponse resp = LoginResponse.of("admin-jwt-token", 100L, "admin", "super_admin");
        when(authService.adminLogin(any(LoginRequest.class))).thenReturn(resp);

        Result<LoginResponse> result = controller.adminLogin(req);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getToken()).isEqualTo("admin-jwt-token");
    }

    @Test
    void validate_shouldReturnUserId() {
        when(authService.validateToken("Bearer valid-token")).thenReturn(1L);

        Result<Long> result = controller.validate("Bearer valid-token");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void validate_shouldPropagateError_whenTokenInvalid() {
        when(authService.validateToken("Bearer invalid"))
                .thenThrow(new BusinessException(AuthErrorCode.TOKEN_INVALID));

        assertThatThrownBy(() -> controller.validate("Bearer invalid"))
                .isInstanceOf(BusinessException.class);
    }
}
