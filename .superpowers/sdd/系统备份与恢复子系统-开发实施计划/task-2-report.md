# Task 2 实施报告：搭建线上统一备份模块

## 实施结果

- 新增单一 Maven 模块 `yigongbao-module-backup`，并接入父 reactor、父依赖管理和 `yigongbao-boot`。
- 模块显式依赖 `common/framework/order/system/basic/design/production` 的公开能力；依赖方向为 `boot -> backup -> 既有业务模块`，备份模块不依赖 boot，未形成循环。
- 新增 `candidate`、`manifest`、`receipt`、`identity`、`restore`、`cleanup`、`security`、`common` 八个领域根包，仅实现 Task 2 所需的配置、安全入口和共享契约，未提前实现后续业务行为。
- 新增 `BackupProperties`，`cosDeletionEnabled` 默认 `false`；模块配置通过 `application-backup.yml` 导入主应用。
- 新增与 Task 1 OpenAPI 完全一致的 23 个稳定错误码，以及包含 `code/message/requestId/data/retryable/retryAfterSeconds/details` 的内部 API 响应信封。
- 新增 `MachineHmacAuthenticationFilter` 入口。在 Task 3 验签器完成前，所有 `/internal/**` 请求默认返回 401 `VERIFICATION_REJECTED`；只有同时满足 `test` profile 和显式测试属性时才允许旁路，非测试环境误配旁路属性仍拒绝请求。
- 新增 ApplicationContext、错误契约、fail-closed 安全行为和无 Mapper XML 规则测试，共 8 个测试。

## 主要文件

- `yigongbao-parent/yigongbao-module-backup/pom.xml`
- `yigongbao-parent/yigongbao-module-backup/src/main/java/com/yigongbao/module/backup/config/**`
- `yigongbao-parent/yigongbao-module-backup/src/main/java/com/yigongbao/module/backup/common/**`
- `yigongbao-parent/yigongbao-module-backup/src/main/java/com/yigongbao/module/backup/security/**`
- `yigongbao-parent/yigongbao-module-backup/src/main/resources/application-backup.yml`
- `yigongbao-parent/yigongbao-module-backup/src/test/**`
- `yigongbao-parent/pom.xml`
- `yigongbao-parent/yigongbao-boot/pom.xml`
- `yigongbao-parent/yigongbao-boot/src/main/java/com/yigongbao/boot/YigongbaoApplication.java`
- `yigongbao-parent/yigongbao-boot/src/main/resources/application.yml`

## TDD 证据

1. RED：先加入上下文、错误契约、安全行为和 Mapper XML 规则测试；执行依赖仅编译的 reactor 测试编译，备份模块因 `BackupModuleConfiguration`、`BackupProperties`、`BackupErrorCode`、`InternalApiResponse` 和 `MachineHmacAuthenticationFilter` 尚不存在而按预期失败。
2. GREEN：完成最小实现后执行：

```powershell
mvn -pl yigongbao-module-backup -am "-Dtest=com.yigongbao.module.backup.**" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

结果：`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`。

## 编译与依赖图验证

以下命令均成功：

```powershell
mvn -pl yigongbao-module-system -am -DskipTests compile
mvn -pl yigongbao-boot -am -DskipTests compile
```

boot reactor 共 14 个项目全部成功，构建顺序中 `yigongbao-module-backup` 位于 `yigongbao-boot` 之前，证明新增依赖图可解析且没有循环；既有模块全部完成编译。

## 指定完整测试命令的既有阻塞

按原样执行：

```powershell
mvn -pl yigongbao-module-backup -am test
```

结果为 `BUILD FAILURE`，但失败发生在依赖 reactor 的既有 `yigongbao-module-basic` 测试，备份模块尚未被 Maven 执行：

- `yigongbao-module-basic`：`Tests run: 314, Failures: 41, Errors: 11, Skipped: 0`。
- `yigongbao-module-backup`：`SKIPPED`。
- 代表性既有错误包括 `RegistrationCertServiceImplTest.refreshExpiredStatus_shouldUpdateExpiredCerts` 的空指针、`OperationLogControllerTest` 上下文加载失败，以及多组 Controller 旧错误码/HTTP 状态断言不匹配。

这些失败在 Task 2 RED 阶段、尚未加入任何备份生产代码时已经同样出现；本任务未修改 basic 模块，也未扩大范围修复其历史测试。备份模块自身 8 个测试已通过，完整 boot 和既有 system 编译检查均通过。

## 关注事项

- Task 3 必须用真实 `MachineRequestVerifier` 替换当前 fail-closed 占位行为，并继续保持 nonce、时间窗、scope 和密钥轮换验证；在此之前内部接口不可用是有意的安全默认值。
- COS 实际删除仍保持默认关闭；Task 20 完成全部硬闸门前不得启用。
- 若验收必须要求原样 `-am test` 全绿，需要单独修复 `yigongbao-module-basic` 的 52 个既有测试失败，这不属于 Task 2 范围。

## 评审修复：context path 下的内部接口鉴权

- 根因：`shouldNotFilter` 直接用包含 context path 的 `request.getRequestURI()` 匹配 `/internal/`；当应用配置 context path `/api` 时，请求 URI 为 `/api/internal/...`，因此误判为非内部路由并跳过过滤器。
- 修复：匹配前从 request URI 中剔除 `request.getContextPath()`，再以 context-relative path 判断 `/internal/` 前缀；默认 fail-closed 行为和“`test` profile + 显式属性”双条件旁路语义保持不变。
- 回归测试：新增 context path 为 `/api` 的 `/internal/backup/**` 请求用例。RED 阶段稳定复现返回 200 且进入 filter chain；GREEN 阶段确认返回 401 `VERIFICATION_REJECTED` 且未进入 filter chain。

验证结果：

```powershell
mvn -pl yigongbao-module-backup -am "-Dtest=com.yigongbao.module.backup.**" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl yigongbao-boot -am -DskipTests compile
```

- 备份模块聚焦测试：`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- boot reactor 编译：14 个项目全部 `SUCCESS`，`BUILD SUCCESS`。
