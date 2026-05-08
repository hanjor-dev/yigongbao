# 多接口错误处理优化 - 实施计划

**基于设计文档**: `docs/superpowers/specs/2026-05-08-multi-error-handling-design.md`  
**创建日期**: 2026-05-08  
**预计工时**: 4-6小时（后端3-4小时，前端1-2小时）

---

## 概述

实现基于优先级的多接口错误处理机制，在后端为每个错误码定义优先级，前端收集短时间内的所有错误并只展示最高优先级的错误。

**核心改动**:
- 后端：ErrorCodeEnum 添加 priority 字段（300+个错误码）
- 后端：Result 类添加 priority 字段
- 前端：HTTP 拦截器实现错误收集和过滤

---

## 任务列表

### 阶段1：后端基础改动（预计1.5小时）

#### Task 1.1: 修改 ErrorCodeEnum 构造函数和字段定义
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
1. 在 ErrorCodeEnum 中添加 `private final Integer priority;` 字段
2. 修改构造函数，添加 priority 参数
3. 添加 `getPriority()` getter 方法（Lombok 自动生成）
4. 暂时为所有现有错误码添加默认值 `3`（后续任务中逐个修改）

**验证**:
- 编译通过
- 运行现有单元测试，确保无回归

**预计时间**: 5分钟

---

#### Task 1.2: 为认证/授权错误分配 priority=1
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
修改以下错误码的 priority 为 1：
- UNAUTHORIZED(401)
- FORBIDDEN(403)
- PASSWORD_ERROR(605)
- OLD_PASSWORD_ERROR(606)
- USERNAME_OR_PASSWORD_ERROR(608)
- ACCOUNT_LOCKED(609)
- LOGIN_MAX_FAILURES(610)
- TOKEN_INVALID(611)
- PERMISSION_DENIED(612)

**验证**:
- 编译通过
- 检查修改的错误码数量（应为9个）

**预计时间**: 10分钟

---

#### Task 1.3: 为系统错误分配 priority=2
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
修改以下错误码的 priority 为 2：
- SERVER_ERROR(500)
- SERVICE_UNAVAILABLE(503)
- RATE_LIMIT_EXCEEDED(429/780)
- SIGN_PARAM_MISSING(781)
- SIGN_TIMESTAMP_EXPIRED(782)
- SIGN_NONCE_USED(783)
- SIGN_INVALID(784)

**验证**:
- 编译通过
- 检查修改的错误码数量（应为7个）

**预计时间**: 10分钟

---

#### Task 1.4: 为核心业务错误分配 priority=3
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
修改以下错误码的 priority 为 3：
- 用户相关：USER_NOT_FOUND(601) 到 USER_EMAIL_EXISTS(636)
- 订单相关：ORDER_NOT_FOUND(677) 到 ORDER_MODIFY_TYPE_NOT_ALLOWED_IN_PHASE(722)
- 设计相关：DESIGN_PACKAGE_NOT_FOUND(758) 到 DESIGNER_NOT_ASSIGNED(772)

**验证**:
- 编译通过
- 检查修改的错误码数量（应为约80个）

**预计时间**: 20分钟

---

#### Task 1.5: 为辅助业务错误分配 priority=4
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
修改以下错误码的 priority 为 4：
- 机构相关：ORG_NOT_FOUND(613) 到 SYSTEM_CONFIG_MISSING(627)
- 医院相关：HOSPITAL_NOT_FOUND(642) 到 HOSPITAL_DEPT_DISABLED(6460)
- 产品相关：PRODUCT_NOT_FOUND(648) 到 PRODUCT_SPEC_IN_USE(757)
- 医生相关：DOCTOR_NOT_FOUND(674) 到 DOCTOR_EXISTS(676)
- 部位相关：BODY_PART_NOT_FOUND(650) 到 BODY_PART_NAME_EXISTS(651)
- 项目相关：REBUILD_PROJECT_NOT_FOUND(652) 到 REBUILD_PROJECT_NAME_EXISTS(653)
- 注册证相关：CERT_NOT_FOUND(654) 到 CERT_EXISTS(655)
- 模板相关：TEMPLATE_NOT_FOUND(656) 到 TEMPLATE_HAS_USERS(659)
- 附件相关：ATTACHMENT_NOT_FOUND(660) 到 ATTACHMENT_FILENAME_ILLEGAL(665)
- 编码规则：CODE_RULE_NOT_FOUND(668) 到 CODE_SEQ_OVERFLOW(673)

**验证**:
- 编译通过
- 检查修改的错误码数量（应为约40个）

**预计时间**: 15分钟

---

