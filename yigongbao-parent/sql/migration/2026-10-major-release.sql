-- ============================================================
-- 医工宝大版本线上数据库统一迁移脚本
-- 生成日期：2026-10-12
--
-- 本脚本合并以下 5 个迁移脚本：
-- 1. 历史订单列表归档与增量迁移基础表/权限
-- 2. 系统公告发布与强制确认弹窗
-- 3. 追加设计批次
-- 4. 三边封拉链袋热封参数配置
-- 5. 2026 年 10 月历史包装工序温度回填
--
-- 执行顺序及依赖：
-- A. 先创建业务表；
-- B. 再初始化菜单、按钮及角色权限；
-- C. 再更新 sys_config；
-- D. 最后更新 production_process 历史参数。
--
-- 注意：MySQL DDL 会隐式提交，本脚本不能使用覆盖全部语句的总事务。
-- 各数据更新脚本内部保留自己的事务边界。
-- 执行前请完成数据库备份，并在应用停机或维护窗口执行。
-- 本脚本不会自动重新生成历史流转卡 Excel；数据库更新后仍需调用
-- GET /api/production/record/{id}/excel 刷新历史流转卡文件。
-- ============================================================

SET NAMES utf8mb4;


-- ============================================================
-- [1/5] 2026-09-28-add-legacy-order-list.sql
-- ============================================================

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
-- 新建归档表时字段已在上方定义；此处对两个字段分别检查，兼容已执行不完整旧版脚本的环境。
SET @legacy_delivery_raw_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'legacy_order_list'
      AND COLUMN_NAME = 'delivery_raw'
);
SET @legacy_delivery_time_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'legacy_order_list'
      AND COLUMN_NAME = 'delivery_time'
);
SET @legacy_delivery_raw_sql := IF(
    @legacy_delivery_raw_exists = 0,
    'ALTER TABLE legacy_order_list ADD COLUMN delivery_raw VARCHAR(64) NULL COMMENT ''交付日期原文 od_delivery'' AFTER expected_delivery_raw',
    'SELECT 1'
);
PREPARE legacy_delivery_raw_stmt FROM @legacy_delivery_raw_sql;
EXECUTE legacy_delivery_raw_stmt;
DEALLOCATE PREPARE legacy_delivery_raw_stmt;

SET @legacy_delivery_time_sql := IF(
    @legacy_delivery_time_exists = 0,
    'ALTER TABLE legacy_order_list ADD COLUMN delivery_time DATETIME NULL COMMENT ''解析后的交付日期'' AFTER delivery_raw',
    'SELECT 1'
);
PREPARE legacy_delivery_time_stmt FROM @legacy_delivery_time_sql;
EXECUTE legacy_delivery_time_stmt;
DEALLOCATE PREPARE legacy_delivery_time_stmt;

-- ============================================================
-- [2/5] 2026-09-30-system-announcement.sql
-- ============================================================


-- 系统公告发布与强制确认弹窗
-- 执行前请确认目标数据库已完成备份。

