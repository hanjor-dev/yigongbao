-- 图纸二维码图片存储改造（2026-07-17）
-- 目标：为既有生产库补充图纸二维码快照字段和文件业务字典。

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'design_drawing'
      AND column_name = 'qr_file_id'
);
SET @add_qr_column_sql = IF(
    @column_exists = 0,
    'ALTER TABLE design_drawing ADD COLUMN qr_file_id VARCHAR(32) DEFAULT NULL COMMENT ''生成该图纸版本时使用的二维码文件ID（10.21）'' AFTER template_file_url',
    'SELECT 1'
);
PREPARE add_qr_column_stmt FROM @add_qr_column_sql;
EXECUTE add_qr_column_stmt;
DEALLOCATE PREPARE add_qr_column_stmt;

INSERT INTO sys_dict (parent_id, dict_code, dict_name, dict_value, level, sort, status)
SELECT 50, '10.21', '图纸二维码图片', NULL, 2, 21, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict WHERE dict_code = '10.21' AND is_deleted = 0
);
