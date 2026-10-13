# 列配置版本兼容实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 统一订单、设计工单、生产流转卡、质检、仓储列配置的版本兼容和新增字段增量合并能力。

**Architecture:** 配置 JSON 增加后端版本号。当前版本配置直接返回，历史版本与系统默认配置按字段合并，仅追加缺失列并保留用户已有显示、排序、宽度和固定属性。保存时统一写入当前版本，避免旧前端配置覆盖新增字段。

**Tech Stack:** Java 17、Spring Boot、Jackson、JUnit 5、Maven。

---

### Task 1: 建立通用列配置合并规则

**Files:**
- Create: `yigongbao-module-system/.../ColumnConfigMergeUtil.java`
- Test: `yigongbao-module-system/.../ColumnConfigMergeUtilTest.java`

- [x] 测试历史配置缺少默认字段时追加字段。
- [x] 测试保留用户已有字段属性且不重复追加。
- [x] 测试新增字段排序从用户最大排序号之后开始。
- [x] 实现泛型合并工具，避免各模块复制算法。

### Task 2: 接入五类配置读取和保存

**Files:**
- Modify: 订单、设计工单、生产流转卡、质检、仓储列配置 VO。
- Modify: 五个列配置服务/查询辅助类。
- Modify: 五类列配置测试。

- [x] 增加当前版本常量和配置 JSON 的 `version` 字段。
- [x] 版本一致时直接返回个人配置。
- [x] 版本过旧或缺失时与默认配置合并。
- [x] 历史配置首次读取后回写新版本，避免重复处理。
- [x] 保存时写入当前版本。
- [x] 保留 `publicOrderCode` 等默认字段的统一处理。

### Task 3: 同步默认配置和回归验证

**Files:**
- Modify: 默认配置属性和初始化 SQL（如配置 JSON 尚未包含版本号）。
- Test: 五类配置接口及相关服务测试。

- [x] 检查五类系统默认配置字段完整性。
- [x] 验证旧 JSON、新 JSON、异常 JSON 和空配置。
- [x] 执行模块测试及必要的 Maven 编译验证。
