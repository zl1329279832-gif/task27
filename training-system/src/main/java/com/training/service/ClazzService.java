package com.training.service;

import com.training.dto.request.ClazzRequest;
import com.training.dto.response.PageResult;
import com.training.entity.Clazz;
import com.training.entity.ClazzStudent;

import java.util.List;

public interface ClazzService {

    PageResult<Clazz> listClazzes(String keyword, String status, Long courseId, int page, int size);

    Clazz getClazzById(Long id);

    Clazz createClazz(ClazzRequest request);

    void updateClazz(Long id, ClazzRequest request);

    void deleteClazz(Long id);

    void addStudent(Long clazzId, Long studentId);

    void removeStudent(Long clazzId, Long studentId);

    List<ClazzStudent> listStudents(Long clazzId);
}
