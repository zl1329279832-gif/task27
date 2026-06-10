package com.training;

import com.training.common.BusinessException;
import com.training.entity.LearningPathItem;
import com.training.entity.RemedialTask;
import com.training.mapper.LearningPathItemMapper;
import com.training.mapper.RemedialTaskMapper;
import com.training.service.AuditLogService;
import com.training.service.impl.RemedialTaskServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Remedial Task Service Tests")
class RemedialTaskServiceTest {

    @Mock private RemedialTaskMapper remedialTaskMapper;
    @Mock private LearningPathItemMapper learningPathItemMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private RemedialTaskServiceImpl remedialTaskService;

    @Nested
    @DisplayName("complete")
    class CompleteTests {

        @Test
        @DisplayName("should set status completed and timestamp")
        void shouldSetStatusCompleted() {
            RemedialTask task = RemedialTask.builder()
                    .id(1L).studentId(1L).courseId(10L).chapterId(1L)
                    .status("IN_PROGRESS").build();
            when(remedialTaskMapper.selectById(1L)).thenReturn(task);

            remedialTaskService.complete(1L);

            verify(remedialTaskMapper).updateById(argThat(t ->
                    "COMPLETED".equals(t.getStatus()) && t.getCompletedAt() != null));
            verify(auditLogService).log(any(), any(), eq("TASK_COMPLETED"), eq("REMEDIAL_TASK"), eq(1L), anyString());
        }

        @Test
        @DisplayName("should update path item when linked to path")
        void shouldUpdatePathItem() {
            RemedialTask task = RemedialTask.builder()
                    .id(1L).studentId(1L).courseId(10L).chapterId(1L)
                    .pathId(5L).status("IN_PROGRESS").build();
            when(remedialTaskMapper.selectById(1L)).thenReturn(task);

            LearningPathItem item = LearningPathItem.builder()
                    .id(10L).pathId(5L).itemType("REMEDIAL").refId(1L).status("PENDING").build();
            when(learningPathItemMapper.selectOne(any())).thenReturn(item);

            remedialTaskService.complete(1L);

            verify(learningPathItemMapper).updateById(argThat(i ->
                    "COMPLETED".equals(i.getStatus())));
        }

        @Test
        @DisplayName("should throw when already completed")
        void shouldThrowWhenAlreadyCompleted() {
            RemedialTask task = RemedialTask.builder()
                    .id(1L).status("COMPLETED").build();
            when(remedialTaskMapper.selectById(1L)).thenReturn(task);

            assertThrows(BusinessException.class, () -> remedialTaskService.complete(1L));
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("should throw when already completed")
        void shouldThrowWhenCompleted() {
            RemedialTask task = RemedialTask.builder()
                    .id(1L).status("COMPLETED").build();
            when(remedialTaskMapper.selectById(1L)).thenReturn(task);

            assertThrows(BusinessException.class, () -> remedialTaskService.cancel(1L));
        }
    }
}
