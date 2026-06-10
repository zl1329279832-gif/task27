package com.training.controller;

import com.training.common.Result;
import com.training.service.AuditLogService;
import com.training.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AUDITOR','ADMIN')")
public class AuditController {

    private final CertificateService certificateService;
    private final AuditLogService auditLogService;

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

    @GetMapping("/logs")
    public Result<?> listAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String action) {
        if (action != null) {
            return Result.success(auditLogService.queryByAction(action, page, size));
        }
        return Result.success(auditLogService.query(targetType, targetId, page, size));
    }

    @GetMapping("/logs/target/{type}/{id}")
    public Result<?> getAuditLogsByTarget(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(auditLogService.query(type, id, page, size));
    }

    @GetMapping("/logs/actor/{actorId}")
    public Result<?> getAuditLogsByActor(
            @PathVariable Long actorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(auditLogService.queryByActor(actorId, page, size));
    }
}
