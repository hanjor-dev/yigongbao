# 图纸下载文件名中文分类修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 图纸下载和新生成文件名使用产品类型字典中文名称，并立即兼容已生成的历史图纸。

**Architecture:** 基础文件服务增加可选下载文件名重载，保留原方法兼容性；设计文档服务集中构造中文图纸文件名，下载时覆盖显示名称、生成时写入正确原始名称。

**Tech Stack:** Java 17、Spring Boot、MyBatis-Plus、JUnit 5、Mockito、Maven。

---

### Task 1: 基础文件服务支持指定下载文件名

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/FileService.java`
- Modify: `yigongbao-parent/yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/file/service/impl/FileServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-basic/src/test/java/com/yigongbao/module/basic/file/service/impl/FileServiceImplTest.java`

- [ ] 先增加测试：调用 `download(id, "零五-医疗器械图纸.xlsx", response)` 后，响应头使用指定名称。
- [ ] 增加兼容测试：调用旧 `download(id, response)` 时仍使用持久化的 `originalFilename`。
- [ ] 运行该测试，确认因重载不存在而失败。
- [ ] 增加重载；原方法委托重载并在未指定时使用 `originalFilename`。
- [ ] 运行基础文件服务测试，确认通过。

### Task 2: 图纸下载和生成统一使用中文分类名

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`
- Test: `yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignDocServiceImplTest.java`

- [ ] 增加历史图纸下载测试：设置已有且数据未变化的图纸，字典 `17.2 -> 医疗器械`，验证调用新下载重载并收到“零五-医疗器械图纸.xlsx”，同时验证 `uploadBytes` 未被调用。
- [ ] 运行历史下载测试，确认因当前仍调用旧下载签名而失败。
- [ ] 提取图纸文件名方法并让下载使用新重载；运行历史下载测试确认通过。
- [ ] 增加新生成图纸测试：验证 `uploadBytes` 使用相同中文名称；运行确认当前仍拼接编码而失败。
- [ ] 生成上传改用统一命名方法；运行新生成测试确认通过。
- [ ] 增加字典名称为空的回退测试，分别验证历史下载和新上传名称使用“未分类”，且不含编码或 `null`；先确认失败。
- [ ] 实现空分类名回退“未分类”，运行回退测试和全部设计文档服务测试确认通过。

### Task 3: 完整验证

**Files:**
- Verify all modified files above.

- [ ] 运行基础模块相关测试。
- [ ] 运行设计模块 `DesignDocServiceImplTest`。
- [ ] 运行设计模块全量测试。
- [ ] 执行 `git diff --check` 并复核工作区，确保未纳入无关文件。
