-- =============================================
-- 数据库增量升级脚本 v3
-- 新增环境数据保存人字段
-- 执行前请先备份数据库！
-- =============================================

USE agri_db;

-- 1. 为环境数据表新增保存人用户名字段
SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'agri_db'
    AND TABLE_NAME = 't_env_data'
    AND COLUMN_NAME = 'save_username'
);

SET @manual_mode_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'agri_db'
    AND TABLE_NAME = 't_env_data'
    AND COLUMN_NAME = 'manual_mode'
);

SET @ddl = IF(
  @col_exists = 0,
  IF(
    @manual_mode_exists = 1,
    'ALTER TABLE t_env_data ADD COLUMN save_username VARCHAR(50) NULL COMMENT ''保存人用户名'' AFTER manual_mode',
    'ALTER TABLE t_env_data ADD COLUMN save_username VARCHAR(50) NULL COMMENT ''保存人用户名''' 
  ),
  'SELECT ''字段 save_username 已存在，跳过ALTER'' AS msg'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 回填历史数据，避免前端为空
UPDATE t_env_data
SET save_username = '历史数据导入'
WHERE save_username IS NULL OR TRIM(save_username) = '';

-- 完成提示
SELECT '数据库升级v3完成：已新增save_username字段' AS result;
