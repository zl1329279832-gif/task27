package com.training.mapper;

import com.training.entity.Exam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExamMapper {

    Exam selectById(Long id);

    List<Exam> selectList(@Param("courseId") Long courseId,
                          @Param("status") String status,
                          @Param("keyword") String keyword);

    int insert(Exam exam);

    int updateById(Exam exam);

    int deleteById(Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
