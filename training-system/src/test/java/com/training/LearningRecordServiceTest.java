package com.training;

import com.training.entity.Chapter;
import com.training.entity.Course;
import com.training.entity.Courseware;
import com.training.entity.LearningRecord;
import com.training.entity.dto.CourseProgressDTO;
import com.training.entity.dto.HeartbeatRequest;
import com.training.mapper.*;
import com.training.service.impl.LearningRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Learning Record Service Tests")
class LearningRecordServiceTest {

    @Mock private LearningRecordMapper learningRecordMapper;
    @Mock private CoursewareMapper coursewareMapper;
    @Mock private ChapterMapper chapterMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private LearningRecordServiceImpl learningRecordService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========================================================================
    // heartbeat tests
    // ========================================================================

    @Test
    @DisplayName("heartbeat: should create new record on first call for a courseware")
    void heartbeat_shouldCreateNewRecordOnFirstCall() {
        // No existing record
        when(learningRecordMapper.selectOne(any())).thenReturn(null);

        Courseware courseware = Courseware.builder()
                .id(1L).chapterId(100L).durationSeconds(600).build();
        when(coursewareMapper.selectById(1L)).thenReturn(courseware);

        HeartbeatRequest req = new HeartbeatRequest();
        req.setCoursewareId(1L);
        req.setCourseId(10L);
        req.setChapterId(100L);
        req.setCurrentPositionSeconds(30);
        req.setStudyDurationSeconds(30);

        learningRecordService.heartbeat(1L, req);

        // Verify a new record was inserted with IN_PROGRESS status
        ArgumentCaptor<LearningRecord> insertCaptor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).insert(insertCaptor.capture());
        LearningRecord inserted = insertCaptor.getValue();
        assertEquals(1L, inserted.getStudentId());
        assertEquals(1L, inserted.getCoursewareId());
        assertEquals("IN_PROGRESS", inserted.getStatus());
        assertNotNull(inserted.getStartedAt());
        assertEquals(30, inserted.getStudyDurationSeconds());
        assertEquals(30, inserted.getLastPositionSeconds());

        // Progress = 30 * 100 / 600 = 5%
        // Also verify updateById is called after insert
        verify(learningRecordMapper).updateById(any(LearningRecord.class));

