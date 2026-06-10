package com.training.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CertificateRevokeRequest {

    @NotNull
    private Long certificateId;

    @NotBlank
    private String reason;
}
