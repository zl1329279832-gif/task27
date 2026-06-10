package com.training.service;

import com.training.dto.request.LoginRequest;
import com.training.dto.request.RegisterRequest;
import com.training.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse refreshToken(String refreshToken);
}
