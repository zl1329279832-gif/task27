package com.training;

import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.entity.dto.ChapterProgressDTO;
import com.training.entity.dto.LearningPathDTO;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.KnowledgePointMasteryService;
import com.training.service.LearningRecordService;
import com.training.service.impl.LearningPathServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Learning Path Service Tests")
class LearningPathServiceTest {

    @Mock
    private LearningPathMapper learningPathMapper;

    @Mock
    private RemedialTaskMapper remedialTaskMapper;

    @Mock
    private MakeupExamMapper makeupExamMapper;

    @Mock
    private AnswerSheetMapper answerSheetMapper;

    @Mock
    private CertificateMapper certificateMapper;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private KnowledgePointMapper knowledgePointMapper;

    @Mock
    private LearningRecordService learningRecordService;

    @Mock
    private KnowledgePointMasteryService knowledgePointMasteryService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LearningPathServiceImpl learningPathService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Default: lock acquisition succeeds
        lenient().when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
    }

    // ========================================================================
    // generatePath tests
    // ========================================================================

    @Nested
    @DisplayName("generatePath")
    class GeneratePathTests {

        @Test
        @DisplayName("should generate path with makeup chapters for unmastered KPs")
        void shouldGeneratePathWithMakeupChaptersForUnmasteredKPs() {
            Long studentId = 1L;
            Long courseId = 10L;

            // All chapters complete — no incomplete chapter steps
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // One unmastered knowledge point
            KnowledgePointMastery mastery = KnowledgePointMastery.builder()
                    .id(1L)
                    .studentId(studentId)
                    .knowledgePointId(10L)
                    .courseId(courseId)
                    .masteryLevel(BigDecimal.valueOf(30))
                    .build();
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(List.of(mastery));

            // KP maps to chapter 100
            KnowledgePoint kp = KnowledgePoint.builder()
                    .id(10L)
                    .name("Java Basics")
                    .chapterId(100L)
                    .courseId(courseId)
                    .build();
            when(knowledgePointMapper.selectById(10L)).thenReturn(kp);

            // Chapter 100 exists
            Chapter chapter = Chapter.builder()
                    .id(100L)
                    .title("Chapter 1")
                    .courseId(courseId)
                    .build();
            when(chapterMapper.selectById(100L)).thenReturn(chapter);

            // No failed exams, no high tab-switch
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            LearningPath path = learningPathService.generatePath(studentId, courseId, "EXAM_FAILED", 99L);

            // Verify path has MAKEUP_CHAPTER step
            assertNotNull(path, "生成的学习路径不应为空");
            assertNotNull(path.getPathData(), "路径步骤列表不应为空");
            assertEquals(1, path.getPathData().size(), "应包含1个步骤");

            LearningPath.PathStep step = path.getPathData().get(0);
            assertEquals("MAKEUP_CHAPTER", step.getStepType(), "步骤类型应为MAKEUP_CHAPTER");
            assertEquals(100L, step.getTargetId(), "目标章节ID应为100");
            assertEquals("Chapter 1", step.getTargetTitle(), "目标标题应为Chapter 1");
            assertEquals("PENDING", step.getStatus(), "步骤状态应为PENDING");
            assertEquals(1, step.getStepOrder(), "步骤序号应为1");

            // Verify remedialTask inserted
            ArgumentCaptor<RemedialTask> taskCaptor = ArgumentCaptor.forClass(RemedialTask.class);
            verify(remedialTaskMapper).insert(taskCaptor.capture());
            RemedialTask insertedTask = taskCaptor.getValue();
            assertEquals(studentId, insertedTask.getStudentId(), "补救任务学生ID应匹配");
            assertEquals(courseId, insertedTask.getCourseId(), "补救任务课程ID应匹配");
            assertEquals(100L, insertedTask.getChapterId(), "补救任务章节ID应为100");
            assertEquals("REMEDIAL_CHAPTER", insertedTask.getTaskType(), "补救任务类型应为REMEDIAL_CHAPTER");
            assertEquals("PENDING", insertedTask.getStatus(), "补救任务状态应为PENDING");
        }

        @Test
        @DisplayName("should generate path with makeup exam step when student failed")
        void shouldGeneratePathWithMakeupExamStepWhenStudentFailed() {
            Long studentId = 2L;
            Long courseId = 20L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // First answerSheetMapper.selectList call: 1 failed sheet; second call: empty (no high tab-switch)
            AnswerSheet failedSheet = AnswerSheet.builder()
                    .id(1L)
                    .examId(5L)
                    .studentId(studentId)
                    .pass(0)
                    .status("GRADED")
                    .build();
            when(answerSheetMapper.selectList(any()))
                    .thenReturn(List.of(failedSheet))
                    .thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            LearningPath path = learningPathService.generatePath(studentId, courseId, "EXAM_FAILED", 99L);

            // Verify path has MAKEUP_EXAM step
            assertNotNull(path, "生成的学习路径不应为空");
            assertEquals(1, path.getPathData().size(), "应包含1个步骤");

            LearningPath.PathStep step = path.getPathData().get(0);
            assertEquals("MAKEUP_EXAM", step.getStepType(), "步骤类型应为MAKEUP_EXAM");
            assertEquals(5L, step.getTargetId(), "目标考试ID应为5");
            assertEquals("PENDING", step.getStatus(), "步骤状态应为PENDING");

            // Verify makeupExam inserted
            ArgumentCaptor<MakeupExam> examCaptor = ArgumentCaptor.forClass(MakeupExam.class);
            verify(makeupExamMapper).insert(examCaptor.capture());
            MakeupExam insertedExam = examCaptor.getValue();
            assertEquals(studentId, insertedExam.getStudentId(), "补考记录学生ID应匹配");
            assertEquals(courseId, insertedExam.getCourseId(), "补考记录课程ID应匹配");
            assertEquals(5L, insertedExam.getExamId(), "补考记录考试ID应为5");
            assertEquals("PENDING", insertedExam.getStatus(), "补考记录状态应为PENDING");
            assertEquals(2, insertedExam.getMaxAttempts(), "补考最大尝试次数应为2");
            assertEquals(0, insertedExam.getAttemptsUsed(), "补考已用尝试次数应为0");
        }

        @Test
        @DisplayName("should include REVIEW steps for high tab-switch students")
        void shouldIncludeReviewStepsForHighTabSwitchStudents() {
            Long studentId = 3L;
            Long courseId = 30L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // First call (failed exams): empty; Second call (high tab-switch): 1 sheet with high tabSwitchCount
            AnswerSheet highTabSheet = AnswerSheet.builder()
                    .id(2L)
                    .examId(6L)
                    .studentId(studentId)
                    .pass(1)
                    .status("GRADED")
                    .tabSwitchCount(5)
                    .build();
            when(answerSheetMapper.selectList(any()))
                    .thenReturn(Collections.emptyList())
                    .thenReturn(List.of(highTabSheet));

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            LearningPath path = learningPathService.generatePath(studentId, courseId, "SUSPICIOUS_BEHAVIOR", 99L);

            // Verify REVIEW step exists
            assertNotNull(path, "生成的学习路径不应为空");
            assertEquals(1, path.getPathData().size(), "应包含1个步骤");

            LearningPath.PathStep step = path.getPathData().get(0);
            assertEquals("REVIEW", step.getStepType(), "步骤类型应为REVIEW");
            assertEquals(courseId, step.getTargetId(), "复习步骤目标ID应为课程ID");
            assertTrue(step.getTargetTitle().contains("诚信复习"), "复习步骤标题应包含诚信复习");
            assertEquals("PENDING", step.getStatus(), "步骤状态应为PENDING");
        }

        @Test
        @DisplayName("should expire previous active paths")
        void shouldExpirePreviousActivePaths() {
            Long studentId = 4L;
            Long courseId = 40L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // No failed exams, no high tab-switch
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Existing GENERATED path
            LearningPath oldPath = LearningPath.builder()
                    .id(100L)
                    .studentId(studentId)
                    .courseId(courseId)
                    .status("GENERATED")
                    .pathData(new ArrayList<>())
                    .totalSteps(0)
                    .completedSteps(0)
                    .build();
            when(learningPathMapper.selectList(any())).thenReturn(List.of(oldPath));

            // Execute
            learningPathService.generatePath(studentId, courseId, "RE_GENERATE", 99L);

            // Verify old path status set to EXPIRED
            ArgumentCaptor<LearningPath> pathCaptor = ArgumentCaptor.forClass(LearningPath.class);
            verify(learningPathMapper).updateById(pathCaptor.capture());
            LearningPath updatedOldPath = pathCaptor.getValue();
            assertEquals(100L, updatedOldPath.getId(), "被过期的路径ID应为100");
            assertEquals("EXPIRED", updatedOldPath.getStatus(), "旧路径状态应被设置为EXPIRED");
        }

        @Test
        @DisplayName("should create RemedialTask for MAKEUP_CHAPTER steps")
        void shouldCreateRemedialTaskForMakeupChapterSteps() {
            Long studentId = 5L;
            Long courseId = 50L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // One unmastered KP
            KnowledgePointMastery mastery = KnowledgePointMastery.builder()
                    .id(2L)
                    .studentId(studentId)
                    .knowledgePointId(20L)
                    .courseId(courseId)
                    .masteryLevel(BigDecimal.valueOf(20))
                    .build();
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(List.of(mastery));

            KnowledgePoint kp = KnowledgePoint.builder()
                    .id(20L)
                    .name("OOP Concepts")
                    .chapterId(200L)
                    .courseId(courseId)
                    .build();
            when(knowledgePointMapper.selectById(20L)).thenReturn(kp);

            Chapter chapter = Chapter.builder()
                    .id(200L)
                    .title("Chapter 2")
                    .courseId(courseId)
                    .build();
            when(chapterMapper.selectById(200L)).thenReturn(chapter);

            // No failed exams, no high tab-switch
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            learningPathService.generatePath(studentId, courseId, "EXAM_FAILED", 99L);

            // Verify remedialTaskMapper.insert called
            ArgumentCaptor<RemedialTask> taskCaptor = ArgumentCaptor.forClass(RemedialTask.class);
            verify(remedialTaskMapper, times(1)).insert(taskCaptor.capture());
            RemedialTask task = taskCaptor.getValue();
            assertEquals(studentId, task.getStudentId(), "补救任务学生ID应匹配");
            assertEquals(courseId, task.getCourseId(), "补救任务课程ID应匹配");
            assertEquals(200L, task.getChapterId(), "补救任务章节ID应为200");
            assertEquals("REMEDIAL_CHAPTER", task.getTaskType(), "补救任务类型应为REMEDIAL_CHAPTER");
            assertEquals("PENDING", task.getStatus(), "补救任务状态应为PENDING");
            assertEquals(BigDecimal.valueOf(100), task.getRequiredProgress(), "补救任务要求进度应为100");
            assertEquals(BigDecimal.ZERO, task.getAchievedProgress(), "补救任务已达成进度应为0");
        }

        @Test
        @DisplayName("should create MakeupExam for MAKEUP_EXAM steps")
        void shouldCreateMakeupExamForMakeupExamSteps() {
            Long studentId = 6L;
            Long courseId = 60L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // Failed exam
            AnswerSheet failedSheet = AnswerSheet.builder()
                    .id(3L)
                    .examId(7L)
                    .studentId(studentId)
                    .pass(0)
                    .status("GRADED")
                    .build();
            when(answerSheetMapper.selectList(any()))
                    .thenReturn(List.of(failedSheet))
                    .thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            learningPathService.generatePath(studentId, courseId, "EXAM_FAILED", 99L);

            // Verify makeupExamMapper.insert called
            ArgumentCaptor<MakeupExam> examCaptor = ArgumentCaptor.forClass(MakeupExam.class);
            verify(makeupExamMapper, times(1)).insert(examCaptor.capture());
            MakeupExam exam = examCaptor.getValue();
            assertEquals(studentId, exam.getStudentId(), "补考记录学生ID应匹配");
            assertEquals(courseId, exam.getCourseId(), "补考记录课程ID应匹配");
            assertEquals(7L, exam.getExamId(), "补考记录考试ID应为7");
            assertEquals("PENDING", exam.getStatus(), "补考记录状态应为PENDING");
            assertEquals(2, exam.getMaxAttempts(), "补考最大尝试次数应为2");
            assertEquals(0, exam.getAttemptsUsed(), "补考已用尝试次数应为0");
            assertEquals(BigDecimal.valueOf(60), exam.getRequiredScore(), "补考要求分数应为60");
        }

        @Test
        @DisplayName("should write audit log")
        void shouldWriteAuditLog() {
            Long studentId = 7L;
            Long courseId = 70L;
            Long operatorId = 99L;

            // No incomplete chapters
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // No failed exams, no high tab-switch
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            LearningPath path = learningPathService.generatePath(studentId, courseId, "MANUAL", operatorId);

            // Verify auditLogService.log called
            ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> targetTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> actorIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(auditLogService).log(
                    actionCaptor.capture(),
                    targetTypeCaptor.capture(),
                    any(),
                    actorIdCaptor.capture(),
                    isNull(),
                    any(Map.class)
            );

            assertEquals("PATH_GENERATED", actionCaptor.getValue(), "审计日志动作应为PATH_GENERATED");
            assertEquals("LEARNING_PATH", targetTypeCaptor.getValue(), "审计日志目标类型应为LEARNING_PATH");
            assertEquals(operatorId, actorIdCaptor.getValue(), "审计日志操作者ID应为operatorId");
        }

        @Test
        @DisplayName("should handle student with no records gracefully")
        void shouldHandleStudentWithNoRecordsGracefully() {
            Long studentId = 8L;
            Long courseId = 80L;

            // No chapter progress at all
            when(learningRecordService.getChapterProgress(studentId, courseId))
                    .thenReturn(Collections.emptyList());

            // No unmastered KPs
            when(knowledgePointMasteryService.getUnmasteredPoints(studentId, courseId, 60.0))
                    .thenReturn(Collections.emptyList());

            // No failed exams, no high tab-switch
            when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

            // No existing active paths
            when(learningPathMapper.selectList(any())).thenReturn(Collections.emptyList());

            // Execute
            LearningPath path = learningPathService.generatePath(studentId, courseId, null, null);

            // Path should have 0 steps
            assertNotNull(path, "生成的学习路径不应为空");
            assertEquals(0, path.getTotalSteps(), "无任何记录时总步骤数应为0");
            assertEquals(0, path.getCompletedSteps(), "无任何记录时已完成步骤数应为0");
            assertTrue(path.getPathData().isEmpty(), "无任何记录时步骤列表应为空");
            assertEquals("GENERATED", path.getStatus(), "路径状态应为GENERATED");
            assertEquals("INITIAL", path.getTriggerReason(), "未指定触发原因时应默认为INITIAL");

            // Verify no remedial tasks or makeup exams created
            verify(remedialTaskMapper, never()).insert(any());
            verify(makeupExamMapper, never()).insert(any());

            // Verify path was still inserted
            verify(learningPathMapper).insert(any(LearningPath.class));

            // Verify audit log was still written
            verify(auditLogService).log(anyString(), anyString(), any(), any(), isNull(), any(Map.class));
        }

        @Test
        @DisplayName("CRITICAL: concurrent generatePath should return existing path when lock fails")
        void concurrentGeneratePathShouldReturnExistingWhenLockFails() {
            Long studentId = 9L;
            Long courseId = 90L;

            // Lock fails (another request is processing)
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            // An existing active path was just created by the other request
            List<LearningPath.PathStep> steps = List.of(
                    LearningPath.PathStep.builder()
                            .stepOrder(1).stepType("MAKEUP_CHAPTER")
                            .targetId(100L).targetTitle("Ch1").status("PENDING")
                            .build());
            LearningPath existingPath = LearningPath.builder()
                    .id(50L).studentId(studentId).courseId(courseId)
                    .status("GENERATED").pathData(steps)
                    .totalSteps(1).completedSteps(0)
                    .build();
            when(learningPathMapper.selectOne(any())).thenReturn(existingPath);

            LearningPath result = learningPathService.generatePath(studentId, courseId, "EXAM_FAIL", 99L);

            // Should return existing path, NOT create a duplicate
            assertNotNull(result);
            assertEquals(50L, result.getId());
            verify(learningPathMapper, never()).insert(any());
            verify(remedialTaskMapper, never()).insert(any());
        }

        @Test
        @DisplayName("CRITICAL: concurrent generatePath should throw when lock fails and no existing path")
        void concurrentGeneratePathShouldThrowWhenNoExistingPath() {
            // Lock fails and no existing path found
            when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);
            when(learningPathMapper.selectOne(any())).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> learningPathService.generatePath(1L, 10L, "EXAM_FAIL", 99L));
            assertTrue(ex.getMessage().contains("正在生成中"));
        }
    }

    // ========================================================================
    // completePathStep tests
    // ========================================================================

    @Nested
    @DisplayName("completePathStep")
    class CompletePathStepTests {

        @Test
        @DisplayName("should update step status and increment completedSteps")
        void shouldUpdateStepStatusAndIncrementCompletedSteps() {
            Long pathId = 1L;
            Integer stepOrder = 1;

            List<LearningPath.PathStep> steps = new ArrayList<>();
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(1)
                    .stepType("CHAPTER_STUDY")
                    .targetId(100L)
                    .targetTitle("Chapter 1")
                    .status("PENDING")
                    .build());
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(2)
                    .stepType("MAKEUP_EXAM")
                    .targetId(5L)
                    .targetTitle("补考: 考试#5")
                    .status("PENDING")
                    .build());

            LearningPath path = LearningPath.builder()
                    .id(pathId)
                    .studentId(1L)
                    .courseId(10L)
                    .status("GENERATED")
                    .pathData(steps)
                    .totalSteps(2)
                    .completedSteps(0)
                    .build();

            when(learningPathMapper.selectById(pathId)).thenReturn(path);

            // Execute
            learningPathService.completePathStep(pathId, stepOrder);

            // Verify step status and path fields
            ArgumentCaptor<LearningPath> pathCaptor = ArgumentCaptor.forClass(LearningPath.class);
            verify(learningPathMapper).updateById(pathCaptor.capture());
            LearningPath updatedPath = pathCaptor.getValue();

            LearningPath.PathStep completedStep = updatedPath.getPathData().get(0);
            assertEquals("COMPLETED", completedStep.getStatus(), "完成的步骤状态应为COMPLETED");
            assertNotNull(completedStep.getCompletedAt(), "完成的步骤应有完成时间");

            LearningPath.PathStep pendingStep = updatedPath.getPathData().get(1);
            assertEquals("PENDING", pendingStep.getStatus(), "未完成的步骤状态应保持PENDING");

            assertEquals(1, updatedPath.getCompletedSteps(), "已完成步骤数应为1");
            assertEquals("IN_PROGRESS", updatedPath.getStatus(), "完成部分步骤后路径状态应为IN_PROGRESS");
        }

        @Test
        @DisplayName("should mark COMPLETED when all steps done")
        void shouldMarkCompletedWhenAllStepsDone() {
            Long pathId = 2L;
            Integer stepOrder = 2;

            List<LearningPath.PathStep> steps = new ArrayList<>();
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(1)
                    .stepType("CHAPTER_STUDY")
                    .targetId(100L)
                    .targetTitle("Chapter 1")
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now().minusHours(1))
                    .build());
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(2)
                    .stepType("MAKEUP_EXAM")
                    .targetId(5L)
                    .targetTitle("补考: 考试#5")
                    .status("PENDING")
                    .build());

            LearningPath path = LearningPath.builder()
                    .id(pathId)
                    .studentId(2L)
                    .courseId(20L)
                    .status("IN_PROGRESS")
                    .pathData(steps)
                    .totalSteps(2)
                    .completedSteps(1)
                    .build();

            when(learningPathMapper.selectById(pathId)).thenReturn(path);

            // Execute — complete the last step
            learningPathService.completePathStep(pathId, stepOrder);

            // Verify path is COMPLETED
            ArgumentCaptor<LearningPath> pathCaptor = ArgumentCaptor.forClass(LearningPath.class);
            verify(learningPathMapper).updateById(pathCaptor.capture());
            LearningPath updatedPath = pathCaptor.getValue();

            assertEquals(2, updatedPath.getCompletedSteps(), "所有步骤完成后已完成步骤数应为2");
            assertEquals("COMPLETED", updatedPath.getStatus(), "所有步骤完成后路径状态应为COMPLETED");
            assertNotNull(updatedPath.getCompletedAt(), "路径完成时应记录完成时间");

            LearningPath.PathStep lastStep = updatedPath.getPathData().get(1);
            assertEquals("COMPLETED", lastStep.getStatus(), "最后完成的步骤状态应为COMPLETED");
        }

        @Test
        @DisplayName("should throw when step already completed")
        void shouldThrowWhenStepAlreadyCompleted() {
            Long pathId = 3L;
            Integer stepOrder = 1;

            List<LearningPath.PathStep> steps = new ArrayList<>();
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(1)
                    .stepType("CHAPTER_STUDY")
                    .targetId(100L)
                    .targetTitle("Chapter 1")
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now().minusHours(2))
                    .build());

            LearningPath path = LearningPath.builder()
                    .id(pathId)
                    .studentId(3L)
                    .courseId(30L)
                    .status("IN_PROGRESS")
                    .pathData(steps)
                    .totalSteps(1)
                    .completedSteps(1)
                    .build();

            when(learningPathMapper.selectById(pathId)).thenReturn(path);

            // Execute & verify
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> learningPathService.completePathStep(pathId, stepOrder),
                    "已完成步骤再次完成时应抛出异常");
            assertEquals("步骤已完成", ex.getMessage(), "异常消息应为'步骤已完成'");
        }

        @Test
        @DisplayName("should throw when path EXPIRED")
        void shouldThrowWhenPathExpired() {
            Long pathId = 4L;
            Integer stepOrder = 1;

            List<LearningPath.PathStep> steps = new ArrayList<>();
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(1)
                    .stepType("CHAPTER_STUDY")
                    .targetId(100L)
                    .targetTitle("Chapter 1")
                    .status("PENDING")
                    .build());

            LearningPath path = LearningPath.builder()
                    .id(pathId)
                    .studentId(4L)
                    .courseId(40L)
                    .status("EXPIRED")
                    .pathData(steps)
                    .totalSteps(1)
                    .completedSteps(0)
                    .build();

            when(learningPathMapper.selectById(pathId)).thenReturn(path);

            // Execute & verify
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> learningPathService.completePathStep(pathId, stepOrder),
                    "已过期路径完成步骤时应抛出异常");
            assertEquals("学习路径已过期", ex.getMessage(), "异常消息应为'学习路径已过期'");
        }

        @Test
        @DisplayName("should sync RemedialTask on chapter step completion")
        void shouldSyncRemedialTaskOnChapterStepCompletion() {
            Long pathId = 5L;
            Integer stepOrder = 1;

            List<LearningPath.PathStep> steps = new ArrayList<>();
            steps.add(LearningPath.PathStep.builder()
                    .stepOrder(1)
                    .stepType("MAKEUP_CHAPTER")
                    .targetId(300L)
                    .targetTitle("Chapter 3")
                    .status("PENDING")
                    .build());

            LearningPath path = LearningPath.builder()
                    .id(pathId)
                    .studentId(5L)
                    .courseId(50L)
                    .status("GENERATED")
                    .pathData(steps)
                    .totalSteps(1)
                    .completedSteps(0)
                    .build();

            when(learningPathMapper.selectById(pathId)).thenReturn(path);

            // Mock existing RemedialTask for this path + chapter
            RemedialTask existingTask = RemedialTask.builder()
                    .id(10L)
                    .studentId(5L)
                    .courseId(50L)
                    .learningPathId(pathId)
                    .chapterId(300L)
                    .taskType("REMEDIAL_CHAPTER")
                    .status("PENDING")
                    .requiredProgress(BigDecimal.valueOf(100))
                    .achievedProgress(BigDecimal.ZERO)
                    .build();
            when(remedialTaskMapper.selectOne(any())).thenReturn(existingTask);

            // Execute
            learningPathService.completePathStep(pathId, stepOrder);

            // Verify RemedialTask status updated to COMPLETED
            ArgumentCaptor<RemedialTask> taskCaptor = ArgumentCaptor.forClass(RemedialTask.class);
            verify(remedialTaskMapper).updateById(taskCaptor.capture());
            RemedialTask updatedTask = taskCaptor.getValue();
            assertEquals("COMPLETED", updatedTask.getStatus(), "补救任务状态应更新为COMPLETED");
            assertNotNull(updatedTask.getCompletedAt(), "补救任务应记录完成时间");
            assertEquals(BigDecimal.valueOf(100), updatedTask.getAchievedProgress(),
                    "补救任务达成进度应等于要求进度");
        }
    }

    // ========================================================================
    // getActivePath tests
    // ========================================================================

    @Nested
    @DisplayName("getActivePath")
    class GetActivePathTests {

        @Test
        @DisplayName("should return active path")
        void shouldReturnActivePath() {
            Long studentId = 10L;
            Long courseId = 100L;

            List<LearningPath.PathStep> steps = List.of(
                    LearningPath.PathStep.builder()
                            .stepOrder(1)
                            .stepType("CHAPTER_STUDY")
                            .targetId(100L)
                            .targetTitle("Chapter 1")
                            .status("PENDING")
                            .build()
            );

            LearningPath activePath = LearningPath.builder()
                    .id(50L)
                    .studentId(studentId)
                    .courseId(courseId)
                    .status("GENERATED")
                    .triggerReason("EXAM_FAILED")
                    .pathData(steps)
                    .totalSteps(1)
                    .completedSteps(0)
                    .generatedAt(LocalDateTime.now().minusHours(1))
                    .build();

            when(learningPathMapper.selectOne(any())).thenReturn(activePath);

            // Execute
            LearningPathDTO result = learningPathService.getActivePath(studentId, courseId);

            // Verify
            assertNotNull(result, "存在活跃路径时不应返回null");
            assertEquals(50L, result.getId(), "路径ID应为50");
            assertEquals(studentId, result.getStudentId(), "学生ID应匹配");
            assertEquals(courseId, result.getCourseId(), "课程ID应匹配");
            assertEquals("GENERATED", result.getStatus(), "路径状态应为GENERATED");
            assertEquals("EXAM_FAILED", result.getTriggerReason(), "触发原因应为EXAM_FAILED");
            assertEquals(1, result.getTotalSteps(), "总步骤数应为1");
            assertEquals(0, result.getCompletedSteps(), "已完成步骤数应为0");
            assertNotNull(result.getSteps(), "步骤列表不应为空");
            assertEquals(1, result.getSteps().size(), "步骤列表应包含1个步骤");
        }

        @Test
        @DisplayName("should return null when no active path")
        void shouldReturnNullWhenNoActivePath() {
            Long studentId = 11L;
            Long courseId = 110L;

            when(learningPathMapper.selectOne(any())).thenReturn(null);

            // Execute
            LearningPathDTO result = learningPathService.getActivePath(studentId, courseId);

            // Verify
            assertNull(result, "不存在活跃路径时应返回null");
        }
    }
}
