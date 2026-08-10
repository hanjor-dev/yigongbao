# Product Ledger Print Start Time Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将生产产品台账导出的开始、结束时间统一改为过滤流转卡打印开始时间。

**Architecture:** 保留 Controller 与 Service 校验流程，只修改 DTO 语义说明以及 `ProductionProductMapper` 中明细和总数查询的时间列。通过现有 SQL 契约测试锁定两段查询的一致性。

**Tech Stack:** Java 21、Spring Boot、MyBatis、JUnit 5、Maven

---

### Task 1: 锁定打印开始时间筛选契约

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductMapperSqlTest.java`

- [x] **Step 1: 修改测试为新语义**

断言明细与总数查询都包含：

```text
AND pr.print_start_time >= #{dto.startTime}
AND pr.print_start_time <= #{dto.endTime}
```

同时断言不再用 `om.create_time` 过滤时间。

- [x] **Step 2: 运行测试确认失败**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductMapperSqlTest test`

Expected: FAIL，现有 SQL 仍使用 `om.create_time`。

### Task 2: 修改台账查询

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/ProductLedgerExportDTO.java:28-32`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java:85-90,135-140`

- [x] **Step 1: 修改 DTO 注释**

将两个字段说明改为“打印开始时间起”和“打印开始时间止”。

- [x] **Step 2: 修改两段 SQL**

把 `om.create_time` 替换为 `pr.print_start_time`，其他条件、排序和输出列保持不变。

- [x] **Step 3: 运行目标测试**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionProductMapperSqlTest test`

Expected: 5 tests，0 failures，0 errors。

- [x] **Step 4: 运行生产模块完整测试**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test`

Expected: BUILD SUCCESS，0 failures，0 errors。
