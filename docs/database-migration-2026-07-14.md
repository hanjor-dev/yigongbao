# 数据库迁移方案 - 线上环境更新至最新结构

**文档版本**: 1.0  
**创建日期**: 2026-07-14  
**目标**: 将线上数据库（ddl-prod.sql）更新为本地最新结构（ddl.sql）

---

## 一、变更概述

本次迁移涉及以下主要变更：

### 1.1 索引变更
- **sys_user表**: 手机号从唯一索引改为普通索引（支持手机号重复）

### 1.2 订单模块新增
- **order_main表**: 新增取消申请状态字段和索引
- **order_cancel_apply表**: 新建订单取消申请表（支持审核流程）

### 1.3 生产模块新增
- **device_daily_usage_counter表**: 新建设备每日上机次数统计表
- **production_record表**: 新增产品关联字段、包装材质字段和多个唯一索引

### 1.4 功能关联
- 订单取消审核流程（commit: c5585aa）
- 产品编号生成机制（commit: 62361d1）
- 流转卡包装材质显示（commit: 8e2339c）

---

## 二、ALTER脚本（按执行顺序）

### 2.1 sys_user表 - 索引变更

```sql
-- ============================================================
-- 变更说明：将手机号从唯一索引改为普通索引
-- 影响：允许不同用户使用相同手机号（业务需求变更）
-- ============================================================

-- 删除原有的唯一索引
DROP INDEX uk_phone ON sys_user;

-- 创建普通索引
CREATE INDEX idx_user_phone ON sys_user(phone);
```

### 2.2 order_main表 - 新增字段和索引

```sql
-- ============================================================
-- 变更说明：支持订单取消申请状态标记
-- 功能：标识订单是否有待审核的取消申请，用于快速查询
-- ============================================================

-- 新增has_pending_cancel_apply字段
ALTER TABLE order_main 
ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0 
COMMENT '是否有待审核的取消申请（0=否，1=是）'
AFTER version;

-- 创建索引（优化待审核订单查询）
CREATE INDEX idx_order_main_has_pending_cancel_apply 
ON order_main(has_pending_cancel_apply);
```

### 2.3 device_daily_usage_counter表 - 新建表

```sql
-- ============================================================
-- 变更说明：新建设备每日上机次数统计表
-- 功能：记录每台设备每日的上机次数，用于产品编号生成
-- 关联：产品编号规则需要设备当日上机次数作为序号组成部分
-- ============================================================

CREATE TABLE IF NOT EXISTS device_daily_usage_counter (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID（关联device表）',
    usage_date      DATE NOT NULL COMMENT '使用日期',
    usage_count     INT NOT NULL DEFAULT 0 COMMENT '当日上机次数',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_device_date (device_id, usage_date),
    KEY idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日上机次数统计表';
```

### 2.4 order_cancel_apply表 - 新建表

```sql
-- ============================================================
-- 变更说明：新建订单取消申请表
-- 功能：存储订单取消申请记录，支持审核流程
-- 关联：order_main.has_pending_cancel_apply字段标识是否有待审核申请
-- ============================================================

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
```

### 2.5 production_record表 - 新增字段和索引

```sql
-- ============================================================
-- 变更说明：支持产品拆分、包装材质记录和生产批号唯一性
-- 功能1：新增product_id和product_name字段，支持按产品ID拆分流转卡
-- 功能2：新增pack_material字段，记录包装材质信息
-- 功能3：新增多个唯一索引，保证数据唯一性和幂等性
-- ============================================================

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

-- 创建联合唯一索引：同一数据包+同一产品只能有一条流转卡
CREATE UNIQUE INDEX uk_package_product 
ON production_record ((CASE WHEN is_deleted = 0 THEN design_package_id ELSE NULL END), 
                      (CASE WHEN is_deleted = 0 THEN product_id ELSE NULL END));

-- 创建生产批号唯一索引（使用函数索引支持逻辑删除）
CREATE UNIQUE INDEX uk_production_batch_no 
ON production_record ((CASE WHEN is_deleted = 0 THEN production_batch_no ELSE NULL END));
```

---

## 三、数据迁移方案

### 3.1 order_main表历史数据处理

```sql
-- ============================================================
-- 说明：初始化has_pending_cancel_apply字段
-- 所有历史订单默认值为0（无待审核取消申请）
-- 该字段已在ALTER语句中设置DEFAULT 0，新增时自动填充
-- ============================================================

-- 验证：检查是否有NULL值
SELECT COUNT(*) FROM order_main WHERE has_pending_cancel_apply IS NULL;

-- 如果有NULL值，执行以下修复（正常情况下不需要）
-- UPDATE order_main SET has_pending_cancel_apply = 0 WHERE has_pending_cancel_apply IS NULL;
```

### 3.2 production_record表历史数据处理

