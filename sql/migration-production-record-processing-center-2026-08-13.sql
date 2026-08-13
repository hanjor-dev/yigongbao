-- 生产流转卡历史加工中心回填
-- 适用版本：MySQL 8.0+
-- 范围：仅处理已接单（producer_id IS NOT NULL）、有效且加工中心 ID/名称缺失的流转卡。
-- 来源优先级：流转卡已有 ID > 订单归属 > 已分配打印设备 > 当前生产人绑定。
-- 安全策略：锁定候选行；任意非空来源冲突、无法解析、中心无效时整笔回滚。

SELECT NOW() AS migration_start_time;

DELIMITER $$

DROP PROCEDURE IF EXISTS ygb_backfill_production_record_center $$
CREATE PROCEDURE ygb_backfill_production_record_center()
BEGIN
    DECLARE v_candidate_count BIGINT DEFAULT 0;
    DECLARE v_conflict_count BIGINT DEFAULT 0;
    DECLARE v_unresolved_count BIGINT DEFAULT 0;
    DECLARE v_invalid_center_count BIGINT DEFAULT 0;
    DECLARE v_remaining_count BIGINT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS tmp_production_record_center_backfill;
        RESIGNAL;
    END;

    DROP TEMPORARY TABLE IF EXISTS tmp_production_record_center_backfill;
    CREATE TEMPORARY TABLE tmp_production_record_center_backfill (
        record_id BIGINT NOT NULL PRIMARY KEY,
        record_no VARCHAR(100) NULL,
        record_center_id BIGINT NULL,
        order_center_id BIGINT NULL,
        device_center_id BIGINT NULL,
        user_center_id BIGINT NULL,
        resolved_center_id BIGINT NULL
    ) ENGINE=InnoDB;

    START TRANSACTION;

    -- 先用单条语句物化候选 ID；该语句完成后集合固定。
    -- 后续不再按候选谓词扫描，避免把未包含在锁集合中的记录带入快照。
    INSERT INTO tmp_production_record_center_backfill (record_id)
    SELECT pr.id
    FROM production_record pr
    WHERE pr.is_deleted = 0
      AND pr.producer_id IS NOT NULL
      AND (pr.processing_center_id IS NULL
           OR pr.processing_center_name IS NULL
           OR TRIM(pr.processing_center_name) = '');

    -- 精确锁定已经物化的记录；应用对这些流转卡的并发更新会等待本事务结束。
    SELECT pr.id
    FROM production_record pr
    JOIN tmp_production_record_center_backfill c ON c.record_id = pr.id
    FOR UPDATE;

    -- 等待锁期间可能已有事务先完成了修复或删除，重新校验后从固定集合剔除。
    DELETE c
    FROM tmp_production_record_center_backfill c
    LEFT JOIN production_record pr ON pr.id = c.record_id
    WHERE pr.id IS NULL
       OR pr.is_deleted <> 0
       OR pr.producer_id IS NULL
       OR NOT (pr.processing_center_id IS NULL
               OR pr.processing_center_name IS NULL
               OR TRIM(pr.processing_center_name) = '');

    -- 只从已锁定、已复核的固定 ID 集合构建来源快照。
    UPDATE tmp_production_record_center_backfill c
    JOIN production_record pr ON pr.id = c.record_id
    LEFT JOIN order_main om
           ON om.id = pr.order_id AND om.is_deleted = 0
    LEFT JOIN device d
           ON d.id = pr.print_device_id AND d.is_deleted = 0
    LEFT JOIN sys_user su
           ON su.id = pr.producer_id AND su.is_deleted = 0
    SET c.record_no = pr.record_no,
        c.record_center_id = pr.processing_center_id,
        c.order_center_id = om.center_id,
        c.device_center_id = d.center_id,
        c.user_center_id = su.center_id,
        c.resolved_center_id = COALESCE(pr.processing_center_id, om.center_id, d.center_id, su.center_id);

    SELECT COUNT(*) INTO v_candidate_count
    FROM tmp_production_record_center_backfill;

    -- 任意两个非空来源不一致都视为歧义，禁止自动修复。
    SELECT COUNT(*) INTO v_conflict_count
    FROM tmp_production_record_center_backfill c
    WHERE (c.record_center_id IS NOT NULL AND c.order_center_id IS NOT NULL
           AND c.record_center_id <> c.order_center_id)
       OR (c.record_center_id IS NOT NULL AND c.device_center_id IS NOT NULL
           AND c.record_center_id <> c.device_center_id)
       OR (c.record_center_id IS NOT NULL AND c.user_center_id IS NOT NULL
           AND c.record_center_id <> c.user_center_id)
       OR (c.order_center_id IS NOT NULL AND c.device_center_id IS NOT NULL
           AND c.order_center_id <> c.device_center_id)
       OR (c.order_center_id IS NOT NULL AND c.user_center_id IS NOT NULL
           AND c.order_center_id <> c.user_center_id)
       OR (c.device_center_id IS NOT NULL AND c.user_center_id IS NOT NULL
           AND c.device_center_id <> c.user_center_id);

    IF v_conflict_count > 0 THEN
        SELECT *
        FROM tmp_production_record_center_backfill
        WHERE (record_center_id IS NOT NULL AND order_center_id IS NOT NULL AND record_center_id <> order_center_id)
           OR (record_center_id IS NOT NULL AND device_center_id IS NOT NULL AND record_center_id <> device_center_id)
           OR (record_center_id IS NOT NULL AND user_center_id IS NOT NULL AND record_center_id <> user_center_id)
           OR (order_center_id IS NOT NULL AND device_center_id IS NOT NULL AND order_center_id <> device_center_id)
           OR (order_center_id IS NOT NULL AND user_center_id IS NOT NULL AND order_center_id <> user_center_id)
           OR (device_center_id IS NOT NULL AND user_center_id IS NOT NULL AND device_center_id <> user_center_id)
        ORDER BY record_id;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'production_record processing-center sources conflict; migration aborted';
    END IF;

    SELECT COUNT(*) INTO v_unresolved_count
    FROM tmp_production_record_center_backfill
    WHERE resolved_center_id IS NULL;

    IF v_unresolved_count > 0 THEN
        SELECT * FROM tmp_production_record_center_backfill
        WHERE resolved_center_id IS NULL ORDER BY record_id;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'claimed production_record has no resolvable processing center; migration aborted';
    END IF;

    SELECT COUNT(*) INTO v_invalid_center_count
    FROM tmp_production_record_center_backfill c
    LEFT JOIN processing_center pc
           ON pc.id = c.resolved_center_id
          AND pc.is_deleted = 0
          AND pc.status = 1
    WHERE pc.id IS NULL OR pc.center_name IS NULL OR TRIM(pc.center_name) = '';

    IF v_invalid_center_count > 0 THEN
        SELECT c.*
        FROM tmp_production_record_center_backfill c
        LEFT JOIN processing_center pc
               ON pc.id = c.resolved_center_id
              AND pc.is_deleted = 0
              AND pc.status = 1
        WHERE pc.id IS NULL OR pc.center_name IS NULL OR TRIM(pc.center_name) = ''
        ORDER BY c.record_id;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'resolved processing center is missing, disabled, deleted, or unnamed; migration aborted';
    END IF;

    UPDATE production_record pr
    JOIN tmp_production_record_center_backfill c ON c.record_id = pr.id
    JOIN processing_center pc
      ON pc.id = c.resolved_center_id
     AND pc.is_deleted = 0
     AND pc.status = 1
    SET pr.processing_center_id = COALESCE(pr.processing_center_id, c.resolved_center_id),
        pr.processing_center_name = pc.center_name,
        pr.update_time = CURRENT_TIMESTAMP
    WHERE pr.is_deleted = 0
      AND pr.producer_id IS NOT NULL
      AND (pr.processing_center_id IS NULL
           OR pr.processing_center_name IS NULL
           OR TRIM(pr.processing_center_name) = '')
      AND (pr.processing_center_id IS NULL OR pr.processing_center_id = c.resolved_center_id);

    SELECT COUNT(*) INTO v_remaining_count
    FROM tmp_production_record_center_backfill c
    JOIN production_record pr ON pr.id = c.record_id
    WHERE pr.processing_center_id IS NULL
       OR pr.processing_center_name IS NULL
       OR TRIM(pr.processing_center_name) = ''
       OR pr.processing_center_id <> c.resolved_center_id;

    IF v_remaining_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'production_record processing-center backfill verification failed; migration rolled back';
    END IF;

    COMMIT;

    SELECT v_candidate_count AS candidate_count,
           v_candidate_count - v_remaining_count AS verified_count,
           'processing-center backfill committed' AS migration_result;

    DROP TEMPORARY TABLE IF EXISTS tmp_production_record_center_backfill;
END $$

DELIMITER ;

CALL ygb_backfill_production_record_center();
DROP PROCEDURE IF EXISTS ygb_backfill_production_record_center;

-- 执行后审计：未接单记录允许继续为空；已接单有效记录不应再有缺失。
SELECT COUNT(*) AS claimed_records_missing_processing_center
FROM production_record
WHERE is_deleted = 0
  AND producer_id IS NOT NULL
  AND (processing_center_id IS NULL
       OR processing_center_name IS NULL
       OR TRIM(processing_center_name) = '');

SELECT NOW() AS migration_finish_time;
