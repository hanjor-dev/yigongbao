# Printer State Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 扩展打印设备 0～6 状态，并用相邻状态白名单同时兼容新设备 `1→2→0`、旧设备 `1→0` 和离线恢复 `6→0` 的打印完成序列。

**Architecture:** WebSocket 地址、认证、消息体、响应和连接管理保持不变；Basic 模块负责校验、保存和发布现有 `DeviceStateChangeEvent`，Production 模块根据 `oldState/newState` 驱动生命周期。公共模块提供统一状态枚举和打印机活跃占用检查接口，Production 实现占用查询，Basic 以失败关闭方式用于手工状态和删除保护。

**Tech Stack:** Java 21、Spring Boot、Spring WebSocket、Spring Transaction、MyBatis-Plus、JUnit 5、Mockito、Maven。

---

## Scope and file map

### New files

- `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/PrinterDeviceStateEnum.java`：0～6 状态码及中文名称的唯一来源。
- `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java`：跨模块查询打印机是否被待打印/打印中流转卡占用。
- `yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/enums/PrinterDeviceStateEnumTest.java`：状态枚举边界测试。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java`：Production 对公共占用检查接口的实现。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImplTest.java`：活跃占用范围测试。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java`：统一打印机列表展示和可分配性解析。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/PrinterAvailabilityServiceTest.java`：连接、状态和活跃占用组合测试。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/device/PrinterDeviceStateConcurrencyIntegrationTest.java`：用真实 H2 事务验证手工状态与设备分配锁顺序。
- `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/controller/DeviceControllerTest.java`：手工状态参数范围回归测试。

### Existing files to modify

- `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/DeviceStatusPushDTO.java`：更新 state 注释，不改变字段。
- `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/vo/DeviceVO.java`：更新状态注释并增加 `stateName`。
- `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/convert/DeviceConvert.java`：从统一枚举填充 `stateName`。
- `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/controller/DeviceController.java`：请求参数上限改为 6，具体设备类型范围仍由 Service 校验。
- `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java`：WS 批次校验、打印机手工状态锁和活跃占用保护、删除保护。
- `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java`：批次状态及手工/删除失败关闭测试。
- `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandlerTest.java`：补齐既有 Mock，并锁定当前 WS JSON 契约。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/constants/ProductionConstants.java`：移除仅支持 0/1 的设备状态常量，改用公共枚举。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java`：开始条件和完成白名单。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`：新旧设备、离线、报警/暂停及非 PRINTING 负例。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterVO.java`：保留兼容字段，新增原始状态、中文名称、连接状态和 available。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`：打印机列表统一解析，正式分配继续在设备锁内复核状态和活跃占用。
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProcessConfigController.java`：复用统一打印机可用性解析，删除重复二态映射。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java`：完整状态和活跃占用分配测试。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`：打印机列表字段和占用状态测试。
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/controller/ProcessConfigControllerTest.java`：统一解析服务委托测试。
- `sql/ddl.sql`、`sql/ddl-prod.sql`：只更新状态注释，无字段类型迁移；现有 H2 测试表使用无注释的 `INT`，已能保存 0～6，不做无意义改动。
- `.docs/接口文档/22_加工中心与设备管理接口文档.md`、`.docs/接口文档/23-2_生产模块接口文档.md`、`.docs/功能设计/生产管理模块PRD_v1.md`、`.docs/测试文档/22_加工中心与设备管理全流程手动测试文档.md`：同步状态字典、完成白名单、手工限制和测试序列；不得改写 WS 地址、认证或消息体。

## Invariants to preserve in every task

```text
START = newState == 1 && record.status == PENDING_PRINT

FINISH = newState == 0
      && oldState in {1, 2, 6}
      && record.status == PRINTING
```

