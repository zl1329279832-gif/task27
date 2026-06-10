package com.training;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.entity.AuditLog;
import com.training.mapper.AuditLogMapper;
import com.training.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Log Service Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Nested
    @DisplayName("log")
    class LogTests {

        @Test
        @DisplayName("should create audit log entry with all fields")
        void shouldCreateAuditLog() {
            when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

            auditLogService.log("PATH_GENERATED", "LEARNING_PATH", 1L,
                    100L, "ADMIN", Map.of("courseId", 10));

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogMapper).insert(captor.capture());

            AuditLog saved = captor.getValue();
            assertEquals("PATH_GENERATED", saved.getAction());
            assertEquals("LEARNING_PATH", saved.getTargetType());
            assertEquals(1L, saved.getTargetId());
            assertEquals(100L, saved.getActorId());
            assertEquals("ADMIN", saved.getActorRole());
            assertNotNull(saved.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("query")
    class QueryTests {

        @Test
        @DisplayName("should query by target type and id")
        void shouldQueryByTarget() {
            auditLogService.query("LEARNING_PATH", 1L, 1, 10);
            verify(auditLogMapper).selectPage(any(), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("should query by actor")
        void shouldQueryByActor() {
            auditLogService.queryByActor(100L, 1, 10);
            verify(auditLogMapper).selectPage(any(), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("should query by action")
        void shouldQueryByAction() {
            auditLogService.queryByAction("PATH_GENERATED", 1, 10);
            verify(auditLogMapper).selectPage(any(), any(LambdaQueryWrapper.class));
        }
    }
}
