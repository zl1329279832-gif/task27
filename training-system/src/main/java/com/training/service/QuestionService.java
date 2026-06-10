package com.training.service;

import com.training.dto.request.QuestionRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Question;

public interface QuestionService {

    PageResult<Question> listQuestions(Long courseId, String questionType, String difficulty, int page, int size);

    Question getQuestionById(Long id);

    Question createQuestion(QuestionRequest request);

    void updateQuestion(Long id, QuestionRequest request);

    void deleteQuestion(Long id);
}
