package com.training;

import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.KnowledgeMasteryService;
import com.training.service.MakeupExamTaskService;
import com.training.service.RemedialTaskService;
import com.training.service.impl.LearningPathServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Learning Path Service Tests")
class LearningPathServiceTest {

    @Mock private LearningPathMapper learningPathMapper;
    @Mock private LearningPathItemMapper learningPathItemMapper;
    @Mock private KnowledgeMasteryService knowledgeMasteryService;
    @Mock private RemedialTaskService remedialTaskService;
    @Mock private MakeupExamTaskService makeupExamTaskService;
    @Mock private AuditLogService auditLogService;
    @Mock private GradeMapper gradeMapper;
    @Mock private ExamMapper examMapper;
    @Mock private ChapterMapper chapterMapper;

    @InjectMocks
    private LearningPathServiceImpl learningPathService;

    @Nested
    @DisplayName("generatePath")
    class GeneratePathTests {

        @Test
        @DisplayName("should create remedial tasks for unmastered chapters")
        void shouldCreateRemedialTasksForUnmasteredChapters() {
            KnowledgeMastery m1 = KnowledgeMastery.builder()
                    .chapterId(1L).masteryLevel("NOT_MASTERED").build();
            KnowledgeMastery m2 = KnowledgeMastery.builder()
                    .chapterId(2L).masteryLevel("PARTIALLY_MASTERED").build();
            KnowledgeMastery m3 = KnowledgeMastery.builder()
                    .chapterId(3L).masteryLevel("MASTERED").build();

            when(knowledgeMasteryService.evaluateMastery(1L, 10L)).thenReturn(List.of(m1, m2, m3));
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            RemedialTask task1 = RemedialTask.builder().id(101L).build();
            RemedialTask task2 = RemedialTask.builder().id(102L).build();
            when(remedialTaskService.create(eq(1L), eq(10L), eq(1L), any(), eq("MASTERY_CHECK"))).thenReturn(task1);
            when(remedialTaskService.create(eq(1L), eq(10L), eq(2L), any(), eq("MASTERY_CHECK"))).thenReturn(task2);

            when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());

            LearningPath result = learningPathService.generatePath(1L, 10L, "MANUAL");

            // 2 remedial tasks for NOT_MASTERED and PARTIALLY_MASTERED
            verify(remedialTaskService, times(2)).create(eq(1L), eq(10L), anyLong(), any(), anyString());
            verify(learningPathItemMapper, times(2)).insert(any(LearningPathItem.class));
            verify(auditLogService).log(any(), any(), eq("PATH_GENERATED"), eq("LEARNING_PATH"), any(), anyString());
        }

        @Test
        @DisplayName("should create makeup exam task for failed exam")
        void shouldCreateMakeupExamTaskForFailedExam() {
            // All chapters mastered
            KnowledgeMastery m = KnowledgeMastery.builder()
                    .chapterId(1L).masteryLevel("MASTERED").build();
            when(knowledgeMasteryService.evaluateMastery(1L, 10L)).thenReturn(List.of(m));
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Failed grade
            Grade failedGrade = Grade.builder().id(1L).studentId(1L).examId(50L).courseId(10L).pass(0).build();
            when(gradeMapper.selectList(any()))
                    .thenReturn(List.of(failedGrade))  // first call: failed
                    .thenReturn(Collections.emptyList()); // second call: passed (none)

            MakeupExamTask task = MakeupExamTask.builder().id(201L).build();
            when(makeupExamTaskService.create(eq(1L), eq(10L), eq(50L), any(), eq("EXAM_FAILED"))).thenReturn(task);

            learningPathService.generatePath(1L, 10L, "MANUAL");

            verify(makeupExamTaskService).create(eq(1L), eq(10L), eq(50L), any(), eq("EXAM_FAILED"));
        }

        @Test
        @DisplayName("should expire existing paths on regeneration")
        void shouldExpireExistingPaths() {
            when(knowledgeMasteryService.evaluateMastery(1L, 10L)).thenReturn(Collections.emptyList());

            LearningPath existingPath = LearningPath.builder()
                    .id(99L).studentId(1L).courseId(10L).status("IN_PROGRESS").build();
            when(learningPathMapper.selectList(any())).thenReturn(List.of(existingPath));
            when(gradeMapper.selectList(any())).thenReturn(Collections.emptyList());

            learningPathService.generatePath(1L, 10L, "MANUAL");

            verify(learningPathMapper).updateById(argThat(p ->
                    p.getId() != null && p.getId().equals(99L) && "EXPIRED".equals(p.getStatus())));
        }
    }

    @Nested
    @DisplayName("checkPathCompletion")
    class CheckPathCompletionTests {

        @Test
        @DisplayName("should mark path completed when all items done")
        void shouldMarkPathCompletedWhenAllItemsDone() {
            LearningPath path = LearningPath.builder()
                    .id(1L).status("IN_PROGRESS").totalItems(2).completedItems(0).build();
            when(learningPathMapper.selectById(1L)).thenReturn(path);

            LearningPathItem i1 = LearningPathItem.builder().id(1L).pathId(1L).status("COMPLETED").build();
            LearningPathItem i2 = LearningPathItem.builder().id(2L).pathId(1L).status("COMPLETED").build();
            when(learningPathItemMapper.selectList(any())).thenReturn(List.of(i1, i2));

            learningPathService.checkPathCompletion(1L);

            verify(learningPathMapper).updateById(argThat(p ->
                    "COMPLETED".equals(p.getStatus()) && p.getCompletedItems() == 2));
        }
    }
}
