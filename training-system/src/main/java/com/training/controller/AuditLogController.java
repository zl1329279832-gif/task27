package com.training.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.common.Result;
import com.training.entity.AuditLog;
import com.training.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<IPage<AuditLog>> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String actionType,
                                         @RequestParam(required = false) String targetType,
                                         @RequestParam(required = false) Long operatorId) {
        return Result.success(auditLogService.list(page, size, actionType, targetType, operatorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public Result<AuditLog> getById(@PathVariable Long id) {
        return Result.success(auditLogService.getById(id));
    }
}
