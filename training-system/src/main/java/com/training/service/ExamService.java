package com.training.service;

import com.training.entity.dto.ExamRequest;
import com.training.entity.dto.ExamStartResponse;
import com.training.entity.dto.AnswerSubmitRequest;
import com.training.entity.Exam;
import com.training.entity.AnswerSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface ExamService {
    Exam create(ExamRequest req, Long instructorId);
    Exam update(Long id, ExamRequest req);
    Exam getById(Long id);
    IPage<Exam> list(int page, int size, Long courseId, String status);
    void delete(Long id);
    void publish(Long id);
    void close(Long id);
    ExamStartResponse startExam(Long examId, Long studentId);
    AnswerSheet submitExam(AnswerSubmitRequest req, Long studentId);
    void reportTabSwitch(Long examId, Long studentId);
    void handleTimeout(Long answerSheetId);
}
