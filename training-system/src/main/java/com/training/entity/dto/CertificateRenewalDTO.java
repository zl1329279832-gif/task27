package com.training.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CertificateRenewalDTO {
    private Long id;
    private Long certificateId;
    private String certNo;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseName;
    private String renewalAction;
    private String status;
    private Long newCertificateId;
    private String rejectionReason;
    private LocalDate originalExpiryDate;
    private LocalDateTime deadline;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
