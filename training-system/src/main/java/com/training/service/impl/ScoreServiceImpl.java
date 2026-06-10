package com.training.service.impl;

import com.training.entity.Score;
import com.training.enums.ScoreStatus;
import com.training.exception.BusinessException;
import com.training.mapper.ScoreMapper;
import com.training.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScoreServiceImpl implements ScoreService {

    private final ScoreMapper scoreMapper;

    @Override
    public Score createScore(Long studentId, Long examId, Long submissionId, BigDecimal totalScore, BigDecimal passScore) {
        log.info("创建成绩记录, studentId: {}, examId: {}, submissionId: {}, totalScore: {}",
                studentId, examId, submissionId, totalScore);
        Score score = new Score();
        score.setStudentId(studentId);
        score.setExamId(examId);
        score.setSubmissionId(submissionId);
        score.setTotalScore(totalScore);
        score.setPassScore(passScore);
        score.setIsPassed(totalScore.compareTo(passScore) >= 0 ? 1 : 0);
        score.setStatus(ScoreStatus.PENDING.name());
        scoreMapper.insert(score);
        return score;
    }

    @Override
    public List<Score> getScoresByStudent(Long studentId) {
        log.info("查询学员成绩列表, studentId: {}", studentId);
        return scoreMapper.selectByStudentId(studentId);
    }

    @Override
    public List<Score> getScoresByExam(Long examId) {
        log.info("查询考试成绩列表, examId: {}", examId);
        return scoreMapper.selectByExamId(examId);
    }

    @Override
    public Score getScoreById(Long id) {
        log.info("查询成绩详情, id: {}", id);
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            throw new BusinessException("成绩记录不存在");
        }
        return score;
    }

    @Override
    public void approveScore(Long id, Long reviewerId) {
        log.info("审核通过成绩, id: {}, reviewerId: {}", id, reviewerId);
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            throw new BusinessException("成绩记录不存在");
        }
        if (!ScoreStatus.PENDING.name().equals(score.getStatus())) {
            throw new BusinessException("只有待审核的成绩才能审核");
        }
        scoreMapper.updateStatus(id, ScoreStatus.APPROVED.name(), reviewerId, LocalDateTime.now());
    }

    @Override
    public void rejectScore(Long id, Long reviewerId) {
        log.info("驳回成绩, id: {}, reviewerId: {}", id, reviewerId);
        Score score = scoreMapper.selectById(id);
        if (score == null) {
            throw new BusinessException("成绩记录不存在");
        }
        if (!ScoreStatus.PENDING.name().equals(score.getStatus())) {
            throw new BusinessException("只有待审核的成绩才能驳回");
        }
        scoreMapper.updateStatus(id, ScoreStatus.REJECTED.name(), reviewerId, LocalDateTime.now());
    }

    @Override
    public Score getBestScore(Long studentId, Long examId) {
        log.info("查询最高成绩, studentId: {}, examId: {}", studentId, examId);
        return scoreMapper.selectBestByStudentAndExam(studentId, examId);
    }
}
