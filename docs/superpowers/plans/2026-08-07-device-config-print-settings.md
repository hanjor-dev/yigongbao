# getDeviceConfig 打印配置回显 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让设备配置接口返回流转卡已保存的打印材质和打印工序参数。

**Architecture:** 在 `DeviceConfigVO` 增加两个可空字段；材质沿用 `BeanUtil` 从 `production_record` 复制，打印参数由服务按流转卡 ID 和 `processType=print` 查询 `production_process` 后填充。请求与数据库结构保持不变。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Maven。

---

### Task 1: 用失败测试定义回显行为

**Files:**
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java`

- [ ] **Step 1: 扩展已有设备配置测试**

构造包含 `material` 的流转卡，并让 `processMapper.selectOne(...)` 返回 `processType=print`、`processParams` 有值的工序；断言 `DeviceConfigVO.material` 和 `DeviceConfigVO.printParams`。

- [ ] **Step 2: 增加打印工序缺失测试**

让打印工序查询返回 `null`，断言设备信息和材质正常返回，`printParams` 为 `null`。

- [ ] **Step 3: 增加不存在记录兼容测试**

让流转卡查询返回 `null`，断言仍抛出 `PRODUCTION_RECORD_NOT_FOUND`，确保新增工序查询不改变原错误语义。

- [ ] **Step 4: 增加 Controller 序列化测试**

在 `ProductionRecordControllerTest` 的 `@WebMvcTest` 增加属性 `spring.jackson.default-property-inclusion=non_null`，让测试切片使用与生产环境一致的 `NON_NULL` 行为且不新增模块依赖。分别让 Service 返回两个新增字段均有值和均为 `null` 的 `DeviceConfigVO`：有值时断言 JSON 返回 `material/printParams`；为空时断言两个字段不存在。

- [ ] **Step 5: 运行测试并确认预期失败**

Run:

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am '-Dtest=ProductionRecordServiceImplTest#getDeviceConfig_*,ProductionRecordControllerTest#getDeviceConfig_*' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 测试编译或断言失败，因为 `DeviceConfigVO` 尚无 `material/printParams` 返回契约。

### Task 2: 最小实现并验证

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/DeviceConfigVO.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/controller/ProductionRecordControllerTest.java`

- [ ] **Step 1: 增加 VO 字段**

在 `DeviceConfigVO` 增加：

```java
private String material;
private String printParams;
```

- [ ] **Step 2: 查询打印工序参数**

在 `getDeviceConfig` 完成 Bean 复制后，按以下条件查询并赋值：

```java
ProductionProcessEntity printProcess = processMapper.selectOne(
        new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .last("LIMIT 1"));
if (printProcess != null) {
    vo.setPrintParams(printProcess.getProcessParams());
}
```

- [ ] **Step 3: 运行针对性测试确认通过**

运行 Task 1 命令，Expected: PASS。

- [ ] **Step 4: 运行相关 Controller/Service 测试**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am '-Dtest=ProductionRecordControllerTest,ProductionRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

- [ ] **Step 5: 运行生产模块全量测试及差异检查**

```powershell
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production -am -DskipTests install
mvn -f yigongbao-parent/pom.xml -pl yigongbao-module-production test
git diff --check
```

Expected: 先安装最新 Reactor 依赖但跳过其既有失败测试，再单独运行生产模块全量测试并全部通过；差异检查无错误，仅包含两个生产代码文件和两个必要测试文件。

- [ ] **Step 6: 保持改动未提交等待用户决定集成方式**

不提交规划文档或业务代码；完成后报告分支、验证结果和可选集成方式。
