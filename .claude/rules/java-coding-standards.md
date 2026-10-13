# 医工宝 Java 编码规范

## 概述

本文档定义了医工宝项目的 Java 编码标准和最佳实践，适用于 `yigongbao-parent` 下所有模块。

---

## 一、命名规范

### 1.1 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | 以 `Controller` 结尾 | `UserController` |
| Service | 以 `Service` 结尾，Impl 以 `ServiceImpl` 结尾 | `UserService`, `UserServiceImpl` |
| Mapper | 以 `Mapper` 结尾 | `UserMapper` |
| Entity | 以 `Entity` 结尾，继承 `BaseEntity` | `UserEntity` |
| VO/DTO | 以 `VO`/`DTO` 结尾 | `UserVO`, `CreateUserDTO` |
| Exception | 以 `Exception` 结尾 | `BusinessException` |
| Enum | 以 `Enum` 结尾 | `OrderStatusEnum` |
| Config/Handler | 以 `Config`/`Handler` 结尾 | `MybatisPlusConfig` |

### 1.2 方法命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 查询 | `get`/`list` 开头 | `getUserById()`, `listUsers()` |
| 新增 | `save`/`create` 开头 | `saveUser()`, `createOrder()` |
| 更新 | `update` 开头 | `updateUser()` |
| 删除 | `remove`/`delete` 开头 | `removeUser()` |
| 布尔判断 | `is`/`has`/`can` 开头 | `isEmpty()`, `hasPermission()` |

### 1.3 变量命名

- 普通变量：驼峰命名 `userName`, `orderList`
- 常量：全大写，下划线分隔 `MAX_RETRY_COUNT`
- 布尔变量：`is`/`has`/`can` 开头 `isValid`, `hasError`
- 集合变量：复数形式或明确类型 `userList`, `userMap`

### 1.4 状态值常量

**禁止直接使用魔法值（0/1），必须使用 `StatusConstants` 常量**：

| 常量 | 值 | 说明 |
|-----|---|------|
| `StatusConstants.NORMAL` / `STATUS_ENABLED` | 1 | 正常/启用 |
| `StatusConstants.DISABLED` / `STATUS_DISABLED` | 0 | 禁用/停用 |
| `StatusConstants.DELETED` | 1 | 已删除 |
| `StatusConstants.YES` / `NO` | 1/0 | 是/否 |

### 1.5 编码规则常量（CodeRuleConstants）

调用编码生成器时，**禁止使用硬编码字符串**，必须使用 `CodeRuleConstants` 常量：

```java
// ✅ 正确
codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);

// ❌ 错误
codeGeneratorService.generate("ORDER_NO");
```

**提供的常量**：`HOSPITAL_NO`, `HDEPT_NO`, `PROJECT_NO`, `PRODUCT_CODE`, `BODYPART_NO`, `TEMPLATE_NO`, `DOCTOR_NO`, `ORDER_NO`, `ORDER_ITEM_NO`, `DATA_PACKAGE_NO`, `INSTRUCTION_NO`, `FILE_NO`

---

## 二、代码格式

- **缩进**：4 个空格，禁止 Tab
- **行长度**：最大 120 字符
- **大括号**：左大括号不换行，右大括号单独一行
- **运算符前后**：各 1 个空格
- **空行规则**：类内方法之间 1 个空行，import 后 1 个空行

```java
// ✅ 正确
public UserVO getUserById(Long id) {
    if (id == null || id <= 0) {
        throw new BusinessException("用户ID无效");
    }
    return userMapper.selectById(id);
}

// ❌ 错误
public UserVO getUserById(Long id){
    if(id==null||id<=0){
        throw new BusinessException("用户ID无效");
    }
}
```

**import 顺序**：1. 非 * 的具体类 → 2. * 的包 → 3. java/javax/第三方

---

## 三、注释规范

- **类注释**：所有类必须添加，包含功能说明、作者、创建时间
- **方法注释**：公共方法必须 Javadoc 注释（功能、参数、返回值、异常）
- **行内注释**：使用 `//`，注释与代码间 1 空格
- **注释原则**：只注释必要的、非常规的逻辑；使用中文

---

## 四、分层架构规范

### 4.1 分层结构

```
src/main/java/com/yigongbao/module/xxx/
├── controller/  # 控制层：参数校验、调用 Service、返回 Result
├── service/     # 业务层：业务逻辑、事务控制
│   └── impl/    # Service 实现类
├── mapper/      # 持久层：数据库 CRUD（禁止业务逻辑）
├── entity/      # 实体层：数据库表映射（禁止业务逻辑）
├── vo/          # 视图对象：返回给前端
├── dto/         # 数据传输对象：接收前端数据
├── convert/     # 转换器：Entity/VO/DTO 转换
└── enums/       # 枚举
```

### 4.2 各层职责

| 层级 | 职责 | 禁止事项 |
|------|------|----------|
| Controller | 参数校验、调用 Service、返回 Result | 禁止直接操作数据库、禁止记录日志 |
| Service | 业务逻辑、事务管理 | 禁止处理 HTTP 请求 |
| Mapper | 数据库 CRUD | 禁止业务逻辑 |
| Entity | 数据模型 | 禁止业务逻辑 |

