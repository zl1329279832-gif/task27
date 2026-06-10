package com.training.service;

import com.training.entity.Score;
import com.training.enums.ScoreStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ScoreMapper;
import com.training.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ScoreMapper scoreMapper;

    @InjectMocks
    private ScoreServiceImpl scoreService;

    @Test
    void testCreateScore_Passed() {
        Long studentId = 1L;
        Long examId = 1L;
        Long submissionId = 1L;
        BigDecimal totalScore = new BigDecimal("85");
        BigDecimal passScore = new BigDecimal("60");

        when(scoreMapper.insert(any(Score.class))).thenReturn(1);

        Score result = scoreService.createScore(studentId, examId, submissionId, totalScore, passScore);

        assertNotNull(result);
        assertEquals(1, result.getIsPassed());
        assertEquals(ScoreStatus.PENDING.name(), result.getStatus());
        assertEquals(totalScore, result.getTotalScore());
        assertEquals(passScore, result.getPassScore());
        assertEquals(studentId, result.getStudentId());
        assertEquals(examId, result.getExamId());
        assertEquals(submissionId, result.getSubmissionId());

        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insert(captor.capture());
        Score inserted = captor.getValue();
        assertEquals(1, inserted.getIsPassed());
        assertEquals(ScoreStatus.PENDING.name(), inserted.getStatus());
    }

    @Test
    void testCreateScore_Failed() {
        Long studentId = 1L;
        Long examId = 1L;
        Long submissionId = 1L;
        BigDecimal totalScore = new BigDecimal("50");
        BigDecimal passScore = new BigDecimal("60");

        when(scoreMapper.insert(any(Score.class))).thenReturn(1);

        Score result = scoreService.createScore(studentId, examId, submissionId, totalScore, passScore);

        assertNotNull(result);
        assertEquals(0, result.getIsPassed());

        ArgumentCaptor<Score> captor = ArgumentCaptor.forClass(Score.class);
        verify(scoreMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getIsPassed());
    }

    @Test
    void testApproveScore_Success() {
        Long scoreId = 1L;
        Long reviewerId = 2L;

        Score score = new Score();
        score.setId(scoreId);
        score.setStatus(ScoreStatus.PENDING.name());

        when(scoreMapper.selectById(scoreId)).thenReturn(score);
        when(scoreMapper.updateStatus(eq(scoreId), eq(ScoreStatus.APPROVED.name()),
                eq(reviewerId), any(LocalDateTime.class))).thenReturn(1);

        scoreService.approveScore(scoreId, reviewerId);

        verify(scoreMapper).updateStatus(eq(scoreId), eq(ScoreStatus.APPROVED.name()),
                eq(reviewerId), any(LocalDateTime.class));
    }

    @Test
    void testApproveScore_NotPending() {
        Long scoreId = 1L;
        Long reviewerId = 2L;

        Score score = new Score();
        score.setId(scoreId);
        score.setStatus(ScoreStatus.APPROVED.name());

        when(scoreMapper.selectById(scoreId)).thenReturn(score);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scoreService.approveScore(scoreId, reviewerId));

        assertTrue(exception.getMessage().contains("待审核"));
    }

    @Test
    void testRejectScore_Success() {
        Long scoreId = 1L;
        Long reviewerId = 2L;

        Score score = new Score();
        score.setId(scoreId);
        score.setStatus(ScoreStatus.PENDING.name());

        when(scoreMapper.selectById(scoreId)).thenReturn(score);
        when(scoreMapper.updateStatus(eq(scoreId), eq(ScoreStatus.REJECTED.name()),
                eq(reviewerId), any(LocalDateTime.class))).thenReturn(1);

        scoreService.rejectScore(scoreId, reviewerId);

        verify(scoreMapper).updateStatus(eq(scoreId), eq(ScoreStatus.REJECTED.name()),
                eq(reviewerId), any(LocalDateTime.class));
    }
}
