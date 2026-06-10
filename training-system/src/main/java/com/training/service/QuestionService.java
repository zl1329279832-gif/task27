package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.Question;
import com.training.entity.dto.QuestionRequest;

import java.util.List;

public interface QuestionService {
    Question create(QuestionRequest req, Long instructorId);
    Question update(Long id, QuestionRequest req);
    Question getById(Long id);
    IPage<Question> list(int page, int size, Long courseId, String questionType, String difficulty);
    void delete(Long id);
    void batchCreate(List<QuestionRequest> requests, Long instructorId);
}
