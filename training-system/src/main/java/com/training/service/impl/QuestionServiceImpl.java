package com.training.service.impl;

import com.github.pagehelper.PageHelper;
import com.training.dto.request.QuestionRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Question;
import com.training.exception.BusinessException;
import com.training.mapper.QuestionMapper;
import com.training.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;

    @Override
    public PageResult<Question> listQuestions(Long courseId, String questionType, String difficulty,
                                              int page, int size) {
        log.info("查询题目列表, courseId: {}, questionType: {}, difficulty: {}, page: {}, size: {}",
                courseId, questionType, difficulty, page, size);
        PageHelper.startPage(page, size);
        List<Question> list = questionMapper.selectList(courseId, questionType, difficulty);
        return PageResult.of(list);
    }

    @Override
    public Question getQuestionById(Long id) {
        log.info("查询题目详情, id: {}", id);
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        return question;
    }

    @Override
    public Question createQuestion(QuestionRequest request) {
        log.info("创建题目, courseId: {}, questionType: {}", request.getCourseId(), request.getQuestionType());
        Question question = new Question();
        question.setCourseId(request.getCourseId());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setContent(request.getContent());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setScore(request.getScore());
        question.setStatus("ACTIVE");
        questionMapper.insert(question);
        return question;
    }

    @Override
    public void updateQuestion(Long id, QuestionRequest request) {
        log.info("更新题目, id: {}", id);
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        question.setCourseId(request.getCourseId());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setContent(request.getContent());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setScore(request.getScore());
        questionMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        log.info("删除题目(软删除), id: {}", id);
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        questionMapper.softDelete(id);
    }
}
