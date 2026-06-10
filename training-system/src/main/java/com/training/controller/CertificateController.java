package com.training.controller;

import com.training.common.Result;
import com.training.entity.dto.CertificateIssueRequest;
import com.training.entity.dto.CertificateRevokeRequest;
import com.training.security.CustomUserDetails;
import com.training.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/api/certificates/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> issue(@Valid @RequestBody CertificateIssueRequest req) {
        Long operatorId = getCurrentUserId();
        return Result.success(certificateService.issue(req, operatorId));
    }

    @GetMapping("/api/certificates/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT','AUDITOR')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(certificateService.listByStudent(studentId, page, size));
    }

    @GetMapping("/api/certificates")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<?> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(certificateService.listAll(page, size, status));
    }

    @GetMapping("/api/certificates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','STUDENT')")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(certificateService.getById(id));
    }

    @PostMapping("/api/certificates/{id}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<?> revoke(@PathVariable Long id,
                           @Valid @RequestBody CertificateRevokeRequest req) {
        Long operatorId = getCurrentUserId();
        certificateService.revoke(id, req, operatorId);
        return Result.success();
    }

    // Public verification endpoints - no authentication required
    @GetMapping("/api/certificates/verify/{certNo}")
    public Result<?> verifyByCertNo(@PathVariable String certNo) {
        return Result.success(certificateService.verifyByCertNo(certNo));
    }

    @GetMapping("/api/certificates/verify-link/{token}")
    public Result<?> verifyByToken(@PathVariable String token) {
        return Result.success(certificateService.verifyByToken(token));
    }

    @PostMapping("/api/certificates/{id}/regenerate-token")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> regenerateToken(@PathVariable Long id) {
        return Result.success(certificateService.regenerateVerifyToken(id));
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
