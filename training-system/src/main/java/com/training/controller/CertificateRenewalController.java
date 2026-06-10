package com.training.controller;

import com.training.common.Result;
import com.training.security.CustomUserDetails;
import com.training.service.CertificateRenewalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificate-renewals")
@RequiredArgsConstructor
public class CertificateRenewalController {

    private final CertificateRenewalService certificateRenewalService;

    @PostMapping("/assess/{certId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public Result<?> assess(@PathVariable Long certId) {
        return Result.success(certificateRenewalService.assessRenewal(certId));
    }

    @PostMapping("/initiate/{certId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> initiate(@PathVariable Long certId) {
        Long operatorId = getCurrentUserId();
        return Result.success(certificateRenewalService.initiateRenewal(certId, operatorId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> complete(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        certificateRenewalService.completeRenewal(id, operatorId);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> reject(@PathVariable Long id, @RequestParam String reason) {
        Long operatorId = getCurrentUserId();
        certificateRenewalService.rejectRenewal(id, reason, operatorId);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<?> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(certificateRenewalService.listAll(page, size));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR','STUDENT')")
    public Result<?> listByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(certificateRenewalService.listByStudent(studentId, page, size));
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        return Result.success(certificateRenewalService.getById(id));
    }

    private Long getCurrentUserId() {
        CustomUserDetails user = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return user.getUserId();
    }
}
