package com.training.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificateRevokeRequest {
    @NotBlank(message = "撤销原因不能为空")
    private String reason;
}
