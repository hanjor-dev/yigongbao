# 设备提交时生成生产批号与产品编号 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在提交打印设备时由后台按当天日期生成生产批号和正式产品编号，并支持释放后重新生成。

**Architecture:** 以 `ProductionRecordServiceImpl.assignDevice` 作为唯一正式批号生成入口，批号写入流转卡后再调用现有设备日计数器和产品编号服务。保留历史数据，禁止批号提交接口覆盖正式批号，并让 Excel 直接展示数据库批号。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、JUnit 5、Maven、Apache POI。

---

### Task 1: Add failing regression tests

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java`

- [ ] Add a test proving device assignment uses the current date rather than a stale record batch and passes that batch to formal-number generation.
- [ ] Add a test proving release followed by reassignment creates a new batch/product-number generation event without decrementing usage count.
- [ ] Add a test proving Excel keeps the persisted batch when print start date differs.
- [ ] Run the focused tests and confirm they fail for the missing behavior.

### Task 2: Implement authoritative batch generation

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`

- [ ] Generate `YYMMDD` in the device-assignment flow using the application business date.
- [ ] Persist the generated batch before formal product-number generation.
- [ ] Ensure the existing release flow clears old product numbers and reassignment generates again.
- [ ] Keep all writes in the existing transaction and preserve counter rollback on failure.

### Task 3: Remove frontend batch authority while retaining compatibility

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/SubmitBatchNoDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java`

- [ ] Stop `submitBatchNo` from changing `productionBatchNo`; retain material batch persistence for compatibility.
- [ ] Update validation and API documentation to mark the production batch parameter deprecated/ignored.
- [ ] Add focused service/controller assertions for the compatibility behavior.

### Task 4: Align Excel batch display

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/FlowCardExcelBuilder.java`

- [ ] Always write the persisted production batch to the Excel header.
- [ ] Preserve null/blank fallback behavior.
- [ ] Run builder tests.

### Task 5: Verify and review

- [ ] Run production-module focused tests.
- [ ] Run the full relevant Maven test suite.
- [ ] Inspect `git diff`, confirm unrelated user changes are not staged.
- [ ] Commit only the implementation, tests, and design/plan documents in one Chinese Conventional Commit.
