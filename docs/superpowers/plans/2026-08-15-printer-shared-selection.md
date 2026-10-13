# Printer Shared Selection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow multiple active production records to share an online idle SLA printer after explicit confirmation, and expose a backend occupation query for the frontend warning flow.

**Architecture:** Separate physical printer selectability from production-record occupation. `PrinterAvailabilityService` owns type/connection/state validation, `PrinterDeviceUsageChecker` owns active-record lookups (including exclusion of the current record), and `ProductionRecordServiceImpl` orchestrates permission-aware occupation queries and locked assignments. The existing WebSocket and batch device-state listener remain unchanged.

**Tech Stack:** Java 21, Spring Boot 3.2, Spring MVC, MyBatis-Plus, Sa-Token, JUnit 5, Mockito, AssertJ, Maven

---

## Scope and baseline

- Backend only. Do not edit `frontend/dist` or any WebSocket authentication/payload code.
- Design source: `docs/superpowers/specs/2026-08-15-printer-shared-selection-design.md`.
- Production-module baseline: `mvn -pl yigongbao-module-production test -DskipTests=false` passes with 309 tests.
- `mvn -pl yigongbao-module-production -am test -DskipTests=false` is currently blocked by pre-existing `yigongbao-module-basic` registration-certificate controller failures. Do not treat those failures as regressions from this feature.

## File map

### Create

- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterOccupationVO.java` — minimal occupation response contract.
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordPrinterOccupationTest.java` — focused service tests for existence, type, permission, exclusion, and response behavior.
- `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterSharedAssignmentConcurrencyIntegrationTest.java` — real-transaction proof that concurrent assignments serialize and require confirmation.

### Modify

- `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java` — add occupation-confirmation error code.
- `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java` — add an active-use query that excludes one record.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java` — implement the exclusion-aware lookup.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java` — make availability depend only on SLA type, connection, and state.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/AssignDeviceDTO.java` — add `confirmOccupied`.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/IProductionRecordService.java` — expose the occupation query.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java` — implement query and locked confirmation flow.
- `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java` — add the GET endpoint.
- Existing production tests listed in the tasks below — update contracts and regression expectations.

## Task 1: Add exclusion-aware production occupation lookup

**Files:**

- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImplTest.java`

- [ ] **Step 1: Write failing exclusion tests**

Add records for one device in `PENDING_PRINT`, `PRINTING`, completed, and deleted states. Verify the new method excludes only the supplied record and still finds another active record:

```java
assertThat(checker.isInUseByOtherRecord(10L, currentRecordId)).isTrue();
assertThat(checker.isInUseByOtherRecord(20L, currentRecordId)).isFalse();
assertThat(checker.isInUseByOtherRecord(null, currentRecordId)).isFalse();
```

Update the test helper to accept an explicit record ID so the exclusion predicate is observable.

- [ ] **Step 2: Run the focused test and verify it fails**

Run from `yigongbao-parent`:

```powershell
mvn -pl yigongbao-module-production -Dtest=PrinterDeviceUsageCheckerImplTest test
```

Expected: compilation failure because `isInUseByOtherRecord` does not exist.

- [ ] **Step 3: Add the port method, implementation, and error code**

Add to the common port:

```java
boolean isInUseByOtherRecord(Long deviceId, Long excludedRecordId);
```

Implement it with the same active definition used by `findActiveDeviceIds`:

```java
return recordMapper.exists(new LambdaQueryWrapper<ProductionRecordEntity>()
        .eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
        .eq(ProductionRecordEntity::getIsDeleted, 0)
        .in(ProductionRecordEntity::getStatus,
                FlowStatusEnum.PENDING_PRINT.getValue(),
                FlowStatusEnum.PRINTING.getValue())
        .ne(excludedRecordId != null, ProductionRecordEntity::getId, excludedRecordId));
```

Return `false` without querying when `deviceId` is null. Add a unique production error after the existing highest production code:

```java
PRINTER_OCCUPIED_CONFIRM_REQUIRED(840, "打印机已被占用，请确认后重试", 3),
```

