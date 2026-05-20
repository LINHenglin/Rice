-- MySQL 建表参考（默认使用 JPA ddl-auto=update 自动建表；需要手工维护库时可执行本脚本）
-- 数据库: CREATE DATABASE rice_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512) NULL,
    role VARCHAR(32) NULL,
    disabled BIT(1) NOT NULL,
    create_time DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS detection_record (
    detection_record_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    rice_variety VARCHAR(128) NULL,
    symptom_desc TEXT NULL,
    image_url VARCHAR(1024) NULL,
    analysis_result LONGTEXT NULL,
    memory_id VARCHAR(64) NULL,
    diagnosis VARCHAR(512) NULL,
    create_time DATETIME(6) NOT NULL,
    PRIMARY KEY (detection_record_id),
    KEY idx_detection_user_time (user_id, create_time),
    KEY idx_detection_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_session (
    chat_recode_id BIGINT NOT NULL AUTO_INCREMENT,
    memory_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    first_question TEXT NULL,
    create_time DATETIME(6) NOT NULL,
    expiration_time DATETIME(6) NULL,
    PRIMARY KEY (chat_recode_id),
    UNIQUE KEY uk_chat_memory (memory_id),
    KEY idx_chat_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    memory_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_msg_memory_time (memory_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