- `1→2` 只更新设备状态，流转卡仍打印中；`2→0` 才完成。
- `1→0`、`6→0` 完成；`3→0`、`4→0`、`5→0` 不完成。
- 不锁存报警/暂停历史，所以 `3/4→6→0` 最终按 `6→0` 完成。
- 只有连接在线、`state=0`、且不存在 `PENDING_PRINT/PRINTING` 占用的打印机可分配。
- 状态 5“准备就绪”不可分配，状态 1～6 均属于不可分配状态。
- 不改变现有 WebSocket Handler、配置、认证、JSON 字段及 ACK 内容。

---

### Task 1: Add the shared printer state model and usage port

**Files:**
- Create: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/PrinterDeviceStateEnum.java`
- Create: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java`
- Create: `yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/enums/PrinterDeviceStateEnumTest.java`

- [ ] **Step 1: Write failing enum tests**

```java
@ParameterizedTest
@CsvSource({
    "0,空闲", "1,工作中", "2,打印完成", "3,报警",
    "4,暂停", "5,准备就绪", "6,离线"
})
void fromCode_returnsEverySupportedState(int code, String name) {
    assertEquals(name, PrinterDeviceStateEnum.fromCode(code).getName());
}

@Test
void nullCode_isRejected() {
    Integer code = null;
    assertFalse(PrinterDeviceStateEnum.isValid(code));
}

@ParameterizedTest
@ValueSource(ints = {-1, 7})
void outOfRangeCode_isRejected(int code) {
    assertFalse(PrinterDeviceStateEnum.isValid(code));
}
```

补充断言只有 `IDLE` 的 `isAssignableState()` 为 true。

- [ ] **Step 2: Run the test and verify RED**

Run from `yigongbao-parent`:

```powershell
mvn --% -pl yigongbao-common -Dtest=PrinterDeviceStateEnumTest test
```

Expected: FAIL because `PrinterDeviceStateEnum` does not exist.

- [ ] **Step 3: Implement the enum and port**

```java
@Getter
@RequiredArgsConstructor
public enum PrinterDeviceStateEnum {
    IDLE(0, "空闲"),
    WORKING(1, "工作中"),
    PRINT_FINISHED(2, "打印完成"),
    ALARM(3, "报警"),
    PAUSED(4, "暂停"),
    READY(5, "准备就绪"),
    OFFLINE(6, "离线");

    private final Integer code;
    private final String name;

    public static PrinterDeviceStateEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }

    public boolean isAssignableState() {
        return this == IDLE;
    }
}
```

```java
public interface PrinterDeviceUsageChecker {
    Set<Long> findActiveDeviceIds(Collection<Long> deviceIds);

    default boolean isInUse(Long deviceId) {
        return deviceId != null
                && findActiveDeviceIds(List.of(deviceId)).contains(deviceId);
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run: same Maven command as Step 2.

Expected: `PrinterDeviceStateEnumTest` passes.

- [ ] **Step 5: Commit**

```powershell
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/PrinterDeviceStateEnum.java yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/enums/PrinterDeviceStateEnumTest.java
git commit -m "feat(common): define printer device states"
```

---

### Task 2: Implement and integration-test one active-usage definition in Production

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java`
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImplTest.java`

- [ ] **Step 1: Write a failing real-H2 usage test**

Use `@SpringBootTest(classes = ProductionTestConfiguration.class)`、`@ActiveProfiles("test")` and the existing Production H2 schema. Insert these records through the real mapper:

```text
requested device 10 + PENDING_PRINT + is_deleted=0 => returned
requested device 20 + PRINTING + is_deleted=0 => returned
requested device 30 + PRINT_COMPLETED => excluded
requested device 30 + PRINTING + is_deleted=1 => excluded
unrequested device 40 + PRINTING => excluded
```

Then assert:

```java
assertEquals(Set.of(10L, 20L), checker.findActiveDeviceIds(List.of(10L, 20L, 30L)));
assertTrue(checker.isInUse(10L));
assertEquals(Set.of(), checker.findActiveDeviceIds(List.of()));
```

The database result, not a mocked return value, proves the query contains the deleted flag, active status set and requested device range. Add a separate mock-based empty-input test only to prove no mapper query occurs for null/empty input.

- [ ] **Step 2: Run the test and verify RED**

```powershell
mvn --% -pl yigongbao-module-production -am -Dtest=PrinterDeviceUsageCheckerImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the implementation does not exist; a stub without every filter must still fail the fixture assertions.

