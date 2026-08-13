# Production Dashboard History Backfill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Safely backfill processing-center snapshots for claimed historical production records and make production worker/manager dashboard statistics reliable and transparent.

**Architecture:** Deliver an idempotent MySQL migration with source-conflict guards, then centralize production-dashboard time ranges and bucket mapping in a package-private helper shared by the two existing strategies. Preserve API keys and role scopes while separating real-time backlog metrics from selected-period throughput metrics.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ, Maven, MySQL 8 SQL.

---

### Task 1: Establish dashboard regression tests

**Files:**
- Create: `yigongbao-parent/yigongbao-module-dashboard/src/test/java/com/yigongbao/module/dashboard/service/strategy/ProductionWorkerDashboardStrategyTest.java`
- Create: `yigongbao-parent/yigongbao-module-dashboard/src/test/java/com/yigongbao/module/dashboard/service/strategy/ProductionManagerDashboardStrategyTest.java`

- [x] Write worker tests proving current in-production/QC/warehouse cards omit `create_time`, while `myTasks` retains the selected range.
- [x] Write worker test proving mapper exceptions propagate instead of returning an empty dashboard.
- [x] Write manager tests proving missing, nonexistent, disabled, and soft-deleted centers are rejected; records are filtered by center; and current backlog cards omit `create_time`.
- [x] Write manager workload test proving completed counts use `post_processing_end_time` for the selected period.
- [x] Run the two tests and verify they fail for the expected current behavior.

Run:

```powershell
mvn -pl yigongbao-module-dashboard -am "-Dtest=ProductionWorkerDashboardStrategyTest,ProductionManagerDashboardStrategyTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: FAIL because current-stage queries contain `create_time`, errors are swallowed, and manager configuration/workload behavior is inconsistent.

### Task 2: Centralize range and bucket behavior with TDD

**Files:**
- Create: `yigongbao-parent/yigongbao-module-dashboard/src/main/java/com/yigongbao/module/dashboard/service/strategy/ProductionDashboardQueryHelper.java`
- Create: `yigongbao-parent/yigongbao-module-dashboard/src/test/java/com/yigongbao/module/dashboard/service/strategy/ProductionDashboardQueryHelperTest.java`
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/main/java/com/yigongbao/module/dashboard/util/TimeRangeUtil.java`
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/test/java/com/yigongbao/module/dashboard/util/TimeRangeUtilTest.java`

- [x] Write failing tests for exclusive end time, Sunday/Monday mapping, day 29—31 month bucket, cross-month custom daily buckets, and cross-year custom monthly buckets.
- [x] Write a distinct failing regression test for worker month comparison returning a fifth week bucket for days 29—31.
- [x] Verify each test fails for the expected boundary or mapping reason.
- [x] Implement a range object or helper methods returning `startInclusive` and `endExclusive`.
- [x] Implement shared select/group expressions and result-to-index mapping without interpolating user-provided text.
- [x] Inspect generated MyBatis wrapper SQL to prove every period query emits `>= start` and `< endExclusive`, with no remaining `BETWEEN` for dashboard time columns.
- [x] Run helper and time-range tests until green.

### Task 3: Fix both production dashboard strategies

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/main/java/com/yigongbao/module/dashboard/service/strategy/ProductionWorkerDashboardStrategy.java`
- Modify: `yigongbao-parent/yigongbao-module-dashboard/src/main/java/com/yigongbao/module/dashboard/service/strategy/ProductionManagerDashboardStrategy.java`

- [x] Remove catch-all empty-dashboard fallbacks.
- [x] Make current backlog cards real-time while retaining time filters for totals and completed throughput.
- [x] Validate manager user/center and throw a business error for invalid configuration.
- [x] Replace duplicated bucket logic with `ProductionDashboardQueryHelper`.
- [x] Split manager workload aggregation so real-time in-production and period completed use their correct time columns, then merge by producer ID.
- [x] Run Task 1 and Task 2 tests and verify all pass.

### Task 4: Add guarded historical backfill SQL

**Files:**
- Create: `sql/migration-production-record-processing-center-2026-08-13.sql`

- [x] Start a transaction, lock claimed active records that need repair, then build a temporary candidate table so application writes cannot race the candidate snapshot.
- [x] Resolve candidate center with priority: existing record ID, order center, assigned device center, current producer center.
- [x] Add conflict, unresolved, invalid/inactive center assertions before the update.
- [x] Perform an ID-preserving, name-normalizing update inside a transaction.
- [x] Add post-update assertions and audit summaries; drop temporary objects.
- [x] Run static checks proving claimed-only scope, idempotency predicates, conflict guards, and no hard-coded center IDs.
- [x] Run equivalent read-only queries against the connected database to confirm all current candidates are resolvable and conflict-free; do not execute the migration.
- [x] Execute the full script against disposable MySQL 8.0.45 and add a repeatable harness covering success, conflict/unresolved/disabled-center rollback, existing ID preservation, second-run idempotency, and concurrent claim behavior.

### Task 5: Verification and review

**Files:**
- Review all files changed by Tasks 1—4.

- [x] Run dashboard module full tests. (Dashboard passes; the `-am` aggregate is separately blocked by existing upstream `basic` test failures.)

```powershell
mvn -pl yigongbao-module-dashboard -am test
```

- [x] Run production/dashboard dependency package verification.

```powershell
mvn -pl yigongbao-module-dashboard -am "-DskipTests" package
```

- [x] Run `git diff --check` and inspect `git diff --stat`.
- [x] Use `requesting-code-review` and fix any actionable findings.
- [x] Re-run focused tests and package verification after review fixes.

### Task 6: Commit and integrate

**Files:**
- Commit only the spec, plan, SQL, dashboard production code, and dashboard tests for this task.

- [ ] Commit the reviewed implementation on `codex/production-dashboard-history-backfill`.
- [ ] Use `finishing-a-development-branch` to fast-forward merge into local `dev` after verification.
- [ ] Re-run focused dashboard tests on `dev`.
- [ ] Remove the clean worktree and merged feature branch.
- [ ] Report that the migration file was created but not executed against production.
