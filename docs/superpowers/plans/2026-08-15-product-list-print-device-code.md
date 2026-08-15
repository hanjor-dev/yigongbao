# Product List Print Device Code Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an always-present `printDeviceCode` field to every `/production/product/list` result, sourced from the product's production record.

**Architecture:** Extend the existing product detail response model and reuse the page-wide production-record batch lookup already performed by `ProductionProductServiceImpl`. Preserve the current request, pagination, permissions, query count, and missing-record fallback behavior.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, Jackson, JUnit 5, Mockito, MockMvc, AssertJ, Maven.

---

### Task 1: Define and serialize the response contract

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/vo/ProductionProductDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/controller/ProductionProductControllerTest.java`

- [ ] **Step 1: Write failing response-contract tests**

Add a reflection assertion that `ProductionProductDetailVO` declares `printDeviceCode`. Add MockMvc response assertions for a populated value and an explicit JSON null value. Build test VOs without compile-time dependence on the new accessor by setting the field reflectively only after checking it exists.

- [ ] **Step 2: Run the controller test and verify RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production '-Dtest=ProductionProductControllerTest' test -DskipTests=false
```

Expected: FAIL because `ProductionProductDetailVO` does not declare `printDeviceCode` and the response omits that field.

- [ ] **Step 3: Add the minimal response field**

Add this field in the flow-card section of `ProductionProductDetailVO`:

```java
@JsonInclude(JsonInclude.Include.ALWAYS)
private String printDeviceCode;
```

The field-level inclusion overrides the project-wide `NON_NULL` rule only for this response property.

- [ ] **Step 4: Run the controller test and verify GREEN**

Run the Step 2 command. Expected: all `ProductionProductControllerTest` tests PASS and JSON contains `printDeviceCode` for both populated and null cases.

- [ ] **Step 5: Commit the contract change**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/vo/ProductionProductDetailVO.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/controller/ProductionProductControllerTest.java
git commit -m "feat(production): expose product printer code"
```

### Task 2: Map the flow-card printer code without extra queries

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/service/impl/ProductionProductServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/service/impl/ProductionProductServiceImplTest.java`

- [ ] **Step 1: Add service fixtures for the paged query**

Extend the service test with mocks for `ProductionRecordMapper`, `UserMapper`, and `UserHospitalService`. Mock `StpUtil.getLoginIdAsLong` inside a `try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class))` block so static state is closed after every test. Mock `DataScopeTypeEnum.ALL`, the accessible-record query, the product page, and the existing batch record lookup.

- [ ] **Step 2: Write failing mapping and query-shape tests**

Add tests proving:

- Products from two records receive their respective `printDeviceCode` values.
- A record with a null printer code produces a null VO value.
- A missing related record preserves the null fallback.
- Multiple products use the existing single batch hydration query after the accessible-record query; verify `recordMapper.selectList(...)` is called exactly twice in total—once for permission filtering and once for batch hydration—rather than once per product. There is no device mapper dependency.

- [ ] **Step 3: Run the service test and verify RED**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production '-Dtest=ProductionProductServiceImplTest' test -DskipTests=false
```

Expected: FAIL because `pageProductDetails` does not copy `ProductionRecordEntity.printDeviceCode` into the product detail VO.

- [ ] **Step 4: Add the minimal mapping**

Inside the existing `record != null` mapping block, add:

```java
vo.setPrintDeviceCode(record.getPrintDeviceCode());
```

Do not add a mapper, join, query, DTO, or database field.

- [ ] **Step 5: Run targeted tests and verify GREEN**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production '-Dtest=ProductionProductServiceImplTest,ProductionProductControllerTest' test -DskipTests=false
```

Expected: all targeted tests PASS.

- [ ] **Step 6: Commit the mapping change**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/service/impl/ProductionProductServiceImpl.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/product/service/impl/ProductionProductServiceImplTest.java
git commit -m "feat(production): map product printer code"
```

### Task 3: Verify, review, fix, and integrate

**Files:**
- Review all files changed relative to `dev`
- Modify only files required by confirmed review findings

- [ ] **Step 1: Run whitespace and scope checks**

```powershell
git diff --check dev...HEAD
git diff --stat dev...HEAD
```

Expected: no whitespace errors and only the design/plan plus four backend/test files in scope.

- [ ] **Step 2: Run the production module full suite**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test -DskipTests=false
```

Expected: all production tests PASS.

- [ ] **Step 3: Run an independent code review**

Review API compatibility, null serialization, permission preservation, batch-query behavior, missing-record fallback, test quality, and unrelated changes.

- [ ] **Step 4: Fix confirmed review findings with TDD**

For each real issue, add or adjust a failing regression test, verify RED, implement the minimal fix, and verify GREEN. If no issues are found, make no speculative changes.

- [ ] **Step 5: Re-run complete verification**

Run the full production test command and `git diff --check` again. Expected: zero failures and no whitespace errors.

- [ ] **Step 6: Merge into `dev` and verify the merged result**

From the root worktree, merge `codex/product-list-print-device-code` into `dev`, then re-run the production module full suite. Remove the clean feature worktree and branch only after merged verification succeeds.