- [ ] **Step 3: Implement the checker**

Use one bulk `selectList` selecting only `printDeviceId`, then return a distinct `Set<Long>`. Return an empty set without querying when input is null or empty. Do not query device state here; this port answers only production occupation.

- [ ] **Step 4: Run the test and verify GREEN**

Run: same Maven command as Step 2.

Expected: all checker tests pass.

- [ ] **Step 5: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImplTest.java
git commit -m "feat(production): expose active printer usage"
```

---

### Task 3: Validate and display all incoming device states without changing WS

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/DeviceStatusPushDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/vo/DeviceVO.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/convert/DeviceConvert.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java:198-288`
- Test: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java`

- [ ] **Step 1: Write failing Basic tests**

Add parameterized tests proving states 0～6 are accepted and persisted for existing printers and auto-created printers. Add invalid tests for null, -1 and 7 that assert:

```java
assertThrows(BusinessException.class, () -> deviceService.batchUpdateDeviceStatus(dto));
verify(deviceMapper, never()).updateById(any());
verifyNoInteractions(eventPublisher);
```

Add a conversion test asserting state 2 produces `stateName="打印完成"`.

Add a mixed existing-device batch: printer state 6 plus non-printer state 2. Assert the whole batch throws, neither device is updated, no logs are saved and no event is published. Add the valid counterpart with printer state 6 plus non-printer state 1.

Extend the first-report auto-create test with `verifyNoInteractions(eventPublisher)` so initial registration cannot start or finish a flow card.

- [ ] **Step 2: Run the tests and verify RED**

```powershell
mvn --% -pl yigongbao-module-basic -am -Dtest=DeviceServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: invalid values are currently accepted or fail with an uncontrolled null error; `stateName` does not exist.

- [ ] **Step 3: Implement service validation and state name**

After loading `existingDevices` but before adding anything to `toCreate`、`toUpdate`、`stateLogs` or `stateChangeEvents`, validate the complete batch by device type:

```java
private void validateReportedState(DeviceEntity existing, Integer state) {
    if (existing == null || DeviceTypeEnum.PRINTER_SLA.getCode().equals(existing.getDeviceType())) {
        if (!PrinterDeviceStateEnum.isValid(state)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER.getCode(),
                    "打印设备状态必须为0-6");
        }
        return;
    }
    if (state == null || (state != 0 && state != 1)) {
        throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER.getCode(),
                "非打印设备状态必须为0或1");
    }
}
```

New devices on this WebSocket path are treated as auto-registered printers. Perform this validation pass over every item before the later write-building pass, so a mixed invalid batch cannot partially write.

Add to `DeviceVO`:

```java
private Integer state;
private String stateName;
```

Populate `stateName` in `DeviceConvert.toVO`. Update comments only in `DeviceStatusPushDTO`; do not rename `centerName`, `devices`, `id` or `state`.

- [ ] **Step 4: Run tests and verify GREEN**

Run: same Maven command as Step 2.

Expected: Basic device tests pass.

- [ ] **Step 5: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/dto/DeviceStatusPushDTO.java yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/vo/DeviceVO.java yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/convert/DeviceConvert.java yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java
git commit -m "feat(basic): accept complete printer states"
```

---

### Task 4: Make manual state and deletion fail closed under the device lock

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/controller/DeviceController.java:75-88`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java:134-165,387-407`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java`
- Create: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/controller/DeviceControllerTest.java`
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/device/PrinterDeviceStateConcurrencyIntegrationTest.java`

- [ ] **Step 1: Write failing service tests**

Inject a mock `PrinterDeviceUsageChecker` into `DeviceServiceImpl`. Cover:

- printer state 0～6 is accepted only when the checker returns false;
- non-printer state 2～6 is rejected;
- an active printer rejects every manual state and never calls `updateById` or saves a log;
- a missing checker or checker exception rejects printer manual update and deletion without data changes;
- an active printer cannot be deleted even when its device state is 0;
- non-printer deletion keeps existing behavior.

