# accountType 字段类型迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 accountType 字段从 Integer 类型迁移到 String 类型，使用字典编码 '6.1'（企业账户）和 '6.2'（业务账户）替代原有的整数值 1/2

**Architecture:** 
- 数据库层：account_type 字段从 TINYINT 改为 VARCHAR(10)
- 代码层：所有 accountType 字段从 Integer 改为 String，常量值从 1/2 改为 "6.1"/"6.2"
- 验证层：DTO 验证从 @Min/@Max 改为 @Pattern 正则验证

**Tech Stack:** 
- MySQL 8.0（DDL 变更）
- MyBatis Plus 3.5.8（实体映射）
- Jakarta Validation（DTO 验证）
- Spring Boot 3.x

---

## 文件结构映射

### 需要修改的文件（按修改顺序）

**1. 常量定义（1个文件）**
- `yigongbao-common/src/main/java/com/yigongbao/common/constant/StatusConstants.java`
  - 修改4个常量定义和1个方法

**2. 数据库DDL（2个文件）**
- `sql/ddl.sql` - 生产环境DDL
  - sys_user 表 account_type 字段
  - sys_role 表 account_type 字段
- `yigongbao-module-system/src/test/resources/schema.sql` - 测试环境DDL
  - 同步修改

**3. 实体类（2个文件）**
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/entity/UserEntity.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/entity/RoleEntity.java`

**4. DTO 类（6个文件）**
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/CreateUserDTO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/UpdateUserDTO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/UserPageDTO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/dto/CreateRoleDTO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/dto/UpdateRoleDTO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/dto/RolePageDTO.java`

**5. VO 类（2个文件）**
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/vo/RoleVO.java`

**6. Service 层（1个文件）**
- `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java`
  - 删除默认值逻辑（line 391-395）
  - 修改校验逻辑中的比较

**7. 初始化数据（1个文件）**
- `sql/init.sql` - 角色数据的 account_type 值

**8. 测试代码（2个文件）**
- `yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/controller/UserControllerTest.java`
- `yigongbao-module-system/src/test/java/com/yigongbao/module/system/role/controller/RoleControllerTest.java`

---

## 任务分解

### Task 1: 修改常量定义

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/constant/StatusConstants.java:79-106`

- [ ] **Step 1: 修改常量定义**

将整数常量改为字符串常量，值从 1/2 改为 "6.1"/"6.2"：

```java
// ==================== 账户分类 ====================
/**
 * 账户分类-企业账户
 */
public static final String ACCOUNT_TYPE_ENTERPRISE = "6.1";

/**
 * 账户分类-业务账户
 */
public static final String ACCOUNT_TYPE_BUSINESS = "6.2";

/**
 * 账户分类-企业账户名称
 */
public static final String ACCOUNT_TYPE_ENTERPRISE_NAME = "企业账户";

/**
 * 账户分类-业务账户名称
 */
public static final String ACCOUNT_TYPE_BUSINESS_NAME = "业务账户";

/**
 * 根据账户分类获取名称
 */
public static String getAccountTypeName(String accountType) {
    if (accountType == null) {
        return "";
    }
    return ACCOUNT_TYPE_ENTERPRISE.equals(accountType) ? ACCOUNT_TYPE_ENTERPRISE_NAME : ACCOUNT_TYPE_BUSINESS_NAME;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-common/src/main/java/com/yigongbao/common/constant/StatusConstants.java
git commit -m "refactor(constant): 将accountType常量从Integer改为String，使用字典编码6.1/6.2"
```

---

### Task 2: 修改数据库DDL

**Files:**
- Modify: `sql/ddl.sql:120,160`
- Modify: `yigongbao-module-system/src/test/resources/schema.sql`

- [ ] **Step 1: 修改生产DDL - sys_role表**

在 `sql/ddl.sql` line 120：

```sql
account_type        VARCHAR(10)     NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）',
```

- [ ] **Step 2: 修改生产DDL - sys_user表**

在 `sql/ddl.sql` line 160：

```sql
account_type        VARCHAR(10)     NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）',
```

- [ ] **Step 3: 修改测试DDL - 同步修改schema.sql**

在 `schema.sql` 中找到 sys_role 和 sys_user 表的 account_type 字段，同步修改为 VARCHAR(10)。

- [ ] **Step 4: Commit**

