-- =============================================
-- 华为云RDS MySQL数据库表结构
-- 适配ESP32智能农业监控系统
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS agri_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agri_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_username (username)
) COMMENT='用户表';

-- 2. 环境数据表（扩展字段以支持ESP32完整数据）
CREATE TABLE IF NOT EXISTS t_env_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据ID',
    sensor_id INT COMMENT '传感器ID',
    
    -- 传感器数据
    temperature DOUBLE COMMENT '温度(℃)',
    humidity DOUBLE COMMENT '湿度(%RH)',
    soil_moisture DOUBLE COMMENT '土壤湿度(%)',
    soil_adc INT COMMENT '土壤湿度ADC原始值(0-4095)',
    light_intensity INT COMMENT '光照强度(lux)',
    co2 INT COMMENT 'CO2浓度(ppm)',
    tvoc INT COMMENT 'TVOC挥发性有机物(ppb)',
    
    -- 设备状态
    pump_state BOOLEAN DEFAULT FALSE COMMENT '水泵状态',
    fan_state BOOLEAN DEFAULT FALSE COMMENT '风扇状态',
    light_state BOOLEAN DEFAULT FALSE COMMENT '照明灯状态',
    manual_mode BOOLEAN DEFAULT FALSE COMMENT '手动模式',
    save_username VARCHAR(50) COMMENT '保存人用户名',
    
    -- 时间戳
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    
    INDEX idx_collect_time (collect_time),
    INDEX idx_sensor_id (sensor_id)
) COMMENT='环境数据表';

-- 3. 设备状态表（存储最新状态和阈值配置）
CREATE TABLE IF NOT EXISTS t_device_status (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    device_id VARCHAR(100) UNIQUE NOT NULL COMMENT '设备ID',
    
    -- 当前状态
    pump_state BOOLEAN DEFAULT FALSE COMMENT '水泵状态',
    fan_state BOOLEAN DEFAULT FALSE COMMENT '风扇状态',
    light_state BOOLEAN DEFAULT FALSE COMMENT '照明灯状态',
    manual_mode BOOLEAN DEFAULT FALSE COMMENT '手动/自动模式',
    online_status BOOLEAN DEFAULT TRUE COMMENT '在线状态',
    
    -- 阈值设置
    light_threshold INT DEFAULT 800 COMMENT '光照阈值(lux)',
    pump_threshold INT DEFAULT 3200 COMMENT '水泵干旱阈值(ADC)',
    fan_temp_threshold INT DEFAULT 30 COMMENT '风扇温度阈值(℃)',
    fan_co2_threshold INT DEFAULT 1000 COMMENT '风扇CO2阈值(ppm)',
    
    -- 时间戳
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_device_id (device_id)
) COMMENT='设备状态表';

-- 4. 预警日志表
CREATE TABLE IF NOT EXISTS t_warning_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    warning_type VARCHAR(50) COMMENT '预警类型(温度/湿度/土壤/光照/CO2)',
    warning_level VARCHAR(20) COMMENT '预警级别(info/warning/danger)',
    warning_message VARCHAR(500) COMMENT '预警消息',
    sensor_value DOUBLE COMMENT '传感器当前值',
    threshold_value DOUBLE COMMENT '阈值',
    device_action VARCHAR(100) COMMENT '触发的设备动作',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_handled TINYINT DEFAULT 0 COMMENT '是否已处理',
    
    INDEX idx_create_time (create_time),
    INDEX idx_warning_type (warning_type),
    INDEX idx_is_handled (is_handled)
) COMMENT='预警日志表';

-- 5. 预警规则表
CREATE TABLE IF NOT EXISTS t_warning_rule (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    sensor_type VARCHAR(50) NOT NULL COMMENT '传感器类型(temp_hum/soil/light/co2)',
    min_value DOUBLE COMMENT '最小阈值',
    max_value DOUBLE COMMENT '最大阈值',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用(0-禁用 1-启用)',
    
    INDEX idx_sensor_type (sensor_type)
) COMMENT='预警规则表';