- [ ] **Step 4: Install the changed reactor dependencies, then run common and occupation tests**

```powershell
mvn -pl yigongbao-module-production -am install -DskipTests
mvn -pl yigongbao-common -Dtest=ErrorCodeEnumTest test
mvn -pl yigongbao-module-production -Dtest=PrinterDeviceUsageCheckerImplTest test
```

Expected: the reactor compiles and installs the updated common contract first; both focused test commands then pass. Do not run a production-only build against a stale locally installed common JAR.

- [ ] **Step 5: Commit the occupation primitive**

```powershell
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/service/PrinterDeviceUsageChecker.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImpl.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterDeviceUsageCheckerImplTest.java
git commit -m "feat(production): query printer use by other records"
```

## Task 2: Decouple printer selectability from active production use

**Files:**

- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/PrinterAvailabilityServiceTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/controller/ProcessConfigControllerTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java`

- [ ] **Step 1: Rewrite availability expectations first**

Update tests so an online idle SLA printer remains selectable regardless of active record use. Remove occupation mocks and verifications from printer-list tests. Assert:

```java
assertThat(service.isAvailable(onlineIdleSlaPrinter)).isTrue();
assertThat(result).extracting(PrinterVO::getAvailable).containsExactly(true, true);
assertThat(result).extracting(PrinterVO::getStatus).containsExactly(0, 0);
assertThat(result).extracting(PrinterVO::getStatusName).containsExactly("空闲", "空闲");
```

Add a non-`PRINTER_SLA` case expecting `DEVICE_TYPE_MISMATCH` from the hard guard, and update test device helpers to set `DeviceTypeEnum.PRINTER_SLA.getCode()` by default. Keep non-printer `ProcessConfigController` expectations unchanged because that controller uses its separate `toNonPrinterVO` path.

- [ ] **Step 2: Run focused tests and verify failure**

```powershell
mvn -pl yigongbao-module-production -Dtest=PrinterAvailabilityServiceTest,ProcessConfigControllerTest,ProductionRecordServiceImplTest,ProductionRecordAssignDeviceTest test
```

Expected: failures show active usage still makes idle printers unavailable and current method signatures still require `activeUsage`.

- [ ] **Step 3: Implement physical selectability only**

Remove `PrinterDeviceUsageChecker` from `PrinterAvailabilityService`. Replace the old signatures with:

```java
public boolean isAvailable(DeviceEntity device) {
    return device != null
            && DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())
            && Integer.valueOf(1).equals(device.getConnectionStatus())
            && PrinterDeviceStateEnum.IDLE.getCode().equals(device.getState());
}

public void requireAvailable(DeviceEntity device) {
    if (!DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())) {
        throw new BusinessException(ErrorCodeEnum.DEVICE_TYPE_MISMATCH);
    }
    if (!isAvailable(device)) {
        throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
    }
}
```

Map each `PrinterVO` without querying active use. Set `status/statusName/available` to `0/"空闲"/true` when physically selectable and `1/"不可用"/false` otherwise. Preserve `deviceState`, `deviceStateName`, and `connectionStatus`.

- [ ] **Step 4: Run focused availability and list tests**

```powershell
mvn -pl yigongbao-module-production -Dtest=PrinterAvailabilityServiceTest,ProcessConfigControllerTest,ProductionRecordServiceImplTest,ProductionRecordAssignDeviceTest test
```

Expected: pass; occupied online-idle printers remain selectable, while states 1–6, offline, unknown, null, and non-printer devices remain blocked.

- [ ] **Step 5: Commit the availability separation**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/PrinterAvailabilityService.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/PrinterAvailabilityServiceTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/process/controller/ProcessConfigControllerTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java
git commit -m "refactor(production): separate printer availability from use"
```

## Task 3: Expose the permission-aware occupation query API

**Files:**

- Create: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterOccupationVO.java`
- Create: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordPrinterOccupationTest.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/IProductionRecordService.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java`

- [ ] **Step 1: Write controller contract tests**

Mock `recordService.getPrinterOccupation(7L, 8L)` to return `new PrinterOccupationVO(true)`, then assert:

