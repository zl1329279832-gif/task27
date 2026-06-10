package com.training.mapper;

import com.training.entity.Score;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScoreMapper {

    Score selectById(Long id);

    List<Score> selectByStudentId(Long studentId);

    List<Score> selectByExamId(Long examId);

    Score selectBySubmissionId(Long submissionId);

    Score selectBestByStudentAndExam(@Param("studentId") Long studentId,
                                     @Param("examId") Long examId);

    int insert(Score score);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("reviewedBy") Long reviewedBy,
                     @Param("reviewedAt") LocalDateTime reviewedAt);
}
