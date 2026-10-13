-- 生产流转卡 Excel 格式更新后的缓存失效
-- 适用版本：MySQL 8.0+
-- 执行时机：所有旧版本应用实例已停止或摘流，且新版本应用实例已经生效后。
-- 安全策略：不删除旧文件、不修改业务内容或业务更新时间；仅将已有缓存的生成时间置空，
--          使下次 GET /production/record/{id}/excel 按新格式重新生成。

SELECT NOW() AS migration_start_time;

-- 前置审计：此 WHERE 与 UPDATE 完全一致。
SELECT COUNT(*) AS cached_flow_cards_to_invalidate
FROM production_record
WHERE is_deleted = 0
  AND flow_card_generate_time IS NOT NULL
  AND flow_card_file_url IS NOT NULL
  AND TRIM(flow_card_file_url) <> '';

START TRANSACTION;

UPDATE production_record
SET flow_card_generate_time = NULL,
    update_time = update_time
WHERE is_deleted = 0
  AND flow_card_generate_time IS NOT NULL
  AND flow_card_file_url IS NOT NULL
  AND TRIM(flow_card_file_url) <> '';

SELECT ROW_COUNT() AS invalidated_flow_card_count;

COMMIT;

-- 后置核验：结果应为 0；重复执行脚本时受影响行数也应为 0。
SELECT COUNT(*) AS cached_flow_cards_still_pending_invalidation
FROM production_record
WHERE is_deleted = 0
  AND flow_card_generate_time IS NOT NULL
  AND flow_card_file_url IS NOT NULL
  AND TRIM(flow_card_file_url) <> '';

SELECT NOW() AS migration_end_time;
