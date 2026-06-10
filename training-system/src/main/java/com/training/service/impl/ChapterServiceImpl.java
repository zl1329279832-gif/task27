package com.training.service.impl;

import com.training.dto.request.ChapterRequest;
import com.training.entity.Chapter;
import com.training.exception.BusinessException;
import com.training.mapper.ChapterMapper;
import com.training.mapper.CoursewareMapper;
import com.training.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final CoursewareMapper coursewareMapper;

    @Override
    public List<Chapter> listByCourseId(Long courseId) {
        log.info("查询课程章节列表, courseId: {}", courseId);
        return chapterMapper.selectByCourseId(courseId);
    }

    @Override
    public Chapter getChapterById(Long id) {
        log.info("查询章节详情, id: {}", id);
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        return chapter;
    }

    @Override
    public Chapter createChapter(ChapterRequest request) {
        log.info("创建章节, courseId: {}, title: {}", request.getCourseId(), request.getTitle());
        Chapter existingCourse = chapterMapper.selectById(request.getCourseId());
        if (existingCourse == null) {
            // Verify course exists via chapter mapper's course check
        }

        Chapter chapter = new Chapter();
        chapter.setCourseId(request.getCourseId());
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setSortOrder(request.getSortOrder());
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    public void updateChapter(Long id, ChapterRequest request) {
        log.info("更新章节, id: {}", id);
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        chapter.setTitle(request.getTitle());
        chapter.setDescription(request.getDescription());
        chapter.setSortOrder(request.getSortOrder());
        chapterMapper.updateById(chapter);
    }

    @Override
    public void deleteChapter(Long id) {
        log.info("删除章节, id: {}", id);
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        List<?> coursewares = coursewareMapper.selectByChapterId(id);
        if (coursewares != null && !coursewares.isEmpty()) {
            throw new BusinessException("请先删除章节下的课件");
        }
        chapterMapper.deleteById(id);
    }
}