### 4.3 各层核心要点

**Controller**：`@RequiredArgsConstructor` 注入 Service，返回 `Result.success()/error()`，**禁止 try-catch**

**ServiceImpl**：
- 继承 `ServiceImpl<Mapper, Entity>`，实现 `IService<Entity>`
- **必须添加方法级注释和行级注释**
- **必须记录日志**（入参、关键节点、异常），遵循 `log.info/warn/error` 规范
- 优先使用 `ErrorCodeEnum` 抛出业务异常
- 使用 `@Transactional(rollbackFor = Exception.class)` 控制事务

**Mapper**：禁止在 XML 中写 SQL，全部基于 MyBatis-Plus 代码操作

**Entity**：继承 `BaseEntity`（公共字段已包含：id, createTime, updateTime, createBy, updateBy, isDeleted）

---

## 五、异常处理规范

### 5.1 业务异常

使用 `BusinessException`，**优先使用 ErrorCodeEnum**：

```java
// ✅ 优先使用
throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
throw new BusinessException(ErrorCodeEnum.USERNAME_EXISTS);

// ✅ 支持格式化
throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "username");

// ⚠️ 备选
throw new BusinessException(400, "自定义错误信息");
```

### 5.2 错误码规范

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 客户端请求错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 600+ | 业务自定义错误 |

**ErrorCodeEnum 扩展**：新枚举值格式 `ORDER_NOT_FOUND(605, "订单不存在")`

### 5.3 全局异常处理

- 统一使用 `GlobalExceptionHandler` 处理异常
- **禁止在 Controller 中 try-catch**
- Controller 方法参数校验失败由 `GlobalExceptionHandler` 捕获

---

## 六、日志规范

**【强制】Controller 层禁止输出日志**，日志记录由 ServiceImpl 负责。

| 级别 | 使用场景 |
|------|----------|
| DEBUG | 开发调试信息 |
| INFO | 正常业务流程（方法入参、关键节点、成功/失败标识） |
| WARN | 可恢复异常、需要关注信息 |
| ERROR | 系统异常（包含堆栈信息） |

**日志原则**：禁止记录敏感信息（密码、身份证号等），使用中文描述。

---

## 七、数据库规范

### 7.1 表和字段

- 表名全小写，下划线分隔，按模块添加前缀（如 `sys_user`, `erp_order`）
- 通用字段：id, create_time, update_time, create_by, update_by, is_deleted
- 状态字段使用数值型：0-禁用，1-正常
- 所有表必须有表注释

### 7.2 公共字段（BaseEntity）

所有业务 Entity 继承 `BaseEntity`，自动拥有以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| createTime | LocalDateTime | 创建时间（自动填充） |
| updateTime | LocalDateTime | 更新时间（自动填充） |
| createBy | Long | 创建人ID |
| updateBy | Long | 更新人ID |
| isDeleted | Integer | 逻辑删除（@TableLogic） |

### 7.3 索引规范

- 主键：`pk_xxx`，唯一索引：`uk_xxx`，普通索引：`idx_xxx`
- **索引名必须使用表名前缀区分**（如 `idx_sys_org_status`），禁止重复
- **禁止手动定义 is_deleted 字段索引**（MyBatis-Plus 自动创建）

### 7.4 逻辑删除与唯一索引规范（重要）

**问题**：`@TableLogic` 逻辑删除只将 `is_deleted` 置为 1，不修改字段值，导致已删除记录继续占用唯一索引槽位，多次删除/重建相同数据会报 `Duplicate entry` 异常。

**强制规则：凡是同时满足以下两个条件的表，必须使用函数索引代替普通唯一索引：**
1. 表含有 `is_deleted` 逻辑删除字段
2. 表含有唯一索引约束

**函数索引写法**（MySQL 8.0+）：

```sql
-- 替换原有 UNIQUE KEY 写法：
-- UNIQUE KEY uk_username (username)   ← 禁止

-- 改为建表后追加函数索引：
CREATE UNIQUE INDEX uk_username
    ON sys_user ((CASE WHEN is_deleted = 0 THEN username ELSE NULL END));
```

**语义说明**：
- `is_deleted=0`（未删除）：表达式返回字段值，唯一约束生效，同名记录只能存一条
- `is_deleted=1`（已删除）：表达式返回 `NULL`，MySQL 唯一索引中多个 `NULL` 不互相冲突，已删除记录可无限次重建

**联合唯一索引写法**（如同一机构内部门名唯一）：

```sql
-- 对联合索引中的业务字段做条件化，另一字段保留原值
CREATE UNIQUE INDEX uk_dept_name_org
    ON sys_dept ((CASE WHEN is_deleted = 0 THEN dept_name ELSE NULL END), org_id);
```

**建表模板**：

