package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.AuditLog;

public interface AuditLogService {

    void log(String action, String targetType, Long targetId, Long actorId, String actorRole, Object details);

    IPage<AuditLog> query(String targetType, Long targetId, int page, int size);

    IPage<AuditLog> queryByActor(Long actorId, int page, int size);

    IPage<AuditLog> queryByAction(String action, int page, int size);
}