#### Task 1.6: 为配置/基础数据错误分配 priority=5
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

**实现步骤**:
修改以下错误码的 priority 为 5：
- 配置相关：CONFIG_NOT_FOUND(635) 到 CONFIG_SYSTEM_NOT_ALLOW_DELETE(638)
- 字典相关：DICT_CODE_EXISTS(639) 到 DICT_NAME_EXISTS(640)
- 资源相关：RESOURCE_NOT_FOUND(631) 到 RESOURCE_HAS_ROLES(634)
- 角色相关：USER_ROLE_NOT_FOUND(622) 到 USER_ROLE_EXISTS(627)
- 部门相关：DEPT_NOT_FOUND(630) 到 DEPT_INTERNAL_ORG_LIMIT(635)
- 日志相关：LOG_NOT_FOUND(666) 到 LOG_EXPORT_FAILED(667)
- 验证码相关：CAPTCHA_TOO_FREQUENT(637) 到 CAPTCHA_GRAPHIC_ERROR(776)
- 其他通用错误：PARAM_ERROR(400) 到 DATA_HAS_CHILDREN(641)

**验证**:
- 编译通过
- 检查修改的错误码数量（应为约60个）
- 确认所有错误码都已分配 priority（无遗漏）

**预计时间**: 20分钟

---

#### Task 1.7: 修改 Result 类添加 priority 字段
**文件**: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/result/Result.java`

**实现步骤**:
1. 在 Result 类中添加 `private Integer priority;` 字段
2. 修改 `error(ErrorCodeEnum errorCode)` 方法，添加 `result.setPriority(errorCode.getPriority());`
3. 修改 `error(Integer code, String message)` 方法，添加 `result.setPriority(null);`（自定义错误无优先级）
4. 确保 `success()` 方法中 priority 为 null

**验证**:
- 编译通过
- 运行现有单元测试

**预计时间**: 10分钟

---

#### Task 1.8: 验证 GlobalExceptionHandler 使用 Result.error(ErrorCodeEnum)
**文件**: `yigongbao-parent/yigongbao-framework/src/main/java/com/yigongbao/framework/exception/GlobalExceptionHandler.java`

**实现步骤**:
1. 检查所有异常处理方法是否使用 `Result.error(ErrorCodeEnum)`
2. 如果有使用 `Result.error(code, message)` 的地方，评估是否需要改为使用 ErrorCodeEnum
3. 确认无需额外改动（因为已经使用 ErrorCodeEnum）

**验证**:
- 代码review 通过
- 所有异常处理都返回带 priority 的 Result

**预计时间**: 10分钟

---

### 阶段2：后端测试（预计30分钟）

#### Task 2.1: 编写 ErrorCodeEnum 优先级测试
**文件**: `yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/enums/ErrorCodeEnumTest.java`

**实现步骤**:
1. 创建测试类 ErrorCodeEnumTest
2. 编写测试方法 `testAllErrorCodesHavePriority()`：
   - 遍历所有 ErrorCodeEnum 值
   - 断言每个错误码的 priority 不为 null
   - 断言 priority 在 1-5 范围内
3. 编写测试方法 `testPriorityDistribution()`：
   - 统计各优先级的错误码数量
   - 输出分布情况（用于人工review）

**验证**:
- 测试通过
- 所有错误码都有有效的 priority

**预计时间**: 15分钟

---

#### Task 2.2: 编写 Result 优先级测试
**文件**: `yigongbao-parent/yigongbao-common/src/test/java/com/yigongbao/common/result/ResultTest.java`

**实现步骤**:
1. 创建或修改测试类 ResultTest
2. 编写测试方法 `testErrorWithPriority()`：
   - 调用 `Result.error(ErrorCodeEnum.UNAUTHORIZED)`
   - 断言返回的 Result 包含正确的 priority
3. 编写测试方法 `testSuccessNoPriority()`：
   - 调用 `Result.success(data)`
   - 断言返回的 Result 的 priority 为 null

**验证**:
- 测试通过
- Result 正确包含 priority 字段

**预计时间**: 15分钟

---

### 阶段3：前端改动（预计1-1.5小时）

**注意**: 前端代码不在当前仓库中，以下任务为前端开发人员参考。

#### Task 3.1: 修改 HTTP 响应拦截器添加错误收集逻辑
**文件**: 前端项目的 HTTP 拦截器文件（如 `src/utils/request.js` 或 `src/api/interceptor.js`）

**实现步骤**:
1. 在拦截器文件顶部添加错误队列变量：
   ```javascript
   let errorQueue = [];
   let isCollecting = false;
   ```
2. 修改响应错误拦截器：
   - 从 response.data 中提取 `{code, message, priority}`
   - 将错误添加到 errorQueue
   - 如果不在收集期，开启500ms时间窗口
   - 时间窗口结束后调用 showHighestPriorityError()

**验证**:
- 代码编译通过
- 手工测试：触发单个错误，验证正常展示

**预计时间**: 20分钟

---

#### Task 3.2: 实现错误展示逻辑
**文件**: 前端项目的 HTTP 拦截器文件

**实现步骤**:
1. 实现 `showHighestPriorityError()` 函数：
   - 检查 errorQueue 是否为空
   - 按 priority 排序（升序）
   - 取第一个错误展示
   - 开发环境下记录被过滤的错误到控制台
2. 清空 errorQueue 和重置 isCollecting 标志

**验证**:
- 代码编译通过
- 手工测试：触发多个错误，验证只展示最高优先级的错误

**预计时间**: 15分钟

---

#### Task 3.3: 编写前端单元测试
**文件**: 前端项目的测试文件（如 `src/utils/__tests__/request.test.js`）

**实现步骤**:
1. 编写测试用例 `test error collection mechanism`：
   - Mock 多个接口错误
   - 验证错误队列正确收集
2. 编写测试用例 `test priority sorting`：
   - Mock 不同优先级的错误
   - 验证只展示最高优先级的错误
3. 编写测试用例 `test time window`：
   - 验证500ms时间窗口机制

**验证**:
- 所有测试通过
- 覆盖率达到80%以上

**预计时间**: 25分钟

---

### 阶段4：集成测试和验证（预计1小时）

#### Task 4.1: 前后端联调测试
**环境**: 本地开发环境

**测试步骤**:
1. 启动后端服务
2. 启动前端项目
3. 测试场景1：页面加载时触发多个接口错误
   - 预期：只弹出一个最高优先级的错误提示
4. 测试场景2：不同优先级组合
   - 认证错误 + 业务错误 → 只展示认证错误
   - 系统错误 + 业务错误 → 只展示系统错误
   - 多个业务错误 → 展示第一个
5. 测试场景3：时间窗口机制
   - 连续触发错误，验证不会无限延迟

**验证**:
- 所有测试场景通过
- 用户体验符合预期

**预计时间**: 30分钟

---

#### Task 4.2: 手工测试和性能验证
**环境**: 本地开发环境

**测试步骤**:
1. 测试不同页面的多接口加载场景
2. 验证500ms延迟是否影响用户体验
3. 验证开发环境下控制台是否正确记录被过滤的错误
4. 测试边界情况：
   - 单个错误（应立即展示）
   - 大量错误（应只展示一个）
   - 错误码无 priority（应正常处理）

**验证**:
- 所有边界情况正常处理
- 性能符合预期

**预计时间**: 30分钟

---

## 提交策略

建议分3次提交：

### Commit 1: 后端基础改动
**包含**: Task 1.1 - 1.8  
**提交信息**:
```
feat(error): 为错误码添加优先级支持

