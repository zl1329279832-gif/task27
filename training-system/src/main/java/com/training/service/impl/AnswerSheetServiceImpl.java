package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.common.BusinessException;
import com.training.entity.*;
import com.training.mapper.*;
import com.training.service.AnswerSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerSheetServiceImpl implements AnswerSheetService {

    private final AnswerSheetMapper answerSheetMapper;
    private final AnswerDetailMapper answerDetailMapper;
    private final QuestionMapper questionMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamMapper examMapper;
    private final GradeMapper gradeMapper;

    @Override
    public IPage<AnswerSheet> list(int page, int size, Long examId, Long studentId) {
        LambdaQueryWrapper<AnswerSheet> wrapper = new LambdaQueryWrapper<>();
        if (examId != null) wrapper.eq(AnswerSheet::getExamId, examId);
        if (studentId != null) wrapper.eq(AnswerSheet::getStudentId, studentId);
        wrapper.orderByDesc(AnswerSheet::getCreatedAt);
        return answerSheetMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public AnswerSheetDetailDTO getDetail(Long id, Long currentUserId, String currentRole) {
        AnswerSheet sheet = answerSheetMapper.selectById(id);
        if (sheet == null) throw new BusinessException("答卷不存在");

        // Permission check: student can only view own, instructor/admin/auditor can view all
        if ("STUDENT".equals(currentRole) && !sheet.getStudentId().equals(currentUserId)) {
            throw new BusinessException("无权查看此答卷");
        }

        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>()
                        .eq(AnswerDetail::getAnswerSheetId, id));

        AnswerSheetDetailDTO dto = new AnswerSheetDetailDTO();
        dto.setAnswerSheet(sheet);

        List<AnswerSheetDetailDTO.AnswerDetailItem> items = new ArrayList<>();
        for (AnswerDetail detail : details) {
            AnswerSheetDetailDTO.AnswerDetailItem item = new AnswerSheetDetailDTO.AnswerDetailItem();
            item.setDetailId(detail.getId());
            item.setQuestionId(detail.getQuestionId());
            item.setStudentAnswer(detail.getStudentAnswer());
            item.setIsCorrect(detail.getIsCorrect());
            item.setScoreEarned(detail.getScoreEarned());
            item.setGradingNote(detail.getGradingNote());

            // Use snapshot data — never re-read from live question table
            if (detail.getSnapshotContent() != null) {
                item.setQuestionDeleted(false);
                item.setQuestionContent(detail.getSnapshotContent());
                item.setQuestionType(detail.getSnapshotQuestionType());
                item.setCorrectAnswer(detail.getSnapshotCorrectAnswer());
                item.setMaxScore(detail.getSnapshotScore());
            } else {
                // Legacy data without snapshot — fall back to live query
                Question question = questionMapper.selectById(detail.getQuestionId());
                if (question == null || "DELETED".equals(question.getStatus())) {
                    item.setQuestionDeleted(true);
                    item.setQuestionContent("[该题目已删除]");
                    item.setQuestionType("UNKNOWN");
                    item.setCorrectAnswer(null);
                    ExamQuestion eq = detail.getExamQuestionId() != null ?
                            examQuestionMapper.selectById(detail.getExamQuestionId()) : null;
                    item.setMaxScore(eq != null && eq.getScoreOverride() != null ?
                            eq.getScoreOverride() : 10);
                } else {
                    item.setQuestionDeleted(false);
                    item.setQuestionContent(question.getContent());
                    item.setQuestionType(question.getQuestionType());
                    item.setCorrectAnswer(question.getCorrectAnswer());
                    ExamQuestion eq = detail.getExamQuestionId() != null ?
                            examQuestionMapper.selectById(detail.getExamQuestionId()) : null;
                    item.setMaxScore(eq != null && eq.getScoreOverride() != null ?
                            eq.getScoreOverride() : question.getScore());
                }
            }

            items.add(item);
        }
        dto.setDetails(items);
        return dto;
    }

    @Override
    @Transactional
    public AnswerSheet regrade(Long id, Long graderId) {
        AnswerSheet sheet = answerSheetMapper.selectById(id);
        if (sheet == null) throw new BusinessException("答卷不存在");
        if ("IN_PROGRESS".equals(sheet.getStatus())) throw new BusinessException("答卷尚未提交");

        Exam exam = examMapper.selectById(sheet.getExamId());
        List<AnswerDetail> details = answerDetailMapper.selectList(
                new LambdaQueryWrapper<AnswerDetail>()
                        .eq(AnswerDetail::getAnswerSheetId, id));

        double totalScore = 0;
        boolean allGraded = true;

        for (AnswerDetail detail : details) {
            // Use snapshot data for regrading — never read live question table
            int qScore = detail.getSnapshotScore() != null ? detail.getSnapshotScore() : 10;

            if (detail.getSnapshotCorrectAnswer() == null) {
                // Snapshot missing (legacy data) — give full score
                detail.setIsCorrect(1);
                detail.setScoreEarned((double) qScore);
                detail.setGradingNote("快照数据缺失，自动给满分");
                answerDetailMapper.updateById(detail);
                totalScore += qScore;
                continue;
            }

            if (detail.getIsCorrect() == null) {
                // Still needs manual grading for subjective questions
                allGraded = false;
                continue;
            }

            if (detail.getIsCorrect() == 1) {
                totalScore += qScore;
            }
        }

        sheet.setScore(totalScore);
        sheet.setPass(totalScore >= exam.getPassScore() ? 1 : 0);
        if (allGraded) {
            sheet.setGradingCompletedAt(LocalDateTime.now());
        }
        answerSheetMapper.updateById(sheet);

        // Update or create grade
        Grade existingGrade = gradeMapper.selectOne(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getAnswerSheetId, id));
        if (existingGrade != null && allGraded) {
            existingGrade.setScore(totalScore);
            existingGrade.setPass(totalScore >= exam.getPassScore() ? 1 : 0);
            existingGrade.setGradedBy(graderId);
            existingGrade.setGradedAt(LocalDateTime.now());
            gradeMapper.updateById(existingGrade);
        } else if (existingGrade == null && allGraded) {
            Grade grade = Grade.builder()
                    .answerSheetId(id)
                    .studentId(sheet.getStudentId())
                    .examId(sheet.getExamId())
                    .courseId(exam.getCourseId())
                    .score(totalScore)
                    .totalScore(exam.getTotalScore())
                    .pass(totalScore >= exam.getPassScore() ? 1 : 0)
                    .gradedBy(graderId)
                    .gradedAt(LocalDateTime.now())
                    .build();
            gradeMapper.insert(grade);
        }

        return sheet;
    }
}
