-- 备份候选事件号分配器；通过单行 UPDATE 的 InnoDB 行锁保证多实例并发唯一。
CREATE TABLE IF NOT EXISTS backup_candidate_event_sequence (
    id TINYINT NOT NULL COMMENT '固定单行ID',
    next_event_id BIGINT NOT NULL COMMENT '最近已分配事件号',
    PRIMARY KEY (id),
    CONSTRAINT chk_backup_candidate_event_sequence_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='备份候选事件号分配器';

INSERT INTO backup_candidate_event_sequence (id, next_event_id)
SELECT 1, COALESCE(MAX(event_id), 0) FROM order_backup_candidate
ON DUPLICATE KEY UPDATE next_event_id = GREATEST(next_event_id, VALUES(next_event_id));

-- 验证：SELECT next_event_id FROM backup_candidate_event_sequence WHERE id = 1;
