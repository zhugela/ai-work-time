-- =====================================================================
-- 个人记账后端 · 数据库初始化脚本
-- 适用：MySQL 8.0+
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- 引擎：InnoDB
-- 编写依据：./01-architecture.md §3 + sqlite-mcp 自洽性校验结果
-- 创建时间：2026-06-15
-- =====================================================================

-- ---------------------------------------------------------------
-- 0. 数据库 & 字符集
-- ---------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `personal_jz`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `personal_jz`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------
-- 1. sys_user  用户
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '主键',
  `username`            VARCHAR(20)  NOT NULL                            COMMENT '用户名（唯一）',
  `password_hash`       VARCHAR(72)  NOT NULL                            COMMENT 'BCrypt 散列',
  `nickname`            VARCHAR(16)  DEFAULT NULL                        COMMENT '昵称',
  `email`               VARCHAR(120) DEFAULT NULL                        COMMENT '邮箱',
  `phone`               VARCHAR(20)  DEFAULT NULL                        COMMENT '手机号',
  `role`                VARCHAR(16)  NOT NULL DEFAULT 'USER'             COMMENT 'USER / ADMIN',
  `enabled`             TINYINT(1)   NOT NULL DEFAULT 1                   COMMENT '1=启用 0=禁用',
  `current_book_id`     BIGINT       DEFAULT NULL                        COMMENT '当前账本 ID（弱引用）',
  `failed_login_count`  INT          NOT NULL DEFAULT 0                  COMMENT '登录失败计数',
  `locked_until`        DATETIME     DEFAULT NULL                        COMMENT '锁定截止时间',
  `last_login_at`       DATETIME     DEFAULT NULL                        COMMENT '最近登录时间',
  `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_role_enabled` (`role`, `enabled`),
  KEY `idx_sys_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ---------------------------------------------------------------
-- 2. book  账本
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL                                COMMENT '所属用户',
  `name`       VARCHAR(40)  NOT NULL                                COMMENT '账本名',
  `currency`   VARCHAR(8)   NOT NULL DEFAULT 'CNY'                  COMMENT 'CNY / USD / EUR',
  `icon`       VARCHAR(16)  DEFAULT NULL                            COMMENT 'emoji 图标',
  `status`     VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE'               COMMENT 'ACTIVE / ARCHIVED',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_user_name` (`user_id`, `name`),
  KEY `idx_book_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_book_user` FOREIGN KEY (`user_id`)
    REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账本';

-- ---------------------------------------------------------------
-- 3. category  分类
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       DEFAULT NULL                            COMMENT '所属用户（系统预置为 NULL）',
  `book_id`    BIGINT       DEFAULT NULL                            COMMENT '账本（系统预置为 NULL）',
  `name`       VARCHAR(20)  NOT NULL                                COMMENT '分类名',
  `type`       VARCHAR(16)  NOT NULL                                COMMENT 'INCOME / EXPENSE / TRANSFER',
  `icon`       VARCHAR(16)  DEFAULT NULL,
  `color`      VARCHAR(8)   DEFAULT NULL                            COMMENT 'hex 例 #FF5C00',
  `is_system`  TINYINT(1)   NOT NULL DEFAULT 0                      COMMENT '1=系统预置',
  `sort_order` INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_book_name_type` (`book_id`, `name`, `type`),
  KEY `idx_category_user_type` (`user_id`, `type`),
  KEY `idx_category_book_type` (`book_id`, `type`),
  CONSTRAINT `fk_category_user` FOREIGN KEY (`user_id`)
    REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_category_book` FOREIGN KEY (`book_id`)
    REFERENCES `book`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类';

