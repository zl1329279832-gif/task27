package com.training.service;

import com.training.dto.request.ExamRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Exam;

public interface ExamService {

    PageResult<Exam> listExams(Long courseId, String status, String keyword, int page, int size);

    Exam getExamById(Long id);

    Exam createExam(ExamRequest request, Long createdBy);

    void updateExam(Long id, ExamRequest request);

    void deleteExam(Long id);

    void publishExam(Long id);

    void closeExam(Long id);
}
