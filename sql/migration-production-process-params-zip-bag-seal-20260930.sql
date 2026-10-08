-- ============================================================
-- 医工宝生产环境迁移
-- 变更：三边封拉链袋热封参数文案及默认温度
-- 说明：只更新 sys_config 中的工序参数配置，不修改历史工序记录。
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

-- 迁移前检查目标配置是否存在且为合法 JSON。
SELECT id,
       config_key,
       JSON_VALID(config_value) AS config_json_valid,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.label')) AS model_seal_label,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.label')) AS model_temperature_label,
       JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.default') AS model_temperature_default,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.label')) AS guide_seal_label,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.label')) AS guide_temperature_label,
       JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.default') AS guide_temperature_default
FROM sys_config
WHERE config_key = 'production.process.params.config'
  AND is_deleted = 0;

UPDATE sys_config
SET config_value = JSON_SET(
        config_value,
        '$.pack."17.1".zipBagSeal.label', '三边封拉链袋热封',
        '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.label', '三边封拉链袋热封温度',
        '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.default', 120,
        '$.pack."17.2".zipBagSeal.label', '三边封拉链袋热封',
        '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.label', '三边封拉链袋热封温度',
        '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.default', 120
    )
WHERE config_key = 'production.process.params.config'
  AND config_type = 'json'
  AND is_deleted = 0
  AND JSON_VALID(config_value) = 1
  AND JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal') IS NOT NULL
  AND JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal') IS NOT NULL;

SELECT ROW_COUNT() AS updated_config_rows;

-- 迁移后检查：应返回两类标签均为新文案、默认温度均为 120。
SELECT id,
       config_key,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.label')) AS model_seal_label,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.label')) AS model_temperature_label,
       JSON_EXTRACT(config_value, '$.pack."17.1".zipBagSeal.dependents.zipBagSealTemperature.default') AS model_temperature_default,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.label')) AS guide_seal_label,
       JSON_UNQUOTE(JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.label')) AS guide_temperature_label,
       JSON_EXTRACT(config_value, '$.pack."17.2".zipBagSeal.dependents.zipBagSealTemperature.default') AS guide_temperature_default
FROM sys_config
WHERE config_key = 'production.process.params.config'
  AND is_deleted = 0;

COMMIT;
