package com.training;

import com.training.entity.AnswerSheet;
import com.training.entity.Exam;
import com.training.mapper.AnswerSheetMapper;
import com.training.mapper.ExamMapper;
import com.training.scheduler.ExamTimeoutScheduler;
import com.training.service.ExamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exam Timeout Scheduler Tests")
class ExamTimeoutSchedulerTest {

    @Mock private AnswerSheetMapper answerSheetMapper;
    @Mock private ExamMapper examMapper;
    @Mock private ExamService examService;

    @InjectMocks
    private ExamTimeoutScheduler scheduler;

    @Test
    @DisplayName("should auto-submit answer sheet when deadline has passed")
    void shouldAutoSubmitWhenDeadlinePassed() {
        Exam exam = Exam.builder()
                .id(1L).durationMinutes(60).build();

        // Started 61 minutes ago with 60 min duration -> timed out
        AnswerSheet sheet = AnswerSheet.builder()
                .id(100L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(61))
                .remainingSeconds(3600)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));
        when(examMapper.selectById(1L)).thenReturn(exam);

        scheduler.checkTimeoutAnswerSheets();

        verify(examService).handleTimeout(100L);
    }

    @Test
    @DisplayName("should NOT auto-submit when within time limit")
    void shouldNotAutoSubmitWhenWithinTimeLimit() {
        Exam exam = Exam.builder()
                .id(1L).durationMinutes(60).build();

        // Started 30 minutes ago with 60 min duration -> still has time
        AnswerSheet sheet = AnswerSheet.builder()
                .id(100L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .remainingSeconds(3600)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));
        when(examMapper.selectById(1L)).thenReturn(exam);

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }

    @Test
    @DisplayName("should skip sheets with null startTime")
    void shouldSkipNullStartTime() {
        AnswerSheet sheet = AnswerSheet.builder()
                .id(100L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(null)
                .remainingSeconds(3600)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }

    @Test
    @DisplayName("should handle exception gracefully and continue processing other sheets")
    void shouldHandleExceptionGracefully() {
        Exam exam = Exam.builder()
                .id(1L).durationMinutes(60).build();

        AnswerSheet timedOut = AnswerSheet.builder()
                .id(100L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(61))
                .build();

        AnswerSheet stillValid = AnswerSheet.builder()
                .id(200L).examId(1L).studentId(2L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(timedOut, stillValid));
        when(examMapper.selectById(1L)).thenReturn(exam);

        // First call throws, second should still be processed
        doThrow(new RuntimeException("DB error")).when(examService).handleTimeout(100L);

        scheduler.checkTimeoutAnswerSheets();

        // Both were checked
        verify(examService).handleTimeout(100L);
        verify(examService, never()).handleTimeout(200L); // within time, not timed out
    }

    @Test
    @DisplayName("should use startTime + durationMinutes, NOT remainingSeconds for timeout check")
    void shouldUseAbsoluteDeadlineNotRemainingSeconds() {
        // This tests the fix for the old bug where timeout was computed from remainingSeconds.
        // Scenario: student started 90 min ago, exam is 60 min.
        // Old bug: remainingSeconds=3600 was set at start, elapsed=5400,
        //   old check: elapsed > remainingSeconds (5400 > 3600) -> timeout (correct by accident)
        //   but also: totalAllowed = remainingSeconds + elapsed = 3600+5400 = 9000 (nonsensical)
        // New fix: deadline = startTime + 60 minutes, now > deadline -> timeout

        Exam exam = Exam.builder()
                .id(1L).durationMinutes(60).build();

        // remainingSeconds is still the original 3600 (set at start, never updated)
        AnswerSheet sheet = AnswerSheet.builder()
                .id(100L).examId(1L).studentId(1L).attemptNo(1)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(90))
                .remainingSeconds(3600) // stale value from start
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(sheet));
        when(examMapper.selectById(1L)).thenReturn(exam);

        scheduler.checkTimeoutAnswerSheets();

        verify(examService).handleTimeout(100L);
    }

    @Test
    @DisplayName("should handle empty list of in-progress sheets")
    void shouldHandleEmptyList() {
        when(answerSheetMapper.selectList(any())).thenReturn(List.of());

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }
}
