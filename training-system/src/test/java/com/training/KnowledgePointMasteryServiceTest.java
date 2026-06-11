package com.training;

import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.AuditLogService;
import com.training.service.impl.KnowledgePointMasteryServiceImpl;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Knowledge Point Mastery Service Tests")
class KnowledgePointMasteryServiceTest {

    @Mock private KnowledgePointMasteryMapper masteryMapper;
    @Mock private AnswerDetailMapper answerDetailMapper;
    @Mock private QuestionKnowledgePointMapper questionKnowledgePointMapper;
    @Mock private AnswerSheetMapper answerSheetMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private KnowledgePointMasteryServiceImpl knowledgePointMasteryService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Default: lock acquisition succeeds
        lenient().when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        // 默认无已存在的掌握记录，部分测试不会调用 selectOne，因此使用 lenient
        lenient().when(masteryMapper.selectOne(any())).thenReturn(null);
    }

    // ========================================================================
    // updateMasteryFromExam tests
    // ========================================================================

    @Nested
    @DisplayName("updateMasteryFromExam")
    class UpdateMasteryFromExamTests {

        @Test
        @DisplayName("should create mastery records from exam results")
        void shouldCreateMasteryRecordsFromExamResults() {
            // 准备答题明细：题目1答对，题目2答错
            AnswerDetail detail1 = AnswerDetail.builder()
                    .id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build();
            AnswerDetail detail2 = AnswerDetail.builder()
                    .id(2L).answerSheetId(100L).questionId(2L).isCorrect(0).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail1, detail2));

            // 两道题均关联知识点10
            QuestionKnowledgePoint qkp1 = QuestionKnowledgePoint.builder()
                    .id(1L).questionId(1L).knowledgePointId(10L).build();
            QuestionKnowledgePoint qkp2 = QuestionKnowledgePoint.builder()
                    .id(2L).questionId(2L).knowledgePointId(10L).build();
            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(qkp1))
                    .thenReturn(List.of(qkp2));

            // 不存在已有的掌握记录
            when(masteryMapper.selectOne(any())).thenReturn(null);

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            // 验证插入的掌握记录
            ArgumentCaptor<KnowledgePointMastery> captor = ArgumentCaptor.forClass(KnowledgePointMastery.class);
            verify(masteryMapper).insert(captor.capture());
            KnowledgePointMastery inserted = captor.getValue();

            assertEquals(1L, inserted.getStudentId(), "学生ID应正确");
            assertEquals(10L, inserted.getKnowledgePointId(), "知识点ID应正确");
            assertEquals(10L, inserted.getCourseId(), "课程ID应正确");
            assertEquals(1, inserted.getCorrectCount(), "正确题数应为1");
            assertEquals(2, inserted.getTotalQuestions(), "总题数应为2");
            assertEquals(new BigDecimal("50.00"), inserted.getMasteryLevel(), "掌握度应为50.00");
            assertEquals("UNMASTERED", inserted.getStatus(), "正确率50%应为未掌握");
            assertEquals(1, inserted.getExamAttempts(), "考试次数应为1");
            assertNotNull(inserted.getLastAssessedAt(), "最后评估时间不应为空");
        }

        @Test
        @DisplayName("should mark MASTERED when correctRate above 80%")
        void shouldMarkMasteredWhenCorrectRateAbove80() {
            // 5道题全部答对，均关联知识点10
            List<AnswerDetail> details = List.of(
                    AnswerDetail.builder().id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build(),
                    AnswerDetail.builder().id(2L).answerSheetId(100L).questionId(2L).isCorrect(1).build(),
                    AnswerDetail.builder().id(3L).answerSheetId(100L).questionId(3L).isCorrect(1).build(),
                    AnswerDetail.builder().id(4L).answerSheetId(100L).questionId(4L).isCorrect(1).build(),
                    AnswerDetail.builder().id(5L).answerSheetId(100L).questionId(5L).isCorrect(1).build()
            );
            when(answerDetailMapper.selectList(any())).thenReturn(details);

            // 每道题都关联知识点10
            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(1L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(2L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(3L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(4L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(5L).knowledgePointId(10L).build()));

            when(masteryMapper.selectOne(any())).thenReturn(null);

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            ArgumentCaptor<KnowledgePointMastery> captor = ArgumentCaptor.forClass(KnowledgePointMastery.class);
            verify(masteryMapper).insert(captor.capture());
            KnowledgePointMastery inserted = captor.getValue();

            assertEquals(5, inserted.getCorrectCount(), "正确题数应为5");
            assertEquals(5, inserted.getTotalQuestions(), "总题数应为5");
            assertEquals(new BigDecimal("100.00"), inserted.getMasteryLevel(), "掌握度应为100.00");
            assertEquals("MASTERED", inserted.getStatus(), "正确率100%应标记为已掌握");
        }

        @Test
        @DisplayName("should mark PARTIAL when correctRate between 60-80%")
        void shouldMarkPartialWhenCorrectRateBetween60And80() {
            // 5道题中答对3道，均关联知识点10
            List<AnswerDetail> details = List.of(
                    AnswerDetail.builder().id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build(),
                    AnswerDetail.builder().id(2L).answerSheetId(100L).questionId(2L).isCorrect(1).build(),
                    AnswerDetail.builder().id(3L).answerSheetId(100L).questionId(3L).isCorrect(1).build(),
                    AnswerDetail.builder().id(4L).answerSheetId(100L).questionId(4L).isCorrect(0).build(),
                    AnswerDetail.builder().id(5L).answerSheetId(100L).questionId(5L).isCorrect(0).build()
            );
            when(answerDetailMapper.selectList(any())).thenReturn(details);

            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(1L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(2L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(3L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(4L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(5L).knowledgePointId(10L).build()));

            when(masteryMapper.selectOne(any())).thenReturn(null);

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            ArgumentCaptor<KnowledgePointMastery> captor = ArgumentCaptor.forClass(KnowledgePointMastery.class);
            verify(masteryMapper).insert(captor.capture());
            KnowledgePointMastery inserted = captor.getValue();

            assertEquals(3, inserted.getCorrectCount(), "正确题数应为3");
            assertEquals(5, inserted.getTotalQuestions(), "总题数应为5");
            assertEquals(new BigDecimal("60.00"), inserted.getMasteryLevel(), "掌握度应为60.00");
            assertEquals("PARTIAL", inserted.getStatus(), "正确率60%应标记为部分掌握");
        }

        @Test
        @DisplayName("should update existing mastery incrementally")
        void shouldUpdateExistingMasteryIncrementally() {
            // 已有的掌握记录：5题中答对3题
            KnowledgePointMastery existing = KnowledgePointMastery.builder()
                    .id(1L)
                    .studentId(1L)
                    .knowledgePointId(10L)
                    .courseId(10L)
                    .totalQuestions(5)
                    .correctCount(3)
                    .examAttempts(2)
                    .masteryLevel(new BigDecimal("60.00"))
                    .status("PARTIAL")
                    .build();
            when(masteryMapper.selectOne(any())).thenReturn(existing);

            // 新考试：2道题，答对1道
            AnswerDetail detail1 = AnswerDetail.builder()
                    .id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build();
            AnswerDetail detail2 = AnswerDetail.builder()
                    .id(2L).answerSheetId(100L).questionId(2L).isCorrect(0).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail1, detail2));

            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(1L).knowledgePointId(10L).build()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(2L).knowledgePointId(10L).build()));

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            // 验证调用的是 updateById 而非 insert
            verify(masteryMapper, never()).insert(any());

            ArgumentCaptor<KnowledgePointMastery> captor = ArgumentCaptor.forClass(KnowledgePointMastery.class);
            verify(masteryMapper).updateById(captor.capture());
            KnowledgePointMastery updated = captor.getValue();

            assertEquals(7, updated.getTotalQuestions(), "增量更新后总题数应为7 (5+2)");
            assertEquals(4, updated.getCorrectCount(), "增量更新后正确题数应为4 (3+1)");
            assertEquals(3, updated.getExamAttempts(), "考试次数应递增为3 (2+1)");
            // 4 * 100 / 7 = 57.14
            assertEquals(new BigDecimal("57.14"), updated.getMasteryLevel(), "掌握度应为57.14 (4/7*100)");
            assertEquals("UNMASTERED", updated.getStatus(), "正确率57.14%应为未掌握");
            assertNotNull(updated.getLastAssessedAt(), "最后评估时间应更新");
        }

        @Test
        @DisplayName("should handle questions with no KP mappings")
        void shouldHandleQuestionsWithNoKpMappings() {
            // 有答题明细但题目没有关联知识点
            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            // 题目没有关联的知识点
            when(questionKnowledgePointMapper.selectList(any())).thenReturn(Collections.emptyList());

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            // 无知识点映射，不应插入任何掌握记录
            verify(masteryMapper, never()).insert(any());
            verify(masteryMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("should write audit log after update")
        void shouldWriteAuditLogAfterUpdate() {
            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));

            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(1L).knowledgePointId(10L).build()));

            when(masteryMapper.selectOne(any())).thenReturn(null);

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            // 验证审计日志被调用，参数正确
            verify(auditLogService).log(
                    eq("MASTERY_UPDATE"),
                    eq("ANSWER_SHEET"),
                    eq(100L),
                    eq(1L),
                    eq("STUDENT"),
                    any()
            );
        }

        @Test
        @DisplayName("should use Redis lock to prevent concurrent mastery updates")
        void shouldUseRedisLockForConcurrentProtection() {
            AnswerDetail detail = AnswerDetail.builder()
                    .id(1L).answerSheetId(100L).questionId(1L).isCorrect(1).build();
            when(answerDetailMapper.selectList(any())).thenReturn(List.of(detail));
            when(questionKnowledgePointMapper.selectList(any()))
                    .thenReturn(List.of(QuestionKnowledgePoint.builder().questionId(1L).knowledgePointId(10L).build()));
            when(masteryMapper.selectOne(any())).thenReturn(null);

            knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L);

            // Verify lock was acquired with correct key
            verify(valueOperations).setIfAbsent(
                    eq("mastery:update:1:10"), any(), anyLong(), any(TimeUnit.class));
            // Verify lock was released
            verify(redisTemplate).delete("mastery:update:1:10");
        }

        @Test
        @DisplayName("should reject when lock is already held by concurrent update")
        void shouldRejectWhenLockAlreadyHeld() {
            // Lock already held
            when(valueOperations.setIfAbsent(
                    eq("mastery:update:1:10"), any(), anyLong(), any(TimeUnit.class)))
                    .thenReturn(false);

            assertThrows(Exception.class,
                    () -> knowledgePointMasteryService.updateMasteryFromExam(1L, 10L, 100L));

            // No DB writes should have occurred
            verify(masteryMapper, never()).insert(any());
            verify(masteryMapper, never()).updateById(any());
        }
    }

    // ========================================================================
    // getUnmasteredPoints tests
    // ========================================================================

    @Nested
    @DisplayName("getUnmasteredPoints")
    class GetUnmasteredPointsTests {

        @Test
        @DisplayName("should return only points below threshold")
        void shouldReturnOnlyPointsBelowThreshold() {
            // 模拟返回低于阈值的掌握记录
            KnowledgePointMastery low1 = KnowledgePointMastery.builder()
                    .id(1L).studentId(1L).knowledgePointId(10L).courseId(10L)
                    .masteryLevel(new BigDecimal("30.00")).status("UNMASTERED").build();
            KnowledgePointMastery low2 = KnowledgePointMastery.builder()
                    .id(2L).studentId(1L).knowledgePointId(20L).courseId(10L)
                    .masteryLevel(new BigDecimal("50.00")).status("UNMASTERED").build();
            when(masteryMapper.selectList(any())).thenReturn(List.of(low1, low2));

            List<KnowledgePointMastery> result = knowledgePointMasteryService.getUnmasteredPoints(1L, 10L, 60.0);

            // 验证查询被调用，返回低于阈值的记录
            verify(masteryMapper).selectList(any());
            assertEquals(2, result.size(), "应返回2条低于阈值的记录");
            assertEquals(10L, result.get(0).getKnowledgePointId(), "第一条记录知识点ID应为10");
            assertEquals(20L, result.get(1).getKnowledgePointId(), "第二条记录知识点ID应为20");
            assertTrue(result.get(0).getMasteryLevel().compareTo(BigDecimal.valueOf(60)) < 0,
                    "第一条记录掌握度应低于阈值60");
            assertTrue(result.get(1).getMasteryLevel().compareTo(BigDecimal.valueOf(60)) < 0,
                    "第二条记录掌握度应低于阈值60");
        }

        @Test
        @DisplayName("should order by mastery level ascending")
        void shouldOrderByMasteryLevelAscending() {
            // 模拟返回按掌握度升序排列的记录
            KnowledgePointMastery lowest = KnowledgePointMastery.builder()
                    .id(1L).studentId(1L).knowledgePointId(10L).courseId(10L)
                    .masteryLevel(new BigDecimal("10.00")).status("UNMASTERED").build();
            KnowledgePointMastery middle = KnowledgePointMastery.builder()
                    .id(2L).studentId(1L).knowledgePointId(20L).courseId(10L)
                    .masteryLevel(new BigDecimal("35.00")).status("UNMASTERED").build();
            KnowledgePointMastery highest = KnowledgePointMastery.builder()
                    .id(3L).studentId(1L).knowledgePointId(30L).courseId(10L)
                    .masteryLevel(new BigDecimal("55.00")).status("UNMASTERED").build();
            when(masteryMapper.selectList(any())).thenReturn(List.of(lowest, middle, highest));

            List<KnowledgePointMastery> result = knowledgePointMasteryService.getUnmasteredPoints(1L, 10L, 60.0);

            // 验证查询被调用
            verify(masteryMapper).selectList(any());
            assertEquals(3, result.size(), "应返回3条记录");

            // 验证返回结果按掌握度升序排列
            assertTrue(result.get(0).getMasteryLevel().compareTo(result.get(1).getMasteryLevel()) <= 0,
                    "结果应按掌握度升序排列：第一条应不大于第二条");
            assertTrue(result.get(1).getMasteryLevel().compareTo(result.get(2).getMasteryLevel()) <= 0,
                    "结果应按掌握度升序排列：第二条应不大于第三条");
            assertEquals(new BigDecimal("10.00"), result.get(0).getMasteryLevel(), "最低掌握度应排在首位");
            assertEquals(new BigDecimal("55.00"), result.get(2).getMasteryLevel(), "最高掌握度应排在末位");
        }
    }
}
