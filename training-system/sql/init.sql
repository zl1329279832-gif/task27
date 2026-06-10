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
-- 17. knowledge_point
-- ----------------------------
CREATE TABLE knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    chapter_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    parent_kp_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kp_course (course_id),
    INDEX idx_kp_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge points';

-- ----------------------------
-- 18. question_knowledge_point
-- ----------------------------
CREATE TABLE question_knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    UNIQUE KEY uk_question_kp (question_id, knowledge_point_id),
    INDEX idx_qkp_kp (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Question-knowledge point mapping';

-- ----------------------------
-- 19. knowledge_point_mastery
-- ----------------------------
CREATE TABLE knowledge_point_mastery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    mastery_level DECIMAL(5,2) DEFAULT 0.00,
    total_questions INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    exam_attempts INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'UNMASTERED' COMMENT 'UNMASTERED/PARTIAL/MASTERED',
    last_assessed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mastery_student_kp (student_id, knowledge_point_id),
    INDEX idx_mastery_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Student knowledge point mastery';

-- ----------------------------
-- 20. learning_path
-- ----------------------------
CREATE TABLE learning_path (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    clazz_id BIGINT,
    status VARCHAR(50) DEFAULT 'GENERATED' COMMENT 'GENERATED/IN_PROGRESS/COMPLETED/EXPIRED',
    trigger_reason VARCHAR(50) DEFAULT 'INITIAL' COMMENT 'INITIAL/EXAM_FAIL/CERT_RENEWAL/MANUAL',
    path_data JSON COMMENT 'List of PathStep',
    total_steps INT DEFAULT 0,
    completed_steps INT DEFAULT 0,
    generated_by BIGINT,
    generated_at DATETIME,
    completed_at DATETIME,
    expires_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_lp_student_course (student_id, course_id),
    INDEX idx_lp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Adaptive learning paths';

-- ----------------------------
-- 21. remedial_task
-- ----------------------------
CREATE TABLE remedial_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    learning_path_id BIGINT,
    knowledge_point_id BIGINT,
    chapter_id BIGINT,
    task_type VARCHAR(50) NOT NULL COMMENT 'REMEDIAL_CHAPTER/REVIEW_MATERIAL',
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/EXPIRED',
    required_progress DECIMAL(5,2) DEFAULT 100.00,
    achieved_progress DECIMAL(5,2) DEFAULT 0.00,
    deadline DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rt_student (student_id),
    INDEX idx_rt_path (learning_path_id),
    INDEX idx_rt_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Remedial learning tasks';

-- ----------------------------
-- 22. makeup_exam
-- ----------------------------
CREATE TABLE makeup_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    learning_path_id BIGINT,
    answer_sheet_id BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/PASSED/FAILED/EXPIRED',
    max_attempts INT DEFAULT 2,
    attempts_used INT DEFAULT 0,
    required_score DECIMAL(5,2),
    achieved_score DECIMAL(5,2),
    deadline DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_me_student (student_id),
    INDEX idx_me_path (learning_path_id),
    INDEX idx_me_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Makeup exam tasks';

-- ----------------------------
-- 23. certificate_renewal_rule
-- ----------------------------
CREATE TABLE certificate_renewal_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    role_pattern VARCHAR(100) DEFAULT '*' COMMENT 'Role wildcard pattern',
    expiry_threshold_days INT DEFAULT 90,
    min_course_version INT DEFAULT 1,
    min_recent_exam_score DECIMAL(5,2),
    recent_exam_within_months INT,
    renewal_action VARCHAR(50) NOT NULL COMMENT 'FULL_RELEARN/MAKEUP_EXAM/DIRECT_RENEWAL',
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_crr_course (course_id),
    INDEX idx_crr_enabled (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate renewal rules';

-- ----------------------------
-- 24. certificate_renewal
-- ----------------------------
CREATE TABLE certificate_renewal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    renewal_rule_id BIGINT,
    renewal_action VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/REJECTED/EXPIRED',
    new_certificate_id BIGINT,
    learning_path_id BIGINT,
    makeup_exam_id BIGINT,
    rejection_reason VARCHAR(500),
    processed_by BIGINT,
    processed_at DATETIME,
    deadline DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cr_cert (certificate_id),
    INDEX idx_cr_student (student_id),
    INDEX idx_cr_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Certificate renewal records';

-- ----------------------------
-- 25. audit_log
-- ----------------------------
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(100) NOT NULL,
    target_id BIGINT,
    actor_id BIGINT,
    actor_role VARCHAR(50),
    details JSON,
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_target (target_type, target_id),
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit log';

-- ----------------------------
-- 26. course_version_history
-- ----------------------------
CREATE TABLE course_version_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    from_version INT NOT NULL,
    to_version INT NOT NULL,
    change_summary JSON,
    changed_by BIGINT,
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cvh_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Course version history';

-- Add version column to course table
ALTER TABLE course ADD COLUMN version INT DEFAULT 1;

-- ----------------------------
-- Seed data
-- ----------------------------
INSERT INTO sys_user (username, password, real_name, email, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'admin@training.com', 'ADMIN', 'ACTIVE'),
('instructor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Instructor One', 'instructor1@training.com', 'INSTRUCTOR', 'ACTIVE'),
('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Student One', 'student1@training.com', 'STUDENT', 'ACTIVE'),
('auditor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Auditor One', 'auditor1@training.com', 'AUDITOR', 'ACTIVE');
