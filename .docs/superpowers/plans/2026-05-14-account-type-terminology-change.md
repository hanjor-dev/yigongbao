# 账户分类术语修改方案

**日期**: 2026-05-14  
**状态**: 设计中  
**影响范围**: 用户模块、角色模块、部门模块

## 一、背景与目标

### 背景
当前系统使用"内部用户/外部用户"来描述账户分类，字典值为 1/2。现需统一术语为"企业账户/业务账户"，以更准确反映业务含义。

### 目标
1. 术语统一：所有代码注释、验证消息从"内部/外部"改为"企业账户/业务账户"
2. 数据修正：业务员、区域管理员角色的 account_type 从 1 改为 2
3. 逻辑增强：删除默认值逻辑，前端必传 accountType，后端校验与角色类型一致
4. 部门术语同步：部门类型也改为"企业部门/业务部门"

### 核心决策
- **值不变**：企业账户=1，业务账户=2（与字典保持一致）
- **强制校验**：用户的 accountType 必须与其角色的 accountType 一致
- **前端必传**：不再允许 accountType 为空，删除后端默认值逻辑

## 二、影响域分析

### 2.1 数据层修改（2处）
| 文件 | 位置 | 修改内容 |
|------|------|----------|
| sql/init.sql | line 563-564 | 业务员、区域管理员的 account_type: 1→2 |
| yigongbao-module-system/src/test/resources/schema.sql | line 245-248 | 同步修改测试数据 |

### 2.2 实体类注释（3个文件）
| 文件 | 字段 | 原注释 | 新注释 |
|------|------|---------|---------|
| UserEntity.java | accountType | 账户分类（1=内部用户，2=外部用户） | 账户分类（1=企业账户，2=业务账户） |
| RoleEntity.java | accountType | 账户分类（1=内部用户，2=外部用户） | 账户分类（1=企业账户，2=业务账户） |
| DeptEntity.java | deptType | 部门类型（1=内部，2=外部） | 部门类型（1=企业部门，2=业务部门） |

### 2.3 DTO 类修改（8个文件）
需要更新注释和验证消息：
- user/dto/CreateUserDTO.java (line 66-71)
- user/dto/UpdateUserDTO.java
- user/dto/UserPageDTO.java
- role/dto/CreateRoleDTO.java
- role/dto/UpdateRoleDTO.java
- role/dto/RolePageDTO.java
- dept/dto/CreateDeptDTO.java
- dept/dto/UpdateDeptDTO.java

### 2.4 VO 类修改（3个文件）
需要更新注释：
- user/vo/UserVO.java
- role/vo/RoleVO.java
- dept/vo/DeptVO.java

### 2.5 Service 层核心逻辑修改
**UserServiceImpl.java**:
1. **删除默认值逻辑** (line 391-395)
2. **新增校验逻辑**：accountType 必须与角色的 accountType 一致

## 三、详细修改方案

### 3.1 数据修改

**sql/init.sql (line 563-564)**
```sql
-- 修改前
(2,  '业务员',       'salesman',            '负责订单开拓、客户维护，只能看自己关联医院的数据',             1, 'hospitals', 1),
(3,  '区域管理员',   'regional-manager',    '管理本部门下所有机构及订单，拥有订单模块全部操作权限',         1, 'dept',      1),

-- 修改后
(2,  '业务员',       'salesman',            '负责订单开拓、客户维护，只能看自己关联医院的数据',             2, 'hospitals', 1),
(3,  '区域管理员',   'regional-manager',    '管理本部门下所有机构及订单，拥有订单模块全部操作权限',         2, 'dept',      1),
```

**schema.sql (测试环境同步修改)**

### 3.2 CreateUserDTO 修改

**修改点1：注释更新 (line 66-71)**
```java
// 修改前
/**
 * 账户分类（1=内部用户，2=外部用户）
 * 允许为空，为空时默认设置为内部用户（1）
 */

// 修改后
/**
 * 账户分类（1=企业账户，2=业务账户）
 * 必填，必须与所选角色的账户分类一致
 */
@NotNull(message = "账户分类不能为空")
```

**修改点2：验证消息更新 (line 69-70)**
```java
// 修改前
@Min(value = 1, message = "账户分类值不合法，仅支持1（内部用户）或2（外部用户）")
@Max(value = 2, message = "账户分类值不合法，仅支持1（内部用户）或2（外部用户）")

// 修改后
@Min(value = 1, message = "账户分类值不合法，仅支持1（企业账户）或2（业务账户）")
@Max(value = 2, message = "账户分类值不合法，仅支持1（企业账户）或2（业务账户）")
```

### 3.3 UserServiceImpl 核心逻辑修改