CREATE TABLE IF NOT EXISTS system_announcement
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    title              VARCHAR(200) NOT NULL COMMENT '公告标题',
    content_html       MEDIUMTEXT   NOT NULL COMMENT '服务端清洗后的富文本HTML',
    content_text       TEXT         NOT NULL COMMENT '纯文本摘要',
    status             VARCHAR(20)  NOT NULL COMMENT 'DRAFT/PUBLISHED/REVOKED',
    target_type        VARCHAR(20)  NOT NULL COMMENT 'ALL/ROLE/USER',
    target_role_ids    JSON                  COMMENT '草稿阶段选择的角色ID列表',
    target_user_ids    JSON                  COMMENT '草稿阶段选择的用户ID列表',
    force_confirm      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否强制确认',
    target_count       INT          NOT NULL DEFAULT 0 COMMENT '发布时目标人数',
    acknowledged_count INT          NOT NULL DEFAULT 0 COMMENT '已确认人数缓存',
    revoked_count      INT          NOT NULL DEFAULT 0 COMMENT '撤回时未确认人数',
    published_at       DATETIME              COMMENT '发布时间',
    published_by       BIGINT                COMMENT '发布人',
    revoked_at         DATETIME              COMMENT '撤回时间',
    revoked_by         BIGINT                COMMENT '撤回人',
    create_time        DATETIME     NOT NULL,
    create_by          BIGINT       NOT NULL,
    update_time        DATETIME     NOT NULL,
    update_by          BIGINT,
    is_deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sa_status_time (status, published_at),
    KEY idx_sa_creator (create_by, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告主表';

CREATE TABLE IF NOT EXISTS system_announcement_recipient
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '接收记录ID',
    announcement_id    BIGINT      NOT NULL COMMENT '公告ID',
    user_id            BIGINT      NOT NULL COMMENT '接收用户ID',
    user_name_snapshot VARCHAR(100) COMMENT '用户名称快照',
    username_snapshot  VARCHAR(100) COMMENT '账号快照',
    role_snapshot      VARCHAR(500) COMMENT '角色名称快照',
    delivery_status    VARCHAR(20) NOT NULL COMMENT 'PENDING/ACKNOWLEDGED/REVOKED',
    acknowledged_at    DATETIME COMMENT '确认时间',
    revoked_at         DATETIME COMMENT '撤回时间',
    create_time        DATETIME    NOT NULL,
    update_time        DATETIME,
    create_by          BIGINT,
    update_by          BIGINT,
    is_deleted          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sa_recipient (announcement_id, user_id),
    KEY idx_sar_user_status (user_id, delivery_status, announcement_id),
    KEY idx_sar_announcement_status (announcement_id, delivery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告接收记录';

CREATE TABLE IF NOT EXISTS system_announcement_attachment
(
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '附件关联ID',
    announcement_id BIGINT       NOT NULL COMMENT '公告ID',
    file_id         VARCHAR(64) COMMENT '统一文件ID（file_detail.id）',
    file_name       VARCHAR(255) NOT NULL COMMENT '文件名',
    file_url        VARCHAR(1000) NOT NULL COMMENT '文件地址',
    file_type       VARCHAR(100) COMMENT 'MIME类型',
    file_size       BIGINT COMMENT '字节数',
    sort            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    create_time     DATETIME     NOT NULL,
    update_time     DATETIME     NULL,
    create_by       BIGINT       NOT NULL,
    update_by       BIGINT       NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_saa_announcement (announcement_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告附件';

CREATE TABLE IF NOT EXISTS system_announcement_audit_log
(
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    announcement_id BIGINT      NOT NULL COMMENT '公告ID',
    operation_type  VARCHAR(20) NOT NULL COMMENT 'PUBLISH/REVOKE',
    operator_id     BIGINT      NOT NULL COMMENT '操作人',
    operator_name   VARCHAR(100) COMMENT '操作人名称快照',
    before_status   VARCHAR(20) COMMENT '操作前状态',
    after_status    VARCHAR(20) NOT NULL COMMENT '操作后状态',
    target_type     VARCHAR(20) COMMENT '目标类型',
    target_count    INT COMMENT '目标人数',
    client_ip       VARCHAR(64) COMMENT '客户端IP',
    remark          VARCHAR(500) COMMENT '备注',
    operation_time  DATETIME    NOT NULL COMMENT '操作时间',
    create_time     DATETIME    NOT NULL,
    update_time     DATETIME,
    create_by       BIGINT,
    update_by       BIGINT,
    is_deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_saal_announcement_time (announcement_id, operation_time),
    KEY idx_saal_operator_time (operator_id, operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告审计日志';

-- 管理端菜单与按钮权限。使用资源编码幂等，避免重复执行迁移产生重复菜单。
INSERT INTO sys_resource (parent_id, resource_name, resource_code, resource_type, icon, path, component, sort, visible, status, create_time, update_time, is_deleted)
SELECT id, '公告发布', 'notification:announcement', 2, '&#xe668;', '/announcement', 'system/announcement.vue', 4, 1, 1, NOW(), NOW(), 0
FROM sys_resource p
WHERE p.resource_code = 'System' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code = 'notification:announcement' AND x.is_deleted = 0);

INSERT INTO sys_resource (parent_id, resource_name, resource_code, resource_type, sort, visible, status, create_time, update_time, is_deleted)
SELECT p.id, v.resource_name, v.resource_code, 3, v.sort, 1, 1, NOW(), NOW(), 0
FROM sys_resource p
JOIN (
    SELECT '公告列表' resource_name, 'notification:announcement:list' resource_code, 1 sort
    UNION ALL SELECT '查看公告', 'notification:announcement:view', 2
    UNION ALL SELECT '新建公告', 'notification:announcement:create', 3
    UNION ALL SELECT '编辑草稿', 'notification:announcement:update', 4
    UNION ALL SELECT '预览公告', 'notification:announcement:preview', 5
    UNION ALL SELECT '发布公告', 'notification:announcement:publish', 6
    UNION ALL SELECT '撤回公告', 'notification:announcement:revoke', 7
    UNION ALL SELECT '公告统计', 'notification:announcement:statistics', 8
) v
WHERE p.resource_code = 'notification:announcement' AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_resource x WHERE x.resource_code = v.resource_code AND x.is_deleted = 0);

-- 当前阶段仅授予超级管理员（role_code=admin）公告管理权限。
INSERT INTO sys_role_resource (role_id, resource_id)
SELECT r.id, res.id
FROM sys_role r
JOIN sys_resource res
  ON res.resource_code IN (
      'notification:announcement',
      'notification:announcement:list',
      'notification:announcement:view',
      'notification:announcement:create',
      'notification:announcement:update',
      'notification:announcement:preview',
      'notification:announcement:publish',
      'notification:announcement:revoke',
      'notification:announcement:statistics'
  )
 AND res.is_deleted = 0
WHERE r.role_code = 'admin'
  AND r.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_resource rr
      WHERE rr.role_id = r.id AND rr.resource_id = res.id
  );

-- 初始化系统公告示例草稿，帮助管理员了解标题、加粗、列表、链接和附件说明的编写方式。
-- 该公告来自当前数据库中的真实示例记录：状态为草稿、目标为指定账户 user_id=1，当前预计目标人数为 1。
-- 示例本身未发布，因此不写入接收人、附件和发布/撤回审计日志；后续管理员可在后台编辑附件后预览并发布。
INSERT INTO system_announcement
(
    title,
    content_html,
    content_text,
    status,
    target_type,
    target_role_ids,
    target_user_ids,
    force_confirm,
    target_count,
    acknowledged_count,
    revoked_count,
    published_at,
    published_by,
    revoked_at,
    revoked_by,
    create_time,
    create_by,
    update_time,
    update_by,
    is_deleted
)
SELECT
    '【示例——请勿对外发布】系统公告示例',
    '<b>各位用户：</b><div>本公告用于介绍如何新建和发布系统公告，并演示公告正文中的常用内容格式。</div><div><br></div><div><b>一、新建公告操作步骤</b></div><div><ul><li>1. 进入“系统管理—公告发布”页面。</li><li>2. 点击“新建公告”按钮。</li><li>3. 填写公告标题。</li><li>4. 选择推送范围，可选择全员、按角色或指定账户。</li><li>5. 编辑公告正文。</li><li>6. 根据需要添加附件。</li><li>7. 点击“预览”检查公告显示效果。</li><li>8. 确认无误后保存草稿并发布公告。</li><li><br></li></ul></div><div><b>二、正文加粗示例</b></div><div>以下内容可以使用加粗效果突出显示：</div><div><b>重要提醒：公告发布后不可修改，只能进行撤回操作。</b></div><div><b>请在发布前认真检查公告标题、正文内容、推送范围和附件信息。</b></div><div><b><br></b></div><div><b>三、列表内容示例</b></div><div>无序列表适合展示并列事项：</div><div><ul><li>• 公告标题应简洁明确；</li><li>• 公告内容应描述清楚事项、影响范围和处理方式；</li><li>• 发布前应通过预览确认排版效果；</li><li>• 公告发布后需要接收人点击“我已知晓”。</li></ul></div><div>有序列表适合展示操作步骤：</div><div><ul><li>1. 新建公告并填写基础信息；</li><li>2. 编辑正文并设置需要强调的内容；</li><li>3. 添加相关链接和附件；</li><li>4. 预览公告；</li><li>5. 保存草稿或发布公告。</li><li><br></li></ul></div><div><b>四、链接内容示例</b></div><div>如需查看详细操作说明，请访问系统&nbsp;<a href="http://www.baidu.com">帮助中心</a>&nbsp;。</div><div>系统公告使用指南：</div><div><a href="https://example.com/help/announcement">https://example.com/help/announcement</a></div><div>如需查看系统运行状态，请访问：</div><div><a href="https://example.com/status">https://example.com/status</a></div><div>链接通常用于跳转到操作指南、业务系统、政策文件或其他相关页面。</div><div><br></div><div><b>五、附件内容示例（未上传下列文件，仅说明）</b></div><div>公告可以根据需要添加相关附件，例如：</div><div>公告操作手册.pdf</div><div>系统升级说明.docx</div><div>用户使用示例.xlsx</div><div>附件上传完成后，接收人可以在公告内容下方查看附件名称、文件大小并点击下载。</div><div><br></div><div><b>六、注意事项</b></div><div><ul><li>1. 公告发布后正文、推送范围和附件内容均不能修改。</li><li>2. 撤回公告后，已经打开的公告弹窗会自动关闭。</li><li>3. 同一时间只显示一条公告。</li><li>4. 如果用户有多条未确认公告，系统会按照发布时间倒序逐条显示。</li><li>5. 接收人必须点击“我已知晓”后，公告才会标记为已确认。</li></ul></div><div>感谢您的使用。</div>',
    '各位用户： 本公告用于介绍如何新建和发布系统公告，并演示公告正文中的常用内容格式。 一、新建公告操作步骤 1. 进入“系统管理—公告发布”页面。 2. 点击“新建公告”按钮。 3. 填写公告标题。 4. 选择推送范围，可选择全员、按角色或指定账户。 5. 编辑公告正文。 6. 根据需要添加附件。 7. 点击“预览”检查公告显示效果。 8. 确认无误后保存草稿并发布公告。 二、正文加粗示例 以下内容可以使用加粗效果突出显示： 重要提醒：公告发布后不可修改，只能进行撤回操作。 请在发布前认真检查公告标题、正文内容、推送范围和附件信息。 三、列表内容示例 无序列表适合展示并列事项： • 公告标题应简洁明确； • 公告内容应描述清楚事项、影响范围和处理方式； • 发布前应通过预览确认排版效果； • 公告发布后需要接收人点击“我已知晓”。 有序列表适合展示操作步骤： 1. 新建公告并填写基础信息； 2. 编辑正文并设置需要强调的内容； 3. 添加相关链接和附件； 4. 预览公告； 5. 保存草稿或发布公告。 四、链接内容示例 如需查看详细操作说明，请访问系统 帮助中心 。 系统公告使用指南： https://example.com/help/announcement 如需查看系统运行状态，请访问： https://example.com/status 链接通常用于跳转到操作指南、业务系统、政策文件或其他相关页面。 五、附件内容示例（未上传下列文件，仅说明） 公告可以根据需要添加相关附件，例如： 公告操作手册.pdf 系统升级说明.docx 用户使用示例.xlsx 附件上传完成后，接收人可以在公告内容下方查看附件名称、文件大小并点击下载。 六、注意事项 1. 公告发布后正文、推送范围和附件内容均不能修改。 2. 撤回公告后，已经打开的公告弹窗会自动关闭。 3. 同一时间只显示一条公告。 4. 如果用户有多条未确认公告，系统会按照发布时间倒序逐条显示。 5. 接收人必须点击“我已知晓”后，公告才会标记为已确认。 感谢您的使用。',
    'DRAFT',
    'USER',
    JSON_ARRAY(),
    JSON_ARRAY(1),
    1,
    1,
    0,
    0,
    NULL,
    NULL,
    NULL,
    NULL,
    NOW(),
    1,
    NOW(),
    1,
    0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM system_announcement
    WHERE title = '【示例——请勿对外发布】系统公告示例'
      AND is_deleted = 0
);

-- ============================================================
-- [3/5] 2026-10-10-design-append-batch.sql
-- ============================================================


-- 追加设计批次功能统一数据库变更脚本
--
-- 本脚本包含：
-- 1. 新增 design_package_batch 追加设计批次表；
-- 2. design_package、production_record 增加批次关联字段和索引；
-- 3. 补充 design_package_batch 所需的通用审计字段。
--
-- 说明：
-- - 本脚本只负责结构和权限初始化，不回填历史数据的 batch_id；历史数据保持 NULL，原有按 order_id/package_id 的查询不受影响。
-- - DDL 在 MySQL 中会产生隐式提交，因此不使用一个覆盖全部语句的事务；权限数据插入使用幂等条件。
-- - 执行前应在目标环境完成备份，并在变更窗口验证表结构和权限结果。

CREATE TABLE IF NOT EXISTS design_package_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '追加设计批次编号',
    batch_type VARCHAR(32) NOT NULL DEFAULT 'ADDITIONAL' COMMENT '批次类型：NORMAL=正常设计，ADDITIONAL=追加设计',
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADING' COMMENT '批次状态：UPLOADING/PRINT_INFO_EDITING/COMPLETED/CANCELLED',
    source_order_status INT NULL COMMENT '创建批次时订单状态',
    created_by BIGINT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    completed_time DATETIME NULL COMMENT '完成时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_by BIGINT NULL COMMENT '创建人ID（通用审计字段）',
    update_by BIGINT NULL COMMENT '更新人ID（通用审计字段）',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_design_package_batch_no (batch_no),
    KEY idx_design_package_batch_order_status (order_id, status, is_deleted),
    KEY idx_design_package_batch_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设计数据包追加批次';

SET @sql_add_design_package_batch_version := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'version'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号'''
);
PREPARE stmt_add_design_package_batch_version FROM @sql_add_design_package_batch_version;
EXECUTE stmt_add_design_package_batch_version;
DEALLOCATE PREPARE stmt_add_design_package_batch_version;

SET @sql_add_design_package_batch_create_by := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'create_by'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN create_by BIGINT NULL COMMENT ''创建人ID（通用审计字段）'''
);
PREPARE stmt_add_design_package_batch_create_by FROM @sql_add_design_package_batch_create_by;
EXECUTE stmt_add_design_package_batch_create_by;
DEALLOCATE PREPARE stmt_add_design_package_batch_create_by;

SET @sql_add_design_package_batch_update_by := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package_batch'
          AND COLUMN_NAME = 'update_by'
    ),
    'SELECT 1',
    'ALTER TABLE design_package_batch ADD COLUMN update_by BIGINT NULL COMMENT ''更新人ID（通用审计字段）'''
);
PREPARE stmt_add_design_package_batch_update_by FROM @sql_add_design_package_batch_update_by;
EXECUTE stmt_add_design_package_batch_update_by;
DEALLOCATE PREPARE stmt_add_design_package_batch_update_by;

