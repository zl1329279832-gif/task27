package com.training.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRevocation {

    private Long id;

    private Long certificateId;

    private String reason;

    private Long revokedBy;

    private LocalDateTime revokedAt;
}
