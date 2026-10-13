-- Khởi tạo CSDL cho hệ thống IoT Dashboard
-- Chạy: mysql -u root -p < database/init.sql
CREATE DATABASE IF NOT EXISTS iot_database
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE iot_database;

-- Người dùng (hiển thị ở trang Profile)
CREATE TABLE IF NOT EXISTS users (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description TEXT,
  password    VARCHAR(255) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Danh mục cảm biến
CREATE TABLE IF NOT EXISTS sensors (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL UNIQUE,
  unit        VARCHAR(50),
  description TEXT,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Danh mục thiết bị (LED); status là trạng thái hiện tại do ESP32 xác nhận
CREATE TABLE IF NOT EXISTS devices (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL UNIQUE,
  description TEXT,
  status      ENUM('ON','OFF') NOT NULL DEFAULT 'OFF',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Mỗi bản tin MQTT sinh 1 dòng cho mỗi cảm biến, cùng created_at (timestamp do backend gán, UTC+7)
CREATE TABLE IF NOT EXISTS sensor_data (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  sensor_id  BIGINT        NOT NULL,
  value      DECIMAL(10,2) NOT NULL,
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sensor_data_sensor FOREIGN KEY (sensor_id) REFERENCES sensors(id),
  INDEX idx_sensor_data_created_at (created_at),
  INDEX idx_sensor_data_sensor (sensor_id, id)
) ENGINE=InnoDB;

-- Lịch sử bật/tắt. status: PENDING (đang xử lý) -> SUCCESS khi ESP32 xác nhận, ERROR nếu quá hạn
CREATE TABLE IF NOT EXISTS action_history (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id  BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL DEFAULT 1 COMMENT 'Người thực hiện lệnh (hiện luôn là 1)',
  action     VARCHAR(50) NOT NULL COMMENT 'ON | OFF',
  status     ENUM('PENDING','SUCCESS','ERROR') NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_action_history_device FOREIGN KEY (device_id) REFERENCES devices(id),
  CONSTRAINT fk_action_history_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_action_history_created_at (created_at),
  INDEX idx_action_history_status (status)
) ENGINE=InnoDB;

-- Dữ liệu danh mục. id 1..3 được backend dùng: sensor 1=nhiệt độ, 2=độ ẩm, 3=ánh sáng; device n = LED n
INSERT INTO sensors (id, name, unit, description) VALUES
  (1, 'Nhiệt độ', '°C', 'DHT11 - nhiệt độ'),
  (2, 'Độ ẩm',    '%',  'DHT11 - độ ẩm'),
  (3, 'Ánh sáng', '%',  'LDR - mức ánh sáng')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO devices (id, name, description, status) VALUES
  (1, 'Led 1', 'LED đỏ (GPIO18)',   'OFF'),
  (2, 'Led 2', 'LED xanh (GPIO19)', 'OFF'),
  (3, 'Led 3', 'LED vàng (GPIO21)', 'OFF')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (id, name, description, password) VALUES
  (1, 'Hoàng Xuân Vinh', 'IoT Developer', 'N/A')
ON DUPLICATE KEY UPDATE name = VALUES(name);
