# 设计工单评估意见更新 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为设计工单增加接口，更新独立的 `order_main.designer_remark`。

**Architecture:** Controller 接收并校验 DTO，调用设计工单服务；服务层校验订单存在后复用 `OrderMainService.updateById`，只更新 `designerRemark` 字段。接口沿用现有操作日志和统一响应结构。

**Tech Stack:** Spring Boot、Jakarta Validation、MyBatis-Plus、JUnit 5、Mockito。

---

### Task 1: 增加评估意见请求 DTO

**Files:**
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/UpdateEvaluationOpinionDTO.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/controller/DesignWorkorderControllerTest.java`

- [ ] **Step 1: Write the failing controller test**
  增加测试，构造 DTO，调用新增 Controller 方法，断言服务层收到订单 ID 和意见内容，并返回成功结果。

- [ ] **Step 2: Run the focused test to verify it fails**
  运行 `mvn -pl yigongbao-module-design -am -Dtest=DesignWorkorderControllerTest test`；预期因 DTO 或 Controller 方法不存在而失败。

- [ ] **Step 3: Write the minimal DTO**
  新增 `designerRemark` 字段，使用 `@NotBlank` 和 `@Size(max = 2000)`，与数据库 TEXT 字段和项目 DTO 校验风格保持一致。

### Task 2: 增加 Service 更新能力

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java`

- [ ] **Step 1: Write failing service tests**
  增加“订单存在时只更新评估意见”和“订单不存在时抛出 `ORDER_NOT_FOUND`”测试。

- [ ] **Step 2: Run the focused tests to verify they fail**
  运行 `mvn -pl yigongbao-module-design -am -Dtest=DesignWorkorderServiceImplTest test`；预期因服务方法不存在而失败。

- [ ] **Step 3: Implement the service method**
  在接口声明 `updateEvaluationOpinion(Long orderId, String remark)`；实现类先调用 `orderMainService.getById`，不存在则抛出既有异常；构造仅设置 ID 和 `designerRemark` 字段的 `OrderMainEntity`，调用 `updateById`，必要时加事务注解。

- [ ] **Step 4: Run service tests**
  重跑上述命令，预期新增测试通过。

### Task 3: 暴露 Controller 接口

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/controller/DesignWorkorderControllerTest.java`

- [ ] **Step 1: Add the endpoint**
  新增 `POST /{orderId}/evaluation-opinion`，使用 `@Valid @RequestBody UpdateEvaluationOpinionDTO`，调用服务方法并返回 `Result.success()`；添加 Swagger 描述和“更新评估意见”操作日志。

- [ ] **Step 2: Run controller tests**
  重跑 Controller 测试，预期通过。

### Task 4: 完成验证

**Files:**
- No additional files.

- [ ] **Step 1: Run the full design module tests**
  运行 `mvn -pl yigongbao-module-design -am test`，确认相关模块测试通过。

- [ ] **Step 2: Inspect the final diff**
  确认只包含本需求文件和计划/设计文档，未纳入工作区既有的订单导出相关改动。