```sql
CREATE TABLE xxx (
    -- 字段定义...
    is_deleted  TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    PRIMARY KEY (id),
    -- 不要写 UNIQUE KEY，仅写普通索引
    KEY idx_xxx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='xxx表';

-- 紧接建表语句后追加函数唯一索引
CREATE UNIQUE INDEX uk_xxx_code
    ON xxx ((CASE WHEN is_deleted = 0 THEN code ELSE NULL END));
```

**测试环境（H2）处理**：H2 不支持函数索引，`schema.sql` 中直接省略 `UNIQUE KEY` 定义即可（不需要添加函数索引）。单元测试侧重业务逻辑验证，不覆盖重复删除/重建的唯一性约束测试。

---

## 八、接口规范

### 8.1 RESTful 风格

| 方法 | URL | 说明 |
|------|-----|------|
| POST | /api/xxx/users | 查询列表(POST+JSON参数方式) |
| GET | /api/xxx/users/{id} | 查询单个 |
| POST | /api/xxx/users | 创建 |
| PUT | /api/xxx/users/{id} | 更新 |
| DELETE | /api/xxx/users/{id} | 删除 |

### 8.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1710392800000
}
```

**分页响应**：`Result<IPage<T>>`，data 包含 `records/total/size/current/pages`

---

## 九、安全与性能规范

### 9.1 安全规范

- **禁止硬编码**密码、密钥、Token
- 禁止在日志中记录敏感信息
- 敏感接口需登录认证
- 接口参数需校验

### 9.2 性能规范

- **禁止 SELECT ***，只查询需要的字段
- 使用分页查询，避免一次查询大量数据
- 避免在循环中查询数据库

---

## 十、单元测试规范

### 10.1 测试要求

每个功能模块必须包含：
1. **ServiceImpl 单元测试**：验证业务逻辑
2. **Controller 接口测试**：验证参数绑定、参数校验、统一响应格式

### 10.2 测试框架

使用 JUnit 5 + Mockito：

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {
    @Mock private UserMapper userMapper;
    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（继承 ServiceImpl 时必须）
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(userService, userMapper);
    }
}
```

### 10.3 测试要点

- **Mock 参数匹配**：查询用 `any()`，按 ID 查询用具体值
- **每个业务方法**覆盖成功场景 + 失败场景
- **ServiceImpl 反射注入**：继承 `ServiceImpl` 时必须注入 baseMapper
- **Controller 测试**：使用 `@SpringBootTest` + MockMvc，配置 H2 + `satoken.interceptor.enable: false`

### 10.4 断言规范

- 基本断言：`assertNotNull()`, `assertEquals()`, `assertTrue/False()`
- 异常断言：`assertThrows(BusinessException.class, ...)`
- Mock 验证：`verify(mapper, times(1)).selectById(...)`

### 10.5 测试数据准备

- 使用 `@BeforeEach` 初始化通用数据
- 每个测试方法独立准备数据，测试之间相互独立
- 推荐使用 `@Transactional` 回滚保证测试独立性

---

## 十一、系统配置规范

### 11.1 配置方式

| 配置类型 | 定义位置 | 适用场景 |
|----------|----------|----------|
| 静态配置 | `application.yml` / `@ConfigurationProperties` | 几乎不变更的配置 |
| 动态配置 | `sys_config` 表 + `SystemConfigKeyEnum` | 需要运行时调整的参数 |

### 11.2 动态配置使用

```java
// 获取配置值
String value = configService.getConfigValue(SystemConfigKeyEnum.DEFAULT_PASSWORD.getKey());
```

所有配置键必须在 `SystemConfigKeyEnum` 中定义，禁止硬编码键名。

---

## 十二、工具类规范

**【强制】优先使用 Hutool**，禁止创建与 Hutool 功能重复的工具类。

| 工具类 | 用途 | 示例 |
|--------|------|------|
| `StrUtil` | 字符串操作 | `StrUtil.isBlank()`, `StrUtil.isNotBlank()` |
| `CollUtil` | 集合操作 | `CollUtil.isEmpty()`, `CollUtil.isNotEmpty()` |
| `DateUtil` | 日期操作 | `DateUtil.format()`, `DateUtil.parse()` |
| `IdUtil` | ID生成 | `IdUtil.fastSimpleUUID()` |
| `BeanUtil` | Bean转换 | `BeanUtil.copyProperties()` |
| `Convert` | 类型转换 | `Convert.toInt()`, `Convert.toStr()` |
| `JSONUtil` | JSON操作 | `JSONUtil.toJsonStr()` |

---

## 十三、代码审查清单

提交前自查：

- [ ] 代码格式符合规范（缩进、大括号、命名）
- [ ] 类和方法有必要的注释（ServiceImpl 必须有方法级 + 行级注释）
- [ ] 分层清晰，无跨层调用
- [ ] 异常处理规范（优先使用 ErrorCodeEnum）
- [ ] 日志记录完整（ServiceImpl 关键位置有日志）
- [ ] 无硬编码敏感信息
- [ ] 参数校验完整
- [ ] 事务控制合理
- [ ] 单元测试通过

---

**文档版本**：1.9（精简版）
**最后更新**：2026-04-07
