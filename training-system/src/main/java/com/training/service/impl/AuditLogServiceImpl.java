package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.entity.AuditLog;
import com.training.mapper.AuditLogMapper;
import com.training.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void log(String action, String targetType, Long targetId,
                    Long actorId, String actorRole, Object details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .actorId(actorId)
                .actorRole(actorRole)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogMapper.insert(auditLog);
    }

    @Override
    public IPage<AuditLog> query(String targetType, Long targetId, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (targetType != null) wrapper.eq(AuditLog::getTargetType, targetType);
        if (targetId != null) wrapper.eq(AuditLog::getTargetId, targetId);
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<AuditLog> queryByActor(Long actorId, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getActorId, actorId);
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<AuditLog> queryByAction(String action, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLog::getAction, action);
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
