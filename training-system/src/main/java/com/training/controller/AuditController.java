package com.training.controller;

import com.training.common.Result;
import com.training.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AUDITOR')")
public class AuditController {

    private final CertificateService certificateService;

    @GetMapping("/certificates")
    public Result<?> listCertificates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(certificateService.listAll(page, size, status));
    }

    @GetMapping("/certificates/{id}")
    public Result<?> getCertificate(@PathVariable Long id) {
        return Result.success(certificateService.getById(id));
    }
}
