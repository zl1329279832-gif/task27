package com.training.mapper;

import com.training.entity.SubmissionAnswer;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SubmissionAnswerMapper {

    SubmissionAnswer selectById(Long id);

    List<SubmissionAnswer> selectBySubmissionId(Long submissionId);

    SubmissionAnswer selectBySubmissionAndQuestion(@Param("submissionId") Long submissionId,
                                                   @Param("questionId") Long questionId);

    int insert(SubmissionAnswer answer);

    int batchInsert(@Param("list") List<SubmissionAnswer> list);

    int updateAnswer(@Param("id") Long id,
                     @Param("studentAnswer") String studentAnswer,
                     @Param("answeredAt") LocalDateTime answeredAt);

    int updateScore(@Param("id") Long id,
                    @Param("isCorrect") Integer isCorrect,
                    @Param("awardedScore") BigDecimal awardedScore);
}
