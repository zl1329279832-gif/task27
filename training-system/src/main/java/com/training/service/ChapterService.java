package com.training.service;

import com.training.entity.Chapter;
import com.training.entity.dto.ChapterRequest;

import java.util.List;

public interface ChapterService {

    List<Chapter> listByCourseId(Long courseId);

    Chapter getById(Long id);

    Chapter create(Long courseId, ChapterRequest req);

    Chapter update(Long id, ChapterRequest req);

    void delete(Long id);

    void updateSort(List<Long> chapterIds);
}
