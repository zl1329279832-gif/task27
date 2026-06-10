package com.training.mapper;

import com.training.entity.LearningRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LearningRecordMapper {

    LearningRecord selectById(Long id);

    LearningRecord selectByStudentAndCourseware(@Param("studentId") Long studentId, @Param("coursewareId") Long coursewareId);

    List<LearningRecord> selectByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    int insert(LearningRecord record);

    int updateById(LearningRecord record);

    int countCompletedByStudentAndCourse(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
