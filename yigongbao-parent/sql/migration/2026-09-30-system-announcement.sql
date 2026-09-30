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
    create_by       BIGINT       NOT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_saa_announcement (announcement_id, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告附件';

-- file_detail.id 为字符串雪花 ID，兼容已执行旧版脚本的数据库
ALTER TABLE system_announcement_attachment
    MODIFY COLUMN file_id VARCHAR(64) NULL COMMENT '统一文件ID（file_detail.id）';

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
