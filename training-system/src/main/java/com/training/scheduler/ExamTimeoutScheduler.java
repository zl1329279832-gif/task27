package com.training.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.entity.AnswerSheet;
import com.training.mapper.AnswerSheetMapper;
import com.training.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to handle exam timeouts.
 * Scans for IN_PROGRESS answer sheets that have exceeded their time limit
 * and auto-submits them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamTimeoutScheduler {

    private final AnswerSheetMapper answerSheetMapper;
    private final ExamService examService;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkTimeoutAnswerSheets() {
        List<AnswerSheet> inProgressSheets = answerSheetMapper.selectList(
                new LambdaQueryWrapper<AnswerSheet>()
                        .eq(AnswerSheet::getStatus, "IN_PROGRESS"));

        for (AnswerSheet sheet : inProgressSheets) {
            if (sheet.getStartTime() != null && sheet.getRemainingSeconds() != null) {
                long elapsed = java.time.Duration.between(sheet.getStartTime(), LocalDateTime.now()).getSeconds();

                // If elapsed time exceeds the exam duration (remainingSeconds holds the original duration)
                if (elapsed > sheet.getRemainingSeconds()) {
                    log.info("Auto-submitting timed-out answer sheet: {}", sheet.getId());
                    try {
                        examService.handleTimeout(sheet.getId());
                    } catch (Exception e) {
                        log.error("Failed to auto-submit answer sheet {}: {}", sheet.getId(), e.getMessage());
                    }
                }
            }
        }
    }
}
