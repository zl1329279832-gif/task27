package com.training.service.impl;

import com.github.pagehelper.PageHelper;
import com.training.dto.request.ClazzRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Clazz;
import com.training.entity.ClazzStudent;
import com.training.entity.SysUser;
import com.training.exception.BusinessException;
import com.training.mapper.ClazzMapper;
import com.training.mapper.ClazzStudentMapper;
import com.training.mapper.SysUserMapper;
import com.training.service.ClazzService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClazzServiceImpl implements ClazzService {

    private final ClazzMapper clazzMapper;
    private final ClazzStudentMapper clazzStudentMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<Clazz> listClazzes(String keyword, String status, Long courseId, int page, int size) {
        log.info("查询班级列表, keyword: {}, status: {}, courseId: {}, page: {}, size: {}",
                keyword, status, courseId, page, size);
        PageHelper.startPage(page, size);
        List<Clazz> list = clazzMapper.selectList(keyword, status, courseId);
        return PageResult.of(list);
    }

    @Override
    public Clazz getClazzById(Long id) {
        log.info("查询班级详情, id: {}", id);
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        return clazz;
    }

    @Override
    public Clazz createClazz(ClazzRequest request) {
        log.info("创建班级, name: {}", request.getName());
        Clazz clazz = new Clazz();
        clazz.setName(request.getName());
        clazz.setCourseId(request.getCourseId());
        clazz.setInstructorId(request.getInstructorId());
        clazz.setMaxStudents(request.getMaxStudents());
        clazz.setStartDate(request.getStartDate());
        clazz.setEndDate(request.getEndDate());
        clazz.setStatus("ACTIVE");
        clazzMapper.insert(clazz);
        return clazz;
    }

    @Override
    public void updateClazz(Long id, ClazzRequest request) {
        log.info("更新班级, id: {}", id);
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        clazz.setName(request.getName());
        clazz.setCourseId(request.getCourseId());
        clazz.setInstructorId(request.getInstructorId());
        clazz.setMaxStudents(request.getMaxStudents());
        clazz.setStartDate(request.getStartDate());
        clazz.setEndDate(request.getEndDate());
        clazzMapper.updateById(clazz);
    }

    @Override
    public void deleteClazz(Long id) {
        log.info("删除班级, id: {}", id);
        Clazz clazz = clazzMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        int studentCount = clazzStudentMapper.countByClazzId(id);
        if (studentCount > 0) {
            throw new BusinessException("班级中仍有学员，无法删除");
        }
        clazzMapper.deleteById(id);
    }

    @Override
    public void addStudent(Long clazzId, Long studentId) {
        log.info("添加学员, clazzId: {}, studentId: {}", clazzId, studentId);
        SysUser student = sysUserMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException("学员不存在");
        }

        Clazz clazz = clazzMapper.selectById(clazzId);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }

        ClazzStudent existing = clazzStudentMapper.selectByClazzAndStudent(clazzId, studentId);
        if (existing != null) {
            throw new BusinessException("学员已在该班级中");
        }

        int currentCount = clazzStudentMapper.countByClazzId(clazzId);
        if (clazz.getMaxStudents() != null && currentCount >= clazz.getMaxStudents()) {
            throw new BusinessException("班级人数已达上限");
        }

        ClazzStudent clazzStudent = new ClazzStudent();
        clazzStudent.setClazzId(clazzId);
        clazzStudent.setStudentId(studentId);
        clazzStudentMapper.insert(clazzStudent);
    }

    @Override
    public void removeStudent(Long clazzId, Long studentId) {
        log.info("移除学员, clazzId: {}, studentId: {}", clazzId, studentId);
        clazzStudentMapper.deleteByClazzAndStudent(clazzId, studentId);
    }

    @Override
    public List<ClazzStudent> listStudents(Long clazzId) {
        log.info("查询班级学员列表, clazzId: {}", clazzId);
        return clazzStudentMapper.selectByClazzId(clazzId);
    }
}