```java
mockMvc.perform(get("/production/record/{recordId}/printer-occupation", 7L)
        .param("deviceId", "8"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.occupied").value(true));
```

Add a missing-`deviceId` test expecting HTTP 400 and no service interaction.

- [ ] **Step 2: Write focused service tests**

Create `ProductionRecordPrinterOccupationTest` with mocked record, device, user, and usage dependencies. Cover:

- existing authorized record + SLA device + another active record returns `occupied=true`;
- the checker is invoked as `isInUseByOtherRecord(deviceId, recordId)`;
- no other record returns `occupied=false`;
- missing record returns `PRODUCTION_RECORD_NOT_FOUND`;
- missing device returns `PRINT_DEVICE_NOT_FOUND`;
- non-SLA device returns `DEVICE_TYPE_MISMATCH`;
- production worker from another processing center receives the existing permission error and the occupation checker is not called.

- [ ] **Step 3: Run the new tests and verify failure**

```powershell
mvn -pl yigongbao-module-production -Dtest=ProductionRecordControllerTest,ProductionRecordPrinterOccupationTest test
```

Expected: compilation failures because the VO, service method, and endpoint do not exist.

- [ ] **Step 4: Implement the response, service method, and controller**

Use a minimal immutable response:

```java
@Getter
@AllArgsConstructor
public class PrinterOccupationVO {
    private final Boolean occupied;
}
```

Add to `IProductionRecordService`:

```java
PrinterOccupationVO getPrinterOccupation(Long recordId, Long deviceId);
```

Implementation order:

1. read record and throw `PRODUCTION_RECORD_NOT_FOUND` if absent;
2. read device and throw `PRINT_DEVICE_NOT_FOUND` if absent;
3. reject non-SLA device with `DEVICE_TYPE_MISMATCH`;
4. load current user and call existing `validateDeviceOperationAccess`;
5. return `new PrinterOccupationVO(printerDeviceUsageChecker.isInUseByOtherRecord(deviceId, recordId))`.

Controller:

```java
@Operation(summary = "查询打印机是否被其他流转卡选择")
@GetMapping("/{recordId}/printer-occupation")
public Result<PrinterOccupationVO> getPrinterOccupation(
        @PathVariable Long recordId,
        @RequestParam Long deviceId) {
    return Result.success(recordService.getPrinterOccupation(recordId, deviceId));
}
```

- [ ] **Step 5: Run occupation API tests**

```powershell
mvn -pl yigongbao-module-production -Dtest=ProductionRecordControllerTest,ProductionRecordPrinterOccupationTest test
```

Expected: pass with boolean-only response and permission checks before usage disclosure.

- [ ] **Step 6: Commit the occupation API**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/PrinterOccupationVO.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/IProductionRecordService.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordPrinterOccupationTest.java
git commit -m "feat(production): expose printer occupation query"
```

## Task 4: Require confirmation before sharing an occupied printer

**Files:**

- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/AssignDeviceDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterSharedAssignmentConcurrencyIntegrationTest.java`

- [ ] **Step 1: Write assignment confirmation tests**

Replace the old “active usage is unavailable” test with these cases:

```java
// occupied + null/false confirmation
assertThatThrownBy(() -> service.assignDevice(recordId, dto))
        .isInstanceOfSatisfying(BusinessException.class, ex ->
                assertThat(ex.getCode()).isEqualTo(
                        ErrorCodeEnum.PRINTER_OCCUPIED_CONFIRM_REQUIRED.getCode()));

// occupied + true confirmation
dto.setConfirmOccupied(true);
service.assignDevice(recordId, dto);
verify(productNumberService).generateFormalNumbers(recordId, deviceId, usageCount);
```

Also cover:

- confirmation cannot bypass a non-SLA, offline, or states 1–6 device;
- duplicate assignment returns `RECORD_DEVICE_ALREADY_ASSIGNED` before occupation disclosure;
- unauthorized access fails before occupation disclosure;
- usage lookup failure rolls back and does not write products, process, record, counter, or number;
- call order is device lock/physical guard, record lock/status guard, access check, then `isInUseByOtherRecord(deviceId, recordId)`.