```bash
git add sql/ddl.sql yigongbao-module-system/src/test/resources/schema.sql
git commit -m "refactor(ddl): 将account_type字段从TINYINT改为VARCHAR(10)"
```

---

### Task 3: 修改实体类

**Files:**
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/entity/UserEntity.java:61-63`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/entity/RoleEntity.java:39-41`

- [ ] **Step 1: 修改UserEntity**

将 accountType 字段从 Integer 改为 String，更新注释：

```java
/**
 * 账户分类（字典编码：6.1=企业账户，6.2=业务账户）
 */
private String accountType;
```

- [ ] **Step 2: 修改RoleEntity**

同样修改 accountType 字段类型和注释。

- [ ] **Step 3: 编译验证**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/entity/UserEntity.java
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/entity/RoleEntity.java
git commit -m "refactor(entity): 将accountType字段从Integer改为String"
```

---

### Task 4: 修改DTO类

**Files:**
- Modify: `CreateUserDTO.java`, `UpdateUserDTO.java`, `UserPageDTO.java`
- Modify: `CreateRoleDTO.java`, `UpdateRoleDTO.java`, `RolePageDTO.java`

- [ ] **Step 1: 修改CreateUserDTO**

字段类型改为String，验证注解从@Min/@Max改为@Pattern：

```java
/**
 * 账户分类（字典编码：6.1=企业账户，6.2=业务账户）
 * 必填，必须与所选角色的账户分类一致
 */
@NotBlank(message = "账户分类不能为空")
@Pattern(regexp = "^(6\\.1|6\\.2)$", message = "账户分类值不合法，仅支持6.1（企业账户）或6.2（业务账户）")
private String accountType;
```

- [ ] **Step 2: 批量修改其他DTO**

对以下文件执行相同修改（字段类型改为String，更新注释）：
- UpdateUserDTO.java
- UserPageDTO.java  
- CreateRoleDTO.java
- UpdateRoleDTO.java
- RolePageDTO.java

- [ ] **Step 3: 编译验证**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/dto/
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/dto/
git commit -m "refactor(dto): 将accountType字段从Integer改为String，更新验证注解"
```

---

### Task 5: 修改VO类

**Files:**
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java`
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/vo/RoleVO.java`

- [ ] **Step 1: 批量修改VO类**

将 accountType 字段从 Integer 改为 String，更新注释为"字典编码：6.1=企业账户，6.2=业务账户"。