The unit test may additionally verify local ordering:

```java
InOrder order = inOrder(deviceMapper, usageChecker);
order.verify(deviceMapper).selectByIdForUpdate(deviceId);
order.verify(usageChecker).isInUse(deviceId);
```

This Mockito assertion is not accepted as concurrency proof; the real database test below is mandatory.

- [ ] **Step 2: Write failing controller tests**

Using `@WebMvcTest(DeviceController.class)`, assert state 6 reaches the mocked service and state 7 returns validation error. Do not add or change authentication/WS tests here.

- [ ] **Step 3: Write the failing real-transaction concurrency test**

Use `@SpringBootTest(classes = ProductionTestConfiguration.class)`、`@ActiveProfiles("test")`、`@Transactional(propagation = NOT_SUPPORTED)`、two executors and `TransactionTemplate`:

1. Seed one online state-0 printer.
2. Transaction A calls the real `deviceMapper.selectByIdForUpdate(id)`, signals `deviceLocked`, waits on `allowBind`, inserts a `PENDING_PRINT` record bound to that device, then commits.
3. After `deviceLocked`, transaction B calls the real `deviceService.updateDeviceState(id, 6)`.
4. Assert B has not completed before `allowBind` is released, proving it waits for the device row lock.
5. Release A, then assert B fails with `DEVICE_NOT_AVAILABLE` after observing the newly committed active record.
6. Reload the device and assert its state remains 0.

Use bounded waits (`CountDownLatch.await(5, SECONDS)` and `Future.get(5, SECONDS)`); always shut down executors in `finally`.

- [ ] **Step 4: Run all Task 4 tests and verify RED**

```powershell
mvn --% -pl yigongbao-module-basic -am -Dtest=DeviceServiceImplTest,DeviceControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn --% -pl yigongbao-module-production -am -Dtest=PrinterDeviceStateConcurrencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: state 6 is blocked by `@Max(1)`; service methods do not lock/check active usage; the real concurrency test shows the manual update can overwrite state instead of failing after the active record commits.

- [ ] **Step 5: Implement the locked, failure-closed flow**

Use the existing `DeviceMapper.selectByIdForUpdate(id)` inside the existing transaction. For printer devices:

```java
DeviceEntity device = deviceMapper.selectByIdForUpdate(id);
validateStateRangeByDeviceType(device, state);
PrinterDeviceUsageChecker checker = requireUsageChecker();
if (checker.isInUse(device.getId())) {
    throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
}
// only now update state or delete
```

Catch checker infrastructure errors only to add context and rethrow a system/business error; never continue. Change controller validation to `@Min(0) @Max(6)`, leaving type-specific validation in Service.

Formal assignment already locks the device before binding a record. This shared lock order serializes manual update/delete with assignment and closes the check-then-write race.

- [ ] **Step 6: Run unit and concurrency tests and verify GREEN**

Run: both Maven commands from Step 4.

Expected: Basic unit/controller tests and the real H2 concurrency test pass.

- [ ] **Step 7: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/controller/DeviceController.java yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/service/impl/DeviceServiceImpl.java yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/service/DeviceServiceImplTest.java yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/controller/DeviceControllerTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/device/PrinterDeviceStateConcurrencyIntegrationTest.java
git commit -m "fix(basic): protect active printer state changes"
```

---

### Task 5: Replace binary lifecycle logic with the approved transition whitelist

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java:47-232`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/constants/ProductionConstants.java:20-22`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`

- [ ] **Step 1: Write failing start-transition tests**

Parameterize old states 0, 3, 4, 5 and assert `newState=1` starts only a `PENDING_PRINT` record. Add `0→2/3/4/5/6` tests proving none starts printing.

- [ ] **Step 2: Run the start tests and verify RED**

```powershell
mvn --% -pl yigongbao-module-production -am -Dtest=DeviceStatusListenerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: current `0→nonzero` condition incorrectly starts for states 2～6 or misses nonzero→1 recovery.

