CREATE DATABASE IF NOT EXISTS training_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE training_system;

-- ----------------------------
-- 1. sys_user
-- ----------------------------
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'ADMIN/INSTRUCTOR/STUDENT/AUDITOR',
    avatar VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System users';

-- ----------------------------
-- 2. course
-- ----------------------------
CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    instructor_id BIGINT,
    category VARCHAR(100),
    difficulty VARCHAR(20) DEFAULT 'BEGINNER',
    total_hours DECIMAL(5,1),
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_instructor (instructor_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Courses';

-- ----------------------------
-- 3. chapter
-- ----------------------------
CREATE TABLE chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    sort_order INT DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Course chapters';

-- ----------------------------
-- 4. courseware
-- ----------------------------
CREATE TABLE courseware (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    title VARCHAR(200),
    file_path VARCHAR(500),
    file_type VARCHAR(20) COMMENT 'VIDEO/PDF/DOC/LINK',
    file_size BIGINT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Course materials/courseware';

-- ----------------------------
-- 5. clazz
-- ----------------------------
CREATE TABLE clazz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    instructor_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_instructor (instructor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Classes';

-- ----------------------------
-- 6. clazz_student
-- ----------------------------
CREATE TABLE clazz_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_clazz_student (clazz_id, student_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class-student mapping';

-- ----------------------------
-- 7. clazz_course
-- ----------------------------
CREATE TABLE clazz_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    start_date DATE,
    end_date DATE,
    UNIQUE KEY uk_clazz_course (clazz_id, course_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Class-course mapping';

-- ----------------------------
-- 8. learning_record
-- ----------------------------
CREATE TABLE learning_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    courseware_id BIGINT NOT NULL,
    progress_percent DECIMAL(5,2) DEFAULT 0,
    study_duration_seconds INT DEFAULT 0,
    last_position_seconds INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'NOT_STARTED',
    started_at DATETIME,
    completed_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_courseware (student_id, courseware_id),
    INDEX idx_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning progress records';

-- ----------------------------
-- 9. question
-- ----------------------------
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    instructor_id BIGINT,
    content TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL COMMENT 'SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_BLANK/SHORT_ANSWER',
    options JSON,
    correct_answer TEXT NOT NULL,
    score INT DEFAULT 10,
    difficulty VARCHAR(20) DEFAULT 'INTERMEDIATE',
    explanation TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    deleted_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course (course_id),
    INDEX idx_instructor (instructor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam questions';

-- ----------------------------
-- 10. exam
-- ----------------------------
CREATE TABLE exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL,
    total_score INT DEFAULT 100,
    pass_score INT DEFAULT 60,
    question_count INT DEFAULT 0,
    randomize TINYINT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    anti_cheat_enabled TINYINT DEFAULT 0,
    max_tab_switches INT DEFAULT 3,
    status VARCHAR(20) DEFAULT 'DRAFT',
    start_time DATETIME,
    end_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exams';

-- ----------------------------
-- 11. exam_question
-- ----------------------------
CREATE TABLE exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    score_override INT,
    INDEX idx_exam (exam_id),
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam-question mapping';

-- ----------------------------
-- 12. answer_sheet
-- ----------------------------
CREATE TABLE answer_sheet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_no INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    start_time DATETIME,
    submit_time DATETIME,
    remaining_seconds INT,
    tab_switch_count INT DEFAULT 0,
    score DECIMAL(5,1),
    pass TINYINT,
    grading_completed_at DATETIME,
    question_snapshot JSON COMMENT 'Frozen question data at exam start',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exam_student_attempt (exam_id, student_id, attempt_no),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Exam answer sheets';

-- ----------------------------
-- 13. answer_detail
-- ----------------------------
CREATE TABLE answer_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    exam_question_id BIGINT,
    student_answer TEXT,
    correct_answer TEXT COMMENT 'Frozen correct answer at exam start',
    snapshot_score INT COMMENT 'Frozen question score at exam start',
    is_correct TINYINT,
    score_earned DECIMAL(5,1),
    grading_note TEXT,
    INDEX idx_answer_sheet (answer_sheet_id),
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Answer details per question';

-- ----------------------------
-- 14. grade
-- ----------------------------
CREATE TABLE grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    course_id BIGINT,
    score DECIMAL(5,1),
    total_score INT,
    pass TINYINT,
    rank_in_class INT,
    graded_by BIGINT,
    graded_at DATETIME,
    INDEX idx_student (student_id),
    INDEX idx_exam (exam_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Grades';

-- ----------------------------
-- 15. certificate
-- ----------------------------
CREATE TABLE certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    cert_no VARCHAR(100) NOT NULL,
    title VARCHAR(200),
    issue_date DATE,
    expiry_date DATE,
    status VARCHAR(20) DEFAULT 'VALID',
    revoke_reason TEXT,
    revoked_by BIGINT,
    revoked_at DATETIME,
    verify_token VARCHAR(100),
    verify_token_expires_at DATETIME,
    file_path VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cert_no (cert_no),
    INDEX idx_student (student_id),
    INDEX idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificates';

-- ----------------------------
-- 16. certificate_revocation
-- ----------------------------
CREATE TABLE certificate_revocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    reason TEXT,
    revoked_by BIGINT NOT NULL,
    revoked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_certificate (certificate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate revocation log';

-- ----------------------------
-- Schema additions for adaptive learning & certificate renewal
-- ----------------------------
ALTER TABLE course ADD COLUMN version INT DEFAULT 1 AFTER status;
ALTER TABLE question ADD COLUMN chapter_id BIGINT AFTER course_id;
ALTER TABLE question ADD INDEX idx_chapter (chapter_id);
ALTER TABLE certificate ADD COLUMN course_version INT DEFAULT 1 AFTER course_id;

-- ----------------------------
-- 17. knowledge_mastery
-- ----------------------------
CREATE TABLE knowledge_mastery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    mastery_level VARCHAR(30) NOT NULL DEFAULT 'NOT_MASTERED' COMMENT 'NOT_MASTERED/PARTIALLY_MASTERED/MASTERED',
    learning_progress DECIMAL(5,2) DEFAULT 0,
    study_duration_seconds INT DEFAULT 0,
    expected_duration_seconds INT DEFAULT 0,
    wrong_answer_count INT DEFAULT 0,
    total_answer_count INT DEFAULT 0,
    tab_switch_count INT DEFAULT 0,
    last_evaluated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_chapter (student_id, chapter_id),
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_mastery_level (mastery_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge point mastery per student per chapter';

-- ----------------------------
-- 18. learning_path
-- ----------------------------
CREATE TABLE learning_path (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    title VARCHAR(200),
    status VARCHAR(30) DEFAULT 'GENERATED' COMMENT 'GENERATED/IN_PROGRESS/COMPLETED/EXPIRED',
    total_items INT DEFAULT 0,
    completed_items INT DEFAULT 0,
    generated_reason VARCHAR(100) COMMENT 'INITIAL/EXAM_FAILED/RENEWAL/MANUAL',
    expires_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Personalized learning path plans';

-- ----------------------------
-- 19. learning_path_item
-- ----------------------------
CREATE TABLE learning_path_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    path_id BIGINT NOT NULL,
    item_type VARCHAR(20) NOT NULL COMMENT 'REMEDIAL/MAKEUP_EXAM',
    ref_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_path (path_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Learning path items';

-- ----------------------------
-- 20. certificate_renewal_rule
-- ----------------------------
CREATE TABLE certificate_renewal_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    renewal_period_days INT NOT NULL DEFAULT 365,
    advance_notice_days INT DEFAULT 30,
    require_exam_pass TINYINT DEFAULT 1,
    min_exam_score INT,
    version_change_policy VARCHAR(30) DEFAULT 'RELEARN' COMMENT 'DIRECT_RENEW/MAKEUP_EXAM/RELEARN',
    role_exemptions VARCHAR(500) COMMENT 'JSON array of exempt roles',
    enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate renewal rules per course';

-- ----------------------------
-- 21. certificate_renewal
-- ----------------------------
CREATE TABLE certificate_renewal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    decision VARCHAR(30) NOT NULL COMMENT 'DIRECT_RENEW/MAKEUP_EXAM/RELEARN/BLOCKED',
    decision_reason TEXT,
    old_expiry_date DATE,
    new_expiry_date DATE,
    course_version_at_issue INT,
    course_version_current INT,
    latest_exam_score DECIMAL(5,1),
    operator_id BIGINT,
    status VARCHAR(30) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/COMPLETED/REJECTED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_certificate (certificate_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate renewal decision records';

-- ----------------------------
-- 22. remedial_task
-- ----------------------------
CREATE TABLE remedial_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    path_id BIGINT,
    source VARCHAR(30) NOT NULL COMMENT 'MASTERY_CHECK/EXAM_WRONG_ANSWER/RENEWAL',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_path (path_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Remedial learning tasks';

-- ----------------------------
-- 23. makeup_exam_task
-- ----------------------------
CREATE TABLE makeup_exam_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    path_id BIGINT,
    source VARCHAR(30) NOT NULL COMMENT 'EXAM_FAILED/RENEWAL',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SCHEDULED/COMPLETED/CANCELLED',
    scheduled_at DATETIME,
    answer_sheet_id BIGINT,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_path (path_id),
    INDEX idx_exam (exam_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Makeup exam tasks';

-- ----------------------------
-- 24. audit_log
-- ----------------------------
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_role VARCHAR(20),
    action_type VARCHAR(50) NOT NULL COMMENT 'PATH_GENERATED/RENEWAL_EVALUATED/TASK_CREATED/TASK_COMPLETED/CERT_RENEWED/CERT_STATUS_CHANGED',
    target_type VARCHAR(50) NOT NULL COMMENT 'LEARNING_PATH/CERTIFICATE/REMEDIAL_TASK/MAKEUP_EXAM_TASK/CERTIFICATE_RENEWAL',
    target_id BIGINT NOT NULL,
    details TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operator (operator_id),
    INDEX idx_action (action_type),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System audit log';

-- ----------------------------
-- Seed data
-- ----------------------------
INSERT INTO sys_user (username, password, real_name, email, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'admin@training.com', 'ADMIN', 'ACTIVE'),
('instructor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Instructor One', 'instructor1@training.com', 'INSTRUCTOR', 'ACTIVE'),
('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Student One', 'student1@training.com', 'STUDENT', 'ACTIVE'),
('auditor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Auditor One', 'auditor1@training.com', 'AUDITOR', 'ACTIVE');
