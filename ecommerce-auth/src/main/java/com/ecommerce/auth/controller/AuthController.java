package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.request.UpdateProfileRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.dto.response.UserVO;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.adminLogin(request));
    }

    @GetMapping("/validate")
    public Result<Long> validate(@RequestHeader("Authorization") String token) {
        return Result.ok(authService.validateToken(token));
    }

    @GetMapping("/me")
    public Result<UserVO> profile(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(authService.getProfile(userId));
    }

    @PutMapping("/me")
    public Result<UserVO> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(authService.updateProfile(userId, request.getPhone(), request.getAvatar()));
    }
}
