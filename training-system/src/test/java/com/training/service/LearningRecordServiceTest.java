package com.training.service;

import com.training.entity.Chapter;
import com.training.entity.Courseware;
import com.training.entity.LearningRecord;
import com.training.enums.LearningStatus;
import com.training.mapper.ChapterMapper;
import com.training.mapper.CourseMapper;
import com.training.mapper.CoursewareMapper;
import com.training.mapper.LearningRecordMapper;
import com.training.service.impl.LearningRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningRecordServiceTest {

    @Mock
    private LearningRecordMapper learningRecordMapper;

    @Mock
    private CoursewareMapper coursewareMapper;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private LearningRecordServiceImpl learningRecordService;

    @Test
    void testUpdateProgress_NewRecord() {
        Long studentId = 1L;
        Long coursewareId = 10L;
        Long chapterId = 5L;
        Long courseId = 2L;

        Courseware courseware = new Courseware();
        courseware.setId(coursewareId);
        courseware.setChapterId(chapterId);

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setCourseId(courseId);

        when(coursewareMapper.selectById(coursewareId)).thenReturn(courseware);
        when(chapterMapper.selectById(chapterId)).thenReturn(chapter);
        when(learningRecordMapper.selectByStudentAndCourseware(studentId, coursewareId)).thenReturn(null);
        when(learningRecordMapper.insert(any(LearningRecord.class))).thenReturn(1);

        LearningRecord result = learningRecordService.updateProgress(studentId, coursewareId, 50, 100, 300);

        assertNotNull(result);
        assertEquals(LearningStatus.IN_PROGRESS.name(), result.getStatus());
        assertEquals(50, result.getProgress());
        assertEquals(100, result.getLastPosition());
        assertEquals(300, result.getStudyDuration());
        assertEquals(studentId, result.getStudentId());
        assertEquals(coursewareId, result.getCoursewareId());
        assertEquals(courseId, result.getCourseId());
        assertEquals(chapterId, result.getChapterId());
        assertNull(result.getCompletedAt());

        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).insert(captor.capture());
        assertEquals(LearningStatus.IN_PROGRESS.name(), captor.getValue().getStatus());
    }

    @Test
    void testUpdateProgress_CompleteRecord() {
        Long studentId = 1L;
        Long coursewareId = 10L;
        Long chapterId = 5L;
        Long courseId = 2L;

        Courseware courseware = new Courseware();
        courseware.setId(coursewareId);
        courseware.setChapterId(chapterId);

        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setCourseId(courseId);

        LearningRecord existingRecord = new LearningRecord();
        existingRecord.setId(1L);
        existingRecord.setStudentId(studentId);
        existingRecord.setCoursewareId(coursewareId);
        existingRecord.setCourseId(courseId);
        existingRecord.setChapterId(chapterId);
        existingRecord.setProgress(50);
        existingRecord.setLastPosition(100);
        existingRecord.setStudyDuration(300);
        existingRecord.setStatus(LearningStatus.IN_PROGRESS.name());

        when(coursewareMapper.selectById(coursewareId)).thenReturn(courseware);
        when(chapterMapper.selectById(chapterId)).thenReturn(chapter);
        when(learningRecordMapper.selectByStudentAndCourseware(studentId, coursewareId)).thenReturn(existingRecord);
        when(learningRecordMapper.updateById(any(LearningRecord.class))).thenReturn(1);

        LearningRecord result = learningRecordService.updateProgress(studentId, coursewareId, 100, 500, 200);

        assertNotNull(result);
        assertEquals(LearningStatus.COMPLETED.name(), result.getStatus());
        assertEquals(100, result.getProgress());
        assertNotNull(result.getCompletedAt());
        assertEquals(500, result.getLastPosition());
        assertEquals(500, result.getStudyDuration());

        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).updateById(captor.capture());
        assertEquals(LearningStatus.COMPLETED.name(), captor.getValue().getStatus());
        assertNotNull(captor.getValue().getCompletedAt());
    }

    @Test
    void testGetCourseCompletionRate() {
        Long studentId = 1L;
        Long courseId = 2L;

        when(coursewareMapper.countByCourseId(courseId)).thenReturn(10);
        when(learningRecordMapper.countCompletedByStudentAndCourse(studentId, courseId)).thenReturn(8);

        BigDecimal result = learningRecordService.getCourseCompletionRate(studentId, courseId);

        BigDecimal expected = new BigDecimal(8)
                .divide(new BigDecimal(10), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    void testGetCourseCompletionRate_NoCourseware() {
        Long studentId = 1L;
        Long courseId = 2L;

        when(coursewareMapper.countByCourseId(courseId)).thenReturn(0);

        BigDecimal result = learningRecordService.getCourseCompletionRate(studentId, courseId);

        assertEquals(BigDecimal.ZERO, result);
    }
}
