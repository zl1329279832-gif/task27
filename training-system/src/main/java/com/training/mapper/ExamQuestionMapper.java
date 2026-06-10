package com.training.mapper;

import com.training.entity.ExamQuestion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExamQuestionMapper {

    List<ExamQuestion> selectByExamId(Long examId);

    int insert(ExamQuestion examQuestion);

    int batchInsert(@Param("list") List<ExamQuestion> list);

    int deleteByExamId(Long examId);

    int countByExamId(Long examId);
}
