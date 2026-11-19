-- H2 演示库表结构（MySQL 兼容模式）；生产库脚本见 sql/schema-mysql.sql
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    open_id      VARCHAR(64)  NOT NULL,
    nickname     VARCHAR(64),
    phone_mask   VARCHAR(16),
    student_no   VARCHAR(32),
    status       TINYINT      DEFAULT 0,
    credit_score INT          DEFAULT 100,
    invite_code  VARCHAR(16),
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted   TINYINT      DEFAULT 0
);
CREATE UNIQUE INDEX uk_user_open_id ON t_user (open_id);

DROP TABLE IF EXISTS t_address;
CREATE TABLE t_address (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    contact_name VARCHAR(32),
    phone_mask   VARCHAR(16),
    campus       VARCHAR(64),
    building     VARCHAR(64),
    room_mask    VARCHAR(64),
    detail       VARCHAR(255),
    lat          DECIMAL(10, 7),
    lng          DECIMAL(10, 7),
    is_default   TINYINT      DEFAULT 0,
    status       TINYINT      DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted   TINYINT      DEFAULT 0
);
CREATE INDEX idx_address_user ON t_address (user_id, is_default);

DROP TABLE IF EXISTS t_station;
CREATE TABLE t_station (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(64) NOT NULL,
    brand      TINYINT     DEFAULT 0,
    campus     VARCHAR(64),
    address    VARCHAR(255),
    lat        DECIMAL(10, 7),
    lng        DECIMAL(10, 7),
    open_time  VARCHAR(8),
    close_time VARCHAR(8),
    status     TINYINT     DEFAULT 1,
    sort       INT         DEFAULT 0,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT     DEFAULT 0
);

DROP TABLE IF EXISTS t_courier;
CREATE TABLE t_courier (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    real_name       VARCHAR(32),
    student_no      VARCHAR(32),
    id_card_mask    VARCHAR(32),
    student_card_url VARCHAR(255),
    audit_status    TINYINT      DEFAULT 1,
    audit_remark    VARCHAR(255),
    work_status     TINYINT      DEFAULT 1,
    service_start   VARCHAR(8),
    service_end     VARCHAR(8),
    max_weight_gram INT          DEFAULT 5000,
    station_ids     VARCHAR(128),
    level           TINYINT      DEFAULT 1,
    score           DECIMAL(3, 2) DEFAULT 5.00,
    finished_cnt    INT          DEFAULT 0,
    violation_cnt   INT          DEFAULT 0,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT      DEFAULT 0
);
CREATE UNIQUE INDEX uk_courier_user ON t_courier (user_id);

DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no            VARCHAR(32)    NOT NULL,
    parent_order_no     VARCHAR(32),
    user_id             BIGINT         NOT NULL,
    courier_id          BIGINT,
    station_id          BIGINT         NOT NULL,
    order_type          TINYINT        DEFAULT 1,
    status              TINYINT        DEFAULT 10,
    weight_gram         INT            DEFAULT 0,
    actual_weight_gram  INT,
    pickup_code         VARCHAR(128),
    pickup_code_mask    VARCHAR(16),
    pickup_code_verified TINYINT       DEFAULT 0,
    expect_start        TIMESTAMP,
    expect_end          TIMESTAMP,
    address_id          BIGINT,
    address_snapshot    TEXT,
    distance_m          INT            DEFAULT 0,
    base_fee            DECIMAL(10, 2) DEFAULT 0.00,
    weight_fee          DECIMAL(10, 2) DEFAULT 0.00,
    distance_fee        DECIMAL(10, 2) DEFAULT 0.00,
    peak_fee            DECIMAL(10, 2) DEFAULT 0.00,
    type_extra_fee      DECIMAL(10, 2) DEFAULT 0.00,
    insure_fee          DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount     DECIMAL(10, 2) DEFAULT 0.00,
    total_fee           DECIMAL(10, 2) DEFAULT 0.00,
    pay_fee             DECIMAL(10, 2) DEFAULT 0.00,
    price_snapshot      TEXT,
    pay_type            TINYINT,
    pay_no              VARCHAR(64),
    pay_time            TIMESTAMP,
    accept_time         TIMESTAMP,
    pick_time           TIMESTAMP,
    deliver_time        TIMESTAMP,
    finish_time         TIMESTAMP,
    cancel_role         TINYINT,
    cancel_reason       VARCHAR(255),
    insure_flag         TINYINT        DEFAULT 0,
    declared_value      DECIMAL(10, 2),
    photo_pick_url      VARCHAR(255),
    photo_deliver_url   VARCHAR(255),
    timeout_flag        TINYINT        DEFAULT 0,
    urgent              TINYINT        DEFAULT 0,
    remark              VARCHAR(255),
    version             INT            DEFAULT 0,
    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    is_deleted          TINYINT        DEFAULT 0
);
CREATE UNIQUE INDEX uk_order_no ON t_order (order_no);
CREATE INDEX idx_order_user ON t_order (user_id, status, created_at);
CREATE INDEX idx_order_courier ON t_order (courier_id, status);
CREATE INDEX idx_order_station ON t_order (station_id, status, created_at);

DROP TABLE IF EXISTS t_order_status_log;
CREATE TABLE t_order_status_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(32) NOT NULL,
    from_status   TINYINT,
    to_status     TINYINT,
    event         VARCHAR(32),
    operator_id   BIGINT,
    operator_role VARCHAR(16),
    remark        VARCHAR(255),
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_log_order ON t_order_status_log (order_no, created_at);

