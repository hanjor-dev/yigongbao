-- 为历史正式、非取消、非删除订单补建 generation=1 候选事件。
-- 执行前检查：先执行 01 和 05 迁移；复核 status=1010 为草稿、status=9010 为取消。
-- 正式订单判定：非草稿 status<>1010、非取消 status<>9010、is_deleted=0；上线前由业务负责人复核状态集。
-- 重新获得资格的订单由运行时代码在同一事务追加 generation+1，本脚本只处理首次历史基线。

SET @candidate_before := (SELECT COUNT(*) FROM order_backup_candidate WHERE is_deleted=0);
INSERT INTO order_backup_candidate
    (event_id,order_id,generation,eligible_at,order_created_at,event_status,version,create_time,update_time,create_by,update_by,is_deleted)
SELECT o.id,o.id,1,COALESCE(o.update_time,o.create_time,CURRENT_TIMESTAMP(3)),o.create_time,'ACTIVE',0,
       CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),NULL,NULL,0
FROM order_main o
WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
  AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1);
SET @candidate_after := (SELECT COUNT(*) FROM order_backup_candidate WHERE is_deleted=0);

INSERT INTO backup_migration_watermark
    (migration_code,shard_key,high_watermark,scanned_count,inserted_count,unresolved_count,status,stats_json,create_time,update_time,is_deleted)
VALUES ('2026-10-27-candidate-backfill','order_main',
    CAST(COALESCE((SELECT MAX(id) FROM order_main),0) AS CHAR),
    (SELECT COUNT(*) FROM order_main WHERE is_deleted=0 AND status<>1010 AND status<>9010),
    @candidate_after-@candidate_before,
    (SELECT COUNT(*) FROM order_main o WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
      AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1)),
    'COMPLETED',JSON_OBJECT('before',@candidate_before,'after',@candidate_after),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),0)
ON DUPLICATE KEY UPDATE high_watermark=VALUES(high_watermark), scanned_count=VALUES(scanned_count),
 inserted_count=inserted_count+VALUES(inserted_count), unresolved_count=VALUES(unresolved_count), status=VALUES(status),
 stats_json=VALUES(stats_json), update_time=CURRENT_TIMESTAMP(3);

-- 执行后验证：missing_count 必须为 0；重复执行 new_rows 必须为 0。
SELECT COUNT(*) missing_count FROM order_main o WHERE o.is_deleted=0 AND o.status<>1010 AND o.status<>9010
AND NOT EXISTS (SELECT 1 FROM order_backup_candidate c WHERE c.order_id=o.id AND c.generation=1);
SELECT @candidate_after-@candidate_before AS new_rows;
-- 人工回滚：候选事件是审计事实，生产环境禁止删除；状态判定有误时追加纠正事件并暂停调度。
