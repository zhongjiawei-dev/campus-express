-- ============================================================
-- 校园快递代取系统 · MySQL 8.0 生产库初始化脚本
-- 使用方式：mysql -uroot -p < sql/schema-mysql.sql
-- 说明：前 13 张为核心业务表，与后端实体一一对应；
--       后 7 张暂时没有对应代码，后续扩展时启用。
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_express
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE campus_express;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  open_id       VARCHAR(64)  NOT NULL COMMENT '第三方登录标识（演示环境为固定 code）',
  nickname      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  phone_mask    VARCHAR(16)  DEFAULT NULL COMMENT '手机号掩码，明文不落库',
  student_no    VARCHAR(32)  DEFAULT NULL COMMENT '学号',
  status        TINYINT      DEFAULT 0 COMMENT '0 正常 1 禁用',
  credit_score  INT          DEFAULT 100 COMMENT '信用分',
  invite_code   VARCHAR(16)  DEFAULT NULL COMMENT '本人邀请码',
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_open_id (open_id),
  KEY idx_user_phone (phone_mask)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下单用户';

-- ------------------------------------------------------------
-- 2. 收货地址表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_address;
CREATE TABLE t_address (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL COMMENT '所属用户',
  contact_name  VARCHAR(32)  DEFAULT NULL COMMENT '联系人',
  phone_mask    VARCHAR(16)  DEFAULT NULL COMMENT '手机号掩码',
  campus        VARCHAR(64)  DEFAULT NULL COMMENT '校区',
  building      VARCHAR(64)  DEFAULT NULL COMMENT '楼栋',
  room_mask     VARCHAR(64)  DEFAULT NULL COMMENT '房间号掩码',
  detail        VARCHAR(255) DEFAULT NULL COMMENT '详细地址（加密后写入）',
  lat           DECIMAL(10,7) DEFAULT NULL COMMENT '纬度',
  lng           DECIMAL(10,7) DEFAULT NULL COMMENT '经度',
  is_default    TINYINT      DEFAULT 0 COMMENT '1 默认地址',
  status        TINYINT      DEFAULT 0 COMMENT '0 正常 1 停用',
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_address_user (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址';

-- ------------------------------------------------------------
-- 3. 快递站点表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_station;
CREATE TABLE t_station (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL COMMENT '站点名称',
  brand       TINYINT      DEFAULT 0 COMMENT '0 通用 1 菜鸟 2 京东 3 顺丰',
  campus      VARCHAR(64)  DEFAULT NULL,
  address     VARCHAR(255) DEFAULT NULL,
  lat         DECIMAL(10,7) DEFAULT NULL,
  lng         DECIMAL(10,7) DEFAULT NULL,
  open_time   VARCHAR(8)   DEFAULT NULL COMMENT '开始营业时间',
  close_time  VARCHAR(8)   DEFAULT NULL COMMENT '结束营业时间',
  status      TINYINT      DEFAULT 1 COMMENT '1 营业',
  sort        INT          DEFAULT 0,
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted  TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_station_campus (campus, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快递站点';

-- ------------------------------------------------------------
-- 4. 代取员信息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_courier;
CREATE TABLE t_courier (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  user_id          BIGINT       NOT NULL COMMENT '关联用户',
  real_name        VARCHAR(32)  DEFAULT NULL COMMENT '真实姓名',
  student_no       VARCHAR(32)  DEFAULT NULL,
  id_card_mask     VARCHAR(32)  DEFAULT NULL COMMENT '身份证掩码',
  student_card_url VARCHAR(255) DEFAULT NULL COMMENT '学生证照片',
  audit_status     TINYINT      DEFAULT 1 COMMENT '1 待审 2 通过 3 驳回',
  audit_remark     VARCHAR(255) DEFAULT NULL,
  work_status      TINYINT      DEFAULT 1 COMMENT '1 接单中 0 休息',
  service_start    VARCHAR(8)   DEFAULT NULL,
  service_end      VARCHAR(8)   DEFAULT NULL,
  max_weight_gram  INT          DEFAULT 5000 COMMENT '可接最大重量(g)',
  station_ids      VARCHAR(128) DEFAULT NULL COMMENT '服务站点 ID 列表',
  level            TINYINT      DEFAULT 1,
  score            DECIMAL(3,2) DEFAULT 5.00 COMMENT '服务评分',
  finished_cnt     INT          DEFAULT 0,
  violation_cnt    INT          DEFAULT 0,
  created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted       TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_courier_user (user_id),
  KEY idx_courier_audit (audit_status, work_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代取员档案';

-- ------------------------------------------------------------
-- 5. 订单主表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
  id                   BIGINT        NOT NULL AUTO_INCREMENT,
  order_no             VARCHAR(32)   NOT NULL COMMENT '业务订单号',
  parent_order_no      VARCHAR(32)   DEFAULT NULL COMMENT '拼单主单号',
  user_id              BIGINT        NOT NULL,
  courier_id           BIGINT        DEFAULT NULL COMMENT '代取员 ID',
  station_id           BIGINT        NOT NULL,
  order_type           TINYINT       DEFAULT 1 COMMENT '1 普通 2 大件 3 生鲜',
  status               TINYINT       DEFAULT 10 COMMENT '10/20/30/40/50/60/70/80/90/100',
  weight_gram          INT           DEFAULT 0,
  actual_weight_gram   INT           DEFAULT NULL,
  pickup_code          VARCHAR(128)  DEFAULT NULL COMMENT '取件码密文 AES-GCM',
  pickup_code_mask    VARCHAR(16)   DEFAULT NULL COMMENT '取件码掩码',
  pickup_code_verified TINYINT       DEFAULT 0,
  expect_start         DATETIME      DEFAULT NULL,
  expect_end           DATETIME      DEFAULT NULL,
  address_id           BIGINT        DEFAULT NULL,
  address_snapshot     TEXT          COMMENT '下单时地址快照密文',
  distance_m           INT           DEFAULT 0,
  base_fee             DECIMAL(10,2) DEFAULT 0.00,
  weight_fee           DECIMAL(10,2) DEFAULT 0.00,
  distance_fee         DECIMAL(10,2) DEFAULT 0.00,
  peak_fee             DECIMAL(10,2) DEFAULT 0.00,
  type_extra_fee       DECIMAL(10,2) DEFAULT 0.00,
  insure_fee           DECIMAL(10,2) DEFAULT 0.00,
  discount_amount      DECIMAL(10,2) DEFAULT 0.00,
  total_fee            DECIMAL(10,2) DEFAULT 0.00 COMMENT '用户应付',
  pay_fee              DECIMAL(10,2) DEFAULT 0.00 COMMENT '实付',
  price_snapshot       TEXT          COMMENT '计价快照 JSON',
  pay_type             TINYINT       DEFAULT NULL,
  pay_no               VARCHAR(64)   DEFAULT NULL COMMENT '支付流水号',
  pay_time             DATETIME      DEFAULT NULL,
  accept_time          DATETIME      DEFAULT NULL,
  pick_time            DATETIME      DEFAULT NULL,
  deliver_time         DATETIME      DEFAULT NULL,
  finish_time          DATETIME      DEFAULT NULL,
  cancel_role          TINYINT       DEFAULT NULL COMMENT '1 用户 2 代取员 3 平台',
  cancel_reason        VARCHAR(255)  DEFAULT NULL,
  insure_flag          TINYINT       DEFAULT 0,
  declared_value       DECIMAL(10,2) DEFAULT NULL,
  photo_pick_url       VARCHAR(255)  DEFAULT NULL,
  photo_deliver_url    VARCHAR(255)  DEFAULT NULL,
  timeout_flag         TINYINT       DEFAULT 0,
  urgent               TINYINT       DEFAULT 0 COMMENT '加急单',
  remark               VARCHAR(255)  DEFAULT NULL,
  version              INT           DEFAULT 0 COMMENT '乐观锁版本',
  created_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted           TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user (user_id, status, created_at),
  KEY idx_order_courier (courier_id, status),
  KEY idx_order_station (station_id, status, created_at),
  KEY idx_order_parent (parent_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ------------------------------------------------------------
-- 6. 订单状态流水表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_order_status_log;
CREATE TABLE t_order_status_log (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  order_no      VARCHAR(32)  NOT NULL,
  from_status   TINYINT      DEFAULT NULL,
  to_status     TINYINT      DEFAULT NULL,
  event         VARCHAR(32)  DEFAULT NULL COMMENT 'PAY/GRAB/PICK/DELIVER/CONFIRM/CANCEL',
  operator_id   BIGINT       DEFAULT NULL,
  operator_role VARCHAR(16)  DEFAULT NULL COMMENT 'USER/COURIER/ADMIN/SYSTEM',
  remark        VARCHAR(255) DEFAULT NULL,
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_log_order (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流水';

-- ------------------------------------------------------------
-- 7. 钱包表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_wallet;
CREATE TABLE t_wallet (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  user_id       BIGINT        NOT NULL,
  balance       DECIMAL(12,2) DEFAULT 0.00,
  frozen_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '提现冻结',
  total_income  DECIMAL(12,2) DEFAULT 0.00,
  total_expense DECIMAL(12,2) DEFAULT 0.00,
  version       INT           DEFAULT 0 COMMENT '乐观锁版本',
  status        TINYINT       DEFAULT 0,
  created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包';

-- ------------------------------------------------------------
-- 8. 钱包流水表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_wallet_flow;
CREATE TABLE t_wallet_flow (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  flow_no       VARCHAR(32)   NOT NULL,
  wallet_id     BIGINT        NOT NULL,
  user_id       BIGINT        NOT NULL,
  biz_type      TINYINT       DEFAULT NULL COMMENT '1 充值 2 支付 3 结算 4 提现 5 退款 6 冻结 7 赔付',
  direction     TINYINT       DEFAULT NULL COMMENT '1 入账 2 出账',
  amount        DECIMAL(12,2) DEFAULT NULL,
  balance_after DECIMAL(12,2) DEFAULT NULL,
  biz_no        VARCHAR(32)   DEFAULT NULL COMMENT '业务单号',
  status        TINYINT       DEFAULT 2,
  remark        VARCHAR(128)  DEFAULT NULL,
  created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_no (flow_no),
  KEY idx_flow_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水';

-- ------------------------------------------------------------
-- 9. 提现申请表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_withdraw;
CREATE TABLE t_withdraw (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  withdraw_no   VARCHAR(32)   NOT NULL,
  user_id       BIGINT        NOT NULL,
  amount        DECIMAL(12,2) DEFAULT NULL,
  fee           DECIMAL(10,2) DEFAULT 0.00,
  actual_amount DECIMAL(12,2) DEFAULT NULL,
  channel       TINYINT       DEFAULT 1 COMMENT '1 微信零钱',
  account_mask  VARCHAR(64)   DEFAULT NULL,
  status        TINYINT       DEFAULT 1 COMMENT '1 审核中 2 已打款 3 已驳回',
  created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdraw_no (withdraw_no),
  KEY idx_withdraw_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请';

-- ------------------------------------------------------------
-- 10. 计价规则表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_price_rule;
CREATE TABLE t_price_rule (
  id                BIGINT        NOT NULL AUTO_INCREMENT,
  rule_no           VARCHAR(32)   NOT NULL,
  station_id        BIGINT        DEFAULT NULL COMMENT '空表示全局规则',
  order_type        TINYINT       NOT NULL,
  base_fee          DECIMAL(10,2) DEFAULT 3.00,
  free_weight_gram  INT           DEFAULT 3000,
  weight_step_gram  INT           DEFAULT 1000,
  weight_step_fee   DECIMAL(10,2) DEFAULT 1.00,
  free_distance_m   INT           DEFAULT 500,
  distance_step_m   INT           DEFAULT 500,
  distance_step_fee DECIMAL(10,2) DEFAULT 0.80,
  peak_rate         DECIMAL(5,2)  DEFAULT 20.00 COMMENT '高峰基础费溢价百分比',
  peak_windows      VARCHAR(128)  DEFAULT '11:30-13:00,17:00-18:30',
  type_extra_fee    DECIMAL(10,2) DEFAULT 0.00,
  insure_rate       DECIMAL(5,4)  DEFAULT 0.0080,
  insure_min_fee    DECIMAL(10,2) DEFAULT 0.50,
  min_fee           DECIMAL(10,2) DEFAULT 2.00,
  max_fee           DECIMAL(10,2) DEFAULT 50.00,
  effective_from    DATE          DEFAULT NULL,
  effective_to      DATE          DEFAULT NULL,
  status            TINYINT       DEFAULT 1,
  version           INT           DEFAULT 1,
  created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted        TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rule_no (rule_no),
  KEY idx_rule_match (station_id, order_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计价规则';

-- ------------------------------------------------------------
-- 11. 站内消息表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_message;
CREATE TABLE t_message (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  msg_type    TINYINT      DEFAULT 1 COMMENT '1 订单 2 系统 3 钱包 4 审核',
  title       VARCHAR(64)  DEFAULT NULL,
  content     VARCHAR(500) DEFAULT NULL,
  biz_no      VARCHAR(32)  DEFAULT NULL,
  is_read     TINYINT      DEFAULT 0,
  read_time   DATETIME     DEFAULT NULL,
  channel     TINYINT      DEFAULT 1 COMMENT '1 站内 2 微信订阅消息',
  send_status TINYINT      DEFAULT 2 COMMENT '1 待发 2 已发 3 失败',
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  is_deleted  TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_message_user (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息';

-- ------------------------------------------------------------
-- 12. 管理员表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_admin;
CREATE TABLE t_admin (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
  real_name     VARCHAR(32)  DEFAULT NULL,
  role_code     VARCHAR(32)  DEFAULT 'operator' COMMENT 'super/admin/operator',
  status        TINYINT      DEFAULT 1 COMMENT '1 启用 0 停用',
  created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted    TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台管理员';

-- ------------------------------------------------------------
-- 13. 投诉仲裁表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS t_complaint;
CREATE TABLE t_complaint (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  complaint_no   VARCHAR(32)   NOT NULL,
  order_no       VARCHAR(32)   DEFAULT NULL,
  user_id        BIGINT        DEFAULT NULL,
  courier_id     BIGINT        DEFAULT NULL,
  type           TINYINT       DEFAULT NULL COMMENT '1 丢件 2 破损 3 超时 4 服务态度 5 其他',
  description    VARCHAR(500)  DEFAULT NULL,
  images         VARCHAR(500)  DEFAULT NULL COMMENT '凭证图片，逗号分隔',
  demand         VARCHAR(255)  DEFAULT NULL COMMENT '用户诉求',
  status         TINYINT       DEFAULT 0 COMMENT '0 待处理 1 处理中 2 已完结',
  handler_id     BIGINT        DEFAULT NULL,
  result         VARCHAR(500)  DEFAULT NULL COMMENT '仲裁结论',
  compensation   DECIMAL(10,2) DEFAULT NULL COMMENT '赔付金额',
  responsibility TINYINT       DEFAULT NULL COMMENT '1 平台 2 代取员 3 用户',
  deadline       DATETIME      DEFAULT NULL COMMENT '仲裁时限',
  close_time     DATETIME      DEFAULT NULL,
  created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted     TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_complaint_no (complaint_no),
  KEY idx_complaint_status (status, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉与仲裁';

-- ============================================================
-- 以下表暂时没有对应代码，后续扩展时启用
-- ============================================================

-- 14. 订单评价表
DROP TABLE IF EXISTS t_evaluation;
CREATE TABLE t_evaluation (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  order_no     VARCHAR(32)  NOT NULL,
  user_id      BIGINT       NOT NULL,
  courier_id   BIGINT       DEFAULT NULL,
  score        TINYINT      NOT NULL COMMENT '1-5 星',
  tags         VARCHAR(128) DEFAULT NULL COMMENT '标签，逗号分隔',
  content      VARCHAR(500) DEFAULT NULL COMMENT '评价内容（敏感词过滤后）',
  anonymous    TINYINT      DEFAULT 0,
  reply        VARCHAR(255) DEFAULT NULL COMMENT '代取员回复',
  status       TINYINT      DEFAULT 1 COMMENT '1 展示 2 隐藏',
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_eval_order (order_no),
  KEY idx_eval_courier (courier_id, score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价';

-- 15. 系统公告表
DROP TABLE IF EXISTS t_notice;
CREATE TABLE t_notice (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(64)  NOT NULL,
  content      VARCHAR(1000) DEFAULT NULL,
  notice_type  TINYINT      DEFAULT 1 COMMENT '1 运营 2 规则 3 停机维护',
  target_role  VARCHAR(16)  DEFAULT 'ALL' COMMENT 'ALL/USER/COURIER',
  top_flag     TINYINT      DEFAULT 0,
  status       TINYINT      DEFAULT 1 COMMENT '1 已发布 0 草稿',
  publish_time DATETIME     DEFAULT NULL,
  admin_id     BIGINT       DEFAULT NULL,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  is_deleted   TINYINT      DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_notice_status (status, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- 16. 字典表
DROP TABLE IF EXISTS t_dict;
CREATE TABLE t_dict (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  dict_type   VARCHAR(64)  NOT NULL COMMENT '字典类型，如 order_type',
  dict_code   VARCHAR(64)  NOT NULL COMMENT '字典编码',
  dict_label  VARCHAR(64)  NOT NULL COMMENT '展示文案',
  sort        INT          DEFAULT 0,
  status      TINYINT      DEFAULT 1,
  remark      VARCHAR(255) DEFAULT NULL,
  created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dict_type_code (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典';

-- 17. 邀请奖励记录表
DROP TABLE IF EXISTS t_invite_record;
CREATE TABLE t_invite_record (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  inviter_id   BIGINT        NOT NULL COMMENT '邀请人',
  invitee_id   BIGINT        NOT NULL COMMENT '被邀请人',
  invite_code  VARCHAR(16)   NOT NULL,
  reward_amount DECIMAL(10,2) DEFAULT 0.00,
  reward_status TINYINT      DEFAULT 0 COMMENT '0 待发放 1 已发放 2 已失效',
  first_order_no VARCHAR(32) DEFAULT NULL,
  created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_pair (inviter_id, invitee_id),
  KEY idx_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请奖励';

-- 18. 同路线拼单表
DROP TABLE IF EXISTS t_group_order;
CREATE TABLE t_group_order (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  group_no       VARCHAR(32)   NOT NULL COMMENT '拼单组号',
  station_id     BIGINT        NOT NULL,
  building       VARCHAR(64)   DEFAULT NULL COMMENT '目标楼栋',
  leader_order_no VARCHAR(32)  DEFAULT NULL COMMENT '发起人订单',
  member_count   INT           DEFAULT 1,
  discount_rate  DECIMAL(5,2)  DEFAULT 10.00 COMMENT '拼单折扣百分比',
  deadline       DATETIME      DEFAULT NULL COMMENT '成团截止',
  status         TINYINT       DEFAULT 0 COMMENT '0 拼单中 1 已成团 2 已解散',
  created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_group_no (group_no),
  KEY idx_group_match (station_id, building, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='同路线拼单';

-- 19. 保价理赔表
DROP TABLE IF EXISTS t_insurance_claim;
CREATE TABLE t_insurance_claim (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  claim_no      VARCHAR(32)   NOT NULL,
  order_no      VARCHAR(32)   NOT NULL,
  user_id       BIGINT        NOT NULL,
  declared_value DECIMAL(10,2) DEFAULT NULL,
  insure_fee    DECIMAL(10,2) DEFAULT NULL,
  claim_amount  DECIMAL(10,2) DEFAULT NULL,
  reason        VARCHAR(500)  DEFAULT NULL,
  evidence_urls VARCHAR(500)  DEFAULT NULL,
  status        TINYINT       DEFAULT 1 COMMENT '1 待审核 2 已赔付 3 已驳回',
  auditor_id    BIGINT        DEFAULT NULL,
  audit_remark  VARCHAR(255)  DEFAULT NULL,
  created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_claim_no (claim_no),
  KEY idx_claim_order (order_no, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保价理赔';

-- 20. 违规处罚表
DROP TABLE IF EXISTS t_violation;
CREATE TABLE t_violation (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  courier_id   BIGINT       NOT NULL,
  order_no     VARCHAR(32)  DEFAULT NULL,
  violation_type TINYINT    DEFAULT NULL COMMENT '1 爽约 2 超时 3 丢件 4 虚假凭证 5 投诉成立',
  points       INT          DEFAULT 0 COMMENT '扣减信用分',
  fine_amount  DECIMAL(10,2) DEFAULT 0.00,
  decision     VARCHAR(255) DEFAULT NULL,
  admin_id     BIGINT       DEFAULT NULL,
  effective_at DATETIME     DEFAULT NULL,
  created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_violation_courier (courier_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规处罚';

SET FOREIGN_KEY_CHECKS = 1;

-- 说明：演示数据由后端 DataInitializer 在空库首次启动时自动写入，
-- 包含管理员 admin/admin123、3 条计价规则、2 个演示账号与 2 条演示订单。
-- 生产环境如需关闭，可在配置中排除 DataInitializer 或清空演示数据后重新部署。
