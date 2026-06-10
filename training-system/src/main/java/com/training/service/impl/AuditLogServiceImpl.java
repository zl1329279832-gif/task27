package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.AuditLog;
import com.training.mapper.AuditLogMapper;
import com.training.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void log(Long operatorId, String operatorRole, String actionType,
                    String targetType, Long targetId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .operatorId(operatorId)
                .operatorRole(operatorRole)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();
        auditLogMapper.insert(auditLog);
    }

    @Override
    public IPage<AuditLog> list(int page, int size, String actionType,
                                String targetType, Long operatorId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (actionType != null) wrapper.eq(AuditLog::getActionType, actionType);
        if (targetType != null) wrapper.eq(AuditLog::getTargetType, targetType);
        if (operatorId != null) wrapper.eq(AuditLog::getOperatorId, operatorId);
        wrapper.orderByDesc(AuditLog::getCreatedAt);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public AuditLog getById(Long id) {
        AuditLog log = auditLogMapper.selectById(id);
        if (log == null) throw new BusinessException("审计记录不存在");
        return log;
    }
}
