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
-- Seed data
-- ----------------------------
INSERT INTO sys_user (username, password, real_name, email, role, status) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'admin@training.com', 'ADMIN', 'ACTIVE'),
('instructor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Instructor One', 'instructor1@training.com', 'INSTRUCTOR', 'ACTIVE'),
('student1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Student One', 'student1@training.com', 'STUDENT', 'ACTIVE'),
('auditor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Auditor One', 'auditor1@training.com', 'AUDITOR', 'ACTIVE');
