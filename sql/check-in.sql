CREATE TABLE check_in (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '打卡ID',
                          user_id BIGINT NOT NULL COMMENT '用户ID',
                          activity_id BIGINT NOT NULL COMMENT '活动ID',
                          registration_id BIGINT DEFAULT NULL COMMENT '报名ID',
                          image_url VARCHAR(500) NOT NULL COMMENT '打卡图片地址',
                          object_name VARCHAR(255) DEFAULT NULL COMMENT 'MinIO对象名',
                          check_in_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
                          create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          UNIQUE KEY uk_user_activity_check_in (user_id, activity_id),
                          KEY idx_activity_id (activity_id),
                          KEY idx_user_id (user_id)
) COMMENT='活动打卡表';