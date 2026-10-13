# Production Record Claim Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the claiming production worker's center ID and name on `production_record` when the design package download endpoint claims a flow card, and reject workers without a valid center binding.

**Architecture:** Keep the controller contract unchanged and implement the behavior inside the existing transactional `ProductionRecordServiceImpl.downloadDataPackage`. Read one current-user snapshot, validate it before writes, then include producer and center fields in the existing conditional claim update so ownership is atomic. Preserve the existing row lock and order-center first-assignment semantics.

**Tech Stack:** Java 17, Spring Boot transactions, MyBatis-Plus, JUnit 5, Mockito, H2 integration test support, Maven.

---

### Task 1: Add claim-center regression tests

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`

- [ ] Add a success test that captures the `LambdaUpdateWrapper` passed to `recordMapper.update`, renders its SQL setters/parameters, and proves center ID/name are required in the claim update.
- [ ] Add failure tests for missing user, missing center ID, and blank center name; assert `USER_NOT_FOUND` or `PROCESSING_CENTER_NOT_FOUND` and verify neither record nor order update runs.
- [ ] Add a zero-row claim test asserting `PRODUCTION_RECORD_ALREADY_CLAIMED` and no order update.
- [ ] Cover both the same user's repeated request and a different-center user's losing request through the zero-row conditional-update result; assert neither path updates the order or returns an address.
- [ ] Capture the order updates and verify first assignment uses the same user center snapshot as the flow card, while an already assigned order center is never overwritten.
- [ ] Run only the new tests and verify they fail for the expected missing validation/persistence behavior.

Run from `yigongbao-parent`: `mvn -pl yigongbao-module-production -am "-Dtest=ProductionRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Expected: the new tests fail before production changes.

### Task 2: Implement atomic center persistence and validation

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`

- [ ] After loading the login user, throw `USER_NOT_FOUND` if absent.
- [ ] Throw `PROCESSING_CENTER_NOT_FOUND` if `centerId` is null or `centerName` is blank.
- [ ] Add `processingCenterId` and `processingCenterName` to the existing conditional flow-card update, sourced from the same user snapshot as producer fields.
- [ ] Simplify the now-unreachable “user has no center” order branch while preserving first-assignment and non-overwrite behavior.
- [ ] Run `ProductionRecordServiceImplTest`; verify all tests pass.

### Task 3: Verify transaction rollback behavior

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordClaimTransactionTest.java`
- Use a nested focused `@TestConfiguration` in the new test rather than the full application context.
- Import datasource, MyBatis-Plus, JDBC, and transaction auto-configuration; scan only `ProductionRecordMapper`; register the real `ProductionRecordServiceImpl` behind Spring's transaction proxy.
- Use mocks only for the service's downstream collaborators (`DesignPackageMapper`, `OrderMainMapper`, `UserMapper`, flow/event/file helpers, and other constructor dependencies).

- [ ] Add a focused Spring test fixture with an in-memory H2 datasource, real `ProductionRecordMapper`, `JdbcTemplate`, and the real transaction-proxied `ProductionRecordServiceImpl`.
- [ ] Initialize the existing test `production_record` schema, seed a `DESIGN_COMPLETED` row, mock design package/current user/order reads, and make the mocked `OrderMainMapper.update` throw after the real record mapper update succeeds.
- [ ] Call the real `downloadDataPackage` method through its Spring proxy from outside a test transaction and assert the exception propagates.
- [ ] Use `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW` to query after the call; assert status, producer fields, and processing-center fields remain unchanged, proving physical rollback through Spring's transaction manager.
- [ ] Run the integration test and verify it passes.

### Task 4: Review and final verification

**Files:**
- Review all implementation and test diffs against `docs/superpowers/specs/2026-08-12-production-record-claim-center-design.md`.

- [ ] Run `git diff --check`.
- [ ] Run the focused unit and transaction tests.
- [ ] Run from `yigongbao-parent`: `mvn -pl yigongbao-module-production -am "-DskipTests" package`.
- [ ] Request code review; fix all P0/P1/P2 findings and re-run verification.
- [ ] Commit only the files belonging to this implementation.
