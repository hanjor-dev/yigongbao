# 流转状态筛选展示字段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `/flow/select/statuses` 返回的状态选项增加 `show` 字段，标记自动流转状态为不可展示。

**Architecture:** 沿用现有 Controller 枚举遍历逻辑，在通用下拉 VO 增加字段；展示规则集中在状态枚举判断中，接口仅负责组装响应。

**Tech Stack:** Java、Spring MVC、JUnit 5、MockMvc。

---

### Task 1: 添加接口回归测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-order/src/test/java/com/yigongbao/module/order/controller/FlowSelectControllerTest.java`

- [ ] 添加断言，自动流转状态返回 `show=false`，可停留状态返回 `show=true`。
- [ ] 运行测试，确认当前实现因缺少字段而失败。

### Task 2: 实现展示标记

**Files:**
- Modify: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/vo/SelectTreeVO.java`
- Modify: `yigongbao-parent/yigongbao-module-order/src/main/java/com/yigongbao/module/order/controller/FlowSelectController.java`

- [ ] 增加 `show` 字段并在接口组装时按状态设置值。
- [ ] 运行模块测试及相关构建验证。

### Task 3: 交付前检查

- [ ] 检查差异仅包含本需求相关文件，保留用户已有环境配置改动。
- [ ] 按项目提交规范生成中文 Conventional Commit（如执行提交）。
