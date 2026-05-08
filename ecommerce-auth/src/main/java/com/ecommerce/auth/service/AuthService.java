package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse adminLogin(LoginRequest request);
    Long validateToken(String token);
}
