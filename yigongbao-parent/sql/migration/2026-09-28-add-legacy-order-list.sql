-- 旧医工宝历史订单列表归档表、增量迁移任务表及菜单权限
CREATE TABLE IF NOT EXISTS legacy_order_list (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '归档记录主键',
    source_system VARCHAR(32) NOT NULL DEFAULT 'OLD_YIGONGBAO' COMMENT '来源系统标识',
    source_order_id BIGINT NOT NULL COMMENT '旧系统订单主键 od_id',
    source_order_code VARCHAR(64) NULL COMMENT '旧系统订单号 od_num',
    status_raw VARCHAR(64) NULL COMMENT '旧系统订单状态原文 od_status，不转换',
    business_type_raw VARCHAR(128) NULL COMMENT '旧系统业务类型原文 od_orderType',
    print_raw VARCHAR(32) NULL COMMENT '旧系统打印要求原文 od_print',
    legacy_dept_name VARCHAR(128) NULL COMMENT '旧系统所属部门 od_dept',
    operator_name VARCHAR(128) NULL COMMENT '业务员姓名 od_clerkname',
    operator_phone VARCHAR(64) NULL COMMENT '业务员电话 od_clerkTel',
    hospital_name VARCHAR(255) NULL COMMENT '医院名称 od_hospital',
    area_name VARCHAR(128) NULL COMMENT '地区名称 od_dist',
    hospital_dept_name VARCHAR(128) NULL COMMENT '医院科室 od_office',
    doctor_name VARCHAR(128) NULL COMMENT '医生姓名 od_doctor/od_docname',
    doctor_phone VARCHAR(64) NULL COMMENT '医生电话 od_docTel/od_doctel',
    patient_name VARCHAR(255) NULL COMMENT '患者姓名 od_patient',
    patient_age_raw VARCHAR(32) NULL COMMENT '患者年龄原文 od_patientAge',
    patient_gender_raw VARCHAR(32) NULL COMMENT '患者性别原文 od_patientSex',
    postal_flag TINYINT NULL COMMENT '是否邮寄：1是、0否，来源 jy_instruct.inst_mail',
    postal_address_display VARCHAR(1000) NULL COMMENT '邮寄地址，来源 jy_instruct.inst_mailaddress',
    designer_name VARCHAR(128) NULL COMMENT '设计师姓名 od_designername',
    expected_delivery_raw VARCHAR(64) NULL COMMENT '预计交付日期原文 od_preDelivery',
    expected_delivery_time DATETIME NULL COMMENT '解析后的预计交付日期',
    delivery_raw VARCHAR(64) NULL COMMENT '交付日期原文 od_delivery',
    delivery_time DATETIME NULL COMMENT '解析后的交付日期',
    estimated_cost_raw VARCHAR(128) NULL COMMENT '收费原文，严格映射旧系统 od_fee',
    estimated_cost_number DECIMAL(18,2) NULL COMMENT '收费原文可解析时的数值',
    data_evaluation_opinion VARCHAR(500) NULL COMMENT '数据评估意见 od_assess',
    rebuild_project_summary VARCHAR(1000) NULL COMMENT '重建项目摘要，由旧项目字段组装',
    rebuild_project_json JSON NULL COMMENT '重建项目原始结构化快照，预留详情使用',
    design_start_raw VARCHAR(64) NULL COMMENT '设计开始时间原文 ds_time',
    design_start_time DATETIME NULL COMMENT '解析后的设计开始时间',
    design_submit_raw VARCHAR(64) NULL COMMENT '设计提交时间原文 dc_time',
    design_submit_time DATETIME NULL COMMENT '解析后的设计提交时间',
    production_start_raw VARCHAR(64) NULL COMMENT '生产开始时间原文 ps_time',
    production_start_time DATETIME NULL COMMENT '解析后的生产开始时间',
    production_end_raw VARCHAR(64) NULL COMMENT '生产结束时间原文 pc_time',
    production_end_time DATETIME NULL COMMENT '解析后的生产结束时间',
    create_time_raw VARCHAR(64) NULL COMMENT '订单创建时间原文 od_createtime',
    source_create_time DATETIME NULL COMMENT '解析后的旧订单创建时间',
    source_row_hash CHAR(64) NULL COMMENT '旧订单行内容 SHA-256 哈希，用于增量幂等',
    source_snapshot_at DATETIME NULL COMMENT '本次迁移读取快照时间',
    mapping_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '字段映射规则版本',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '归档记录更新时间',
    create_by BIGINT NULL COMMENT '创建人 ID',
    update_by BIGINT NULL COMMENT '更新人 ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_legacy_order_source (source_system, source_order_id),
    KEY idx_legacy_order_code (source_order_code),
    KEY idx_legacy_order_status (status_raw),
    KEY idx_legacy_order_business_type (business_type_raw),
    KEY idx_legacy_order_hospital (hospital_name),
    KEY idx_legacy_order_doctor (doctor_name),
    KEY idx_legacy_order_patient (patient_name),
    KEY idx_legacy_order_create_time (source_create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旧医工宝历史订单列表归档表';

CREATE TABLE IF NOT EXISTS legacy_migration_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '迁移任务主键',
    task_type VARCHAR(32) NOT NULL DEFAULT 'INCREMENTAL' COMMENT '任务类型',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED',
    requested_by BIGINT NULL COMMENT '发起人用户 ID',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务请求时间',
    started_at DATETIME NULL COMMENT '任务开始时间',
    heartbeat_at DATETIME NULL COMMENT '任务最近心跳时间',
    finished_at DATETIME NULL COMMENT '任务结束时间',
    source_snapshot_at DATETIME NULL COMMENT '旧库读取快照时间',
    total_read INT NOT NULL DEFAULT 0 COMMENT '读取旧订单总数',
    inserted_count INT NOT NULL DEFAULT 0 COMMENT '新增归档记录数',
    updated_count INT NOT NULL DEFAULT 0 COMMENT '更新归档记录数',
    skipped_count INT NOT NULL DEFAULT 0 COMMENT '哈希未变化跳过数',
    error_count INT NOT NULL DEFAULT 0 COMMENT '处理失败记录数',
    error_message VARCHAR(2000) NULL COMMENT '任务级错误摘要',
    mapping_version VARCHAR(32) NOT NULL DEFAULT 'v1' COMMENT '字段映射规则版本',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务记录创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务记录更新时间',
    create_by BIGINT NULL COMMENT '创建人 ID',
    update_by BIGINT NULL COMMENT '更新人 ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    KEY idx_legacy_migration_status (status),
    KEY idx_legacy_migration_requested_at (requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旧医工宝历史订单迁移任务表';

CREATE TABLE IF NOT EXISTS legacy_migration_error (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '迁移错误主键',
    task_id BIGINT NOT NULL COMMENT '迁移任务 ID',
    source_order_id BIGINT NULL COMMENT '旧订单主键 od_id',
    source_order_code VARCHAR(64) NULL COMMENT '旧订单号 od_num',
    error_type VARCHAR(64) NOT NULL COMMENT '错误类型',
    error_message VARCHAR(2000) NOT NULL COMMENT '错误消息',
    raw_snapshot TEXT NULL COMMENT '失败行原始快照',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '错误记录时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '错误记录更新时间',
    create_by BIGINT NULL COMMENT '创建人 ID',
    update_by BIGINT NULL COMMENT '更新人 ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    KEY idx_legacy_migration_error_task (task_id),
    KEY idx_legacy_migration_error_order (source_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旧医工宝历史订单迁移错误明细';

SET @business_id := (SELECT id FROM sys_resource WHERE resource_code = 'Business' AND is_deleted = 0 LIMIT 1);
SET @legacy_menu_id := (SELECT id FROM sys_resource WHERE resource_code = 'LegacyOrder' AND is_deleted = 0 LIMIT 1);
INSERT INTO sys_resource (parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status, is_deleted)
SELECT @business_id, '历史订单', 'LegacyOrder', 2, '&#xeb49;', '/legacyOrder', 'business/legacyOrder.vue', 99, 1, 1, 0
WHERE @business_id IS NOT NULL AND @legacy_menu_id IS NULL;
SET @legacy_menu_id := (SELECT id FROM sys_resource WHERE resource_code = 'LegacyOrder' AND is_deleted = 0 LIMIT 1);
INSERT INTO sys_resource (parent_id, resource_name, resource_code, resource_type, sort, visible, status, is_deleted)
SELECT @legacy_menu_id, '增量迁移', 'legacyOrder:Migrate', 3, 1, 1, 1, 0
WHERE @legacy_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_resource WHERE resource_code = 'legacyOrder:Migrate' AND is_deleted = 0);

INSERT IGNORE INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res ON res.resource_code IN ('LegacyOrder', 'legacyOrder:Migrate') AND res.is_deleted = 0
WHERE r.role_code IN ('admin', 'company-admin', 'designer-manager') AND r.is_deleted = 0;

-- 为已存在的历史订单归档表补充旧系统实际交付日期 od_delivery。
-- 新建归档表时字段已在上方定义；此处保留幂等补字段逻辑，兼容已执行旧版脚本的环境。
SET @legacy_delivery_column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'legacy_order_list' AND COLUMN_NAME = 'delivery_raw'
);
SET @legacy_delivery_sql := IF(@legacy_delivery_column_exists = 0,
    'ALTER TABLE legacy_order_list ADD COLUMN delivery_raw VARCHAR(64) NULL COMMENT ''交付日期原文 od_delivery'', ADD COLUMN delivery_time DATETIME NULL COMMENT ''解析后的交付日期'' AFTER expected_delivery_time',
    'SELECT 1'
);
PREPARE legacy_delivery_stmt FROM @legacy_delivery_sql;
EXECUTE legacy_delivery_stmt;
DEALLOCATE PREPARE legacy_delivery_stmt;
