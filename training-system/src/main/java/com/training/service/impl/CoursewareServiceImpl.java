package com.training.service.impl;

import com.training.entity.Chapter;
import com.training.entity.Courseware;
import com.training.exception.BusinessException;
import com.training.mapper.ChapterMapper;
import com.training.mapper.CoursewareMapper;
import com.training.service.CoursewareService;
import com.training.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoursewareServiceImpl implements CoursewareService {

    private final CoursewareMapper coursewareMapper;
    private final FileStorageUtil fileStorageUtil;
    private final ChapterMapper chapterMapper;

    @Override
    public List<Courseware> listByChapterId(Long chapterId) {
        log.info("查询章节课件列表, chapterId: {}", chapterId);
        return coursewareMapper.selectByChapterId(chapterId);
    }

    @Override
    public Courseware getCoursewareById(Long id) {
        log.info("查询课件详情, id: {}", id);
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null) {
            throw new BusinessException("课件不存在");
        }
        return courseware;
    }

    @Override
    public Courseware createCourseware(Long chapterId, String title, String fileType, MultipartFile file,
                                       Integer sortOrder, Integer duration) {
        log.info("创建课件, chapterId: {}, title: {}", chapterId, title);

        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }

        String filePath;
        try {
            filePath = fileStorageUtil.store(file, "courseware");
        } catch (java.io.IOException e) {
            throw new BusinessException("课件上传失败: " + e.getMessage());
        }

        Courseware courseware = new Courseware();
        courseware.setChapterId(chapterId);
        courseware.setTitle(title);
        courseware.setFileType(fileType);
        courseware.setFileUrl(filePath);
        courseware.setFileSize(file.getSize());
        courseware.setSortOrder(sortOrder);
        courseware.setDuration(duration);
        coursewareMapper.insert(courseware);
        return courseware;
    }

    @Override
    public void updateCourseware(Long id, String title, Integer sortOrder, Integer duration) {
        log.info("更新课件, id: {}", id);
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null) {
            throw new BusinessException("课件不存在");
        }
        courseware.setTitle(title);
        courseware.setSortOrder(sortOrder);
        courseware.setDuration(duration);
        coursewareMapper.updateById(courseware);
    }

    @Override
    public void deleteCourseware(Long id) {
        log.info("删除课件, id: {}", id);
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null) {
            throw new BusinessException("课件不存在");
        }
        fileStorageUtil.delete(courseware.getFileUrl());
        coursewareMapper.deleteById(id);
    }

    @Override
    public byte[] downloadCourseware(Long id) {
        log.info("下载课件, id: {}", id);
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null) {
            throw new BusinessException("课件不存在");
        }
        try {
            return fileStorageUtil.load(courseware.getFileUrl());
        } catch (java.io.IOException e) {
            throw new BusinessException("课件下载失败: " + e.getMessage());
        }
    }
}
