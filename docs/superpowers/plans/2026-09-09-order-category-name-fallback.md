# 订单分类名称兜底 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 订单正式创建/提交时，在项目分类名称缺失的历史数据场景下根据有效 `categoryCode` 自动补齐字典名称，避免无必要地阻断下单。

**Architecture:** 保持 `OrderDataValidator` 作为订单明细关联数据的统一入口；复用现有 `RebuildProjectMapper.selectCategoryName` 查询并通过 `RebuildProjectService` 暴露窄接口，仅在项目主数据名称缺失时调用。正式订单路径 fail-closed，草稿保存继续保留原有不完整数据策略。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Maven。

**Spec:** `docs/superpowers/specs/2026-09-09-order-category-name-fallback-design.md`

---

### Task 1: Add regression tests for category-name fallback

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/validator/OrderDataValidatorTest.java` (create if absent)

- [x] **Step 1: Add the test fixture for an order item and mocked project/dictionary dependencies.**
- [x] **Step 2: Add a failing test proving a project with code and missing name is filled from the dictionary.**
- [x] **Step 3: Add a test proving an existing project name is retained and frontend values are ignored.**
- [x] **Step 4: Add tests proving blank code, missing/blank dictionary name, and dictionary exceptions still fail for formal validation.**
- [x] **Step 5: Add tests proving draft mode remains permissive while formal validation is strict.**
- [x] **Step 6: Run the focused test and confirm it fails because the fallback behavior is not implemented.**

Run from `yigongbao-parent` with the installed Maven executable:

```powershell
mvn -pl yigongbao-module-order -am -Dtest=OrderDataValidatorTest test
```

Expected: the fallback test fails before implementation.

### Task 2: Implement dictionary fallback in the validator

**Files:**
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/rebuildProject/mapper/RebuildProjectMapperTest.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/test/resources/schema.sql` (add the minimal `sys_dict` test table/data)
- Modify: existing `selectCategoryName` in `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/mapper/RebuildProjectMapper.java` (do not add a second dictionary query)
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/service/RebuildProjectService.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/rebuildProject/service/impl/RebuildProjectServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/validator/OrderDataValidator.java`

- [x] **Step 1: Update the existing mapper query to filter `status = 1`, `is_deleted = 0`, `dict_name IS NOT NULL`, and `TRIM(dict_name) <> ''` before `LIMIT 1`; this preserves the existing create/update project behavior while preventing blank names.**
- [x] **Step 1a: Add a Mapper/integration regression test using the module test schema proving invalid, disabled, deleted, or blank-name rows cannot win before a valid row.**
- [x] **Step 2: Expose a narrow `getCategoryNameByCode` service method backed by that query, returning no usable name when the dictionary has no valid row.**
- [x] **Step 3: Change formal order item validation to require a nonblank code, retain an existing nonblank project name, and validate the code through the dictionary; use the dictionary name only when the project name is blank.**
- [x] **Step 4: Log project ID, category code, and lookup failure; convert missing/blank lookup results and query exceptions to the existing business failure without writing an empty snapshot.**
- [x] **Step 5: Keep draft-save behavior unchanged and ensure draft-to-formal validation uses the formal path.**

### Task 3: Verify and review

**Files:**
- Review: all files changed by Tasks 1–2

- [x] **Step 1: Run the focused validator tests.**
- [x] **Step 2: Run the full order-module test suite and the basic-module tests covering project creation/update and dictionary lookup; record unrelated existing test failures separately.**
- [x] **Step 3: Inspect the final diff for scope, null handling, and SQL correctness, including the exact `LIMIT 1` ordering.**
- [x] **Step 4: Dispatch an independent code-review subagent and fix the important finding about always validating categoryCode.**
- [x] **Step 5: Re-run verification after every fix.**
- [ ] **Step 6: Before committing, run `git status`, `git diff HEAD`, `git branch --show-current`, and `git log --oneline -10`; stage only directly related files.**
- [ ] **Step 7: Create one Chinese Conventional Commit on the current `dev` branch.**

Executable verification commands:

```powershell
mvn -pl yigongbao-module-order -am test
mvn -pl yigongbao-module-basic -am -Dtest=RebuildProjectMapperTest,RebuildProjectControllerTest test
```
