package com.training.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private Long userId;

    private String username;

    private List<String> roles;
}
