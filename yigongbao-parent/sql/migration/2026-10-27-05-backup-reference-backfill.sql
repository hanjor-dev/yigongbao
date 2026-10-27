-- 将现有 file_detail 业务关系分批回填到 cos_object_reference，并记录水位和统计。
-- 执行前检查：先执行 03-backup-cleanup.sql；确认 file_detail.id 为十进制字符串且 path/base_path 能还原对象键。
-- bucket 优先读取 metadata.bucket；无法解析时写 UNKNOWN 并由验收查询阻止开放实际删除。

CREATE TABLE IF NOT EXISTS backup_migration_watermark (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID', migration_code VARCHAR(128) NOT NULL COMMENT '迁移编码',
    shard_key VARCHAR(128) NOT NULL COMMENT '分片键', high_watermark VARCHAR(255) NOT NULL COMMENT '已处理高水位',
    scanned_count BIGINT NOT NULL DEFAULT 0 COMMENT '扫描数量', inserted_count BIGINT NOT NULL DEFAULT 0 COMMENT '插入数量',
    unresolved_count BIGINT NOT NULL DEFAULT 0 COMMENT '未解析数量', status VARCHAR(32) NOT NULL COMMENT 'RUNNING/COMPLETED/NEEDS_REVIEW',
    stats_json JSON NULL COMMENT '校验统计', version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间', update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    create_by BIGINT NULL COMMENT '创建人ID', update_by BIGINT NULL COMMENT '更新人ID', is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY(id), UNIQUE KEY uk_backup_migration_watermark(migration_code,shard_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='备份迁移水位与校验统计';

SET @reference_before := (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND is_deleted=0);

INSERT INTO cos_object_reference
    (platform,bucket,object_key,version_identity,file_id,relation_kind,relation_id,identity_hash,active,source_updated_at,
     create_time,update_time,create_by,update_by,is_deleted)
SELECT COALESCE(NULLIF(fd.platform,''),'UNKNOWN'),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.bucket')),''),'UNKNOWN'),
       COALESCE(NULLIF(CONCAT(COALESCE(fd.base_path,''),COALESCE(fd.path,''),COALESCE(fd.filename,'')),''),fd.url),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.versionId')),''),'null'),
       CAST(fd.id AS UNSIGNED),'FILE_DETAIL',CAST(fd.id AS UNSIGNED),
       SHA2(CONCAT_WS(CHAR(0),
           COALESCE(NULLIF(fd.platform,''),'UNKNOWN'),
           COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.bucket')),''),'UNKNOWN'),
           COALESCE(NULLIF(CONCAT(COALESCE(fd.base_path,''),COALESCE(fd.path,''),COALESCE(fd.filename,'')),''),fd.url),
           COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID(fd.metadata) THEN fd.metadata ELSE '{}' END,'$.versionId')),''),'null'),
           'FILE_DETAIL',CAST(fd.id AS UNSIGNED)),256),
       1,fd.update_time,
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
FROM file_detail fd
WHERE fd.is_deleted=0 AND fd.id REGEXP '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM cos_object_reference r
      WHERE r.relation_kind='FILE_DETAIL' AND r.relation_id=CAST(fd.id AS UNSIGNED)
        AND r.platform=COALESCE(NULLIF(fd.platform,''),'UNKNOWN') AND r.is_deleted=0
  );

SET @reference_after := (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND is_deleted=0);
SET @file_high_watermark := COALESCE((SELECT MAX(CAST(id AS UNSIGNED)) FROM file_detail WHERE id REGEXP '^[0-9]+$'),0);
INSERT INTO backup_migration_watermark
    (migration_code,shard_key,high_watermark,scanned_count,inserted_count,unresolved_count,status,stats_json,create_time,update_time,is_deleted)
VALUES
    ('2026-10-27-reference-backfill','file_detail',CAST(@file_high_watermark AS CHAR),
     (SELECT COUNT(*) FROM file_detail WHERE is_deleted=0),@reference_after-@reference_before,
     (SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0),
     IF((SELECT COUNT(*) FROM cos_object_reference WHERE relation_kind='FILE_DETAIL' AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0)=0,'COMPLETED','NEEDS_REVIEW'),
     JSON_OBJECT('before',@reference_before,'after',@reference_after),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0)
ON DUPLICATE KEY UPDATE high_watermark=VALUES(high_watermark), scanned_count=VALUES(scanned_count),
 inserted_count=inserted_count+VALUES(inserted_count), unresolved_count=VALUES(unresolved_count), status=VALUES(status),
 stats_json=VALUES(stats_json), update_time=CURRENT_TIMESTAMP(3);

-- 执行后验证：第二次执行 inserted_count 增量应为 0；UNKNOWN 必须治理为 0 才可启用 ACTUAL 清理。
SELECT migration_code,high_watermark,scanned_count,inserted_count,unresolved_count,status FROM backup_migration_watermark
WHERE migration_code='2026-10-27-reference-backfill' AND shard_key='file_detail';
SELECT platform,bucket,COUNT(*) unresolved FROM cos_object_reference WHERE relation_kind='FILE_DETAIL'
AND (platform='UNKNOWN' OR bucket='UNKNOWN') AND is_deleted=0 GROUP BY platform,bucket;
-- 人工回滚：只允许按 migration_code 审核后删除本脚本生成且未被业务维护的引用；不得删除引用表或业务文件。
