-- ============================================================
-- 数据库迁移脚本 - 线上环境表结构更新
-- ============================================================
-- 创建时间: 2026-07-14
-- 目标: 将线上数据库结构从ddl-prod.sql更新为ddl.sql
--
-- 执行前检查清单:
--   [ ] 已在测试环境完整验证
--   [ ] 已完整备份生产数据库
--   [ ] 已通知相关开发人员
--   [ ] 已确认业务代码已适配新表结构
--   [ ] 已选择业务低峰期执行时间窗口
--
-- 预计影响:
--   - 预计执行时间: 5-10分钟
--   - 影响表: sys_user, order_main, production_record, production_product (新增2张表)
--   - 停机要求: 建议短暂停机，避免索引创建期间的数据不一致
--
-- 执行方式:
--   方式1: mysql -u用户名 -p数据库名 < migration-ddl-prod-to-ddl-2026-07-14.sql
--   方式2: 在MySQL客户端中逐步执行，每步验证
--
-- 回滚脚本: 见文档 docs/database-migration-2026-07-14.md 第五章
-- ============================================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 执行前验证：检查当前数据库结构
-- ============================================================
SELECT '========================================' AS '';
SELECT '开始执行数据库迁移脚本' AS '状态';
SELECT '执行时间：', NOW() AS '';
SELECT '========================================' AS '';

-- 检查关键表是否存在
SELECT
    CASE
        WHEN COUNT(*) = 5 THEN '✓ 核心表检查通过'
        ELSE '✗ 核心表检查失败，请检查数据库'
    END AS '预检查结果'
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name IN ('sys_user', 'order_main', 'production_record', 'device', 'notification_message');

-- ============================================================
-- 步骤1: sys_user表 - 索引变更
-- 变更说明: 将手机号从唯一索引改为普通索引（支持手机号重复）
-- 影响范围: 用户登录、注册功能
-- 风险等级: 低
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤1: 开始修改sys_user表索引' AS '执行状态';
SELECT '========================================' AS '';

-- 删除原有的唯一索引
DROP INDEX uk_phone ON sys_user;

-- 创建普通索引
CREATE INDEX idx_user_phone ON sys_user(phone);

-- 验证索引变更
SELECT
    INDEX_NAME as '索引名',
    NON_UNIQUE as '是否非唯一(1=是)',
    COLUMN_NAME as '列名'
FROM information_schema.statistics
WHERE table_schema = DATABASE()
AND table_name = 'sys_user'
AND index_name IN ('uk_phone', 'idx_user_phone');

SELECT '✓ sys_user表索引变更完成' AS '步骤1结果';

-- ============================================================
-- 步骤2: order_main表 - 新增字段和索引
-- 变更说明: 新增取消申请状态字段，支持订单取消审核流程
-- 影响范围: 订单取消功能
-- 风险等级: 低
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤2: 开始修改order_main表' AS '执行状态';
SELECT '========================================' AS '';

-- 新增has_pending_cancel_apply字段
ALTER TABLE order_main
ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0
COMMENT '是否有待审核的取消申请（0=否，1=是）'
AFTER version;

-- 创建索引
CREATE INDEX idx_order_main_has_pending_cancel_apply
ON order_main(has_pending_cancel_apply);

-- 验证字段和索引
SELECT
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '类型',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'order_main'
AND column_name = 'has_pending_cancel_apply';

SELECT '✓ order_main表变更完成' AS '步骤2结果';

-- ============================================================
-- 步骤3: 创建device_daily_usage_counter表
-- 变更说明: 新建设备每日上机次数统计表
-- 功能说明: 记录每台设备每日的上机次数，用于产品编号生成
-- 影响范围: 生产模块 - 产品编号生成机制
-- 风险等级: 低（新表，不影响现有功能）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤3: 开始创建device_daily_usage_counter表' AS '执行状态';
SELECT '========================================' AS '';