Add controller JSON binding assertions for omitted, `false`, and `true` `confirmOccupied` values.

Add a shared-release isolation test: prepare two active records referencing the same printer, release the target pending record, and assert only the target loses its printer fields/process configuration while the second record retains the same printer ID, code, name, and status.

- [ ] **Step 2: Run focused tests and verify failure**

```powershell
mvn -pl yigongbao-module-production -Dtest=ProductionRecordControllerTest,ProductionRecordAssignDeviceTest test
```

Expected: failures because `confirmOccupied` is absent and occupied printers still follow the old hard rejection path.

- [ ] **Step 3: Implement the confirmation flow**

Add to `AssignDeviceDTO`:

```java
/** 是否已确认共享被其他活跃流转卡选择的打印机 */
private Boolean confirmOccupied;
```

In `assignDevice`, preserve `REPEATABLE_READ` and the device-before-record lock order, but reorder validation as follows:

```java
DeviceEntity device = deviceMapper.selectByIdForUpdate(dto.getDeviceId());
if (device == null) {
    throw new BusinessException(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND);
}
printerAvailabilityService.requireAvailable(device);

ProductionRecordEntity record = baseMapper.selectByIdForUpdate(recordId);
if (record == null) {
    throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
}
validateRecordCanAssignDevice(record);

Long userId = StpUtil.getLoginIdAsLong();
UserEntity currentUser = userMapper.selectById(userId);
validateDeviceOperationAccess(currentUser, record, device);

boolean occupied = printerDeviceUsageChecker.isInUseByOtherRecord(device.getId(), recordId);
if (occupied && !Boolean.TRUE.equals(dto.getConfirmOccupied())) {
    throw new BusinessException(ErrorCodeEnum.PRINTER_OCCUPIED_CONFIRM_REQUIRED);
}
```

Keep all existing product weights, record/process updates, usage count, and product numbering after this guard.

- [ ] **Step 4: Run assignment and controller tests**

```powershell
mvn -pl yigongbao-module-production -Dtest=ProductionRecordControllerTest,ProductionRecordAssignDeviceTest test
```

Expected: pass; active occupation requires explicit confirmation and physical/type restrictions remain hard failures.

