# Order Cancel Approval Fix Implementation Plan

> **For agentic workers:** Execute task-by-task with TDD and verification before completion.

**Goal:** Make order cancel-application submission, approval, rejection, and queries consistent, authorized, concurrency-safe, and verifiable.

**Architecture:** Keep the existing Controller/Service boundary. Align application status codes with the deployed SQL contract, use conditional updates for state transitions, validate order access in the Service, and publish notifications after transaction commit.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, MySQL.

---

### Task 1: Add regression tests for status and terminal-order validation

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderCancelApplyServiceImplTest.java`

- [ ] Add tests asserting status codes are 1/2/3 and completed orders cannot submit.
- [ ] Run the focused tests and observe the terminal-order test fail.

### Task 2: Add regression tests for conditional approval updates

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderCancelApplyServiceImplTest.java`

- [ ] Add tests for order update failure and second approval rejection.
- [ ] Run the focused tests and observe failures before implementation.

### Task 3: Add regression tests for query authorization

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/service/impl/OrderCancelApplyServiceImplTest.java`

- [ ] Add tests that non-design-admin users cannot list pending applications and unrelated users cannot view details/history.
- [ ] Run the focused tests and observe failures before implementation.

### Task 4: Implement consistent state transitions and authorization

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/enums/ApplyStatusEnum.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderCancelApplyServiceImpl.java`
- Modify: `sql/ddl.sql`
- Modify: `sql/migration-online-schema-2026-07-16.sql`
- Modify: `sql/migration-online-data-2026-07-16.sql`

- [ ] Align Java and SQL status values.
- [ ] Reject terminal orders and check all conditional update results.
- [ ] Protect approval/rejection with pending-state and order-version predicates.
- [ ] Enforce role/ownership checks for query endpoints.

### Task 5: Make notification delivery transaction-safe

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/listener/OrderCancelApplyEventListener.java`

- [ ] Replace immediate async event listeners with after-commit listeners.

### Task 6: Verify

- [ ] Run focused controller/service tests.
- [ ] Run all order-module tests and compile the reactor.
- [ ] Inspect the final diff and report residual risks.
