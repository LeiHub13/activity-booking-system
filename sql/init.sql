CREATE DATABASE IF NOT EXISTS activity_booking_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE activity_booking_system;

DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS registration;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) COMMENT='用户表';

CREATE TABLE role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    UNIQUE KEY uk_role_code (role_code)
) COMMENT='角色表';

CREATE TABLE user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT='用户角色关联表';

CREATE TABLE activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    title VARCHAR(100) NOT NULL COMMENT '活动标题',
    content TEXT COMMENT '活动内容',
    location VARCHAR(200) DEFAULT NULL COMMENT '活动地点',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    signup_start_time DATETIME NOT NULL COMMENT '报名开始时间',
    signup_end_time DATETIME NOT NULL COMMENT '报名结束时间',
    max_count INT NOT NULL COMMENT '活动人数上限',
    current_count INT NOT NULL DEFAULT 0 COMMENT '当前报名人数',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT '活动状态：DRAFT/PUBLISHED/CLOSED/FINISHED',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_activity_status (status),
    KEY idx_signup_time (signup_start_time, signup_end_time),
    KEY idx_activity_status_create_time (status, create_time)
) COMMENT='活动表';

CREATE TABLE registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报名ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '报名状态：PENDING/APPROVED/REJECTED/CANCELED',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    audit_user_id BIGINT DEFAULT NULL COMMENT '审核人ID',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_activity (user_id, activity_id),
    KEY idx_registration_activity_id (activity_id),
    KEY idx_registration_status (status),
    KEY idx_registration_user_status (user_id, status),
    KEY idx_registration_activity_status (activity_id, status),
    KEY idx_registration_create_time (create_time)
) COMMENT='报名记录表';

CREATE TABLE notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(100) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    type VARCHAR(30) NOT NULL COMMENT '通知类型',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_notice_user_id (user_id),
    KEY idx_notice_is_read (is_read)
) COMMENT='通知表';

INSERT INTO role (role_code, role_name)
VALUES ('ADMIN', '管理员'),
       ('USER', '普通用户');
