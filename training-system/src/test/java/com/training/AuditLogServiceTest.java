package com.training;

import com.training.entity.AuditLog;
import com.training.mapper.AuditLogMapper;
import com.training.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Log Service Tests")
class AuditLogServiceTest {

    @Mock private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    @DisplayName("should persist log with all fields")
    void shouldPersistLogWithAllFields() {
        auditLogService.log(1L, "ADMIN", "PATH_GENERATED", "LEARNING_PATH", 100L, "{\"key\":\"value\"}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals(1L, saved.getOperatorId());
        assertEquals("ADMIN", saved.getOperatorRole());
        assertEquals("PATH_GENERATED", saved.getActionType());
        assertEquals("LEARNING_PATH", saved.getTargetType());
        assertEquals(100L, saved.getTargetId());
        assertEquals("{\"key\":\"value\"}", saved.getDetails());
    }

    @Test
    @DisplayName("should handle null operator fields")
    void shouldHandleNullOperator() {
        auditLogService.log(null, null, "TASK_CREATED", "REMEDIAL_TASK", 50L, "{}");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        assertNull(captor.getValue().getOperatorId());
        assertNull(captor.getValue().getOperatorRole());
        assertEquals("TASK_CREATED", captor.getValue().getActionType());
    }
}