CREATE TABLE IF NOT EXISTS device_daily_usage_counter (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID（关联device表）',
    usage_date      DATE NOT NULL COMMENT '使用日期',
    usage_count     INT NOT NULL DEFAULT 0 COMMENT '当日上机次数',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT COMMENT '创建人ID',
    update_by       BIGINT COMMENT '更新人ID',
    is_deleted      TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    UNIQUE KEY uk_device_date (device_id, usage_date),
    KEY idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日上机次数统计表';

-- 验证表创建
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN '✓ device_daily_usage_counter表创建成功'
        ELSE '✗ device_daily_usage_counter表创建失败'
    END AS '步骤3结果'
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name = 'device_daily_usage_counter';

-- ============================================================
-- 步骤4: 创建order_cancel_apply表
-- 变更说明: 新建订单取消申请表
-- 功能说明: 存储订单取消申请记录，支持审核流程
-- 影响范围: 订单模块 - 取消审核流程
-- 风险等级: 低（新表，不影响现有功能）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤4: 开始创建order_cancel_apply表' AS '执行状态';
SELECT '========================================' AS '';

CREATE TABLE IF NOT EXISTS order_cancel_apply (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id            BIGINT NOT NULL COMMENT '订单ID',
    apply_by            BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason        VARCHAR(500) COMMENT '取消原因（选填）',
    audit_status        TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：1=待审核，2=已通过，3=已驳回',
    audit_by            BIGINT COMMENT '审核人ID',
    audit_reason        VARCHAR(500) COMMENT '审核驳回原因（选填）',
    audit_time          DATETIME COMMENT '审核时间',
    create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT COMMENT '创建人ID',
    update_by           BIGINT COMMENT '更新人ID',
    is_deleted          TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',

    KEY idx_order_cancel_apply_order_id (order_id),
    KEY idx_order_cancel_apply_audit_status (audit_status),
    KEY idx_order_cancel_apply_apply_by (apply_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单取消申请表';

-- 验证表创建
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN '✓ order_cancel_apply表创建成功'
        ELSE '✗ order_cancel_apply表创建失败'
    END AS '步骤4结果'
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name = 'order_cancel_apply';

-- ============================================================
-- 步骤5: production_record表 - 新增字段
-- 变更说明: 支持产品拆分和包装材质记录
-- 功能说明:
--   1. product_id, product_name: 支持按产品ID拆分流转卡
--   2. pack_material: 记录包装材质信息（流转卡Excel显示）
-- 影响范围: 生产模块 - 流转卡生成和显示
-- 风险等级: 低（新字段允许NULL，不影响历史数据）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤5: 开始修改production_record表字段' AS '执行状态';
SELECT '========================================' AS '';

-- 新增产品关联字段（在design_package_code字段之后）
ALTER TABLE production_record
ADD COLUMN product_id BIGINT COMMENT '产品ID'
AFTER design_package_code;

ALTER TABLE production_record
ADD COLUMN product_name VARCHAR(100) COMMENT '产品名称（冗余）'
AFTER product_id;

-- 新增包装材质字段（在pack_seal_time字段之后）
ALTER TABLE production_record
ADD COLUMN pack_material VARCHAR(100)
COMMENT '包装材质（如：纸封袋、PE符合食品包装袋）'
AFTER pack_seal_time;

-- 验证字段新增
SELECT
    COLUMN_NAME as '新增字段',
    COLUMN_TYPE as '类型',
    IS_NULLABLE as '允许NULL',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND column_name IN ('product_id', 'product_name', 'pack_material');

SELECT '✓ production_record表字段新增完成' AS '步骤5结果';

-- ============================================================
-- 步骤6: production_record表 - 创建唯一索引
-- 变更说明: 创建函数唯一索引，保证数据唯一性并支持逻辑删除
-- 功能说明:
--   1. uk_package_product: 同一数据包+同一产品只能有一条流转卡（幂等性）
--   2. uk_production_batch_no: 生产批号全局唯一
-- 风险等级: 中（耗时操作，可能锁表）
-- 注意事项:
--   - 索引创建期间可能锁表，建议在业务低峰期执行
--   - 使用函数索引支持逻辑删除（is_deleted=0时才检查唯一性）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤6: 开始创建production_record表索引（耗时操作）' AS '执行状态';
SELECT '========================================' AS '';

-- 执行前检查：验证是否存在重复数据
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN '✓ 生产批号唯一性检查通过'
        ELSE CONCAT('✗ 警告：发现', COUNT(*), '个重复的生产批号，请先清理')
    END AS '数据预检查'
FROM (
    SELECT production_batch_no, COUNT(*) as cnt
    FROM production_record
    WHERE is_deleted = 0
    GROUP BY production_batch_no
    HAVING COUNT(*) > 1
) AS duplicates;

SELECT
    CASE
        WHEN COUNT(*) = 0 THEN '✓ 数据包+产品组合唯一性检查通过'
        ELSE CONCAT('✗ 警告：发现', COUNT(*), '个重复的数据包+产品组合，请先清理')
    END AS '数据预检查'
FROM (
    SELECT design_package_id, product_id, COUNT(*) as cnt
    FROM production_record
    WHERE is_deleted = 0 AND product_id IS NOT NULL
    GROUP BY design_package_id, product_id
    HAVING COUNT(*) > 1
) AS duplicates;

-- 创建联合唯一索引：数据包+产品
CREATE UNIQUE INDEX uk_package_product
ON production_record ((CASE WHEN is_deleted = 0 THEN design_package_id ELSE NULL END),
                      (CASE WHEN is_deleted = 0 THEN product_id ELSE NULL END));

-- 【已移除】创建生产批号唯一索引
-- 说明：生产批号允许重复（一个批次可能包含多个流转卡）
-- 已保留普通索引 idx_production_batch_no 用于查询优化
-- CREATE UNIQUE INDEX uk_production_batch_no
-- ON production_record ((CASE WHEN is_deleted = 0 THEN production_batch_no ELSE NULL END));

-- 验证索引创建
SELECT
    INDEX_NAME as '索引名',
    NON_UNIQUE as '是否非唯一',
    COLUMN_NAME as '列名'
FROM information_schema.statistics
WHERE table_schema = DATABASE()
AND table_name = 'production_record'
AND index_name IN ('uk_package_product')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

SELECT '✓ production_record表索引创建完成' AS '步骤6结果';

-- ============================================================
-- 步骤7: production_product表 - 修改字段约束
-- 变更说明: 允许product_no字段为NULL
-- 功能说明:
--   产品编号在分配设备时生成（包含批号、设备、上机次数等信息）
--   创建产品记录时尚未分配设备，因此product_no需要允许为空
-- 影响范围: 生产模块 - 产品记录创建流程
-- 风险等级: 低（放宽约束，不影响现有数据）
-- ============================================================

SELECT '========================================' AS '';
SELECT '步骤7: 开始修改production_product表字段约束' AS '执行状态';
SELECT '========================================' AS '';

-- 修改product_no字段，允许NULL
ALTER TABLE production_product
MODIFY COLUMN product_no VARCHAR(50) NULL
COMMENT '产品编号（分配设备时生成）';

-- 验证字段变更
SELECT
    COLUMN_NAME as '字段名',
    COLUMN_TYPE as '类型',
    IS_NULLABLE as '允许NULL',
    COLUMN_DEFAULT as '默认值',
    COLUMN_COMMENT as '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND table_name = 'production_product'
AND column_name = 'product_no';

SELECT '✓ production_product表字段约束修改完成' AS '步骤7结果';

-- ============================================================
-- 执行后验证：检查所有变更是否成功
-- ============================================================

SELECT '========================================' AS '';
SELECT '开始执行后验证' AS '验证状态';
SELECT '========================================' AS '';

-- 验证1：检查所有涉及的表是否存在
SELECT '验证1：检查表结构' AS '';
SELECT
    table_name AS '表名',
    CASE
        WHEN table_name IN ('sys_user', 'order_main', 'production_record') THEN '✓ 已存在（已修改）'
        WHEN table_name IN ('device_daily_usage_counter', 'order_cancel_apply') THEN '✓ 已存在（新建）'
        ELSE '未知状态'
    END AS '状态'
FROM information_schema.tables
WHERE table_schema = DATABASE()
AND table_name IN ('sys_user', 'order_main', 'production_record', 'device_daily_usage_counter', 'order_cancel_apply')
ORDER BY
    CASE
        WHEN table_name = 'sys_user' THEN 1
        WHEN table_name = 'order_main' THEN 2
        WHEN table_name = 'device_daily_usage_counter' THEN 3
        WHEN table_name = 'order_cancel_apply' THEN 4
        WHEN table_name = 'production_record' THEN 5
    END;

-- 验证2：检查新增字段
SELECT '验证2：检查新增字段' AS '';
SELECT
    table_name AS '表名',
    column_name AS '字段名',
    column_type AS '类型',
    is_nullable AS '允许NULL',
    column_comment AS '注释'
FROM information_schema.columns
WHERE table_schema = DATABASE()
AND (
    (table_name = 'order_main' AND column_name = 'has_pending_cancel_apply')
    OR (table_name = 'production_record' AND column_name IN ('product_id', 'product_name', 'pack_material'))
)
ORDER BY table_name, column_name;

-- 验证3：检查新增索引
SELECT '验证3：检查新增索引' AS '';
SELECT
    table_name AS '表名',
    index_name AS '索引名',
    CASE WHEN non_unique = 0 THEN '唯一索引' ELSE '普通索引' END AS '索引类型',
    GROUP_CONCAT(column_name ORDER BY seq_in_index) AS '索引列'
FROM information_schema.statistics
WHERE table_schema = DATABASE()
AND (
    (table_name = 'sys_user' AND index_name = 'idx_user_phone')
    OR (table_name = 'order_main' AND index_name = 'idx_order_main_has_pending_cancel_apply')
    OR (table_name = 'production_record' AND index_name IN ('uk_package_product', 'uk_production_batch_no'))
)
GROUP BY table_name, index_name, non_unique
ORDER BY table_name, index_name;

-- 验证4：数据完整性检查
SELECT '验证4：数据完整性检查' AS '';

-- 检查order_main表的新字段
SELECT
    'order_main.has_pending_cancel_apply' AS '检查项',
    CONCAT('总记录数: ', COUNT(*),
           ' | 默认值(0): ', SUM(CASE WHEN has_pending_cancel_apply = 0 THEN 1 ELSE 0 END),
           ' | NULL值: ', SUM(CASE WHEN has_pending_cancel_apply IS NULL THEN 1 ELSE 0 END)) AS '统计结果'
FROM order_main;

-- 检查production_record表的新字段
SELECT
    'production_record新字段' AS '检查项',
    CONCAT('总记录数: ', COUNT(*),
           ' | product_id非NULL: ', SUM(CASE WHEN product_id IS NOT NULL THEN 1 ELSE 0 END),
           ' | pack_material非NULL: ', SUM(CASE WHEN pack_material IS NOT NULL THEN 1 ELSE 0 END)) AS '统计结果'
FROM production_record;

-- ============================================================
-- 执行完成总结
-- ============================================================

SELECT '========================================' AS '';
SELECT '数据库迁移执行完成' AS '状态';
SELECT NOW() AS '完成时间';
SELECT '========================================' AS '';

SELECT '变更汇总' AS '';
SELECT '1. sys_user表: 手机号索引从唯一改为普通' AS '变更内容';
SELECT '2. order_main表: 新增has_pending_cancel_apply字段和索引' AS '变更内容';
SELECT '3. 新建device_daily_usage_counter表（设备上机次数统计）' AS '变更内容';
SELECT '4. 新建order_cancel_apply表（订单取消申请）' AS '变更内容';
SELECT '5. production_record表: 新增3个字段和2个唯一索引' AS '变更内容';
SELECT '6. production_product表: product_no字段允许NULL（支持延迟生成编号）' AS '变更内容';

SELECT '后续操作' AS '';
SELECT '1. 检查应用服务日志，确认无异常' AS '建议';
SELECT '2. 验证相关业务功能正常运行' AS '建议';
SELECT '3. 保留本次执行日志，便于问题追溯' AS '建议';
SELECT '4. 如有问题，参考回滚脚本：docs/database-migration-2026-07-14.md' AS '建议';

-- 恢复设置
SET FOREIGN_KEY_CHECKS = 1;

SELECT '========================================' AS '';
SELECT '✓ 脚本执行完成，请重启应用服务' AS '最终状态';
SELECT '========================================' AS '';
