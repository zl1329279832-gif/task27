package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.Grade;

import java.util.Map;

public interface GradeService {
    IPage<Grade> listByStudent(Long studentId, int page, int size);
    IPage<Grade> listByExam(Long examId, int page, int size);
    Map<String, Object> getCourseStats(Long courseId);
    Grade getByAnswerSheet(Long answerSheetId);
}
