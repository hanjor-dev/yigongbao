# 设计师备注接口重命名与返回链路 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将设计工单备注接口及代码命名统一为设计师备注，并让该字段通过设计工单和订单查询链路返回。

**Architecture:** 使用 `/design/workorder/{orderId}/designer-remark` 作为唯一写入入口，DTO、Service 方法和测试统一使用 `designerRemark` 语义。通过实体到设计工单列表/详情及订单列表/详情的显式映射返回字段，并补充订单导出字段映射，保持 `dataEvaluationOpinion` 仅表示影像数据评估意见。

**Tech Stack:** Spring Boot、Jakarta Validation、MyBatis-Plus、JUnit 5、MockMvc、Mockito。

---

### Task 1: 重命名设计师备注写入接口

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignWorkorderController.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/SaveDesignerRemarkDTO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignWorkorderService.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/controller/DesignWorkorderControllerTest.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java`

- [ ] Write failing tests for `POST /design/workorder/{id}/designer-remark` and `saveDesignerRemark`.
- [ ] Run focused design tests and verify failure is caused by the missing new path/method.
- [x] Rename DTO to `SaveDesignerRemarkDTO`, controller mapping to `/designer-remark`, and service method to `saveDesignerRemark`.
- [ ] Run focused tests and verify the new contract passes.

### Task 2: Complete design workorder return mapping

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignWorkorderDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java`

- [x] Add assertions that list and detail responses copy `designerRemark` from `OrderMainEntity`.
- [ ] Run the focused service tests and verify the new assertions fail before mapping exists.
- [ ] Add the field and explicit mappings in list/detail conversion.
- [ ] Run the focused tests and verify they pass.

### Task 3: Complete order query and export return mapping

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderListVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/vo/order/OrderDetailVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/helper/OrderQueryHelper.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderMainServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/service/impl/OrderExportServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/config/DefaultConfigProperties.java`
- Test: relevant existing order helper/export tests

- [ ] Add failing assertions for `designerRemark` in order list/detail conversion and export fields.
- [x] Implement field mappings and export field registration.
- [ ] Run order module focused tests.

### Task 4: Verify the complete change

- [x] Search the design module to confirm no runtime use remains of `updateEvaluationOpinion` or `/evaluation-opinion`.
- [ ] Run design and order module tests.
- [ ] Inspect the final diff and ensure unrelated production changes are untouched.
