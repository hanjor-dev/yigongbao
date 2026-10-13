# Production List End Time Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让生产流转卡分页列表的 `postProcessingEndTime` 临时返回打印完成时间，并用注释记录兼容原因。

**Architecture:** 保持实体、数据库和详情接口不变，只在 `pageRecords` 的实体到 VO 映射末端覆盖列表字段。使用服务单元测试锁定该临时响应契约。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Maven

---

### Task 1: 列表时间兼容映射

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java:329-354`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`

- [x] **Step 1: 写失败测试**

新增 `pageRecords_mapsProductionEndTimeToPrintFinishTimeForListCompatibility`，构造不同的打印完成时间和后处理结束时间，断言列表 `postProcessingEndTime` 返回打印完成时间。

- [x] **Step 2: 验证测试按预期失败**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -Dtest=ProductionRecordServiceImplTest#pageRecords_mapsProductionEndTimeToPrintFinishTimeForListCompatibility test`

Expected: FAIL，实际值仍为原后处理结束时间。

- [x] **Step 3: 最小实现**

在 `BeanUtil.copyProperties` 后增加：

```java
// 临时兼容：生产列表“生产结束时间”列仍绑定 postProcessingEndTime，现阶段改为展示打印完成时间。
vo.setPostProcessingEndTime(e.getPrintFinishTime());
```

- [x] **Step 4: 验证目标测试通过**

重复执行 Step 2 命令，Expected: PASS。

- [x] **Step 5: 回归生产模块测试**

Run: `mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test`

Expected: BUILD SUCCESS，0 failures，0 errors。
