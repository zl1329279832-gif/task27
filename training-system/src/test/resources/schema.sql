-- Schema for H2 in-memory test database (MySQL compatibility mode)

CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    cover_image VARCHAR(500),
    instructor_id BIGINT,
    category VARCHAR(100),
    difficulty VARCHAR(50),
    total_hours DECIMAL(10,2),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    title VARCHAR(255),
    sort_order INT,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courseware (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT,
    title VARCHAR(255),
    file_path VARCHAR(500),
    file_type VARCHAR(50),
    file_size BIGINT,
    duration_seconds INT,
    sort_order INT,
    status VARCHAR(50),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    title VARCHAR(255),
    description TEXT,
    duration_minutes INT,
    total_score INT,
    pass_score INT,
    question_count INT,
    randomize INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    anti_cheat_enabled INT DEFAULT 0,
    max_tab_switches INT DEFAULT 3,
    status VARCHAR(50),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT,
    question_id BIGINT,
    sort_order INT,
    score_override INT
);

CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    instructor_id BIGINT,
    content TEXT,
    question_type VARCHAR(50),
    options TEXT,
    correct_answer VARCHAR(500),
    score INT,
    difficulty VARCHAR(50),
    explanation TEXT,
    status VARCHAR(50),
    deleted_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS answer_sheet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT,
    student_id BIGINT,
    attempt_no INT,
    status VARCHAR(50),
    start_time TIMESTAMP,
    submit_time TIMESTAMP,
    remaining_seconds INT,
    tab_switch_count INT DEFAULT 0,
    score DECIMAL(10,2),
    pass INT,
    grading_completed_at TIMESTAMP,
    question_snapshot CLOB,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS answer_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT,
    question_id BIGINT,
    exam_question_id BIGINT,
    student_answer VARCHAR(1000),
    correct_answer VARCHAR(500),
    snapshot_score INT,
    is_correct INT,
    score_earned DECIMAL(10,2),
    grading_note VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    answer_sheet_id BIGINT,
    student_id BIGINT,
    exam_id BIGINT,
    course_id BIGINT,
    score DECIMAL(10,2),
    total_score INT,
    pass INT,
    rank_in_class INT,
    graded_by BIGINT,
    graded_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    cert_no VARCHAR(100),
    title VARCHAR(255),
    issue_date DATE,
    expiry_date DATE,
    status VARCHAR(50),
    revoke_reason VARCHAR(500),
    revoked_by BIGINT,
    revoked_at TIMESTAMP,
    verify_token VARCHAR(255),
    verify_token_expires_at TIMESTAMP,
    file_path VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificate_revocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT,
    reason VARCHAR(500),
    revoked_by BIGINT,
    revoked_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100),
    password VARCHAR(255),
    real_name VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50),
    avatar VARCHAR(500),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    chapter_id BIGINT,
    courseware_id BIGINT,
    progress_percent INT,
    study_duration_seconds INT,
    last_position_seconds INT,
    status VARCHAR(50),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clazz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    instructor_id BIGINT,
    status VARCHAR(50),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clazz_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT,
    course_id BIGINT
);

CREATE TABLE IF NOT EXISTS clazz_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clazz_id BIGINT,
    student_id BIGINT
);

CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    chapter_id BIGINT,
    name VARCHAR(255),
    description TEXT,
    sort_order INT DEFAULT 0,
    parent_kp_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT,
    knowledge_point_id BIGINT
);

CREATE TABLE IF NOT EXISTS knowledge_point_mastery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    knowledge_point_id BIGINT,
    course_id BIGINT,
    mastery_level DECIMAL(5,2) DEFAULT 0,
    total_questions INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    exam_attempts INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'UNMASTERED',
    last_assessed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS learning_path (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    clazz_id BIGINT,
    status VARCHAR(50) DEFAULT 'GENERATED',
    trigger_reason VARCHAR(50) DEFAULT 'INITIAL',
    path_data CLOB,
    total_steps INT DEFAULT 0,
    completed_steps INT DEFAULT 0,
    generated_by BIGINT,
    generated_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS remedial_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    learning_path_id BIGINT,
    knowledge_point_id BIGINT,
    chapter_id BIGINT,
    task_type VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDING',
    required_progress DECIMAL(5,2) DEFAULT 100,
    achieved_progress DECIMAL(5,2) DEFAULT 0,
    deadline TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS makeup_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT,
    course_id BIGINT,
    exam_id BIGINT,
    learning_path_id BIGINT,
    answer_sheet_id BIGINT,
    status VARCHAR(50) DEFAULT 'PENDING',
    max_attempts INT DEFAULT 2,
    attempts_used INT DEFAULT 0,
    required_score DECIMAL(5,2),
    achieved_score DECIMAL(5,2),
    deadline TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificate_renewal_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    role_pattern VARCHAR(100) DEFAULT '*',
    expiry_threshold_days INT DEFAULT 90,
    min_course_version INT DEFAULT 1,
    min_recent_exam_score DECIMAL(5,2),
    recent_exam_within_months INT,
    renewal_action VARCHAR(50),
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled INT DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS certificate_renewal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    certificate_id BIGINT,
    student_id BIGINT,
    course_id BIGINT,
    renewal_rule_id BIGINT,
    renewal_action VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDING',
    new_certificate_id BIGINT,
    learning_path_id BIGINT,
    makeup_exam_id BIGINT,
    rejection_reason VARCHAR(500),
    processed_by BIGINT,
    processed_at TIMESTAMP,
    deadline TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100),
    target_type VARCHAR(100),
    target_id BIGINT,
    actor_id BIGINT,
    actor_role VARCHAR(50),
    details CLOB,
    ip_address VARCHAR(50),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_version_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    from_version INT,
    to_version INT,
    change_summary CLOB,
    changed_by BIGINT,
    changed_at TIMESTAMP
);

ALTER TABLE course ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