- [ ] **Step 3: Implement only the start predicate**

```java
private boolean isPrintStart(Integer newState) {
    return PrinterDeviceStateEnum.WORKING.getCode().equals(newState);
}
```

Keep the existing `PENDING_PRINT` query, conditional record update and downstream side effects unchanged.

- [ ] **Step 4: Run tests and verify start GREEN**

Expected: all start tests pass; completion tests may still be red until the next step.

- [ ] **Step 5: Write failing completion-whitelist tests**

Add parameterized tests:

```java
@ValueSource(ints = {1, 2, 6})
void idleAfterAllowedState_completesPrinting(int oldState) { ... }

@ValueSource(ints = {3, 4, 5})
void idleAfterBlockedState_keepsPrinting(int oldState) { ... }
```

Add explicit sequence assertions:

- `1→2` has no production update; `2→0` completes once;
- `1→0` completes once;
- `1→6` has no production update; `6→0` completes once;
- `3/4→6→0` completes at the final event because there is no alarm/pause latch;
- repeated 0/2 does not repeat schedule or Flow effects.

Parameterize `oldState={1,2,6}` with record status `PENDING_PRINT`, `PRINT_COMPLETED` and one other non-`PRINTING` status. Assert no record update, no process completion, no post-processing schedule and no Flow trigger.

- [ ] **Step 6: Run completion tests and verify RED**

Run: same Maven command as Step 2.

Expected: current `nonzero→0` incorrectly completes 3/4/5→0 and lacks the explicit state-2 intermediate contract.

- [ ] **Step 7: Implement the completion predicate**

```java
private static final Set<Integer> PRINT_FINISH_PREVIOUS_STATES = Set.of(
        PrinterDeviceStateEnum.WORKING.getCode(),
        PrinterDeviceStateEnum.PRINT_FINISHED.getCode(),
        PrinterDeviceStateEnum.OFFLINE.getCode());

private boolean isPrintFinish(Integer oldState, Integer newState) {
    return PrinterDeviceStateEnum.IDLE.getCode().equals(newState)
            && PRINT_FINISH_PREVIOUS_STATES.contains(oldState);
}
```

Run completion only against `PRINTING` records. Preserve the existing conditional update gate so `printFinishTime`, process completion, post-processing schedule and Flow aggregation execute only after exactly one successful status transition. Remove the two binary device constants from `ProductionConstants` after all call sites use the enum.

- [ ] **Step 8: Run listener tests and verify GREEN**

Run: same Maven command as Step 2.

Expected: `DeviceStatusListenerTest` passes with all existing side-effect assertions retained.

- [ ] **Step 9: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/constants/ProductionConstants.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java
git commit -m "feat(production): support printer completion sequences"
```

---

### Task 6: Unify printer availability across lists and assignment

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java`
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/PrinterAvailabilityServiceTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java:623-720,932-939`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProcessConfigController.java:74-129`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/controller/ProcessConfigControllerTest.java`

- [ ] **Step 1: Write failing resolver tests**

Test the truth table:

```text
online + state0 + no active record = available
offline connection + state0 = unavailable
online + state1..6 = unavailable
online + state0 + PENDING_PRINT/PRINTING active record = unavailable
```

Assert `PrinterVO` preserves `status/statusName` for current consumers and also returns:

```java
deviceState
deviceStateName
connectionStatus
available
```

- [ ] **Step 2: Run resolver tests and verify RED**

```powershell
mvn --% -pl yigongbao-module-production -am -Dtest=PrinterAvailabilityServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: resolver and new VO fields do not exist.

- [ ] **Step 3: Implement one resolver**

Bulk-load active device IDs once through `PrinterDeviceUsageChecker.findActiveDeviceIds`. Compute:

```java
boolean available = connectionStatus == 1
        && PrinterDeviceStateEnum.IDLE.getCode().equals(device.getState())
        && !activeDeviceIds.contains(device.getId());
```

