package com.training.controller;

import com.training.dto.request.LoginRequest;
import com.training.dto.request.RegisterRequest;
import com.training.dto.response.AuthResponse;
import com.training.dto.response.Result;
import com.training.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("刷新Token");
        AuthResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }
}
