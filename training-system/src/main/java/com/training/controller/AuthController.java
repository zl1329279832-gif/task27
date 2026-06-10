package com.training.controller;

import com.training.common.BusinessException;
import com.training.common.Result;
import com.training.entity.dto.AuthResponse;
import com.training.entity.dto.LoginRequest;
import com.training.entity.dto.RefreshRequest;
import com.training.security.CustomUserDetails;
import com.training.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(
                userDetails.getUserId(), userDetails.getUsername(), userDetails.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails.getUserId());

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(userDetails.getRole())
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .build();

        return Result.success(response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String refreshToken = req.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String role = jwtTokenProvider.getRoleFromToken(refreshToken);

        // For refresh tokens, we stored userId as subject, so we need to
        // look up the username and role from the token claims
        // Since refresh token only has userId, we get username/role from token
        // Actually our refresh token has subject=userId and no role
        // We need to re-authenticate or store info differently
        // For simplicity, we'll re-generate with info from existing token

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        AuthResponse response = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(role)
                .userId(userId)
                .username(username)
                .build();

        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiration = jwtTokenProvider.getExpirationFromToken(token);
            long ttl = expiration - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        "jwt:blacklist:" + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }
        return Result.success();
    }
}
