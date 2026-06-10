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
public class Certificate {

    private Long id;

    private String certNo;

    private Long studentId;

    private Long courseId;

    private Long examId;

    private String title;

    private String description;

    private LocalDateTime issuedAt;

    private LocalDateTime expiredAt;

    private String verifyToken;

    private LocalDateTime verifyTokenExpiresAt;

    private String status;

    private String fileUrl;

    private LocalDateTime createdAt;
}