```sql
-- ============================================================
-- 说明：历史流转卡的product_id、product_name、pack_material字段
-- 这些字段允许为NULL，历史数据无需回填
-- 新版本创建的流转卡会自动填充这些字段
-- ============================================================

-- 验证：查看历史数据中NULL值的分布
SELECT 
    COUNT(*) as total_records,
    SUM(CASE WHEN product_id IS NULL THEN 1 ELSE 0 END) as null_product_id,
    SUM(CASE WHEN pack_material IS NULL THEN 1 ELSE 0 END) as null_pack_material
FROM production_record;

-- 无需执行数据迁移，新字段允许NULL值
```

---

## 四、执行顺序和注意事项

### 4.1 推荐执行顺序

```
1. 备份数据库（必须！）
2. 索引变更（sys_user表）- 影响最小
3. 新建表（device_daily_usage_counter、order_cancel_apply）- 无依赖
4. order_main表变更 - 新增字段和索引
5. production_record表变更 - 新增字段和索引（最复杂）
6. 验证数据完整性
7. 重启应用服务
```

### 4.2 关键注意事项

#### 4.2.1 索引变更影响
- **sys_user.uk_phone → idx_user_phone**: 
  - 删除唯一索引可能需要较长时间（取决于数据量）
  - 建议在业务低峰期执行
  - 影响范围：用户登录、注册功能

#### 4.2.2 生产批号唯一索引
- **production_record.uk_production_batch_no**:
  - 使用函数索引确保逻辑删除兼容性
  - 检查历史数据是否有重复的production_batch_no（已删除记录除外）
  
```sql
-- 验证脚本：检查是否有重复的生产批号
SELECT production_batch_no, COUNT(*) as cnt
FROM production_record
WHERE is_deleted = 0
GROUP BY production_batch_no
HAVING COUNT(*) > 1;
```

#### 4.2.3 联合唯一索引
- **production_record.uk_package_product**:
  - 确保同一数据包的同一产品只有一条流转卡
  - 检查历史数据是否违反此约束
  
```sql
-- 验证脚本：检查是否有重复的(design_package_id, product_id)组合
SELECT design_package_id, product_id, COUNT(*) as cnt
FROM production_record
WHERE is_deleted = 0 AND product_id IS NOT NULL
GROUP BY design_package_id, product_id
HAVING COUNT(*) > 1;
```

### 4.3 停机时间评估
- **预计影响时间**: 5-10分钟
- **索引创建时间**: 取决于数据量，建议提前评估
- **建议执行窗口**: 业务低峰期（凌晨2:00-4:00）

---

## 五、回滚方案

### 5.1 sys_user表回滚

```sql
-- 回滚：将普通索引改回唯一索引
DROP INDEX idx_user_phone ON sys_user;
CREATE UNIQUE INDEX uk_phone ON sys_user ((CASE WHEN is_deleted = 0 THEN phone ELSE NULL END));
```

### 5.2 order_main表回滚

```sql
-- 删除索引
DROP INDEX idx_order_main_has_pending_cancel_apply ON order_main;

-- 删除字段
ALTER TABLE order_main DROP COLUMN has_pending_cancel_apply;
```

### 5.3 新建表回滚

```sql
-- 删除新建的表（谨慎！会丢失数据）
DROP TABLE IF EXISTS device_daily_usage_counter;
DROP TABLE IF EXISTS order_cancel_apply;
```

### 5.4 production_record表回滚

```sql
-- 删除索引
DROP INDEX uk_package_product ON production_record;
DROP INDEX uk_production_batch_no ON production_record;

-- 删除字段
ALTER TABLE production_record DROP COLUMN pack_material;
ALTER TABLE production_record DROP COLUMN product_name;
ALTER TABLE production_record DROP COLUMN product_id;
```

---

## 六、验证脚本

### 6.1 验证表结构

```sql
-- 验证sys_user表索引
SHOW INDEX FROM sys_user WHERE Key_name IN ('uk_phone', 'idx_user_phone');

-- 验证order_main表结构
DESCRIBE order_main;
SHOW INDEX FROM order_main WHERE Key_name = 'idx_order_main_has_pending_cancel_apply';

-- 验证新建表
SHOW TABLES LIKE '%device_daily_usage_counter%';
SHOW TABLES LIKE '%order_cancel_apply%';

-- 验证production_record表结构
DESCRIBE production_record;
SHOW INDEX FROM production_record WHERE Key_name IN ('uk_package_product', 'uk_production_batch_no');
```

### 6.2 验证数据完整性

```sql
-- 验证order_main表字段值
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN has_pending_cancel_apply = 0 THEN 1 ELSE 0 END) as normal,
    SUM(CASE WHEN has_pending_cancel_apply = 1 THEN 1 ELSE 0 END) as has_pending,
    SUM(CASE WHEN has_pending_cancel_apply IS NULL THEN 1 ELSE 0 END) as null_count
FROM order_main;

-- 验证production_record表字段
SELECT 
    COUNT(*) as total,
    SUM(CASE WHEN product_id IS NOT NULL THEN 1 ELSE 0 END) as has_product_id,
    SUM(CASE WHEN pack_material IS NOT NULL THEN 1 ELSE 0 END) as has_pack_material
FROM production_record;
```

---

