-- 个人记账后端 Flyway 初始化脚本（V1__init.sql）
-- 适用 MySQL 8.0+ / H2 MODE=MySQL

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(20) NOT NULL,
  password_hash VARCHAR(72) NOT NULL,
  nickname VARCHAR(16),
  email VARCHAR(120),
  phone VARCHAR(20),
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  enabled TINYINT NOT NULL DEFAULT 1,
  current_book_id BIGINT,
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME,
  last_login_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE book (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(40) NOT NULL,
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  icon VARCHAR(16),
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_book_user_name UNIQUE (user_id, name),
  CONSTRAINT fk_book_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT,
  book_id BIGINT,
  name VARCHAR(20) NOT NULL,
  type VARCHAR(16) NOT NULL,
  icon VARCHAR(16),
  color VARCHAR(8),
  is_system TINYINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_category_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
);

CREATE TABLE tx_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  pay_method VARCHAR(16) NOT NULL DEFAULT 'CASH',
  occurred_at DATETIME NOT NULL,
  remark VARCHAR(200),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT chk_tx_amount CHECK (amount > 0),
  CONSTRAINT fk_tx_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
  CONSTRAINT fk_tx_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_tx_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE RESTRICT
);
CREATE INDEX idx_tx_book_date ON tx_record(book_id, occurred_at DESC);
CREATE INDEX idx_tx_user_date ON tx_record(user_id, occurred_at DESC);
CREATE INDEX idx_tx_book_category ON tx_record(book_id, category_id);

CREATE TABLE budget (
  id BIGINT NOT NULL AUTO_INCREMENT,
  book_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  year_month CHAR(7) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  warn_threshold DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT uk_budget_book_cat_ym UNIQUE (book_id, category_id, year_month),
  CONSTRAINT chk_budget_amount CHECK (amount > 0),
  CONSTRAINT chk_budget_threshold CHECK (warn_threshold >= 0 AND warn_threshold <= 2),
  CONSTRAINT fk_budget_book FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
  CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

CREATE TABLE auth_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(512) NOT NULL,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  revoked TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT uk_auth_token_token UNIQUE (token),
  CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
);

CREATE TABLE audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(32),
  target_id BIGINT,
  ip VARCHAR(45),
  user_agent VARCHAR(255),
  result VARCHAR(16) NOT NULL,
  detail TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_user_time ON audit_log(user_id, created_at DESC);
