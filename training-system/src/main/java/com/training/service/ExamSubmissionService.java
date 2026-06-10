package com.training.service;

import com.training.dto.request.AnswerSubmitRequest;
import com.training.dto.request.HeartbeatRequest;
import com.training.dto.request.SaveAnswerRequest;
import com.training.dto.response.ExamStartResponse;
import com.training.entity.ExamSubmission;
import com.training.entity.SubmissionAnswer;

import java.util.List;
import java.util.Map;

public interface ExamSubmissionService {

    ExamStartResponse startExam(Long examId, Long studentId);

    void saveAnswer(SaveAnswerRequest request, Long studentId);

    Map<String, Object> submitExam(AnswerSubmitRequest request, Long studentId);

    void handleHeartbeat(HeartbeatRequest request, Long studentId);

    ExamSubmission getSubmission(Long submissionId, Long studentId);

    List<SubmissionAnswer> getSubmissionAnswers(Long submissionId);

    void autoSubmitTimedOut();
}
