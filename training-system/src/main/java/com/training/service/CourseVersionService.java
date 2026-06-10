package com.training.service;

import com.training.entity.Course;
import com.training.entity.CourseVersionHistory;

import java.util.List;

public interface CourseVersionService {

    Course upgradeVersion(Long courseId, String changeSummary, Long operatorId);

    List<CourseVersionHistory> getVersionHistory(Long courseId);

    int getCurrentVersion(Long courseId);
}
