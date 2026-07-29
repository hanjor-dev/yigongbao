# 生产产品台账导出逻辑修正 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让生产产品台账导出覆盖从开始打印到正常完成的全部产品状态，并保持各角色数据范围正确。

**Architecture:** 保留现有 Controller → Service → Mapper → ExcelBuilder 链路。只在 Mapper 的总数查询和明细查询中统一替换产品状态集合；权限仍由 Service 根据角色 `data_scope_type` 注入 `hospitalIds` 或 `centerIds`。

**Tech Stack:** Java 17、Spring Boot、MyBatis、MyBatis-Plus、JUnit 5、Maven。

---

### Task 1: Add regression coverage for export status scope

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/mapper/ProductionProductMapperSqlTest.java`

- [x] **Step 1: Write the failing test**

  Add a source-level regression test that reads `ProductionProductMapper.java` and asserts both export SQL statements contain `in_process`, `fail`, `pass`, `pending_warehouse_in`, `warehoused`, `warehouse_out`, and `completed`, and do not use the old four-status predicate.

- [x] **Step 2: Run the focused test and verify it fails**

  Run:

  ```powershell
  .\mvnw.cmd -pl yigongbao-parent/yigongbao-module-production -am -Dtest=ProductionProductMapperSqlTest test
  ```

  Expected: FAIL because the current mapper still contains only `in_process`, `fail`, `pass`, and `completed`.

### Task 2: Expand the shared export status predicate

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java:58-59`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java:108-109`

- [x] **Step 1: Replace the status list in the detail query**

  Use the seven normal post-print states:

  ```sql
  pp.status IN ('in_process', 'fail', 'pass', 'pending_warehouse_in', 'warehoused', 'warehouse_out', 'completed')
  ```

- [x] **Step 2: Replace the status list in the count query**

  Apply the identical predicate to `countProductLedgerData` so the count and exported rows have the same scope.

- [x] **Step 3: Run the focused regression test**

  Run the Task 1 command and expect PASS.

### Task 3: Verify role and module regressions

**Files:**
- No additional production files expected.

- [x] **Step 1: Run all production module tests**

  Run:

  ```powershell
  .\mvnw.cmd -pl yigongbao-parent/yigongbao-module-production -am test
  ```

- [x] **Step 2: Inspect the diff and verify unrelated changes are untouched**

  Confirm only the mapper and regression test are implementation changes; pre-existing unrelated worktree files remain unchanged.

- [x] **Step 3: Check role behavior against the existing Service implementation**

  Confirm `CENTER` still injects the current user’s `center_id`, `ALL` still has no automatic scope filter, and unsupported scopes are still rejected.
