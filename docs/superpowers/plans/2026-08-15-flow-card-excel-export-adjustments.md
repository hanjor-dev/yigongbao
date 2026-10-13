# Production Flow Card Excel Export Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct four production flow-card Excel export behaviors and force existing cached exports to regenerate after deployment.

**Architecture:** Keep all formatting decisions inside `FlowCardExcelBuilder`, where the workbook cells are populated. Preserve the existing air-compressor data path and prove it with a regression test. Use a one-time idempotent SQL update to invalidate cached Excel generation timestamps without changing schema or deleting files.

**Tech Stack:** Java 21, Spring Boot 3.2, Apache POI, JUnit 5, Maven, MySQL.

---

### Task 1: Add failing Excel regression tests

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java`

- [x] Add `buildHeaderUsesPrintStartDateAsProductionBatchNo` with print start `2026-08-13T14:15:24`; assert Excel C4 using POI `getRow(3).getCell(2)` contains `20260813`.
- [x] Add `buildHeaderFallsBackToStoredBatchNoWithoutPrintStartTime`; assert the stored batch remains visible in C4.
- [x] Add `buildHeaderShowsDashWhenPrintStartAndStoredBatchAreBlank`; cover both `null` and whitespace stored batches and assert C4 is `-`.
- [x] Add `buildHeaderShowsCompleteTimeLabelsAndKeepsSeconds`; assert Excel C5/C6 using POI `getRow(4/5).getCell(2)` contain second-precision start/end values and the complete `结束时间:` label.
- [x] Add one parameterized-style loop test for `wash`, `cure`, and `clean_dry`; assert their parameter cells contain minute-level start/end text and do not contain seconds.
- [x] Add `buildCleanDryShowsAirCompressorDeviceNo`; assert Excel D13 using POI `getRow(12).getCell(3)` contains `AIR-001`.
- [x] Keep the verified upstream evidence in the delivery record: `DeviceEntity` names its unique device-number property `deviceId`; `ProductionProcessServiceImpl.startProcess` calls `secondaryDevice.getDeviceId()` and saves that value to `ProductionProcessEntity.secondaryDeviceNo`; `ProductionRecordServiceImpl.generateFlowCardExcel` maps it into `ProcessInfo`; the Builder regression test covers the final template write. Do not add unrelated Service refactoring.
- [x] Run `mvn --% -pl yigongbao-module-production -Dtest=FlowCardExcelBuilderTest -Dsurefire.failIfNoSpecifiedTests=false test` from `yigongbao-parent`.
- [x] Confirm the new batch, label, and minute-format assertions fail for the expected current values; the air-compressor assertion may already pass and serves as characterization coverage.

### Task 2: Implement minimal Builder changes

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/FlowCardExcelBuilder.java`

- [x] Add a `yyyyMMdd` date formatter for the exported batch number.
- [x] Add a `yyyy-MM-dd HH:mm` formatter for post-processing times.
- [x] In `fillHeader`, use the print-start date as batch number when present and fall back to `productionBatchNo` when absent.
- [x] Change the print finish label from `结束:` to `结束时间:`.
- [x] In `convertProcessParams`, format post-processing start/end times with the minute formatter only.
- [x] Re-run the targeted Builder test and confirm all tests pass.

### Task 3: Add one-time cache invalidation migration

**Files:**
- Create: `sql/migration-flow-card-excel-format-2026-08-15.sql`

- [x] Define one exact migration predicate: `is_deleted = 0 AND flow_card_generate_time IS NOT NULL AND flow_card_file_url IS NOT NULL AND TRIM(flow_card_file_url) <> ''`.
- [x] Add a pre-update audit query using that exact predicate.
- [x] Add an idempotent update using that exact predicate and return `ROW_COUNT()` as the affected-row count.
- [x] Add a post-update verification query using the same predicate; the result must be `0`.
- [x] Do not clear `flow_card_file_url` and explicitly preserve `update_time`; the next request will replace the file after successful generation without polluting business update timestamps.
- [x] Document that the SQL must run only after all old-version application instances are stopped or removed from traffic and the new version is active.

### Task 4: Verify and review

**Files:**
- Review all files changed in Tasks 1–3.

- [x] Run the targeted Builder tests.
- [x] Run the production module test suite with reactor dependencies: `mvn --% -pl yigongbao-module-production -am test` (blocked in the unmodified basic module; production module then verified independently).
- [x] Run `git diff --check` and inspect `git diff --stat` plus the full diff.
- [x] Confirm the four acceptance criteria and cache behavior line by line.
- [x] Update `task_plan.md`, `findings.md`, and `progress.md` with final evidence.
- [x] Commit only the reviewed Builder source, Builder test, migration SQL, specification, and implementation plan to `codex/flow-card-excel-adjustments`.