Expose one policy entry used by every caller:

```java
public boolean isAvailable(DeviceEntity device, boolean activeUsage) { ... }

public void requireAvailable(DeviceEntity device, boolean activeUsage) {
    if (!isAvailable(device, activeUsage)) {
        throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
    }
}
```

List callers obtain `activeUsage` from one bulk set; locked assignment obtains it from `usageChecker.isInUse(deviceId)`. Keep compatibility `status=0` for available and `status=1` for unavailable, and update the misleading comment/documentation instead of changing its numeric meaning.

- [ ] **Step 4: Write failing list and assignment tests**

Cover both `ProductionRecordServiceImpl.listPrinters` and `ProcessConfigController` using the shared resolver. Add assignment tests that verify `PrinterAvailabilityService.requireAvailable(...)` is the policy invoked inside the device lock. States 1～6 and state 0 with another active record must throw `DEVICE_NOT_AVAILABLE`; state 0 with no conflict succeeds.

- [ ] **Step 5: Run list/assignment tests and verify RED**

```powershell
mvn --% -pl yigongbao-module-production -am -Dtest=PrinterAvailabilityServiceTest,ProductionRecordAssignDeviceTest,ProductionRecordServiceImplTest,ProcessConfigControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: state-only list mapping can show state0 as available despite active usage, and new fields are missing.

- [ ] **Step 6: Wire both list paths and retain locked assignment validation**

Replace both duplicate stream mappings and the private `resolveDeviceStatus` with `PrinterAvailabilityService`. In `assignDevice`, keep the existing order and call the same policy:

```text
lock device
→ usageChecker.isInUse(deviceId)
→ printerAvailabilityService.requireAvailable(device, activeUsage)
→ lock/revalidate target record
→ bind
```

Remove the duplicate hand-coded state/connection predicate and the separate conflict-policy branch after tests prove the shared entry is used. The device row lock serializes competing assignments and manual changes; the locked path recomputes active usage and never trusts the list result.

- [ ] **Step 7: Run tests and verify GREEN**

Run: same command as Step 5.

Expected: all four classes pass.

- [ ] **Step 8: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/PrinterAvailabilityServiceTest.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterVO.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProcessConfigController.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/controller/ProcessConfigControllerTest.java
git commit -m "fix(production): unify printer availability"
```

---

### Task 7: Lock the existing WebSocket contract and update schema/docs

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandlerTest.java`
- Modify: `sql/ddl.sql`
- Modify: `sql/ddl-prod.sql`
- Modify: `.docs/接口文档/22_加工中心与设备管理接口文档.md`
- Modify: `.docs/接口文档/23-2_生产模块接口文档.md`
- Modify: `.docs/功能设计/生产管理模块PRD_v1.md`
- Modify: `.docs/测试文档/22_加工中心与设备管理全流程手动测试文档.md`

- [ ] **Step 1: Repair the existing handler test fixture and add characterization coverage**

Add mocks for `ProcessingCenterMapper` and `IProcessingCenterService`, which are constructor dependencies already used by the handler. Keep the current payload fields and assert the exact ACK:

```java
verify(session).sendMessage(new TextMessage("{\"code\":200,\"message\":\"success\"}"));
```

Add a state-6 payload test proving the existing JSON shape reaches `batchUpdateDeviceStatus`; do not modify `DeviceWebSocketHandler` or WebSocket config.

This is an explicit characterization/regression step, not a new red-green behavior change: the existing success test is RED because its fixture is missing current constructor dependencies; after repairing those mocks, the state-6 contract test is expected to pass immediately against unchanged production code.

- [ ] **Step 2: Run handler tests**

```powershell
mvn --% -pl yigongbao-module-basic -am -Dtest=DeviceWebSocketHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: tests pass after the missing mocks are supplied; production WS code remains unchanged.

- [ ] **Step 3: Update schema comments only**

Use this exact state description wherever the printer state column is documented:

```text
0=空闲，1=工作中，2=打印完成，3=报警，4=暂停，5=准备就绪，6=离线
```

