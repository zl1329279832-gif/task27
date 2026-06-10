package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.Question;
import com.training.entity.dto.QuestionRequest;
import com.training.mapper.QuestionMapper;
import com.training.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;

    @Override
    public Question create(QuestionRequest req, Long instructorId) {
        Question question = Question.builder()
                .courseId(req.getCourseId())
                .instructorId(instructorId)
                .content(req.getContent())
                .questionType(req.getQuestionType())
                .options(req.getOptions())
                .correctAnswer(req.getCorrectAnswer())
                .score(req.getScore() != null ? req.getScore() : 10)
                .difficulty(req.getDifficulty() != null ? req.getDifficulty() : "INTERMEDIATE")
                .explanation(req.getExplanation())
                .status("ACTIVE")
                .build();
        questionMapper.insert(question);
        return question;
    }

    @Override
    public Question update(Long id, QuestionRequest req) {
        Question question = questionMapper.selectById(id);
        if (question == null || "DELETED".equals(question.getStatus()))
            throw new BusinessException("题目不存在");

        question.setContent(req.getContent() != null ? req.getContent() : question.getContent());
        question.setQuestionType(req.getQuestionType() != null ? req.getQuestionType() : question.getQuestionType());
        question.setOptions(req.getOptions() != null ? req.getOptions() : question.getOptions());
        question.setCorrectAnswer(req.getCorrectAnswer() != null ? req.getCorrectAnswer() : question.getCorrectAnswer());
        question.setScore(req.getScore() != null ? req.getScore() : question.getScore());
        question.setDifficulty(req.getDifficulty() != null ? req.getDifficulty() : question.getDifficulty());
        question.setExplanation(req.getExplanation() != null ? req.getExplanation() : question.getExplanation());
        questionMapper.updateById(question);
        return question;
    }

    @Override
    public Question getById(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) throw new BusinessException("题目不存在");
        return question;
    }

    @Override
    public IPage<Question> list(int page, int size, Long courseId, String questionType, String difficulty) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, "ACTIVE");
        if (courseId != null) wrapper.eq(Question::getCourseId, courseId);
        if (questionType != null) wrapper.eq(Question::getQuestionType, questionType);
        if (difficulty != null) wrapper.eq(Question::getDifficulty, difficulty);
        wrapper.orderByDesc(Question::getCreatedAt);
        return questionMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void delete(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) throw new BusinessException("题目不存在");
        // Soft delete - keep for historical answer sheets
        question.setStatus("DELETED");
        question.setDeletedAt(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    @Override
    public void batchCreate(List<QuestionRequest> requests, Long instructorId) {
        for (QuestionRequest req : requests) {
            create(req, instructorId);
        }
    }
}