DROP TABLE IF EXISTS t_wallet;
CREATE TABLE t_wallet (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    balance       DECIMAL(12, 2) DEFAULT 0.00,
    frozen_amount DECIMAL(12, 2) DEFAULT 0.00,
    total_income  DECIMAL(12, 2) DEFAULT 0.00,
    total_expense DECIMAL(12, 2) DEFAULT 0.00,
    version       INT            DEFAULT 0,
    status        TINYINT        DEFAULT 0,
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT        DEFAULT 0
);
CREATE UNIQUE INDEX uk_wallet_user ON t_wallet (user_id);

DROP TABLE IF EXISTS t_wallet_flow;
CREATE TABLE t_wallet_flow (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    flow_no       VARCHAR(32)    NOT NULL,
    wallet_id     BIGINT         NOT NULL,
    user_id       BIGINT         NOT NULL,
    biz_type      TINYINT,
    direction     TINYINT,
    amount        DECIMAL(12, 2),
    balance_after DECIMAL(12, 2),
    biz_no        VARCHAR(32),
    status        TINYINT        DEFAULT 2,
    remark        VARCHAR(128),
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_flow_no ON t_wallet_flow (flow_no);
CREATE INDEX idx_flow_user ON t_wallet_flow (user_id, created_at);

DROP TABLE IF EXISTS t_withdraw;
CREATE TABLE t_withdraw (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    withdraw_no   VARCHAR(32)    NOT NULL,
    user_id       BIGINT         NOT NULL,
    amount        DECIMAL(12, 2),
    fee           DECIMAL(10, 2) DEFAULT 0.00,
    actual_amount DECIMAL(12, 2),
    channel       TINYINT        DEFAULT 1,
    account_mask  VARCHAR(64),
    status        TINYINT        DEFAULT 1,
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT        DEFAULT 0
);
CREATE UNIQUE INDEX uk_withdraw_no ON t_withdraw (withdraw_no);

DROP TABLE IF EXISTS t_price_rule;
CREATE TABLE t_price_rule (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_no          VARCHAR(32)    NOT NULL,
    station_id       BIGINT,
    order_type       TINYINT        NOT NULL,
    base_fee         DECIMAL(10, 2) DEFAULT 3.00,
    free_weight_gram INT            DEFAULT 3000,
    weight_step_gram INT            DEFAULT 1000,
    weight_step_fee  DECIMAL(10, 2) DEFAULT 1.00,
    free_distance_m  INT            DEFAULT 500,
    distance_step_m  INT            DEFAULT 500,
    distance_step_fee DECIMAL(10, 2) DEFAULT 0.80,
    peak_rate        DECIMAL(5, 2)  DEFAULT 20.00,
    peak_windows     VARCHAR(128)   DEFAULT '11:30-13:00,17:00-18:30',
    type_extra_fee   DECIMAL(10, 2) DEFAULT 0.00,
    insure_rate      DECIMAL(5, 4)  DEFAULT 0.0080,
    insure_min_fee   DECIMAL(10, 2) DEFAULT 0.50,
    min_fee          DECIMAL(10, 2) DEFAULT 2.00,
    max_fee          DECIMAL(10, 2) DEFAULT 50.00,
    effective_from   DATE,
    effective_to     DATE,
    status           TINYINT        DEFAULT 1,
    version          INT            DEFAULT 1,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT        DEFAULT 0
);
CREATE INDEX idx_rule_match ON t_price_rule (station_id, order_type, status);

DROP TABLE IF EXISTS t_evaluation;
CREATE TABLE t_evaluation (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no    VARCHAR(32)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    courier_id  BIGINT,
    score       TINYINT      NOT NULL,
    tags        VARCHAR(128),
    content     VARCHAR(500),
    anonymous   TINYINT      DEFAULT 0,
    reply       VARCHAR(255),
    status      TINYINT      DEFAULT 1,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      DEFAULT 0
);
CREATE UNIQUE INDEX uk_eval_order ON t_evaluation (order_no);
CREATE INDEX idx_eval_courier ON t_evaluation (courier_id, score);

DROP TABLE IF EXISTS t_message;
CREATE TABLE t_message (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    msg_type    TINYINT      DEFAULT 1,
    title       VARCHAR(64),
    content     VARCHAR(500),
    biz_no      VARCHAR(32),
    is_read     TINYINT      DEFAULT 0,
    read_time   TIMESTAMP,
    channel     TINYINT      DEFAULT 1,
    send_status TINYINT      DEFAULT 2,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT      DEFAULT 0
);
CREATE INDEX idx_message_user ON t_message (user_id, is_read, created_at);

DROP TABLE IF EXISTS t_admin;
CREATE TABLE t_admin (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    real_name     VARCHAR(32),
    role_code     VARCHAR(32) DEFAULT 'operator',
    status        TINYINT     DEFAULT 1,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT     DEFAULT 0
);
CREATE UNIQUE INDEX uk_admin_username ON t_admin (username);

DROP TABLE IF EXISTS t_complaint;
CREATE TABLE t_complaint (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_no   VARCHAR(32) NOT NULL,
    order_no       VARCHAR(32),
    user_id        BIGINT,
    courier_id     BIGINT,
    type           TINYINT,
    description    VARCHAR(500),
    images         VARCHAR(500),
    demand         VARCHAR(255),
    status         TINYINT     DEFAULT 0,
    handler_id     BIGINT,
    result         VARCHAR(500),
    compensation   DECIMAL(10, 2),
    responsibility TINYINT,
    deadline       TIMESTAMP,
    close_time     TIMESTAMP,
    created_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    is_deleted     TINYINT     DEFAULT 0
);
CREATE INDEX idx_complaint_status ON t_complaint (status, deadline);