## 七、完整执行脚本（可直接执行）

```sql
-- ============================================================
-- 数据库迁移脚本 - 线上环境更新
-- 执行前必须：1. 备份数据库  2. 在测试环境验证
-- 执行时机：业务低峰期
-- ============================================================

-- 开始事务（建议分批执行，而非全部在一个事务中）
START TRANSACTION;

-- ============================================================
-- 步骤1：sys_user表索引变更
-- ============================================================
DROP INDEX uk_phone ON sys_user;
CREATE INDEX idx_user_phone ON sys_user(phone);

COMMIT;

-- ============================================================
-- 步骤2：order_main表变更
-- ============================================================
START TRANSACTION;

ALTER TABLE order_main 
ADD COLUMN has_pending_cancel_apply TINYINT DEFAULT 0 
COMMENT '是否有待审核的取消申请（0=否，1=是）'
AFTER version;

CREATE INDEX idx_order_main_has_pending_cancel_apply 
ON order_main(has_pending_cancel_apply);

COMMIT;

-- ============================================================
-- 步骤3：新建device_daily_usage_counter表
-- ============================================================
CREATE TABLE IF NOT EXISTS device_daily_usage_counter (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    device_id       BIGINT NOT NULL COMMENT '设备ID（关联device表）',
    usage_date      DATE NOT NULL COMMENT '使用日期',
    usage_count     INT NOT NULL DEFAULT 0 COMMENT '当日上机次数',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_device_date (device_id, usage_date),
    KEY idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备每日上机次数统计表';

-- ============================================================
-- 步骤4：新建order_cancel_apply表
-- ============================================================
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

-- ============================================================
-- 步骤5：production_record表变更（分步执行）
-- ============================================================
START TRANSACTION;

-- 新增产品关联字段
ALTER TABLE production_record 
ADD COLUMN product_id BIGINT COMMENT '产品ID'
AFTER design_package_code;

ALTER TABLE production_record 
ADD COLUMN product_name VARCHAR(100) COMMENT '产品名称（冗余）'
AFTER product_id;

-- 新增包装材质字段
ALTER TABLE production_record 
ADD COLUMN pack_material VARCHAR(100) 
COMMENT '包装材质（如：纸封袋、PE符合食品包装袋）'
AFTER pack_seal_time;

COMMIT;

-- ============================================================
-- 步骤6：production_record表创建索引（耗时操作，单独执行）
-- ============================================================

-- 创建联合唯一索引
CREATE UNIQUE INDEX uk_package_product 
ON production_record ((CASE WHEN is_deleted = 0 THEN design_package_id ELSE NULL END), 
                      (CASE WHEN is_deleted = 0 THEN product_id ELSE NULL END));

-- 创建生产批号唯一索引
CREATE UNIQUE INDEX uk_production_batch_no 
ON production_record ((CASE WHEN is_deleted = 0 THEN production_batch_no ELSE NULL END));

-- ============================================================
-- 执行完成后验证
-- ============================================================
SELECT '迁移完成，请执行验证脚本检查数据完整性' AS status;
```

---

## 八、总结与建议

### 8.1 变更影响评估

| 影响范围 | 影响程度 | 风险等级 |
|---------|---------|---------|
| 用户登录注册 | 中 | 低（索引变更，不影响业务逻辑） |
| 订单取消流程 | 高 | 低（新增功能，不影响现有订单） |
| 产品编号生成 | 高 | 中（需要设备上机次数统计，影响生产流程） |
| 流转卡生成 | 中 | 低（新增字段，历史数据允许NULL） |

### 8.2 关键风险点

1. **sys_user.uk_phone索引删除**
   - 风险：如果业务代码依赖手机号唯一性，可能导致数据不一致
   - 建议：迁移前确认业务代码已适配

2. **production_record唯一索引**
   - 风险：历史数据可能存在重复的生产批号
   - 建议：迁移前运行验证脚本，清理重复数据

3. **索引创建时间**
   - 风险：大表创建索引可能锁表较长时间
   - 建议：在业务低峰期执行，提前评估耗时

### 8.3 执行建议

1. **必须在测试环境先执行一遍**，验证无误后再在生产环境执行
2. **必须备份数据库**，建议使用mysqldump或物理备份
3. **分步执行**，每步执行后验证，避免一次性执行所有变更
4. **准备回滚方案**，出现问题立即回滚
5. **通知相关开发人员**，确保应用代码已适配新表结构
6. **监控应用日志**，迁移后观察是否有异常

### 8.4 相关Git Commit

本次迁移对应以下commit的数据库变更：
- `8e2339c` - 流转卡Excel支持显示包装材质
- `c5585aa` - 添加订单取消申请表和相关字段
- `e80867c` - production_record表增加product_category字段
- `62361d1` - 完善订单乐观锁并集成产品编号生成

---

**文档完成时间**: 2026-07-14  
**建议执行时间**: 业务低峰期（凌晨2:00-4:00）  
**预计停机时间**: 5-10分钟  
**责任人**: 数据库管理员 + 后端开发负责人

