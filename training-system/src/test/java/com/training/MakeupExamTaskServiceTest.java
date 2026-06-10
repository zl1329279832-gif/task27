package com.training;

import com.training.common.BusinessException;
import com.training.entity.LearningPathItem;
import com.training.entity.MakeupExamTask;
import com.training.mapper.LearningPathItemMapper;
import com.training.mapper.MakeupExamTaskMapper;
import com.training.service.AuditLogService;
import com.training.service.impl.MakeupExamTaskServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Makeup Exam Task Service Tests")
class MakeupExamTaskServiceTest {

    @Mock private MakeupExamTaskMapper makeupExamTaskMapper;
    @Mock private LearningPathItemMapper learningPathItemMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private MakeupExamTaskServiceImpl makeupExamTaskService;

    @Nested
    @DisplayName("complete")
    class CompleteTests {

        @Test
        @DisplayName("should set status completed with answer sheet link")
        void shouldSetStatusCompleted() {
            MakeupExamTask task = MakeupExamTask.builder()
                    .id(1L).studentId(1L).courseId(10L).examId(50L)
                    .status("SCHEDULED").build();
            when(makeupExamTaskMapper.selectById(1L)).thenReturn(task);

            makeupExamTaskService.complete(1L, 999L);

            verify(makeupExamTaskMapper).updateById(argThat(t ->
                    "COMPLETED".equals(t.getStatus())
                            && t.getAnswerSheetId().equals(999L)
                            && t.getCompletedAt() != null));
        }

        @Test
        @DisplayName("should update path item when linked to path")
        void shouldUpdatePathItem() {
            MakeupExamTask task = MakeupExamTask.builder()
                    .id(1L).studentId(1L).courseId(10L).examId(50L)
                    .pathId(5L).status("SCHEDULED").build();
            when(makeupExamTaskMapper.selectById(1L)).thenReturn(task);

            LearningPathItem item = LearningPathItem.builder()
                    .id(10L).pathId(5L).itemType("MAKEUP_EXAM").refId(1L).status("PENDING").build();
            when(learningPathItemMapper.selectOne(any())).thenReturn(item);

            makeupExamTaskService.complete(1L, 999L);

            verify(learningPathItemMapper).updateById(argThat(i ->
                    "COMPLETED".equals(i.getStatus())));
        }
    }

    @Nested
    @DisplayName("schedule")
    class ScheduleTests {

        @Test
        @DisplayName("should set scheduled time and status")
        void shouldSetScheduledAtAndStatus() {
            MakeupExamTask task = MakeupExamTask.builder()
                    .id(1L).status("PENDING").build();
            when(makeupExamTaskMapper.selectById(1L)).thenReturn(task);

            LocalDateTime scheduledAt = LocalDateTime.of(2026, 7, 1, 10, 0);
            makeupExamTaskService.schedule(1L, scheduledAt);

            verify(makeupExamTaskMapper).updateById(argThat(t ->
                    "SCHEDULED".equals(t.getStatus())
                            && t.getScheduledAt().equals(scheduledAt)));
        }

        @Test
        @DisplayName("should throw when task is not pending")
        void shouldThrowWhenNotPending() {
            MakeupExamTask task = MakeupExamTask.builder()
                    .id(1L).status("COMPLETED").build();
            when(makeupExamTaskMapper.selectById(1L)).thenReturn(task);

            assertThrows(BusinessException.class,
                    () -> makeupExamTaskService.schedule(1L, LocalDateTime.now()));
        }
    }
}