- [ ] **Step 2: 编译验证**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/vo/UserVO.java
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/role/vo/RoleVO.java
git commit -m "refactor(vo): 将accountType字段从Integer改为String"
```

---

### Task 6: 修改Service层逻辑

**Files:**
- Modify: `yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java:391-395`

- [ ] **Step 1: 删除默认值逻辑**

删除 line 391-395 的默认值设置代码：

```java
// 删除以下代码块
// accountType 为空时默认设置为内部用户（1）
if (dto.getAccountType() == null) {
    dto.setAccountType(1);
    log.info("账户分类未指定，默认设置为内部用户");
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-system/src/main/java/com/yigongbao/module/system/user/service/impl/UserServiceImpl.java
git commit -m "refactor(service): 删除accountType默认值逻辑，改为前端必传"
```

---

### Task 7: 修改初始化数据

**Files:**
- Modify: `sql/init.sql:558-578`

- [ ] **Step 1: 修改角色数据的account_type值**

将所有角色的 account_type 从整数改为字符串：
- 企业角色（超级管理员、设计师、设计管理员、生产员、生产管理员、质管、库管、财务、公司管理员）：改为 '6.1'
- 业务角色（业务员、区域管理员）：改为 '6.2'

```sql
INSERT INTO sys_role (id, role_name, role_code, role_desc, account_type, data_scope_type, status)
VALUES
(1,  '超级管理员',   'admin',               '...', '6.1', 'all',       1),
(2,  '业务员',       'salesman',            '...', '6.2', 'hospitals', 1),
(3,  '区域管理员',   'regional-manager',    '...', '6.2', 'dept',      1),
(4,  '设计师',       'designer',            '...', '6.1', 'self',      1),
(5,  '设计管理员',   'designer-manager',    '...', '6.1', 'all',       1),
(6,  '生产员',       'production-worker',   '...', '6.1', 'all',       1),
(7,  '生产管理员',   'production-manager',  '...', '6.1', 'all',       1),
(8,  '质管',         'qc-inspector',        '...', '6.1', 'all',       1),
(9,  '库管',         'warehouse-manager',   '...', '6.1', 'all',       1),
(10, '财务',         'finance',             '...', '6.1', 'all',       1),
(11, '公司管理员',   'company-admin',       '...', '6.1', 'all',       1);
```

- [ ] **Step 2: Commit**

```bash
git add sql/init.sql
git commit -m "refactor(data): 将角色account_type从整数改为字典编码字符串"
```

---

### Task 8: 修改测试代码

**Files:**
- Modify: `yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/controller/UserControllerTest.java`
- Modify: `yigongbao-module-system/src/test/java/com/yigongbao/module/system/role/controller/RoleControllerTest.java`

- [ ] **Step 1: 修改测试数据**

将测试代码中所有 accountType 的值从整数 1/2 改为字符串 "6.1"/"6.2"。

- [ ] **Step 2: 运行测试**

Run: `mvn test -Dtest=UserControllerTest,RoleControllerTest`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-system/src/test/java/com/yigongbao/module/system/user/controller/UserControllerTest.java
git add yigongbao-module-system/src/test/java/com/yigongbao/module/system/role/controller/RoleControllerTest.java
git commit -m "test: 更新测试数据中的accountType为字符串类型"
```

---

### Task 9: 最终验证和数据库迁移

**Files:**
- N/A (验证和数据库操作)

- [ ] **Step 1: 运行完整测试套件**

Run: `mvn clean test`
Expected: All tests PASS

- [ ] **Step 2: 检查现有数据一致性**

在生产数据库执行检查SQL：

```sql
SELECT u.id, u.username, u.account_type AS user_type, r.account_type AS role_type
FROM sys_user u
JOIN sys_role r ON u.role_id = r.id
WHERE u.account_type != r.account_type AND u.is_deleted = 0;
```

Expected: 返回0行（无不一致数据）

- [ ] **Step 3: 执行数据库迁移**

**警告：此步骤会修改生产数据库结构，需要在维护窗口执行**

```sql
-- 1. 修改sys_role表
ALTER TABLE sys_role MODIFY COLUMN account_type VARCHAR(10) NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）';

-- 2. 更新sys_role数据
UPDATE sys_role SET account_type = '6.1' WHERE account_type = '1';
UPDATE sys_role SET account_type = '6.2' WHERE account_type = '2';

-- 3. 修改sys_user表
ALTER TABLE sys_user MODIFY COLUMN account_type VARCHAR(10) NOT NULL COMMENT '账户分类（字典编码：6.1=企业账户，6.2=业务账户）';

-- 4. 更新sys_user数据
UPDATE sys_user SET account_type = '6.1' WHERE account_type = '1';
UPDATE sys_user SET account_type = '6.2' WHERE account_type = '2';
```

- [ ] **Step 4: 验证数据迁移**

```sql
-- 检查角色数据
SELECT id, role_name, account_type FROM sys_role;

-- 检查用户数据
SELECT id, username, account_type FROM sys_user LIMIT 10;
```

Expected: 所有 account_type 值为 '6.1' 或 '6.2'

- [ ] **Step 5: 最终提交**

```bash
git push origin <branch-name>
```

---

## 验收标准

- [ ] 所有常量从整数改为字符串，值为 "6.1"/"6.2"
- [ ] DDL 中 account_type 字段类型为 VARCHAR(10)
- [ ] 所有实体、DTO、VO 的 accountType 字段类型为 String
- [ ] CreateUserDTO 的 accountType 使用 @Pattern 验证
- [ ] UserServiceImpl 已删除默认值逻辑
- [ ] 初始化数据中角色的 account_type 为字符串
- [ ] 所有测试通过
- [ ] 生产数据库迁移成功，数据一致性检查通过

---

## 风险提示

1. **数据库迁移风险**：ALTER TABLE 会锁表，建议在低峰期执行
2. **数据一致性**：迁移前务必备份数据库
3. **前端兼容性**：前端需要同步更新，传递字符串类型的 accountType

---

**计划状态**: 已完成  
**预计工时**: 2-3小时  
**下一步**: 选择执行方式（subagent-driven 或 inline execution）
