# 订单虚拟单号 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保留原 `orderCode` 的前提下，为正式订单增加固定 12 位的 `publicOrderCode`，并贯通订单、设计、生产、质检、仓储列表/详情、导出、搜索和历史数据。

**Architecture:** `order_main.public_order_code` 是唯一、不可变的对外标识；现有 `order_code` 继续作为内部流水号和兼容字段。生产、质检、仓储优先通过订单关联查询虚拟单号，不新增无必要的冗余字段；列表默认配置采用追加字段方式，导出字段列表同时保留两种编号。

**Tech Stack:** Java 17/Spring Boot/MyBatis-Plus, MySQL, JUnit 5/Mockito, SQL migration scripts.

---

### Task 1: 建立虚拟单号生成边界和测试

**Files:**
- Create: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/PublicOrderCodeGenerator.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/PublicOrderCodeGeneratorTest.java`

- [x] 写生成长度、字符集和格式测试。
- [x] 运行测试确认在生成器不存在时按预期失败。
- [x] 使用安全随机源实现 12 位生成器，并处理可读字符集。
- [x] 运行生成器测试通过。

### Task 2: 扩展订单数据模型和正式订单创建

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderMainServiceImplStateTransitionTest.java`

- [x] 先补充直提订单和草稿转正式订单都设置虚拟单号的测试。
- [x] 运行测试确认新增断言失败。
- [x] 增加实体字段和生成器依赖。
- [x] 在两个正式订单入口生成虚拟单号，禁止修改原 `orderCode`。
- [x] 运行订单服务测试通过。

### Task 3: 数据库迁移和历史订单补齐

**Files:**
- Create: `sql/migration-order-public-code-20260908.sql`
- Create: `sql/backfill-order-public-code-20260908.sql`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`

- [x] 增加 `public_order_code` 字段和逻辑唯一索引。
- [x] 编写可重复执行的历史订单补齐 SQL，仅更新空值，不改动 `order_code`。
- [x] 增加执行后的空值、重复值和格式校验查询。
- [x] 在全量 DDL 中同步新字段。

### Task 4: 订单和设计列表/详情返回及搜索

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DesignQueryHelper.java`

- [x] 先补充转换和搜索 SQL 断言。
- [x] 运行测试确认失败。
- [x] 显式映射 `publicOrderCode`，保留原 `orderCode`。
- [x] 订单列表和设计工单列表模糊搜索增加虚拟单号。
- [x] 运行订单、设计模块测试。

### Task 5: 生产、质检、仓储列表和详情返回

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/mapper/ProductionRecordMapper.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/ProductionRecordVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/warehouse/vo/WarehouseRecordVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/warehouse/vo/WarehouseDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/warehouse/vo/WarehouseProductVO.java`
- Modify: 相关生产/质检/仓储 service、mapper 和转换测试

- [x] 先补充查询结果中同时存在原流水号和虚拟单号的测试。
- [x] 运行测试确认失败。
- [x] 通过 `order_id`/`order_main` 关联读取虚拟单号，不改写原订单流水号。
- [x] 更新各列表的字段白名单和查询映射。
- [x] 运行生产模块针对性测试。

### Task 6: 列配置、导出字段和默认配置同步

**Files:**
- Modify: `sql/init.sql`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/util/ColumnConfigValidator.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`
- Modify: order/design/production/quality/warehouse 配置相关校验和测试

- [x] 验证五个 `sys_config` JSON 配置中追加 `publicOrderCode`，不删除 `orderCode/orderNo`。
- [x] 更新默认配置常量，保证新环境与迁移环境一致。
- [x] 增加导出字段元数据、标签和 Excel 值映射。
- [x] 运行配置和导出测试。

### Task 7: 全量验证

- [x] 执行订单、设计、生产模块针对性测试。
- [x] 执行 Maven 编译/测试。
- [x] 校验 SQL 脚本可重复执行、字段长度为 12、历史数据无空值/重复值。
- [x] 复核 `git diff`，确保没有删除或改名原 `orderCode`。