- ErrorCodeEnum 添加 priority 字段（1-5级）
- Result 响应结构添加 priority 字段
- 为所有错误码分配优先级（认证>系统>核心业务>辅助业务>配置）
```

### Commit 2: 后端测试
**包含**: Task 2.1 - 2.2  
**提交信息**:
```
test(error): 添加错误码优先级测试

- 验证所有错误码都有有效的 priority
- 验证 Result 正确返回 priority
```

### Commit 3: 前端改动（前端仓库）
**包含**: Task 3.1 - 3.3  
**提交信息**:
```
feat(error): 实现多接口错误优先级过滤

- HTTP 拦截器添加错误收集机制
- 500ms时间窗口内只展示最高优先级错误
- 添加单元测试
```

---

## 风险和注意事项

1. **优先级分配review**: Task 1.2-1.6 完成后，需要人工review 优先级分配是否合理
2. **前端兼容性**: 如果有多个前端项目，需要同步更新所有项目的拦截器
3. **回归测试**: 后端改动后需要运行完整的回归测试套件
4. **文档更新**: 完成后需要更新接口文档，说明响应结构新增 priority 字段

---

## 后续优化（可选）

1. **动态优先级**: 根据业务场景动态调整优先级
2. **错误聚合**: 相同类型的错误聚合展示
3. **错误重试**: 对于某些可重试的错误，自动重试而不展示

---

## 参考资料

- 设计文档: `docs/superpowers/specs/2026-05-08-multi-error-handling-design.md`
- ErrorCodeEnum: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Result: `yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/result/Result.java`
