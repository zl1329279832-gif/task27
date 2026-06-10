package com.training.scheduler;

import com.training.service.ExamSubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExamTimeoutScheduler {

    private final ExamSubmissionService examSubmissionService;

    @Scheduled(fixedRate = 60000)
    public void checkTimeoutExams() {
        try {
            examSubmissionService.autoSubmitTimedOut();
        } catch (Exception e) {
            log.error("自动提交超时考试失败", e);
        }
    }
}
