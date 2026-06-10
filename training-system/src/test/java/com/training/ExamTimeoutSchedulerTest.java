package com.training;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.entity.AnswerSheet;
import com.training.mapper.AnswerSheetMapper;
import com.training.scheduler.ExamTimeoutScheduler;
import com.training.service.ExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Exam Timeout Scheduler Tests")
class ExamTimeoutSchedulerTest {

    @Mock private AnswerSheetMapper answerSheetMapper;
    @Mock private ExamService examService;

    @InjectMocks
    private ExamTimeoutScheduler scheduler;

    @Test
    @DisplayName("should call handleTimeout for sheets that exceeded duration")
    void shouldHandleTimedOutSheets() {
        // Started 70 minutes ago with 60-minute duration → timed out
        AnswerSheet timedOut = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(70))
                .remainingSeconds(3600) // 60 minutes
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(timedOut));

        scheduler.checkTimeoutAnswerSheets();

        verify(examService).handleTimeout(1L);
    }

    @Test
    @DisplayName("should NOT call handleTimeout for sheets still within time limit")
    void shouldNotHandleSheetsWithinTimeLimit() {
        // Started 30 minutes ago with 60-minute duration → still valid
        AnswerSheet active = AnswerSheet.builder()
                .id(2L).examId(1L).studentId(2L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .remainingSeconds(3600) // 60 minutes
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(List.of(active));

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }

    @Test
    @DisplayName("should handle mixed timed-out and active sheets")
    void shouldHandleMixedSheets() {
        AnswerSheet timedOut = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(120))
                .remainingSeconds(3600)
                .build();

        AnswerSheet active = AnswerSheet.builder()
                .id(2L).examId(1L).studentId(2L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(10))
                .remainingSeconds(3600)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(Arrays.asList(timedOut, active));

        scheduler.checkTimeoutAnswerSheets();

        verify(examService).handleTimeout(1L);
        verify(examService, never()).handleTimeout(2L);
    }

    @Test
    @DisplayName("should handle exception from handleTimeout gracefully without affecting other sheets")
    void shouldHandleExceptionGracefully() {
        AnswerSheet sheet1 = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(90))
                .remainingSeconds(3600)
                .build();

        AnswerSheet sheet2 = AnswerSheet.builder()
                .id(2L).examId(1L).studentId(2L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(80))
                .remainingSeconds(3600)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(Arrays.asList(sheet1, sheet2));
        doThrow(new RuntimeException("DB error")).when(examService).handleTimeout(1L);

        // Should not throw — exception is caught and logged
        scheduler.checkTimeoutAnswerSheets();

        // sheet2 should still be processed despite sheet1 failure
        verify(examService).handleTimeout(1L);
        verify(examService).handleTimeout(2L);
    }

    @Test
    @DisplayName("should skip sheets with null startTime or remainingSeconds")
    void shouldSkipSheetsWithNullFields() {
        AnswerSheet noStart = AnswerSheet.builder()
                .id(1L).examId(1L).studentId(1L)
                .status("IN_PROGRESS")
                .startTime(null)
                .remainingSeconds(3600)
                .build();

        AnswerSheet noRemaining = AnswerSheet.builder()
                .id(2L).examId(1L).studentId(2L)
                .status("IN_PROGRESS")
                .startTime(LocalDateTime.now().minusMinutes(90))
                .remainingSeconds(null)
                .build();

        when(answerSheetMapper.selectList(any())).thenReturn(Arrays.asList(noStart, noRemaining));

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }

    @Test
    @DisplayName("should do nothing when no IN_PROGRESS sheets exist")
    void shouldDoNothingWhenNoInProgressSheets() {
        when(answerSheetMapper.selectList(any())).thenReturn(Collections.emptyList());

        scheduler.checkTimeoutAnswerSheets();

        verify(examService, never()).handleTimeout(anyLong());
    }
}
