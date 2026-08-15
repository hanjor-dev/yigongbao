# Production Flow Integrity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce production process order, QC stage integrity, UDI ownership/uniqueness, and packaging device/process invariants.

**Architecture:** Keep the existing service boundaries. Add validation before state writes, add a narrowly scoped online migration for the UDI uniqueness guarantee, and use service tests as executable regression specifications.

**Tech Stack:** Spring Boot, MyBatis-Plus, JUnit 5/Mockito, MySQL.

---

### Task 1: Process order and print-event boundary

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImplTest.java`

- [x] Add failing tests rejecting `print` through start/finish and starting `cure` before `wash` completes.
- [x] Run the focused test and confirm the assertions fail.
- [x] Add minimal type/precedence validation and rerun the focused test.

### Task 2: QC stage and UDI ownership

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/qc/service/impl/BatchUpdateUdiTest.java`
- Create: `sql/migration-production-udi-unique-2026-08-15.sql`

- [x] Add failing tests rejecting QC before `QC_IN_PROGRESS` and UDI items from another record.
- [x] Run focused tests and confirm they fail.
- [x] Add record-stage and set-based product ownership checks.
- [x] Add idempotent migration that aborts on duplicate active UDI then creates a functional unique index.
- [x] Rerun focused tests.

### Task 3: Packaging invariants

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/service/impl/ProductionPackServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/pack/service/impl/ProductionPackServiceImplTest.java`

- [x] Add failing tests rejecting a non-sealing device and a missing packaging process update.
- [x] Run focused test and confirm it fails.
- [x] Add type validation and require exactly one updated packaging process.
- [x] Rerun focused test.

### Task 4: Integration verification

- [x] Run `mvn test -pl yigongbao-module-production -q` from `yigongbao-parent`.
- [x] Run `git diff --check` and inspect the diff.
- [ ] Commit only code, tests, migration, spec, and plan.
