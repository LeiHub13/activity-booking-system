CREATE TABLE point_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '积分账户ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    balance INT NOT NULL DEFAULT 0 COMMENT '当前积分余额',
    total_earned INT NOT NULL DEFAULT 0 COMMENT '累计获得积分',
    total_spent INT NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) COMMENT='用户积分账户表';

CREATE TABLE point_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '积分流水ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '积分变动值，正数为获得，负数为消耗',
    type VARCHAR(30) NOT NULL COMMENT '类型：REGISTER/CHECK_IN/RATING/EXCHANGE',
    biz_id BIGINT DEFAULT NULL COMMENT '关联业务ID（报名ID/打卡ID/评价ID/兑换ID）',
    remark VARCHAR(255) DEFAULT NULL COMMENT '积分变动说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_type_biz (user_id, type, biz_id),
    KEY idx_user_id (user_id)
) COMMENT='积分流水表';

CREATE TABLE point_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '商品描述',
    points_required INT NOT NULL COMMENT '兑换所需积分',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量，-1表示不限量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='积分商品表';

CREATE TABLE point_exchange (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '兑换记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称快照',
    points_cost INT NOT NULL COMMENT '消耗积分快照',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '兑换时间',
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id)
) COMMENT='积分兑换记录表';

CREATE TABLE activity_rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    rating TINYINT NOT NULL COMMENT '评分 1-5',
    content VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_activity_rating (user_id, activity_id),
    KEY idx_activity_id (activity_id),
    KEY idx_user_id (user_id)
) COMMENT='活动评价表';