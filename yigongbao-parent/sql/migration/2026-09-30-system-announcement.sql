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
