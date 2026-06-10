package com.training.service;

import com.training.entity.Courseware;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CoursewareService {

    List<Courseware> listByChapterId(Long chapterId);

    Courseware getCoursewareById(Long id);

    Courseware createCourseware(Long chapterId, String title, String fileType, MultipartFile file, Integer sortOrder, Integer duration);

    void updateCourseware(Long id, String title, Integer sortOrder, Integer duration);

    void deleteCourseware(Long id);

    byte[] downloadCourseware(Long id);
}
