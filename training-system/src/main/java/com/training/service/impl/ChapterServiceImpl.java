package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.Chapter;
import com.training.entity.dto.ChapterRequest;
import com.training.mapper.ChapterMapper;
import com.training.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;

    @Override
    public List<Chapter> listByCourseId(Long courseId) {
        QueryWrapper<Chapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        queryWrapper.orderByAsc("sort_order");
        return chapterMapper.selectList(queryWrapper);
    }

    @Override
    public Chapter getById(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(404, "章节不存在");
        }
        return chapter;
    }

    @Override
    @Transactional
    public Chapter create(Long courseId, ChapterRequest req) {
        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setTitle(req.getTitle());
        chapter.setSortOrder(req.getSortOrder());
        chapter.setDescription(req.getDescription());
        chapterMapper.insert(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public Chapter update(Long id, ChapterRequest req) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(404, "章节不存在");
        }
        chapter.setTitle(req.getTitle());
        chapter.setSortOrder(req.getSortOrder());
        chapter.setDescription(req.getDescription());
        chapterMapper.updateById(chapter);
        return chapter;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(404, "章节不存在");
        }
        chapterMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateSort(List<Long> chapterIds) {
        for (int i = 0; i < chapterIds.size(); i++) {
            Chapter chapter = chapterMapper.selectById(chapterIds.get(i));
            if (chapter == null) {
                throw new BusinessException(404, "章节不存在, id=" + chapterIds.get(i));
            }
            chapter.setSortOrder(i);
            chapterMapper.updateById(chapter);
        }
    }
}
