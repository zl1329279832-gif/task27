package com.training.service;

import com.training.dto.request.ChapterRequest;
import com.training.entity.Chapter;

import java.util.List;

public interface ChapterService {

    List<Chapter> listByCourseId(Long courseId);

    Chapter getChapterById(Long id);

    Chapter createChapter(ChapterRequest request);

    void updateChapter(Long id, ChapterRequest request);

    void deleteChapter(Long id);
}
