package com.training.mapper;

import com.training.entity.ExamSubmission;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExamSubmissionMapper {

    ExamSubmission selectById(Long id);

    ExamSubmission selectByIdWithAnswers(Long id);

    List<ExamSubmission> selectByExamAndStudent(@Param("examId") Long examId,
                                                @Param("studentId") Long studentId);

    ExamSubmission selectInProgressByExamAndStudent(@Param("examId") Long examId,
                                                    @Param("studentId") Long studentId);

    int countByExamAndStudent(@Param("examId") Long examId,
                              @Param("studentId") Long studentId);

    List<ExamSubmission> selectByStatus(String status);

    int insert(ExamSubmission submission);

    int updateById(ExamSubmission submission);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("submitTime") LocalDateTime submitTime,
                     @Param("totalScore") BigDecimal totalScore);
}
