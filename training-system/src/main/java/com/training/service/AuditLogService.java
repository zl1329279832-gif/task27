package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.AuditLog;

public interface AuditLogService {

    void log(Long operatorId, String operatorRole, String actionType,
             String targetType, Long targetId, String details);

    IPage<AuditLog> list(int page, int size, String actionType,
                         String targetType, Long operatorId);

    AuditLog getById(Long id);
}
