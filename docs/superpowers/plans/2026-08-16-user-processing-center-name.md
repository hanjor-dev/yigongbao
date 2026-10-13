# 用户加工中心名称同步修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 更新生产员或生产管理员且请求仅包含 `centerId` 时，同步持久化对应加工中心的名称。

**Architecture:** `UserServiceImpl.updateUser` 已负责根据生效角色校验加工中心。将该分支扩展到两种生产角色，并让其复用的加工中心实体为 `centerName` 提供唯一可信来源。使用现有 Mockito 服务单测捕获传入 `updateById` 的实体，验证冗余字段。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、JUnit 5、Mockito。

---

### Task 1: 锁定生产角色的名称回填行为

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

为生产员和生产管理员分别构造具有旧 `centerName` 的用户、仅含 `centerId` 的 `UpdateUserDTO`，模拟 `processingCenterMapper.selectById` 返回名为“加工中心B”的实体；使用 `ArgumentCaptor<UserEntity>` 断言保存实体的 `centerId` 与 `centerName`。

- [ ] **Step 2: Run the targeted test to verify it fails**

Run: `mvn -pl yigongbao-module-system -am -Dtest=UserServiceImplTest#updateUser_whenProductionWorkerChangesCenter_shouldSyncCenterName,UserServiceImplTest#updateUser_whenProductionManagerChangesCenter_shouldSyncCenterName test`

Expected: production-manager case fails because the current implementation only performs the lookup and name assignment for `PRODUCTION_WORKER`.

### Task 2: 用加工中心主数据回填名称

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java:645-662`
- Test: `yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java`

- [ ] **Step 1: Write the minimal implementation**

在 `updateUser` 的加工中心校验分支中同时匹配 `RoleCodeEnum.PRODUCTION_WORKER` 和 `RoleCodeEnum.PRODUCTION_MANAGER`。当 `dto.getCenterId()` 非空时，将已查询到的 `center.getCenterName()` 写入 `entity`；保留缺少/不存在加工中心的现有异常。

- [ ] **Step 2: Run the targeted tests to verify they pass**

Run: `mvn -pl yigongbao-module-system -am -Dtest=UserServiceImplTest#updateUser_whenProductionWorkerChangesCenter_shouldSyncCenterName,UserServiceImplTest#updateUser_whenProductionManagerChangesCenter_shouldSyncCenterName test`

Expected: PASS.

- [ ] **Step 3: Run the module test suite**

Run: `mvn -pl yigongbao-module-system -am test`

Expected: PASS.

- [ ] **Step 4: Commit the focused fix**

```bash
git add yigongbao-parent/yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java yigongbao-parent/yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/service/impl/UserServiceImplTest.java
git commit -m "fix: 同步生产用户加工中心名称"
```
