package com.training.mapper;

import com.training.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuestionMapper {

    Question selectById(Long id);

    List<Question> selectList(@Param("courseId") Long courseId,
                              @Param("questionType") String questionType,
                              @Param("difficulty") String difficulty);

    List<Question> selectRandomByCourseId(@Param("courseId") Long courseId,
                                          @Param("count") int count);

    int insert(Question question);

    int updateById(Question question);

    int softDelete(Long id);

    int countByCourseId(Long courseId);
}
