-- Versioned references and legacy order migration are applied by SchemaMigration after initialization.
CREATE TABLE IF NOT EXISTS auth_sessions (
  id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
  user_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  KEY idx_sessions_user_active (user_id, revoked_at, expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS request_dedup (
  user_id BIGINT NOT NULL,
  scope VARCHAR(24) NOT NULL,
  request_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  payload_hash CHAR(64) NOT NULL,
  response_json MEDIUMTEXT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, scope, request_key),
  KEY idx_request_dedup_created (created_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  student_no VARCHAR(20) NULL,
  password VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NULL,
  avatar TEXT NULL,
  bio VARCHAR(200) NULL,
  location VARCHAR(100) NULL,
  qq VARCHAR(20) NULL,
  wechat VARCHAR(40) NULL,
  status INT NOT NULL DEFAULT 1,
  role INT NOT NULL DEFAULT 0,
  created_time DATETIME NULL,
  updated_time DATETIME NULL,
  deleted INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_student_no (student_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(40) NOT NULL,
  description VARCHAR(200) NULL,
  parent_id BIGINT NULL,
  icon VARCHAR(100) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status INT NOT NULL DEFAULT 1,
  created_time DATETIME NULL,
  updated_time DATETIME NULL,
  UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(60) NOT NULL,
  description VARCHAR(2000) NULL,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2) NULL,
  category VARCHAR(40) NULL,
  `condition` VARCHAR(20) NULL,
  trade_place VARCHAR(40) NULL,
  images MEDIUMTEXT NULL,
  view_count INT NOT NULL DEFAULT 0,
  status INT NOT NULL DEFAULT 0,
  created_time DATETIME NULL,
  updated_time DATETIME NULL,
  sold_time DATETIME NULL,
  KEY idx_products_sale_created (status, created_time, id),
  KEY idx_products_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_time DATETIME NULL,
  UNIQUE KEY uk_favorite (user_id, product_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  product_id BIGINT NULL,
  content VARCHAR(500) NOT NULL,
  is_read INT NOT NULL DEFAULT 0,
  created_time DATETIME NULL,
  KEY idx_messages_conversation (sender_id, receiver_id, created_time),
  KEY idx_messages_unread (receiver_id, is_read)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `orders` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  status INT NOT NULL DEFAULT 0,
  created_time DATETIME NULL,
  updated_time DATETIME NULL,
  completed_time DATETIME NULL,
  snapshot_json MEDIUMTEXT NULL,
  UNIQUE KEY uk_orders_no (order_no),
  KEY idx_orders_product_status (product_id, status),
  KEY idx_orders_buyer (buyer_id, created_time),
  KEY idx_orders_seller (seller_id, created_time)
) ENGINE=InnoDB;

-- Register your own account and assign an administrator role locally.

INSERT IGNORE INTO categories (name, description, sort_order, status, created_time, updated_time) VALUES
('books', '教材书籍', 1, 1, NOW(), NOW()),
('electronics', '电子产品', 2, 1, NOW(), NOW()),
('transport', '出行工具', 3, 1, NOW(), NOW()),
('gaming', '游戏数码', 4, 1, NOW(), NOW()),
('clothing', '服饰穿搭', 5, 1, NOW(), NOW()),
('living', '生活用品', 6, 1, NOW(), NOW()),
('other', '其他', 7, 1, NOW(), NOW());