-- 为原有设计数据包增加批次关联。历史数据保留 NULL，新增追加数据包写入对应 batch_id。
SET @sql_add_design_package_batch_id := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package'
          AND COLUMN_NAME = 'batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE design_package ADD COLUMN batch_id BIGINT NULL COMMENT ''追加设计批次ID'''
);
PREPARE stmt_add_design_package_batch_id FROM @sql_add_design_package_batch_id;
EXECUTE stmt_add_design_package_batch_id;
DEALLOCATE PREPARE stmt_add_design_package_batch_id;

SET @sql_add_design_package_batch_index := IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'design_package'
          AND INDEX_NAME = 'idx_design_package_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE design_package ADD KEY idx_design_package_batch_id (batch_id)'
);
PREPARE stmt_add_design_package_batch_index FROM @sql_add_design_package_batch_index;
EXECUTE stmt_add_design_package_batch_index;
DEALLOCATE PREPARE stmt_add_design_package_batch_index;

-- 为生产流转卡增加批次关联。历史生产记录保留 NULL，追加批次新记录写入对应 batch_id。
SET @sql_add_production_record_batch_id := IF(
    EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'production_record'
          AND COLUMN_NAME = 'batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE production_record ADD COLUMN batch_id BIGINT NULL COMMENT ''追加设计批次ID'''
);
PREPARE stmt_add_production_record_batch_id FROM @sql_add_production_record_batch_id;
EXECUTE stmt_add_production_record_batch_id;
DEALLOCATE PREPARE stmt_add_production_record_batch_id;

SET @sql_add_production_record_batch_index := IF(
    EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'production_record'
          AND INDEX_NAME = 'idx_production_record_batch_id'
    ),
    'SELECT 1',
    'ALTER TABLE production_record ADD KEY idx_production_record_batch_id (batch_id)'
);
PREPARE stmt_add_production_record_batch_index FROM @sql_add_production_record_batch_index;
EXECUTE stmt_add_production_record_batch_index;
DEALLOCATE PREPARE stmt_add_production_record_batch_index;

-- 发布后建议执行以下只读校验：
-- SELECT COUNT(*) FROM design_package_batch;
-- SHOW COLUMNS FROM design_package_batch;

-- ============================================================
-- [4/5] migration-production-process-params-zip-bag-seal-20260930.sql
-- ============================================================


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

-- ============================================================
-- [5/5] migration-production-process-params-zip-bag-seal-temperature-20261012.sql
-- ============================================================


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
