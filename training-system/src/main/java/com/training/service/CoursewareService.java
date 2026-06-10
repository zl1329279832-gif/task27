package com.training.service;

import com.training.entity.Courseware;
import com.training.entity.dto.CoursewareRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CoursewareService {

    List<Courseware> listByChapterId(Long chapterId);

    Courseware getById(Long id);

    Courseware create(Long chapterId, CoursewareRequest req, MultipartFile file);

    Courseware update(Long id, CoursewareRequest req);

    void delete(Long id);

    Resource download(Long id);
}
