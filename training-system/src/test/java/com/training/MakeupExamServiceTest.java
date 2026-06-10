package com.training;

import com.training.common.BusinessException;
import com.training.entity.CertificateRenewal;
import com.training.entity.MakeupExam;
import com.training.mapper.CertificateRenewalMapper;
import com.training.mapper.MakeupExamMapper;
import com.training.service.AuditLogService;
import com.training.service.impl.MakeupExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Makeup Exam Service Tests")
class MakeupExamServiceTest {

    @Mock private MakeupExamMapper makeupExamMapper;
    @Mock private CertificateRenewalMapper certificateRenewalMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private MakeupExamServiceImpl makeupExamService;

    private MakeupExam pendingMakeup;

    @BeforeEach
    void setUp() {
        pendingMakeup = MakeupExam.builder()
                .id(1L)
                .studentId(1L)
                .courseId(10L)
                .examId(5L)
                .learningPathId(2L)
                .status("PENDING")
                .maxAttempts(2)
                .attemptsUsed(0)
                .requiredScore(BigDecimal.valueOf(60))
                .build();
    }

    @Nested
    @DisplayName("startMakeupExam")
    class StartMakeupExamTests {

        @Test
        @DisplayName("should start makeup exam successfully")
        void shouldStartSuccessfully() {
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);
            when(makeupExamMapper.updateById(any())).thenReturn(1);

            makeupExamService.startMakeupExam(1L);

            ArgumentCaptor<MakeupExam> captor = ArgumentCaptor.forClass(MakeupExam.class);
            verify(makeupExamMapper).updateById(captor.capture());
            assertEquals("IN_PROGRESS", captor.getValue().getStatus());
            assertEquals(1, captor.getValue().getAttemptsUsed());
        }

        @Test
        @DisplayName("should throw when max attempts exceeded")
        void shouldThrowWhenMaxAttempts() {
            pendingMakeup.setAttemptsUsed(2);
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> makeupExamService.startMakeupExam(1L));
            assertTrue(ex.getMessage().contains("最大补考次数"));
        }

        @Test
        @DisplayName("should throw when deadline passed")
        void shouldThrowWhenDeadlinePassed() {
            pendingMakeup.setDeadline(LocalDateTime.now().minusDays(1));
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> makeupExamService.startMakeupExam(1L));
            assertTrue(ex.getMessage().contains("已过期"));
        }

        @Test
        @DisplayName("should throw when status is not PENDING")
        void shouldThrowWhenNotPending() {
            pendingMakeup.setStatus("IN_PROGRESS");
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> makeupExamService.startMakeupExam(1L));
            assertTrue(ex.getMessage().contains("状态不允许"));
        }
    }

    @Nested
    @DisplayName("recordMakeupExamResult")
    class RecordResultTests {

        @Test
        @DisplayName("should mark PASSED when score meets requirement")
        void shouldPassWhenScoreMeetsRequirement() {
            pendingMakeup.setStatus("IN_PROGRESS");
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);
            when(makeupExamMapper.updateById(any())).thenReturn(1);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(null);

            makeupExamService.recordMakeupExamResult(1L, 80.0);

            ArgumentCaptor<MakeupExam> captor = ArgumentCaptor.forClass(MakeupExam.class);
            verify(makeupExamMapper).updateById(captor.capture());
            assertEquals("PASSED", captor.getValue().getStatus());
            assertEquals(0, BigDecimal.valueOf(80.0).compareTo(captor.getValue().getAchievedScore()));
        }

        @Test
        @DisplayName("should mark FAILED when score below requirement")
        void shouldFailWhenBelowRequirement() {
            pendingMakeup.setStatus("IN_PROGRESS");
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);
            when(makeupExamMapper.updateById(any())).thenReturn(1);

            makeupExamService.recordMakeupExamResult(1L, 40.0);

            ArgumentCaptor<MakeupExam> captor = ArgumentCaptor.forClass(MakeupExam.class);
            verify(makeupExamMapper).updateById(captor.capture());
            assertEquals("FAILED", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should update linked CertificateRenewal on pass")
        void shouldUpdateRenewalOnPass() {
            pendingMakeup.setStatus("IN_PROGRESS");
            CertificateRenewal renewal = CertificateRenewal.builder()
                    .id(10L).makeupExamId(1L).status("IN_PROGRESS").build();

            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);
            when(makeupExamMapper.updateById(any())).thenReturn(1);
            when(certificateRenewalMapper.selectOne(any())).thenReturn(renewal);
            when(certificateRenewalMapper.updateById(any())).thenReturn(1);

            makeupExamService.recordMakeupExamResult(1L, 80.0);

            ArgumentCaptor<CertificateRenewal> captor = ArgumentCaptor.forClass(CertificateRenewal.class);
            verify(certificateRenewalMapper).updateById(captor.capture());
            assertEquals("COMPLETED", captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should throw when makeup exam not IN_PROGRESS")
        void shouldThrowWhenNotInProgress() {
            pendingMakeup.setStatus("PENDING");
            when(makeupExamMapper.selectById(1L)).thenReturn(pendingMakeup);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> makeupExamService.recordMakeupExamResult(1L, 80.0));
            assertTrue(ex.getMessage().contains("未在进行中"));
        }
    }
}
