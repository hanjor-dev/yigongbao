-- ============================================================
-- 医工宝生产环境迁移
-- 变更：更新 2026 年 10 月 1 日起历史包装工序的三边封拉链袋热封温度
-- 目标：production_process.process_params->zipBagSealTemperature: 130 -> 120
-- 说明：
--   1. 仅处理有效的 production_record / production_process；
--   2. 仅处理生产单创建时间为 2026-10-01（含）至 2026-11-01（不含）的数据；
--   3. 仅处理 process_type=pack 且当前参数值确实为 130 的记录；
--   4. JSON_SET 写入数字 120，并保留同一 JSON 中的其他参数；
--   5. 本脚本只更新数据库中的工序参数，不会自动重新生成已经上传的 Excel；
--      迁移完成后还需要调用 GET /api/production/record/{id}/excel 刷新对应流转卡。
-- ============================================================

SET NAMES utf8mb4;

SET @migration_start = '2026-10-01 00:00:00';
SET @migration_end   = '2026-11-01 00:00:00';

-- 迁移前审计：确认目标数量及当前值分布。
SELECT
    COUNT(*) AS target_process_count,
    COUNT(DISTINCT pp.production_record_id) AS target_record_count,
    MIN(pr.create_time) AS first_record_time,
    MAX(pr.create_time) AS last_record_time
FROM production_process pp
JOIN production_record pr
  ON pr.id = pp.production_record_id
WHERE pr.is_deleted = 0
  AND pp.is_deleted = 0
  AND pp.process_type = 'pack'
  AND pr.create_time >= @migration_start
  AND pr.create_time < @migration_end
  AND JSON_VALID(pp.process_params) = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature')) = '130';

SELECT
    JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature')) AS current_temperature,
    COUNT(*) AS process_count
FROM production_process pp
JOIN production_record pr
  ON pr.id = pp.production_record_id
WHERE pr.is_deleted = 0
  AND pp.is_deleted = 0
  AND pp.process_type = 'pack'
  AND pr.create_time >= @migration_start
  AND pr.create_time < @migration_end
  AND JSON_VALID(pp.process_params) = 1
GROUP BY JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature'))
ORDER BY current_temperature;

START TRANSACTION;

UPDATE production_process pp
JOIN production_record pr
  ON pr.id = pp.production_record_id
SET pp.process_params = JSON_SET(
        pp.process_params,
        '$.zipBagSealTemperature',
        120
    ),
    pp.update_time = CURRENT_TIMESTAMP
WHERE pr.is_deleted = 0
  AND pp.is_deleted = 0
  AND pp.process_type = 'pack'
  AND pr.create_time >= @migration_start
  AND pr.create_time < @migration_end
  AND JSON_VALID(pp.process_params) = 1
  AND JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature')) = '130';

SELECT ROW_COUNT() AS updated_process_count;

COMMIT;

-- 迁移后核验：目标范围内应不再存在 zipBagSealTemperature=130，
-- 并应返回实际更新后的 120 数量。
SELECT
    SUM(JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature')) = '130') AS remaining_temperature_130,
    SUM(JSON_UNQUOTE(JSON_EXTRACT(pp.process_params, '$.zipBagSealTemperature')) = '120') AS updated_temperature_120,
    COUNT(*) AS target_pack_process_count
FROM production_process pp
JOIN production_record pr
  ON pr.id = pp.production_record_id
WHERE pr.is_deleted = 0
  AND pp.is_deleted = 0
  AND pp.process_type = 'pack'
  AND pr.create_time >= @migration_start
  AND pr.create_time < @migration_end
  AND JSON_VALID(pp.process_params) = 1;