- [ ] **Step 5: Commit assignment confirmation**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/dto/AssignDeviceDTO.java yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java
git commit -m "feat(production): confirm shared printer assignment"
```

- [ ] **Step 6: Write a real concurrent-assignment integration test**

Create `PrinterSharedAssignmentConcurrencyIntegrationTest` using the H2 production test context and real Spring transactions. Seed one online idle SLA printer, an admin user, two assignable records, their print-process rows, and required products. In two executor threads, establish Sa-Token login context and call the real `IProductionRecordService.assignDevice` for different records targeting the same printer.

Use a test-only probe around `DeviceMapper.selectByIdForUpdate` (the same latch pattern as `PrinterDeviceStateConcurrencyIntegrationTest`) to prove the second request attempts the device lock while the first transaction holds it. Assertions:

1. first request commits successfully;
2. second request with omitted/false confirmation remains blocked until the first commits;
3. after acquiring the lock, the second request sees the committed active binding and returns `PRINTER_OCCUPIED_CONFIRM_REQUIRED` without partial writes;
4. retrying the second record with `confirmOccupied=true` succeeds;
5. both records finally reference the same printer and remain independently persisted.

Keep fixtures minimal and clean them in `@BeforeEach`. Always clear thread-local login state in each executor thread and shut down the executor in `finally`.

- [ ] **Step 7: Run assignment unit and concurrency tests**

```powershell
mvn -pl yigongbao-module-production -Dtest=ProductionRecordControllerTest,ProductionRecordAssignDeviceTest,PrinterSharedAssignmentConcurrencyIntegrationTest test
```

Expected: all pass; the integration test demonstrates the query/commit race is closed by the device row lock and server-side confirmation guard.

- [ ] **Step 8: Commit the concurrency proof**

```powershell
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/PrinterSharedAssignmentConcurrencyIntegrationTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordAssignDeviceTest.java
git commit -m "test(production): verify concurrent shared printer assignment"
```

## Task 5: Verify batch flow semantics and complete regression

**Files:**

- Test only: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java`
- Test only: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/device/PrinterDeviceStateConcurrencyIntegrationTest.java`

- [ ] **Step 1: Run the existing batch listener regression**

```powershell
mvn -pl yigongbao-module-production -Dtest=DeviceStatusListenerTest test
```

Expected: pass, including `onDeviceStateChange_newWorking_advancesEveryAffectedOrder`, `onDeviceStateChange_workingToIdle_advancesEveryAffectedOrder`, and the `3/4 -> 0` non-completion cases. Do not modify listener production code unless a test demonstrates a regression caused by this feature.

- [ ] **Step 2: Run device-lock and shared-assignment concurrency regressions**

```powershell
mvn -pl yigongbao-module-production -Dtest=PrinterDeviceStateConcurrencyIntegrationTest,PrinterSharedAssignmentConcurrencyIntegrationTest test
```

Expected: pass; device operations and competing assignments serialize on the device row lock, unconfirmed sharing is rejected after the lock wait, and confirmed retry succeeds.

- [ ] **Step 3: Run all feature-focused tests together**

```powershell
mvn -pl yigongbao-module-production -Dtest=PrinterAvailabilityServiceTest,PrinterDeviceUsageCheckerImplTest,ProductionRecordPrinterOccupationTest,ProductionRecordControllerTest,ProductionRecordAssignDeviceTest,ProductionRecordServiceImplTest,ProcessConfigControllerTest,DeviceStatusListenerTest,PrinterDeviceStateConcurrencyIntegrationTest,PrinterSharedAssignmentConcurrencyIntegrationTest test
```

Expected: all selected tests pass with zero failures and zero errors.

- [ ] **Step 4: Compile/install the affected reactor, then run common and full production tests**

```powershell
mvn -pl yigongbao-module-production -am install -DskipTests
mvn -pl yigongbao-common -Dtest=ErrorCodeEnumTest test
mvn -pl yigongbao-module-production test -DskipTests=false
```

Expected: all affected reactor sources compile against the new common interface; common error-code tests pass; production build succeeds with at least the 309-test baseline plus newly added tests. The known Basic controller-test baseline is not invoked by the skip-tests reactor install and remains outside this feature.

- [ ] **Step 5: Check scope and diff hygiene**

```powershell
git diff --check dev...HEAD
git status --short
git diff --stat dev...HEAD
```

Expected: no whitespace errors; only the backend/spec/plan files named above are changed; no frontend, WebSocket, generated `target`, or user-owned untracked files are included.

- [ ] **Step 6: Commit any test-only regression additions if needed**

If no tests required modification, skip this commit. Otherwise:

```powershell
git add yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/listener/DeviceStatusListenerTest.java yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/device/PrinterDeviceStateConcurrencyIntegrationTest.java
git commit -m "test(production): cover shared printer batch flow"
```

## Final review checklist

- [ ] `available` means SLA printer + online + state 0 only; active production use does not disable selection.
- [ ] Occupation query excludes the current record, checks device type and processing-center permission, and returns only `occupied`.
- [ ] Assignment requires `confirmOccupied=true` only when another active record uses the printer.
- [ ] A real two-transaction integration test proves a waiting unconfirmed assignment observes the first committed binding, fails without partial writes, and succeeds after confirmed retry.
- [ ] Confirmation cannot bypass device type, connection, state, record status, duplicate assignment, permissions, weights, process, or numbering checks.
- [ ] Device lock precedes record lock; permission and duplicate checks precede occupation disclosure.
- [ ] WebSocket and `DeviceStatusListener` implementation are unchanged.
- [ ] Multiple records still start and complete together under the existing state matrix.
- [ ] Releasing one pending record from a shared printer leaves every other shared record unchanged.
- [ ] Production module full suite passes; unrelated Basic baseline failures remain documented and untouched.