**修改点1：删除默认值逻辑 (line 391-395)**
```java
// 删除以下代码
// accountType 为空时默认设置为内部用户（1）
if (dto.getAccountType() == null) {
    dto.setAccountType(1);
    log.info("账户分类未指定，默认设置为内部用户");
}
```

**修改点2：新增校验逻辑（在角色校验后添加）**
```java
// 在 line 396 附近（角色业务规则校验之前）添加
// 校验用户账户分类与角色账户分类一致
if (!dto.getAccountType().equals(roleEntity.getAccountType())) {
    log.warn("用户账户分类与角色不匹配，userAccountType={}, roleAccountType={}, roleCode={}", 
        dto.getAccountType(), roleEntity.getAccountType(), roleEntity.getRoleCode());
    throw new BusinessException(ErrorCodeEnum.USER_ACCOUNT_TYPE_MISMATCH);
}
```

### 3.4 错误码新增

**ErrorCodeEnum.java**
```java
// 新增错误码
USER_ACCOUNT_TYPE_MISMATCH(605, "用户账户分类与角色类型不匹配"),
```

### 3.5 其他文件注释修改（批量替换）

**实体类、DTO、VO 中的注释统一替换：**
- `账户分类（1=内部用户，2=外部用户）` → `账户分类（1=企业账户，2=业务账户）`
- `部门类型（1=内部，2=外部）` → `部门类型（1=企业部门，2=业务部门）`

**涉及文件清单（共14个）：**
- UserEntity.java, UserVO.java, CreateUserDTO.java, UpdateUserDTO.java, UserPageDTO.java
- RoleEntity.java, RoleVO.java, CreateRoleDTO.java, UpdateRoleDTO.java, RolePageDTO.java
- DeptEntity.java, DeptVO.java, CreateDeptDTO.java, UpdateDeptDTO.java

## 四、风险点与注意事项

### 4.1 数据一致性风险
**风险**：现有数据库中可能存在用户的 accountType 与其角色的 accountType 不一致的情况。

**应对措施**：
修改前执行数据检查 SQL：
```sql
SELECT u.id, u.username, u.account_type AS user_type, r.account_type AS role_type
FROM sys_user u
JOIN sys_role r ON u.role_id = r.id
WHERE u.account_type != r.account_type AND u.is_deleted = 0;
```

### 4.2 前端兼容性
**风险**：前端可能未传 accountType 参数，导致创建用户失败。

**应对措施**：
1. 后端先部署，前端同步更新
2. 前端需要在选择角色后自动带出 accountType
3. 前端表单增加 accountType 必填校验

### 4.3 UpdateUserDTO 处理
**注意**：更新用户时也需要校验 accountType 与角色的一致性（如果传入了新的 roleId）。

## 五、实施步骤

### 阶段1：数据检查与修正
1. 执行数据一致性检查 SQL
2. 修改 init.sql 中的角色数据（业务员、区域管理员）
3. 同步修改 schema.sql 测试数据

### 阶段2：代码修改
1. 新增错误码 `USER_ACCOUNT_TYPE_MISMATCH`
2. 修改 CreateUserDTO：更新注释、添加 @NotNull、更新验证消息
3. 修改 UserServiceImpl：删除默认值逻辑、新增校验逻辑
4. 批量替换所有实体、DTO、VO 的注释（14个文件）

### 阶段3：测试验证
1. 单元测试：UserServiceImplTest 新增 accountType 校验测试用例
2. 接口测试：UserControllerTest 验证必填和校验逻辑
3. 手工测试：创建企业账户、业务账户用户

### 阶段4：前端联调
1. 前端选择角色后自动带出 accountType
2. 前端表单增加 accountType 必填校验
3. 联调测试完整创建流程

## 六、验收标准

- [ ] 所有注释已从"内部/外部"改为"企业账户/业务账户"
- [ ] 业务员、区域管理员角色的 account_type 为 2
- [ ] CreateUserDTO 的 accountType 字段标记为 @NotNull
- [ ] UserServiceImpl 已删除默认值逻辑
- [ ] UserServiceImpl 已新增 accountType 与角色类型一致性校验
- [ ] 错误码 USER_ACCOUNT_TYPE_MISMATCH 已定义
- [ ] 单元测试覆盖新增校验逻辑
- [ ] 创建用户时 accountType 为空会报错
- [ ] 创建用户时 accountType 与角色类型不一致会报错
- [ ] 前端可正常创建企业账户和业务账户用户

## 七、回滚方案

如遇问题需回滚：
1. 恢复 init.sql 中角色数据（account_type 改回 1）
2. 恢复 UserServiceImpl 的默认值逻辑
3. 移除 @NotNull 注解，恢复为可选
4. 移除 accountType 校验逻辑

---

**文档状态**: 已完成  
**下一步**: 开始实施阶段1（数据检查与修正）
