# Drawing Category Contract Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复图纸分类聚合和版本操作契约，补齐分类接口测试，并将前端对接文档完善到可直接联调。

**Architecture:** 保持现有 Controller、Service 接口路径和 VO 不变。`DesignDocServiceImpl` 负责按当前打印产品分类过滤图纸并校验最新版 ID；`DesignWorkorderServiceImpl` 通过批量分类查询决定兼容字段。所有行为先写失败测试，再做最小实现。

**Tech Stack:** Java 21、Spring Boot 3.2、MyBatis-Plus、JUnit 5、Mockito、MockMvc、Maven、Markdown。

---

### Task 1: 最新图纸分组过滤

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignDocServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`

- [ ] 增加历史 NULL 与 17.1/17.2 并存时只返回当前分类的失败测试。
- [ ] 增加已经移除的分类不返回、纯历史 NULL 仍返回的失败测试。
- [ ] 运行 `DesignDocServiceImplTest`，确认测试因当前未过滤分类而失败。
- [ ] 批量读取当前 `design_product.product_category`，按规格过滤图纸分组。
- [ ] 重跑测试，确认新增测试通过。

### Task 2: 工单详情兼容字段

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignWorkorderServiceImpl.java`

- [ ] 增加无图纸返回 `latestDrawings=[]` 的失败测试。
- [ ] 增加多分类只生成一张图纸时 `latestDrawing=null` 的失败测试。
- [ ] 增加单分类和纯历史数据包继续回填 `latestDrawing` 的测试。
- [ ] 运行 `DesignWorkorderServiceImplTest`，确认测试按预期失败。
- [ ] 使用一次批量产品查询构建包分类集合，并按实际分类数赋值兼容字段。
- [ ] 重跑测试，确认新增测试通过。

### Task 3: 修订和确认最新版 ID 校验

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignDocServiceImplTest.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`

- [ ] 增加修订时 `{id}` 与分类最新版不一致返回 `DOC_VERSION_NOT_FOUND` 且不上传的失败测试。
- [ ] 增加确认历史版本返回 `DOC_VERSION_NOT_FOUND` 且不更新的失败测试。
- [ ] 增加正确分类最新版 ID 成功操作的分类测试。
- [ ] 运行目标嵌套测试，确认错误 ID 测试先失败。
- [ ] 在文件上传/状态更新前比较 `latest.id` 与路径 ID。
- [ ] 重跑测试，确认无错误 ID 副作用。

### Task 4: 五个 Controller 分类参数测试

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/controller/DesignDocControllerTest.java`

- [ ] 为下载、预览、版本列表、修订上传、确认分别增加 `productCategory=17.1/17.2` 请求。
- [ ] 验证各请求调用分类重载，且不调用旧重载。
- [ ] 运行 `DesignDocControllerTest`，确认 5 个接口全部通过。

### Task 5: 完善前端对接文档

**Files:**
- Modify: `.docs/前端接入/图纸按产品分类拆分对接文档.md`

- [ ] 补充工单详情接口、Result 包装和字段完整路径。
- [ ] 补充首次生成前分类来源及 `category -> productCategory` 映射。
- [ ] 补充五个接口的请求类型、参数、返回、二进制下载和 multipart 上传示例。
- [ ] 补充 HTTP 200 业务码判断、410/767/768/771 错误码及兼容字段规则。
- [ ] 对照 Controller 和 VO 逐项复核文档字段。

### Task 6: 完整验证与提交

**Files:**
- Verify all files above only; preserve unrelated workspace changes.

- [ ] 运行设计模块目标测试，确认 0 failures/0 errors。
- [ ] 运行设计模块完整测试和 Maven 编译。
- [ ] 检查 `git diff --check`、任务相关 diff 和工作区状态。
- [ ] 使用代码审查技能复核最终差异，修复有效问题后重新验证。
- [ ] 仅暂存本任务文件并提交代码。
