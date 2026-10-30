-- ============================================================
-- 社区团购（生鲜自提）拼团系统 数据库 schema
-- MySQL 8.0 / utf8mb4 / InnoDB
-- 每次启动由 spring.sql.init 自动执行（先删后建，重置演示数据）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 系统用户（居民 / 团长 / 平台管理员）
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    nickname     VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    phone        VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    role         VARCHAR(20)  NOT NULL COMMENT '角色: RESIDENT/HEAD/ADMIN',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 2. 商品
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(100) NOT NULL COMMENT '商品名称',
    category     VARCHAR(50)  DEFAULT '' COMMENT '分类(蔬菜/水果/肉禽/水产/蛋奶)',
    spec         VARCHAR(50)  DEFAULT '' COMMENT '规格(如 500g/份)',
    unit         VARCHAR(10)  DEFAULT '份' COMMENT '单位',
    price        DECIMAL(10,2) NOT NULL COMMENT '原价',
    group_price  DECIMAL(10,2) NOT NULL COMMENT '团购价',
    stock        INT          NOT NULL DEFAULT 0 COMMENT '平台总库存',
    image_url    VARCHAR(255) DEFAULT '' COMMENT '图片地址',
    description  VARCHAR(500) DEFAULT '' COMMENT '描述',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品表';

-- 3. 团购活动（状态机: PENDING -> ONGOING -> SUCCESS/FAILED）
DROP TABLE IF EXISTS group_activity;
CREATE TABLE group_activity (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id   BIGINT       NOT NULL COMMENT '关联商品',
    creator_id   BIGINT       DEFAULT NULL COMMENT '创建者(团长/管理员)用户ID',
    title        VARCHAR(120) NOT NULL COMMENT '活动标题',
    origin_price DECIMAL(10,2) NOT NULL COMMENT '原价',
    group_price  DECIMAL(10,2) NOT NULL COMMENT '团购价',
    total_stock  INT          NOT NULL DEFAULT 0 COMMENT '总库存(验收用1000)',
    stock        INT          NOT NULL DEFAULT 0 COMMENT '剩余库存',
    per_limit    INT          NOT NULL DEFAULT 5 COMMENT '每人限购',
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ONGOING/SUCCESS/FAILED',
    start_time   DATETIME     DEFAULT NULL COMMENT '开始时间',
    end_time     DATETIME     DEFAULT NULL COMMENT '结束时间',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_product (product_id),
    KEY idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='团购活动表';

-- 4. 参团订单
DROP TABLE IF EXISTS group_order;
CREATE TABLE group_order (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no            VARCHAR(40)  NOT NULL COMMENT '订单号',
    activity_id         BIGINT       NOT NULL COMMENT '关联活动',
    user_id             BIGINT       NOT NULL COMMENT '下单用户',
    product_id          BIGINT       DEFAULT NULL COMMENT '商品',
    pickup_point_id     BIGINT       DEFAULT NULL COMMENT '自提点ID（下单时选择）',
    quantity            INT          NOT NULL DEFAULT 1 COMMENT '数量',
    amount              DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status              VARCHAR(20)  NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/CANCELLED/PICKED',
    verification_code   VARCHAR(20)  DEFAULT NULL COMMENT '自提核销码',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    pay_time            DATETIME     DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_verification_code (verification_code),
    KEY idx_activity (activity_id),
    KEY idx_user (user_id),
    KEY idx_pickup_point (pickup_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参团订单表';

-- 5. 自提点
DROP TABLE IF EXISTS pickup_point;
CREATE TABLE pickup_point (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(100) NOT NULL COMMENT '自提点名称',
    address      VARCHAR(200) NOT NULL COMMENT '地址',
    manager      VARCHAR(50)  DEFAULT '' COMMENT '负责人',
    phone        VARCHAR(20)  DEFAULT '' COMMENT '联系电话',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自提点表';

-- 6. 核销记录（幂等: 同一核销码仅成功一次）
DROP TABLE IF EXISTS pickup_verification;
CREATE TABLE pickup_verification (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id           BIGINT       NOT NULL COMMENT '关联订单',
    verification_code  VARCHAR(20)  NOT NULL COMMENT '核销码',
    pickup_point_id    BIGINT       DEFAULT NULL COMMENT '核销自提点',
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/INVALID',
    verify_time        DATETIME     DEFAULT NULL COMMENT '核销时间',
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    UNIQUE KEY uk_code (verification_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='核销记录表';

-- 7. 支付记录（幂等回调）
DROP TABLE IF EXISTS payment;
CREATE TABLE payment (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    pay_no        VARCHAR(40)  NOT NULL COMMENT '支付流水号',
    order_id      BIGINT       NOT NULL COMMENT '关联订单',
    order_no      VARCHAR(40)  NOT NULL COMMENT '订单号',
    amount        DECIMAL(10,2) NOT NULL COMMENT '金额',
    channel       VARCHAR(20)  DEFAULT 'WECHAT' COMMENT '支付渠道',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/REFUNDED',
    callback_time DATETIME     DEFAULT NULL COMMENT '回调时间',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='支付记录表';

SET FOREIGN_KEY_CHECKS = 1;
