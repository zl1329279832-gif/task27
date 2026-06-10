CREATE DATABASE IF NOT EXISTS training_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE training_db;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    avatar VARCHAR(200),
    status TINYINT DEFAULT 1 COMMENT '1=active 0=disabled',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 3. 用户角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 4. 课程表
-- ----------------------------
CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    cover_image VARCHAR(500),
    instructor_id BIGINT NOT NULL,
    category VARCHAR(50),
    difficulty VARCHAR(20),
    total_hours INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    completion_threshold DECIMAL(5,2) DEFAULT 80.00 COMMENT 'course completion rate threshold for certificate',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ----------------------------
-- 5. 章节表
-- ----------------------------
CREATE TABLE IF NOT EXISTS chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='章节表';

-- ----------------------------
-- 6. 课件表
-- ----------------------------
CREATE TABLE IF NOT EXISTS courseware (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    file_type VARCHAR(20) NOT NULL COMMENT 'VIDEO/PDF/DOC/PPT/LINK',
    file_url VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    duration INT DEFAULT 0 COMMENT 'duration in seconds for video',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_chapter_id (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课件表';

-- ----------------------------
-- 7. 班级表
-- ----------------------------
CREATE TABLE IF NOT EXISTS clazz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    course_id BIGINT NOT NULL,
    instructor_id BIGINT,
    description TEXT,
    start_date DATE,
    end_date DATE,
    max_students INT DEFAULT 50,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- ----------------------------
-- 8. 班级学员关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS clazz_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_clazz_student (clazz_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级学员关联表';

-- ----------------------------
-- 9. 学习记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS learning_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    courseware_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'NOT_STARTED',
    progress INT DEFAULT 0 COMMENT 'percentage 0-100',
    last_position INT DEFAULT 0 COMMENT 'seconds for video, page for doc',
    study_duration INT DEFAULT 0 COMMENT 'total study seconds',
    started_at DATETIME,
    completed_at DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_courseware (student_id, courseware_id),
    INDEX idx_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- ----------------------------
-- 10. 题目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL COMMENT 'SINGLE_CHOICE/MULTI_CHOICE/TRUE_FALSE/FILL_BLANK',
    options JSON COMMENT 'array of option objects for choice questions',
    correct_answer VARCHAR(500) NOT NULL,
    analysis TEXT COMMENT 'answer analysis',
    score DECIMAL(5,2) DEFAULT 10.00,
    difficulty VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    deleted_at DATETIME NULL COMMENT 'soft delete',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_id (course_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';

-- ----------------------------
-- 11. 考试表
-- ----------------------------
CREATE TABLE IF NOT EXISTS exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    course_id BIGINT NOT NULL,
    duration_minutes INT NOT NULL COMMENT 'exam duration in minutes',
    total_score DECIMAL(5,2) DEFAULT 100.00,
    pass_score DECIMAL(5,2) NOT NULL,
    question_count INT NOT NULL COMMENT 'number of questions to randomly select',
    max_attempts INT DEFAULT 1 COMMENT 'max retake attempts',
    status VARCHAR(20) DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试表';

-- ----------------------------
-- 12. 考试题目关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_snapshot JSON NOT NULL COMMENT 'frozen question snapshot at exam publish time',
    sort_order INT DEFAULT 0,
    score DECIMAL(5,2) COMMENT 'score override for this exam',
    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试题目关联表';

-- ----------------------------
-- 13. 考试提交表
-- ----------------------------
CREATE TABLE IF NOT EXISTS exam_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    submit_time DATETIME,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/SUBMITTED/TIMED_OUT/AUTO_SUBMITTED',
    total_score DECIMAL(5,2),
    tab_switch_count INT DEFAULT 0,
    attempt_number INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exam_student (exam_id, student_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试提交表';

-- ----------------------------
-- 14. 提交答案表
-- ----------------------------
CREATE TABLE IF NOT EXISTS submission_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_snapshot JSON NOT NULL COMMENT 'complete question snapshot for historical review',
    student_answer TEXT,
    is_correct TINYINT,
    awarded_score DECIMAL(5,2) DEFAULT 0,
    answered_at DATETIME,
    INDEX idx_submission_id (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提交答案表';

-- ----------------------------
-- 15. 成绩表
-- ----------------------------
CREATE TABLE IF NOT EXISTS score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    total_score DECIMAL(5,2) NOT NULL,
    pass_score DECIMAL(5,2) NOT NULL,
    is_passed TINYINT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_id (student_id),
    INDEX idx_exam_id (exam_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表';

-- ----------------------------
-- 16. 证书表
-- ----------------------------
CREATE TABLE IF NOT EXISTS certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cert_no VARCHAR(50) NOT NULL UNIQUE,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    exam_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    issued_at DATETIME NOT NULL,
    expired_at DATETIME,
    verify_token VARCHAR(500),
    verify_token_expires_at DATETIME,
    status VARCHAR(20) DEFAULT 'VALID' COMMENT 'VALID/REVOKED',
    file_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_course (student_id, course_id),
    INDEX idx_cert_no (cert_no),
    INDEX idx_verify_token (verify_token(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书表';

-- ----------------------------
-- 17. 证书撤销表
-- ----------------------------
CREATE TABLE IF NOT EXISTS certificate_revocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    revoked_by BIGINT NOT NULL,
    revoked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_certificate_id (certificate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证书撤销表';

-- ----------------------------
-- 初始数据
-- ----------------------------

-- Initial roles
INSERT INTO sys_role (role_name, role_code, description) VALUES
('管理员', 'ADMIN', '系统管理员，拥有全部权限'),
('讲师', 'INSTRUCTOR', '课程讲师，管理课程和考试'),
('学员', 'STUDENT', '学员，参与学习和考试'),
('审核员', 'AUDITOR', '审核员，审核证书和成绩');

-- Admin user (password: admin123, BCrypt encoded)
INSERT INTO sys_user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@training.com', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