-- 6. 控制历史表
CREATE TABLE IF NOT EXISTS t_control_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    device_id VARCHAR(100) NOT NULL COMMENT '设备ID',
    control_type VARCHAR(50) COMMENT '控制类型(pump/fan/light/mode/threshold)',
    control_value VARCHAR(200) COMMENT '控制值',
    control_source VARCHAR(50) DEFAULT 'manual' COMMENT '控制来源(manual/auto)',
    operator VARCHAR(50) COMMENT '操作者',
    result VARCHAR(20) DEFAULT 'success' COMMENT '执行结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    
    INDEX idx_device_id (device_id),
    INDEX idx_create_time (create_time)
) COMMENT='控制历史表';

-- 7. 异常检测配置表
CREATE TABLE IF NOT EXISTS t_exception_config (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(255) NOT NULL COMMENT '配置值',
    config_group VARCHAR(50) NOT NULL COMMENT '分组(detection/notification/handling/severity)',
    description VARCHAR(200) COMMENT '配置说明',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_group (config_group)
) COMMENT='异常检测配置表';

-- 异常配置默认数据
INSERT IGNORE INTO t_exception_config (config_key, config_value, config_group, description) VALUES
('detection_enabled',          'true',  'detection',    '异常检测总开关'),
('detection_interval',         '60',    'detection',    '检测频率（秒）'),
('detection_temp_enabled',     'true',  'detection',    '温度检测开关'),
('detection_humidity_enabled', 'true',  'detection',    '湿度检测开关'),
('detection_soil_enabled',     'true',  'detection',    '土壤湿度检测开关'),
('detection_light_enabled',    'true',  'detection',    '光照检测开关'),
('detection_co2_enabled',      'true',  'detection',    'CO2检测开关'),
('notify_websocket',           'true',  'notification', 'WebSocket实时推送'),
('notify_sound',               'true',  'notification', '声音提醒'),
('notify_popup_duration',      '8',     'notification', '弹窗显示时长（秒）'),
('notify_repeat',              'false', 'notification', '重复异常通知'),
('handling_auto_handle',       'false', 'handling',     '自动处理异常'),
('handling_cooldown',          '5',     'handling',     '异常冷却时间（分钟）'),
('handling_max_logs',          '1000',  'handling',     '最大日志保留数'),
('severity_warning_ratio',     '1.0',   'severity',     '警告级别比例'),
('severity_danger_ratio',      '1.5',   'severity',     '危险级别比例'),
('severity_critical_ratio',    '2.0',   'severity',     '严重级别比例');

-- 初始化默认数据
-- 插入测试用户 (密码: 123456，BCrypt加密)
INSERT IGNORE INTO t_user (username, password, phone, email) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin@example.com');

-- 初始化设备状态
INSERT IGNORE INTO t_device_status (device_id, light_threshold, pump_threshold, fan_temp_threshold, fan_co2_threshold)
VALUES ('69568516c00ccb6d4b302187_esp32-001', 800, 3200, 30, 1000);

-- 创建视图：最新环境数据
CREATE OR REPLACE VIEW v_latest_env_data AS
SELECT 
    e.*,
    CASE 
        WHEN e.temperature < 15 THEN '偏低'
        WHEN e.temperature BETWEEN 15 AND 25 THEN '正常'
        WHEN e.temperature BETWEEN 25 AND 35 THEN '偏高'
        ELSE '过高'
    END AS temp_status,
    CASE 
        WHEN e.soil_moisture < 40 THEN '干旱'
        WHEN e.soil_moisture BETWEEN 40 AND 70 THEN '正常'
        ELSE '过湿'
    END AS soil_status,
    CASE 
        WHEN e.light_intensity < 800 THEN '偏暗'
        WHEN e.light_intensity BETWEEN 800 AND 3000 THEN '正常'
        ELSE '过亮'
    END AS light_status
FROM t_env_data e
WHERE e.id = (SELECT MAX(id) FROM t_env_data WHERE sensor_id = e.sensor_id)
ORDER BY e.collect_time DESC;