Do not add columns, indexes, migrations or protocol metadata.

- [ ] **Step 4: Update API and manual-test documentation**

Document:

- current WS URL/auth/body/ACK unchanged;
- state 1 starts printing;
- state 2 is an intermediate new-device signal;
- `1→0`、`2→0`、`6→0` complete only a `PRINTING` card;
- `3→0`、`4→0`、`5→0` do not complete;
- only state0 plus no active occupation is assignable;
- printer manual update accepts 0～6 only when no active card; non-printer remains 0/1;
- no alarm notification or handling feature.

Remove or correct every conflicting 0/1-only statement in the named documents. Do not opportunistically rewrite unrelated historical documents.

- [ ] **Step 5: Verify targeted docs and diffs**

```powershell
rg -n "0=空闲，1=占用|state: 1→0|状态 2 是唯一|6→0.*不" sql/ddl.sql sql/ddl-prod.sql .docs/接口文档/22_加工中心与设备管理接口文档.md .docs/接口文档/23-2_生产模块接口文档.md .docs/功能设计/生产管理模块PRD_v1.md .docs/测试文档/22_加工中心与设备管理全流程手动测试文档.md
git diff --check
```

Expected: no stale rule remains in the scoped files; diff check exits 0.

- [ ] **Step 6: Commit**

```powershell
git add yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandlerTest.java sql/ddl.sql sql/ddl-prod.sql .docs/接口文档/22_加工中心与设备管理接口文档.md .docs/接口文档/23-2_生产模块接口文档.md .docs/功能设计/生产管理模块PRD_v1.md .docs/测试文档/22_加工中心与设备管理全流程手动测试文档.md
git commit -m "docs(device): document complete printer states"
```

---

### Task 8: Run complete verification and review the feature diff

**Files:**
- Review all files changed since Task 1.

- [ ] **Step 1: Run focused regression tests**

```powershell
mvn --% -pl yigongbao-module-basic,yigongbao-module-production -am -Dtest=PrinterDeviceStateEnumTest,DeviceServiceImplTest,DeviceControllerTest,DeviceWebSocketHandlerTest,PrinterDeviceUsageCheckerImplTest,PrinterDeviceStateConcurrencyIntegrationTest,PrinterAvailabilityServiceTest,DeviceStatusListenerTest,ProductionRecordAssignDeviceTest,ProductionRecordServiceImplTest,ProcessConfigControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all named tests pass with 0 failures and 0 errors.

- [ ] **Step 2: Run Basic module tests**

```powershell
mvn --% -pl yigongbao-module-basic -am test
```

Expected: BUILD SUCCESS. If an unrelated baseline failure occurs, capture its exact class and assertion; do not weaken new tests.

- [ ] **Step 3: Run Production module tests**

```powershell
mvn --% -pl yigongbao-module-production -am test
```

Expected: BUILD SUCCESS. If Reactor is blocked by a known upstream module baseline, install the already-verified dependency with tests skipped and rerun the Production module directly; record both commands and outputs.

- [ ] **Step 4: Run compile and static diff checks**

```powershell
mvn --% -pl yigongbao-module-basic,yigongbao-module-production -am -DskipTests compile
git diff --check
git status --short
```

Expected: compile succeeds, diff check exits 0, and only intended feature files are modified.

- [ ] **Step 5: Review the final state matrix against tests**

Explicitly map tests to:

```text
0→1 START
1→2 NO-OP
2→0 FINISH
1→0 FINISH
1→6 NO-OP
6→0 FINISH
3→0 NO-OP
4→0 NO-OP
5→0 NO-OP
```

Also verify the active-record guard on every finish transition and the state0-plus-active-record allocation block.

- [ ] **Step 6: Request code review**

Use `@requesting-code-review` on the complete feature diff. Fix all blocking/high-risk findings and rerun the relevant verification commands.

- [ ] **Step 7: Commit any review-only fixes**

```powershell
git add <only reviewed feature files>
git commit -m "fix(device): address printer state review"
```

Skip this commit if review requires no changes.
