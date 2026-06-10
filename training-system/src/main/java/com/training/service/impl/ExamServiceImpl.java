package com.training.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.training.dto.request.ExamRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Exam;
import com.training.entity.ExamQuestion;
import com.training.entity.Question;
import com.training.enums.ExamStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ExamMapper;
import com.training.mapper.ExamQuestionMapper;
import com.training.mapper.QuestionMapper;
import com.training.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamMapper examMapper;
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<Exam> listExams(Long courseId, String status, String keyword, int page, int size) {
        log.info("查询考试列表, courseId: {}, status: {}, keyword: {}, page: {}, size: {}",
                courseId, status, keyword, page, size);
        PageHelper.startPage(page, size);
        List<Exam> list = examMapper.selectList(courseId, status, keyword);
        return PageResult.of(list);
    }

    @Override
    public Exam getExamById(Long id) {
        log.info("查询考试详情, id: {}", id);
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        return exam;
    }

    @Override
    public Exam createExam(ExamRequest request, Long createdBy) {
        log.info("创建考试, title: {}, createdBy: {}", request.getTitle(), createdBy);
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setCourseId(request.getCourseId());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalScore(request.getTotalScore());
        exam.setPassScore(request.getPassScore());
        exam.setQuestionCount(request.getQuestionCount());
        exam.setMaxAttempts(request.getMaxAttempts() != null ? request.getMaxAttempts() : 1);
        exam.setStatus(ExamStatus.DRAFT.name());
        exam.setCreatedBy(createdBy);
        examMapper.insert(exam);
        return exam;
    }

    @Override
    public void updateExam(Long id, ExamRequest request) {
        log.info("更新考试, id: {}", id);
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        exam.setTitle(request.getTitle());
        exam.setCourseId(request.getCourseId());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setTotalScore(request.getTotalScore());
        exam.setPassScore(request.getPassScore());
        exam.setQuestionCount(request.getQuestionCount());
        if (request.getMaxAttempts() != null) {
            exam.setMaxAttempts(request.getMaxAttempts());
        }
        examMapper.updateById(exam);
    }

    @Override
    public void deleteExam(Long id) {
        log.info("删除考试, id: {}", id);
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!ExamStatus.DRAFT.name().equals(exam.getStatus())) {
            throw new BusinessException("只能删除草稿状态的考试");
        }
        examMapper.deleteById(id);
    }

    @Override
    public void publishExam(Long id) {
        log.info("发布考试, id: {}", id);
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!ExamStatus.DRAFT.name().equals(exam.getStatus())) {
            throw new BusinessException("只有草稿状态的考试才能发布");
        }

        // Get available questions from question bank
        List<Question> questions = questionMapper.selectRandomByCourseId(exam.getCourseId(), exam.getQuestionCount());
        if (questions.size() < exam.getQuestionCount()) {
            throw new BusinessException("题库中可用题目数量不足");
        }

        // Create exam questions with snapshots
        List<ExamQuestion> examQuestions = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            try {
                // Build snapshot JSON including key question fields
                Map<String, Object> snapshot = new HashMap<>();
                snapshot.put("id", question.getId());
                snapshot.put("content", question.getContent());
                snapshot.put("questionType", question.getQuestionType());
                snapshot.put("options", question.getOptions());
                snapshot.put("correctAnswer", question.getCorrectAnswer());
                snapshot.put("analysis", question.getAnalysis());
                snapshot.put("score", question.getScore());

                String snapshotJson = objectMapper.writeValueAsString(snapshot);

                ExamQuestion examQuestion = ExamQuestion.builder()
                        .examId(exam.getId())
                        .questionId(question.getId())
                        .questionSnapshot(snapshotJson)
                        .sortOrder(i + 1)
                        .score(question.getScore())
                        .build();
                examQuestions.add(examQuestion);
            } catch (Exception e) {
                log.error("序列化题目快照失败, questionId: {}", question.getId(), e);
                throw new BusinessException("题目快照生成失败");
            }
        }

        // Batch insert exam questions
        examQuestionMapper.batchInsert(examQuestions);

        // Update exam status to PUBLISHED
        examMapper.updateStatus(id, ExamStatus.PUBLISHED.name());
        log.info("考试发布成功, id: {}, 共抽取{}道题目", id, questions.size());
    }

    @Override
    public void closeExam(Long id) {
        log.info("关闭考试, id: {}", id);
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        if (!ExamStatus.PUBLISHED.name().equals(exam.getStatus())) {
            throw new BusinessException("只有已发布的考试才能关闭");
        }
        examMapper.updateStatus(id, ExamStatus.CLOSED.name());
    }
}