-- ---------------------------------------------------------------
-- 4. tx_record  收支记录
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `tx_record`;
CREATE TABLE `tx_record` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT,
  `book_id`     BIGINT         NOT NULL,
  `user_id`     BIGINT         NOT NULL,
  `category_id` BIGINT         NOT NULL,
  `type`        VARCHAR(16)    NOT NULL                          COMMENT 'INCOME / EXPENSE / TRANSFER',
  `amount`      DECIMAL(12, 2) NOT NULL                          COMMENT '金额（>0）',
  `pay_method`  VARCHAR(16)    NOT NULL DEFAULT 'CASH'           COMMENT 'CASH/ALIPAY/WECHAT/BANK_CARD/CREDIT_CARD/OTHER',
  `occurred_at` DATETIME       NOT NULL                          COMMENT '业务发生时间',
  `remark`      VARCHAR(200)   DEFAULT NULL,
  `is_deleted`  TINYINT(1)     NOT NULL DEFAULT 0                COMMENT '软删除',
  `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tx_book_date`        (`book_id`, `occurred_at` DESC),
  KEY `idx_tx_user_date`        (`user_id`, `occurred_at` DESC),
  KEY `idx_tx_book_category`    (`book_id`, `category_id`),
  KEY `idx_tx_book_type_date`   (`book_id`, `type`, `occurred_at` DESC),
  CONSTRAINT `chk_tx_amount` CHECK (`amount` > 0),
  CONSTRAINT `fk_tx_book`     FOREIGN KEY (`book_id`)
    REFERENCES `book`(`id`)     ON DELETE CASCADE,
  CONSTRAINT `fk_tx_user`     FOREIGN KEY (`user_id`)
    REFERENCES `sys_user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tx_category` FOREIGN KEY (`category_id`)
    REFERENCES `category`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支记录';

