-- 武汉嘉一三维技术应用有限公司关联全部医疗机构
-- 执行时间：2026-09-15
-- 影响表：sys_org_hospital
-- 目标机构：武汉嘉一三维技术应用有限公司（sys_org.id = 594）
--
-- 说明：
-- 1. 仅补充尚未存在的关联，不删除已有数据，可重复执行。
-- 2. 医疗机构范围与 OrgServiceImpl 中 listByIds 的逻辑删除语义保持一致：
--    org_type = '1.3' 且 is_deleted = 0。
-- 3. 通过目标机构 ID、名称、类型和逻辑删除状态共同校验目标；目标不匹配时不会写入数据。
-- 4. 机构名称使用二进制比较，避免客户端连接排序规则与字段排序规则不同导致比较失败。

SET @target_org_id := 594;
SET @target_org_name := '武汉嘉一三维技术应用有限公司';

START TRANSACTION;

INSERT INTO sys_org_hospital (
    distributor_org_id,
    hospital_org_id,
    create_time
)
SELECT
    target_org.id,
    hospital_org.id,
    CURRENT_TIMESTAMP
FROM sys_org AS target_org
CROSS JOIN sys_org AS hospital_org
WHERE target_org.id = @target_org_id
  AND CAST(target_org.org_name AS BINARY) = CAST(@target_org_name AS BINARY)
  AND target_org.org_type IN ('1.2', '1.4')
  AND target_org.is_deleted = 0
  AND hospital_org.org_type = '1.3'
  AND hospital_org.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_org_hospital AS existing_relation
      WHERE existing_relation.distributor_org_id = target_org.id
        AND existing_relation.hospital_org_id = hospital_org.id
  );

SET @inserted_relation_count := ROW_COUNT();

COMMIT;

-- 执行结果核验：missing_relation_count 应为 0。
SELECT
    @target_org_id AS distributor_org_id,
    @target_org_name AS distributor_org_name,
    @inserted_relation_count AS inserted_relation_count,
    (
        SELECT COUNT(*)
        FROM sys_org AS hospital_org
        WHERE hospital_org.org_type = '1.3'
          AND hospital_org.is_deleted = 0
    ) AS expected_relation_count,
    (
        SELECT COUNT(*)
        FROM sys_org_hospital AS relation
        INNER JOIN sys_org AS hospital_org
            ON hospital_org.id = relation.hospital_org_id
           AND hospital_org.org_type = '1.3'
           AND hospital_org.is_deleted = 0
        WHERE relation.distributor_org_id = @target_org_id
    ) AS actual_relation_count,
    (
        SELECT COUNT(*)
        FROM sys_org AS hospital_org
        WHERE hospital_org.org_type = '1.3'
          AND hospital_org.is_deleted = 0
          AND NOT EXISTS (
              SELECT 1
              FROM sys_org_hospital AS relation
              WHERE relation.distributor_org_id = @target_org_id
                AND relation.hospital_org_id = hospital_org.id
          )
    ) AS missing_relation_count;
