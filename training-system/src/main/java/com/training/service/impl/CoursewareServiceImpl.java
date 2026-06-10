package com.training.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.training.common.BusinessException;
import com.training.entity.Courseware;
import com.training.entity.dto.CoursewareRequest;
import com.training.mapper.CoursewareMapper;
import com.training.service.CoursewareService;
import com.training.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoursewareServiceImpl implements CoursewareService {

    private final CoursewareMapper coursewareMapper;
    private final FileStorageUtil fileStorageUtil;

    @Override
    public List<Courseware> listByChapterId(Long chapterId) {
        QueryWrapper<Courseware> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chapter_id", chapterId);
        queryWrapper.ne("status", "DELETED");
        queryWrapper.orderByAsc("sort_order");
        return coursewareMapper.selectList(queryWrapper);
    }

    @Override
    public Courseware getById(Long id) {
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null || "DELETED".equals(courseware.getStatus())) {
            throw new BusinessException(404, "课件不存在");
        }
        return courseware;
    }

    @Override
    @Transactional
    public Courseware create(Long chapterId, CoursewareRequest req, MultipartFile file) {
        Courseware courseware = new Courseware();
        courseware.setChapterId(chapterId);
        courseware.setTitle(req.getTitle());
        courseware.setFileType(req.getFileType());
        courseware.setDurationSeconds(req.getDurationSeconds());
        courseware.setSortOrder(req.getSortOrder());

        if ("LINK".equals(req.getFileType())) {
            courseware.setFilePath(req.getLinkUrl());
        } else if (file != null && !file.isEmpty()) {
            String filePath = fileStorageUtil.store(file, "courseware");
            courseware.setFilePath(filePath);
            courseware.setFileSize(file.getSize());

            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase();
                courseware.setFileType(extension);
            }
        }

        courseware.setStatus("ACTIVE");
        coursewareMapper.insert(courseware);
        return courseware;
    }

    @Override
    @Transactional
    public Courseware update(Long id, CoursewareRequest req) {
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null || "DELETED".equals(courseware.getStatus())) {
            throw new BusinessException(404, "课件不存在");
        }
        courseware.setTitle(req.getTitle());
        courseware.setFileType(req.getFileType());
        courseware.setDurationSeconds(req.getDurationSeconds());
        courseware.setSortOrder(req.getSortOrder());
        if (StringUtils.hasText(req.getLinkUrl())) {
            courseware.setFilePath(req.getLinkUrl());
        }
        coursewareMapper.updateById(courseware);
        return courseware;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Courseware courseware = coursewareMapper.selectById(id);
        if (courseware == null || "DELETED".equals(courseware.getStatus())) {
            throw new BusinessException(404, "课件不存在");
        }
        courseware.setStatus("DELETED");
        coursewareMapper.updateById(courseware);
    }

    @Override
    public Resource download(Long id) {
        Courseware courseware = getById(id);
        if (!StringUtils.hasText(courseware.getFilePath())) {
            throw new BusinessException("课件文件路径为空");
        }
        return fileStorageUtil.getResource(courseware.getFilePath());
    }
}