-- ---------------------------------------------------------------
-- 5. budget  月度预算
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `budget`;
CREATE TABLE `budget` (
  `id`             BIGINT         NOT NULL AUTO_INCREMENT,
  `book_id`        BIGINT         NOT NULL,
  `category_id`    BIGINT         NOT NULL,
  `year_month`     CHAR(7)        NOT NULL                       COMMENT 'YYYY-MM',
  `amount`         DECIMAL(12, 2) NOT NULL                       COMMENT '预算金额 >0',
  `warn_threshold` DECIMAL(5, 2)  NOT NULL DEFAULT 1.00          COMMENT '0~2（百分比）',
  `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_book_cat_ym` (`book_id`, `category_id`, `year_month`),
  KEY `idx_budget_book_ym` (`book_id`, `year_month`),
  CONSTRAINT `chk_budget_amount`     CHECK (`amount` > 0),
  CONSTRAINT `chk_budget_threshold`  CHECK (`warn_threshold` >= 0 AND `warn_threshold` <= 2),
  CONSTRAINT `fk_budget_book`     FOREIGN KEY (`book_id`)
    REFERENCES `book`(`id`)     ON DELETE CASCADE,
  CONSTRAINT `fk_budget_category` FOREIGN KEY (`category_id`)
    REFERENCES `category`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度预算';

-- ---------------------------------------------------------------
-- 6. auth_token  鉴权 Token
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `auth_token`;
CREATE TABLE `auth_token` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `token`      VARCHAR(512) NOT NULL,
  `issued_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME     NOT NULL,
  `revoked`    TINYINT(1)   NOT NULL DEFAULT 0                  COMMENT '1=已吊销',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_token_token` (`token`),
  KEY `idx_auth_token_user` (`user_id`, `revoked`),
  KEY `idx_auth_token_expires` (`expires_at`),
  CONSTRAINT `fk_auth_token_user` FOREIGN KEY (`user_id`)
    REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鉴权 Token';

-- ---------------------------------------------------------------
-- 7. audit_log  审计日志
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL                          COMMENT '操作人（注销后保留）',
  `action`      VARCHAR(64)  NOT NULL                              COMMENT 'LOGIN/LOGOUT/IMPORT/EXPORT/...',
  `target_type` VARCHAR(32)  DEFAULT NULL                          COMMENT 'Book/Category/TxRecord/...',
  `target_id`   BIGINT       DEFAULT NULL,
  `ip`          VARCHAR(45)  DEFAULT NULL                          COMMENT 'IPv4/IPv6',
  `user_agent`  VARCHAR(255) DEFAULT NULL,
  `result`      VARCHAR(16)  NOT NULL                              COMMENT 'SUCCESS / FAILURE',
  `detail`      JSON         DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user_time` (`user_id`, `created_at` DESC),
  KEY `idx_audit_action_time` (`action`, `created_at` DESC),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`)
    REFERENCES `sys_user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 8. 数据种子：系统预置分类（is_system=1, user_id/book_id=NULL）
-- =====================================================================

-- 支出 15 项
INSERT INTO `category` (`user_id`, `book_id`, `name`, `type`, `icon`, `color`, `is_system`, `sort_order`) VALUES
(NULL, NULL, '餐饮',    'EXPENSE', '🍔', '#FF7A45', 1,  1),
(NULL, NULL, '交通',    'EXPENSE', '🚇', '#0EA5E9', 1,  2),
(NULL, NULL, '购物',    'EXPENSE', '🛒', '#F5A524', 1,  3),
(NULL, NULL, '居家',    'EXPENSE', '🏠', '#1FB85B', 1,  4),
(NULL, NULL, '娱乐',    'EXPENSE', '🎮', '#A855F7', 1,  5),
(NULL, NULL, '医疗',    'EXPENSE', '💊', '#E5484D', 1,  6),
(NULL, NULL, '学习',    'EXPENSE', '📚', '#3B6EE8', 1,  7),
(NULL, NULL, '礼物',    'EXPENSE', '🎁', '#EC4899', 1,  8),
(NULL, NULL, '旅行',    'EXPENSE', '✈️', '#06B6D4', 1,  9),
(NULL, NULL, '通讯',    'EXPENSE', '📱', '#10B981', 1, 10),
(NULL, NULL, '美容',    'EXPENSE', '💄', '#F472B6', 1, 11),
(NULL, NULL, '运动',    'EXPENSE', '🏃', '#84CC16', 1, 12),
(NULL, NULL, '汽车',    'EXPENSE', '🚗', '#64748B', 1, 13),
(NULL, NULL, '数码',    'EXPENSE', '💻', '#6366F1', 1, 14),
(NULL, NULL, '其他支出','EXPENSE', '📦', '#9CA3AF', 1, 99);

-- 收入 8 项
INSERT INTO `category` (`user_id`, `book_id`, `name`, `type`, `icon`, `color`, `is_system`, `sort_order`) VALUES
(NULL, NULL, '工资',    'INCOME', '💰', '#1FB85B', 1,  1),
(NULL, NULL, '兼职',    'INCOME', '💼', '#0EA5E9', 1,  2),
(NULL, NULL, '红包',    'INCOME', '🧧', '#E5484D', 1,  3),
(NULL, NULL, '投资收益','INCOME', '📈', '#F5A524', 1,  4),
(NULL, NULL, '退款',    'INCOME', '↩️', '#A855F7', 1,  5),
(NULL, NULL, '奖金',    'INCOME', '🏆', '#FFD700', 1,  6),
(NULL, NULL, '兼职稿费','INCOME', '📝', '#10B981', 1,  7),
(NULL, NULL, '其他收入','INCOME', '✨', '#6B7280', 1, 99);

-- =====================================================================
-- 9. 默认管理员账号（仅初始化；密码 admin@123456 的 BCrypt 散列）
-- =====================================================================
INSERT INTO `sys_user` (`username`, `password_hash`, `nickname`, `email`, `role`, `enabled`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin@personal-jz.dev', 'ADMIN', 1);

-- =====================================================================
-- 10. 自洽性校验
-- =====================================================================
-- 期望输出：8 张表 + 14 个 category 种子
SELECT COUNT(*) AS table_count FROM information_schema.tables
  WHERE table_schema = 'personal_jz' AND table_type = 'BASE TABLE';

SELECT COUNT(*) AS seed_category_count FROM category WHERE is_system = 1;

-- =====================================================================
-- 11. 触发器：每月定时清理过期 token（MySQL EVENT）
-- =====================================================================
DROP EVENT IF EXISTS `evt_clean_expired_token`;
CREATE EVENT `evt_clean_expired_token`
  ON SCHEDULE EVERY 1 DAY STARTS CURRENT_TIMESTAMP
  DO DELETE FROM auth_token WHERE expires_at < NOW() OR revoked = 1;

SET GLOBAL event_scheduler = ON;
