# Production Ledger Inclusive End Date Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让生产产品台账导出的结束日期包含所选日期全天，并确保次日零点及之后的数据不被误导出。

**Architecture:** 保留接口 DTO 的 `LocalDateTime` 类型。Service 创建查询 DTO 副本，把副本的结束时间规范化为所选日期的次日零点并据此校验范围，Mapper 使用跨 MySQL/H2 的 `print_start_time < endTime` 半开区间；SQL 契约测试、Service 单元测试和真实 Mapper/H2 边界测试共同锁定行为。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、MySQL、H2 MySQL compatibility mode、JUnit 5、Mockito、Maven

---

## 文件结构

- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java` — 明细和计数查询的结束日期条件。
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java` — 创建查询副本、规范化结束时间并按实际排他上界校验范围。
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductMapperSqlTest.java` — 锁定两条 SQL 的半开区间契约。
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductLedgerDateBoundaryTest.java` — 通过隔离的 H2 MySQL 模式数据库执行 Mapper，验证日期边界。
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordLedgerDateValidationTest.java` — 隔离验证 Service 的有效范围判断。

### Task 1: 建立 Mapper 日期查询失败契约

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductMapperSqlTest.java`

- [ ] **Step 1: 修改 SQL 契约测试**

将两条导出 SQL 的结束条件断言改为：

```java
assertTrue(query.contains(
        "AND pr.print_start_time &lt; #{dto.endTime}"));
assertFalse(query.contains("AND pr.print_start_time &lt;= #{dto.endTime}"));
```

保留 `pr.print_start_time >= startTime`、权限、状态和逻辑删除断言。

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductMapperSqlTest test
```

Expected: FAIL，旧 SQL 仍包含 `<= #{dto.endTime}`。

- [ ] **Step 3: 保留失败测试，暂不修改生产 SQL**

Task 3 的真实 Mapper 边界测试也确认 RED 后，再统一修改生产 SQL并转绿。

- [ ] **Step 4: 提交 Task 1 测试**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductMapperSqlTest.java
git commit -m "test(production): require exclusive ledger end time"
```

### Task 2: 锁定并修正 Service 时间范围校验

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordLedgerDateValidationTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`

- [ ] **Step 1: 编写同日有效范围失败测试**

构造 `startTime=2026-08-13T15:00:00`、`endTime=2026-08-13T00:00:00`，静态模拟 `StpUtil.getLoginIdAsLong()`，模拟 `UserHospitalService.getDataScopeType()` 返回 ALL、Mapper 查询到一条数据和 Excel 构建成功。捕获传给计数和明细 Mapper 的 DTO，断言不抛出异常，且两次查询看到的 `endTime` 都是 `2026-08-14T00:00:00`。

- [ ] **Step 2: 编写次日零点无效范围测试**

构造 `startTime=2026-08-14T00:00:00`、`endTime=2026-08-13T15:00:00`，断言抛出“开始时间不能晚于结束时间”，且 Mapper 未执行。

- [ ] **Step 3: 运行测试并确认 RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionRecordLedgerDateValidationTest test
```

Expected: 同日有效范围测试 FAIL，因为旧逻辑直接比较原始 `startTime/endTime`。

- [ ] **Step 4: 按排他上界校验**

在 Service 中先复制 DTO，再规范化副本中非空的结束时间；仅当开始时间也非空时校验：

```java
LocalDateTime exclusiveEnd = dto.getEndTime().toLocalDate().plusDays(1).atStartOfDay();
dto.setEndTime(exclusiveEnd);
if (dto.getStartTime() != null && !dto.getStartTime().isBefore(exclusiveEnd)) {
    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "开始时间不能晚于结束时间");
}
```

增加重复调用测试：同一原始 DTO 连续调用两次，捕获两次 Mapper 参数均为相同的次日零点，并断言原始 DTO 未被修改。

- [ ] **Step 5: 运行测试并确认 GREEN**

Run Task 2 Step 3 命令。

Expected: 两个边界测试全部通过。

- [ ] **Step 6: 提交 Task 2**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordLedgerDateValidationTest.java
git commit -m "test(production): cover ledger date validation"
```

### Task 3: 执行 Mapper 真实边界验证

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductLedgerDateBoundaryTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java`

- [ ] **Step 1: 搭建隔离 Mapper 测试数据库**

参考 `ProductionRecordClaimTransactionTest` 的 `MybatisSqlSessionFactoryBean` 配置，使用唯一内存库：

```text
jdbc:h2:mem:product-ledger-date-boundary;MODE=MySQL;DB_CLOSE_DELAY=-1
```

在测试配置中建立 Mapper 查询所需的最小 `production_product`、`production_record`、`production_process`、`order_main` 表。目标 SQL 不使用数据库专属日期函数，因此可在 MySQL 和 H2 MySQL 模式执行。

- [ ] **Step 2: 编写 Mapper 边界测试数据**

插入三个其他条件完全相同、状态均为 `in_process` 的产品记录：

```text
2026-08-13 20:00:00  应包含
2026-08-14 00:00:00  应排除
2026-08-14 15:00:00  应排除
```

DTO 使用 Service 已规范化的排他上界 `endTime=2026-08-14T00:00:00`；分别调用 `listProductLedgerData` 和 `countProductLedgerData`，断言明细只有第一条、总数为 1。

- [ ] **Step 3: 在修改生产 SQL 前运行边界测试并确认 RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductLedgerDateBoundaryTest test
```

Expected: FAIL；旧 `<= endTime` 会错误包含次日零点记录，明细和计数大于 1。

- [ ] **Step 4: 最小修改明细与计数 SQL**

两处统一替换为：

```java
"  AND pr.print_start_time &lt; #{dto.endTime} " +
```

- [ ] **Step 5: 运行契约与边界测试并确认 GREEN**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductMapperSqlTest,ProductionProductLedgerDateBoundaryTest test
```

Expected: SQL 契约测试通过；边界测试通过，明细和计数均为 1。

- [ ] **Step 6: 提交 Task 3**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductLedgerDateBoundaryTest.java
git commit -m "fix(production): include ledger end date"
```

### Task 4: 全量验证

**Files:**
- Verify only; no new production scope.

- [ ] **Step 1: 运行相关回归测试**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductMapperSqlTest,ProductionProductLedgerDateBoundaryTest,ProductionRecordLedgerDateValidationTest test
```

Expected: 0 failures，0 errors。

- [ ] **Step 2: 运行生产模块完整测试**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test
```

Expected: BUILD SUCCESS，0 failures，0 errors。

- [ ] **Step 3: 检查最终差异**

```powershell
git diff --check
git status --short
git diff HEAD~3 -- yigongbao-parent/yigongbao-module-production
```

确认只包含日期边界修复和相关测试，现有用户未跟踪文件未被纳入。
