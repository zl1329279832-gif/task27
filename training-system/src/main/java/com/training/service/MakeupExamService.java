package com.training.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.entity.MakeupExam;
import com.training.entity.dto.MakeupExamDTO;

public interface MakeupExamService {

    MakeupExam create(MakeupExam makeupExam);

    IPage<MakeupExam> listByStudent(Long studentId, int page, int size);

    void startMakeupExam(Long makeupExamId);

    void recordMakeupExamResult(Long makeupExamId, double score);
}