        // Verify Redis cache is set
        verify(valueOperations).set(eq("learning:1:1"), any(), eq(1L), any());
    }

    @Test
    @DisplayName("heartbeat: should update existing record on subsequent calls")
    void heartbeat_shouldUpdateExistingRecord() {
        LearningRecord existing = new LearningRecord();
        existing.setId(1L);
        existing.setStudentId(1L);
        existing.setCoursewareId(1L);
        existing.setStudyDurationSeconds(300);
        existing.setLastPositionSeconds(300);
        existing.setProgressPercent(50.0);
        existing.setStatus("IN_PROGRESS");
        when(learningRecordMapper.selectOne(any())).thenReturn(existing);

        Courseware courseware = Courseware.builder()
                .id(1L).chapterId(100L).durationSeconds(600).build();
        when(coursewareMapper.selectById(1L)).thenReturn(courseware);

        HeartbeatRequest req = new HeartbeatRequest();
        req.setCoursewareId(1L);
        req.setCourseId(10L);
        req.setChapterId(100L);
        req.setCurrentPositionSeconds(360);
        req.setStudyDurationSeconds(30);

        learningRecordService.heartbeat(1L, req);

        // Verify record was updated (not inserted)
        verify(learningRecordMapper, never()).insert(any());

        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).updateById(captor.capture());
        LearningRecord updated = captor.getValue();

        // Duration incremented: 300 + 30 = 330
        assertEquals(330, updated.getStudyDurationSeconds());
        // Position updated to current position
        assertEquals(360, updated.getLastPositionSeconds());
        // Status remains IN_PROGRESS (progress < 100%)
        assertEquals("IN_PROGRESS", updated.getStatus());
        // Progress = 330 * 100 / 600 = 55%
    }

    @Test
    @DisplayName("heartbeat: should transition to COMPLETED when progress reaches 100%")
    void heartbeat_shouldTransitionToCompletedAt100Percent() {
        LearningRecord existing = new LearningRecord();
        existing.setId(1L);
        existing.setStudentId(1L);
        existing.setCoursewareId(1L);
        existing.setStudyDurationSeconds(570);
        existing.setLastPositionSeconds(570);
        existing.setProgressPercent(95.0);
        existing.setStatus("IN_PROGRESS");
        when(learningRecordMapper.selectOne(any())).thenReturn(existing);

        Courseware courseware = Courseware.builder()
                .id(1L).chapterId(100L).durationSeconds(600).build();
        when(coursewareMapper.selectById(1L)).thenReturn(courseware);

        HeartbeatRequest req = new HeartbeatRequest();
        req.setCoursewareId(1L);
        req.setCourseId(10L);
        req.setChapterId(100L);
        req.setCurrentPositionSeconds(600);
        req.setStudyDurationSeconds(30);

        learningRecordService.heartbeat(1L, req);

        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).updateById(captor.capture());
        LearningRecord updated = captor.getValue();

        // Duration: 570 + 30 = 600, progress = min(100, 600*100/600) = 100%
        assertEquals(600, updated.getStudyDurationSeconds());
        assertEquals("COMPLETED", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    @DisplayName("heartbeat: should set 100% immediately for zero-duration courseware (e.g., PDF)")
    void heartbeat_shouldSet100PercentForZeroDurationCourseware() {
        when(learningRecordMapper.selectOne(any())).thenReturn(null);

        // PDF or document type with durationSeconds = 0
        Courseware courseware = Courseware.builder()
                .id(2L).chapterId(100L).durationSeconds(0).build();
        when(coursewareMapper.selectById(2L)).thenReturn(courseware);

        HeartbeatRequest req = new HeartbeatRequest();
        req.setCoursewareId(2L);
        req.setCourseId(10L);
        req.setChapterId(100L);
        req.setStudyDurationSeconds(5);

        learningRecordService.heartbeat(1L, req);

        // Should be inserted and then updated to COMPLETED immediately
        verify(learningRecordMapper).insert(any(LearningRecord.class));

        ArgumentCaptor<LearningRecord> captor = ArgumentCaptor.forClass(LearningRecord.class);
        verify(learningRecordMapper).updateById(captor.capture());
        LearningRecord updated = captor.getValue();
        assertEquals("COMPLETED", updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    // ========================================================================
    // getCompletionRate tests
    // ========================================================================

    @Test
    @DisplayName("getCompletionRate: should calculate correctly across multiple coursewares")
    void getCompletionRate_shouldCalculateCorrectly() {
        // Course with 1 chapter, 2 coursewares
        Course course = Course.builder().id(10L).title("Java Basics").build();
        Chapter chapter = Chapter.builder().id(1L).courseId(10L).sortOrder(1).title("Chapter 1").build();
        Courseware cw1 = Courseware.builder().id(1L).chapterId(1L).durationSeconds(600).build();
        Courseware cw2 = Courseware.builder().id(2L).chapterId(1L).durationSeconds(600).build();

        // cw1 is 100% complete, cw2 is 50% complete
        LearningRecord lr1 = new LearningRecord();
        lr1.setCoursewareId(1L);
        lr1.setProgressPercent(100.0);
        lr1.setStudyDurationSeconds(600);

        LearningRecord lr2 = new LearningRecord();
        lr2.setCoursewareId(2L);
        lr2.setProgressPercent(50.0);
        lr2.setStudyDurationSeconds(300);

        when(courseMapper.selectById(10L)).thenReturn(course);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        when(coursewareMapper.selectList(any())).thenReturn(List.of(cw1, cw2));
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr1, lr2));

        double rate = learningRecordService.getCompletionRate(1L, 10L);

        // (100 + 50) / 2 = 75.0
        assertEquals(75.0, rate, 0.01);
    }

    @Test
    @DisplayName("getCompletionRate: should return 0 when no learning records exist")
    void getCompletionRate_shouldReturnZeroWithNoRecords() {
        Course course = Course.builder().id(10L).title("Java Basics").build();
        Chapter chapter = Chapter.builder().id(1L).courseId(10L).sortOrder(1).build();
        Courseware cw1 = Courseware.builder().id(1L).chapterId(1L).build();

        when(courseMapper.selectById(10L)).thenReturn(course);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        when(coursewareMapper.selectList(any())).thenReturn(List.of(cw1));
        when(learningRecordMapper.selectList(any())).thenReturn(List.of());

        double rate = learningRecordService.getCompletionRate(1L, 10L);

        // No records -> 0.0 / 1 = 0.0
        assertEquals(0.0, rate, 0.01);
    }

    @Test
    @DisplayName("getCompletionRate: should return 100 when all coursewares are complete")
    void getCompletionRate_shouldReturn100WhenAllComplete() {
        Course course = Course.builder().id(10L).title("Java Basics").build();
        Chapter chapter = Chapter.builder().id(1L).courseId(10L).sortOrder(1).build();
        Courseware cw1 = Courseware.builder().id(1L).chapterId(1L).build();
        Courseware cw2 = Courseware.builder().id(2L).chapterId(1L).build();

        LearningRecord lr1 = new LearningRecord();
        lr1.setCoursewareId(1L);
        lr1.setProgressPercent(100.0);
        lr1.setStudyDurationSeconds(600);

        LearningRecord lr2 = new LearningRecord();
        lr2.setCoursewareId(2L);
        lr2.setProgressPercent(100.0);
        lr2.setStudyDurationSeconds(400);

        when(courseMapper.selectById(10L)).thenReturn(course);
        when(chapterMapper.selectList(any())).thenReturn(List.of(chapter));
        when(coursewareMapper.selectList(any())).thenReturn(List.of(cw1, cw2));
        when(learningRecordMapper.selectList(any())).thenReturn(List.of(lr1, lr2));

        double rate = learningRecordService.getCompletionRate(1L, 10L);

        assertEquals(100.0, rate, 0.01);
    }
}
