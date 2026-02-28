-- =============================================
-- 数据库增量升级脚本
-- 适配预警日志扩展字段
-- 执行前请先备份数据库！
-- =============================================

USE agri_db;

-- 1. 为预警日志表添加 threshold 和 description 字段
ALTER TABLE t_warning_log
  ADD COLUMN threshold DOUBLE COMMENT '触发时阈值' AFTER trigger_value,
  ADD COLUMN description VARCHAR(500) COMMENT '预警描述文本' AFTER threshold;

-- 2. 将旧的中文 warningType 迁移为标准英文类型
UPDATE t_warning_log SET warning_type = 'temperature' WHERE warning_type LIKE '%温度%';
UPDATE t_warning_log SET warning_type = 'humidity'    WHERE warning_type LIKE '%湿度%';
UPDATE t_warning_log SET warning_type = 'soil'        WHERE warning_type LIKE '%土壤%';
UPDATE t_warning_log SET warning_type = 'light'       WHERE warning_type LIKE '%光照%';
UPDATE t_warning_log SET warning_type = 'co2'         WHERE warning_type LIKE '%CO%';

-- 完成提示
SELECT '数据库升级完成' AS result;
