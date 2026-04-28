# 机构管理 & 部门管理重构设计文档

**日期：** 2026-04-27  
**状态：** 待实现  
**分支：** feature/yigongbao-module-design

> **注意**：当前处于开发阶段，无需考虑历史数据迁移，数据可丢弃。

---

## 一、背景与目标

### 问题
1. 系统中"医院管理"（`hospital` 表）与"机构管理"（`sys_org` 表）概念重叠，医疗机构类型的机构即为医院，维护两套数据造成冗余。
2. 部门（`sys_dept`）当前是机构的下级，不符合实际业务结构（部门应为机构的上级管理单元）。
3. 机构类型中"生产企业"可被动态创建，但业务上只应存在唯一一个生产企业。
4. 字段命名不准确：`businessLicense`（营业执照）实为资质文件；`agentProductLine`（代理产品线）实为资质类型。
5. `doctor` 和 `hospital_group_template` 模块依赖已废弃的 `hospital` 表，需同步迁移。
6. 用户归属、订单类型限制缺乏与机构资质的联动。

### 目标
- 废弃 `hospital` 表，统一在 `sys_org` 中管理所有机构（含医疗机构）
- 调整部门为机构的上级管理单元，支持一个机构属于多个部门；部门显式区分内部/外部类型
- 生产企业改为系统预设唯一机构，不可动态创建
- 字段语义修正，新增资质文件上传配置
- `doctor` 和 `hospital_group_template` 模块同步适配新数据模型
- 创建用户时根据部门类型（内部/外部）走不同的信息填写流程
- 创建订单时根据用户所属机构的 `qualification_type` 自动限制可选订单类型

---

## 二、数据模型变更

### 2.1 `sys_org` 表字段变更

| 操作 | 旧字段 | 新字段 | 说明 |
|------|--------|--------|------|
| 重命名 | `business_license` | `qualification_file` | 存资质文件路径（压缩包） |
| 重命名+类型变更 | `agent_product_line` VARCHAR | `qualification_type` TINYINT | 1=医疗器械，2=非医疗器械 |
| 删除 | `agent_area` | — | 去掉代理区域字段 |

### 2.2 新增关联表

**`sys_org_hospital`（经销商-医疗机构多对多）：**
```sql
CREATE TABLE sys_org_hospital (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    distributor_org_id BIGINT NOT NULL COMMENT '经销商机构ID',
    hospital_org_id    BIGINT NOT NULL COMMENT '医疗机构org_id',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_distributor_hospital (distributor_org_id, hospital_org_id),
    KEY idx_hospital (hospital_org_id)
) COMMENT='经销商-医疗机构关联表';
```

**`sys_dept_org`（部门-机构多对多）：**
```sql
CREATE TABLE sys_dept_org (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id     BIGINT NOT NULL COMMENT '部门ID',
    org_id      BIGINT NOT NULL COMMENT '机构ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_org (dept_id, org_id),
    KEY idx_org (org_id)
) COMMENT='部门-机构关联表';
```

### 2.3 `sys_dept` 表变更
- 删除 `org_id` 字段（改用 `sys_dept_org` 关联表）
- 新增 `dept_type` TINYINT 字段：1=内部部门（关联生产企业），2=外部部门（关联经销商）

### 2.4 `sys_user_hospital` 表变更
- 保留表结构不变
- `hospital_id` 字段语义变更：从"hospital 表主键"改为"sys_org 中 orgType=医疗机构 的 org_id"
- 无 DDL 变更，仅业务层语义调整

### 2.5 `doctor` 表字段变更

| 操作 | 旧字段 | 新字段 | 说明 |
|------|--------|--------|------|
| 语义变更 | `hospital_id` | `hospital_id`（保留字段名） | 改为引用 `sys_org.id`（医疗机构类型） |
| 删除 | `hospital_dept_id` | — | `hospital_dept` 表废弃，科室概念去掉 |

### 2.6 `hospital_group_template_detail` 表字段变更

| 操作 | 旧字段 | 新字段 | 说明 |
|------|--------|--------|------|
| 语义变更 | `hospital_id` | `hospital_id`（保留字段名） | 改为引用 `sys_org.id`（医疗机构类型） |

### 2.7 废弃的表
- `hospital`
- `hospital_dept`
- `sys_user_hospital` 中的数据清空（表结构保留）

---

## 三、字典变更

### 机构类型（dict_code=1）
| 操作 | 值 | 名称 | 说明 |
|------|-----|------|------|
| 保留（禁用） | 1.1 | 生产企业 | 设置 `disabled=1`，前端不可选择，保留用于预设机构的类型名称展示 |
| 保留 | 1.2 | 经销商 | — |
| 保留 | 1.3 | 医疗机构 | — |
| 删除 | 1.4 | 其他 | 直接删除 |

> 生产企业字典值不删除，仅标记 `disabled=1`。前端创建机构时过滤 disabled 选项，Service 层额外校验禁止传入 `1.1`。

### 文件业务类型（FileBizTypeEnum）
- `HOSPITAL_CERT("hospital_cert", "10.12", "医院资质文件", null)`
- 改为：`ORG_CERT("org_cert", "10.12", "机构资质文件", "org.cert")`
- `configPrefix` 从 `null` 改为 `"org.cert"`，使上传时自动读取格式/大小配置

---

## 四、sys_config 新增配置项

| 枚举常量 | config_key | 默认值 | 说明 |
|---------|-----------|--------|------|
| `ORG_CERT_ALLOWED_EXTENSIONS` | `org.cert.allowed_extensions` | `zip,rar,tar,7z` | 资质文件允许格式 |
| `ORG_CERT_MAX_SIZE_MB` | `org.cert.max_size_mb` | `500` | 资质文件最大大小(MB) |
| `MANUFACTURER_ORG_ID` | `manufacturer.org.id` | （init.sql 插入后填入） | 生产企业机构ID |

---

## 五、业务规则

### 5.1 机构创建/更新
- `orgType` 禁止传生产企业类型（Service 层抛 `BusinessException`）
- `qualificationType=1`（医疗器械）时，`qualificationFile` 必填
- `orgType=经销商` 时，可传 `hospitalOrgIds` 列表，写入 `sys_org_hospital`

### 5.2 生产企业预设
- `init.sql` 插入唯一生产企业机构记录，`orgType=1.1`
- `sys_config` 中 `manufacturer.org.id` 存其 ID
- 字典 `1.1` 保留但设置 `disabled=1`，前端过滤 disabled 选项，Service 层额外校验禁止传入

### 5.3 部门管理
- 创建部门时必须指定 `dept_type`（1=内部，2=外部）
- 内部部门只能关联生产企业机构（`orgType=1.1`），外部部门只能关联经销商机构（`orgType=1.2`），Service 层校验
- 创建/更新部门时，可传 `orgIds` 列表，写入 `sys_dept_org`
- 查询"某部门下所有机构"：`SELECT org_id FROM sys_dept_org WHERE dept_id = ?`
- 查询"某机构所属部门"：`SELECT dept_id FROM sys_dept_org WHERE org_id = ?`
- `DeptService.listAllDept(Long orgId)` 语义改为：查询关联了指定机构的所有部门
- 唯一调用方为 `DeptController.listAll`（前端下拉选择），无破坏性影响

### 5.5 创建用户流程
创建用户时，根据所选部门的 `dept_type` 走不同流程：

**内部部门（dept_type=1）：**
- 用户 `org_id` 由后端自动填充为生产企业 org_id（从 `sys_config.manufacturer.org.id` 取），前端无需传入
- 必填工号（`employeeNo`）字段

**外部部门（dept_type=2）：**
- `CreateUserDTO` 中需同时传入 `deptId`（部门）和 `orgId`（该部门下的某个经销商机构，前端二次选择）
- 后端校验 `orgId` 必须存在于 `sys_dept_org` 中（即该机构确实属于所选部门），且 `orgType=1.2`
- 再从该经销商机构关联的医疗机构（`sys_org_hospital`）中多选，传入 `hospitalOrgIds`，写入 `sys_user_hospital`
- 工号非必填
- 部门-机构关联变更时，用户 `org_id` 不联动（用户归属机构以创建时选择为准，需手动更新）

### 5.6 创建订单时的订单类型限制
- 创建订单时，后端根据当前用户的 `org_id` 查询 `sys_org.qualification_type`
- `qualification_type=1`（医疗器械）：`orderType` 可选 1 或 2
- `qualification_type=2`（非医疗器械）：`orderType` 强制为 2，后端校验传入值必须为 2
- `OrderDataValidator` 中新增 `validateOrderType(userId, orderType)` 校验方法
- **调用策略**：`DIRECT`/`SUBMIT` 模式调用；草稿（`DRAFT`）模式跳过；订单修改（`UpdateOrderDTO`）中 `orderType` 不允许变更，修改接口不调用此校验

### 5.4 数据权限（DataScope）
- `HOSPITALS` scope 逻辑不变，底层从 `sys_user_hospital` 查用户可见的医疗机构 org_id
- `UserHospitalService.getHospitalIdsByUserId()` 返回值语义改为 `sys_org.id`（医疗机构）

### 5.5 医生管理
- `DoctorServiceImpl` 中校验 `hospitalId` 改为查 `sys_org`（`orgType=1.3`）
- 删除所有 `hospitalDeptId` 相关校验和填充逻辑（`HospitalDeptService` 注入移除）
- `DoctorVO`、`CreateDoctorDTO`、`UpdateDoctorDTO` 删除 `hospitalDeptId`/`hospitalDeptName` 字段
- `quickAdd` 去重逻辑中 `hospitalDeptId` 条件移除

### 5.6 医院分组模板
- `HospitalGroupTemplateDetailEntity.hospitalId` 语义改为 `sys_org.id`（医疗机构类型）
- 创建/更新模板时，校验 `hospitalId` 改为查 `sys_org`（`orgType=1.3`）
- VO 中填充医院名称改为从 `sys_org.org_name` 取值
- `getTemplateById` 返回的明细列表中，每条明细新增 `assigned` 布尔字段：`true` 表示该医疗机构 org_id 已被至少一个用户关联（查 `sys_user_hospital`），前端据此做提醒

---

## 六、影响模块清单

### module-system（sys_org / sys_dept / sys_user）
| 文件 | 变更类型 |
|------|---------|
| `OrgEntity` | 字段重命名/删除/新增 |
| `OrgVO` / `CreateOrgDTO` / `UpdateOrgDTO` | 同上 + 新增 `hospitalOrgIds` |
| `OrgServiceImpl` | 新增经销商关联医疗机构逻辑；禁止创建生产企业类型校验 |
| `DeptEntity` | 删除 `orgId`；新增 `deptType`（1=内部，2=外部） |
| `DeptVO` / `CreateDeptDTO` / `DeptPageDTO` | 同上 + 新增 `orgIds` |
| `DeptServiceImpl` | 重写所有 `orgId` 相关逻辑（创建校验、名称唯一性、`listAllDept`）改用 `sys_dept_org`；新增 `deptType` 与关联机构类型一致性校验 |
| `UserEntity` | 新增 `employeeNo` 字段 |
| `CreateUserDTO` / `UpdateUserDTO` / `UserVO` | 新增 `employeeNo`；新增 `hospitalOrgIds`（外部用户医疗机构多选） |
| `UserServiceImpl` | 移除 `HospitalService` 注入；`createUser()` 按 `deptType` 分支：内部自动填充 `org_id`=生产企业，外部写 `sys_user_hospital`；`validateHospitalScope` 改查 `sys_org`（`orgType=1.3`）；`toVOWithNames` 医院名称改从 `sys_org` 取 |
| `UserHospitalEntity` / `UserHospitalMapper` | 语义注释更新，无 DDL 变更 |
| `UserHospitalServiceImpl` | 查询目标改为 `sys_org`（医疗机构类型） |

### module-basic（hospital / doctor / hospitalGroupTemplate）
| 文件 | 变更类型 |
|------|---------|
| `hospital` 包全部 11 个文件 | 删除 |
| `hospitalDept` 包全部文件 | 删除 |
| `DoctorEntity` | 删除 `hospitalDeptId` 字段；`hospitalId` 语义改为 `sys_org.id` |
| `DoctorVO` / `CreateDoctorDTO` / `UpdateDoctorDTO` | 删除 `hospitalDeptId`/`hospitalDeptName` 字段 |
| `DoctorServiceImpl` | 移除 `HospitalService`/`HospitalDeptService` 注入，改注入 `OrgService`；校验 `hospitalId` 改查 `sys_org`（`orgType=1.3`）；移除科室相关逻辑；`fillExtraFieldsBatch` 医院名称改从 `sys_org` 批量取 |
| `HospitalGroupTemplateDetailEntity` | `hospitalId` 语义改为 `sys_org.id` |
| `HospitalGroupTemplateDetailVO` | 新增 `assigned` 布尔字段 |
| `HospitalGroupTemplateServiceImpl` | 移除 `HospitalMapper`/`HospitalService` 注入，改注入 `OrgService` 和 `UserHospitalMapper`；`saveDetails` 校验改查 `sys_org`；`getDetails` 填充名称改从 `sys_org.org_name` 批量取；批量查 `sys_user_hospital` 填充 `assigned` 字段 |

### module-order
| 文件 | 变更类型 |
|------|---------|
| `OrderDataValidator` | 移除 `HospitalService`/`HospitalDeptService` 注入；`lookupHospital` 改查 `sys_org`（`orgType=1.3`）；地区字段从 `OrgEntity` 取；移除所有科室相关逻辑；`validateHospitalScope` 改用新 scope 逻辑；新增 `validateOrderType(userId, orderType)` |
| `OrderMainServiceImpl` | 移除 `HospitalMapper` 注入，改注入 `OrgService`；`fillAreaFromHospital` 改查 `sys_org`；创建/提交订单时调用 `validateOrderType`（`DIRECT`/`SUBMIT` 模式，草稿模式跳过） |
| `OrderQueryHelper` / `OrderExportServiceImpl` | scope 过滤数据源不变（仍用 `UserHospitalService`） |
| 订单 DTO/VO/Entity 中 `hospitalId` 字段 | 语义改为 `sys_org.id`，字段名保持不变 |
| 订单 VO/Entity 中 `hospitalName` 字段 | 新建订单时从 `sys_org.org_name` 填充 |
| 订单 DTO/VO/Entity 中 `hospitalDeptId`/`hospitalDeptName` 字段 | 删除（科室概念废弃） |

### module-design
| 文件 | 变更类型 |
|------|---------|
| `DesignWorkorderServiceImpl` | `UserHospitalService` 注入不变，scope 逻辑自动适配 |

### yigongbao-common
| 文件 | 变更类型 |
|------|---------|
| `FileBizTypeEnum` | `HOSPITAL_CERT` → `ORG_CERT`，更新 name/configPrefix |
| `SystemConfigKeyEnum` | 新增 3 个枚举值 |

### SQL 文件
| 文件 | 变更类型 |
|------|---------|
| `sql/ddl.sql` | `sys_org` 字段变更；`sys_dept` 删除 `org_id`、新增 `dept_type`；`sys_user` 新增 `employee_no`；新增 `sys_org_hospital`、`sys_dept_org`；删除 `hospital`/`hospital_dept` 表；`doctor` 删除 `hospital_dept_id` 字段 |
| `sql/init.sql` | 插入生产企业预设机构；更新字典数据（1.1 禁用、1.4 删除）；新增 sys_config 配置项 |
| 各模块 `test/resources/schema.sql`（5个） | 同步更新 |

---

## 七、不在本次范围内

- 前端页面改造

---

## 八、实现变更记录（相对原设计的调整）

### 8.1 模块归属调整
- `doctor` 和 `hospitalGroupTemplate` 模块从 `module-basic` 迁移到 `module-system`，解决循环依赖问题
- 接口路由变更：`/basic/doctor` → `/system/doctor`，`/basic/hospital-group-template` → `/system/hospital-group-template`

### 8.2 新增常量和枚举
- `DictCodeConstants` 新增：`ORG_TYPE_PRODUCER("1.1")`、`ORG_TYPE_DEALER("1.2")`、`ORG_TYPE_HOSPITAL("1.3")`、`SETTLEMENT_TYPE("8")`
- `ErrorCodeEnum` 新增：`ORG_CERT_FILE_REQUIRED(621)`、`ORG_DEPT_TYPE_MISMATCH(622)`、`ORG_NOT_BELONG_TO_DEPT(623)`、`ORG_TYPE_MUST_BE_DEALER(624)`、`ORG_QUALIFICATION_LIMIT(625)`、`EMPLOYEE_NO_REQUIRED(626)`
- 修复 `ErrorCodeEnum` 中 `ORG_TYPE_NOT_ALLOWED` 与 `DEPT_NOT_FOUND` 编码冲突（619→620）

### 8.3 Service 层调用规范修复
- `UserHospitalService` 新增 `getAssignedHospitalIds(List<Long>)` 方法
- `UserService` 新增 `countByDeptId(Long)` 方法
- `DeptServiceImpl` 改用 `UserService` 替代直接注入 `UserMapper`
- `UserHospitalServiceImpl` 改用 `OrgService` 替代直接注入 `OrgMapper`
- `HospitalGroupTemplateServiceImpl` 改用 `UserHospitalService` 替代直接注入 `UserHospitalMapper`
- `OrderDataValidator` 改用 `UserService` 替代直接注入 `UserMapper`

### 8.4 业务逻辑完善
- `OrgServiceImpl`：经销商关联医疗机构时校验 `hospitalOrgIds` 必须为医疗机构类型
- `UserServiceImpl`：外部部门用户创建时 `orgId` 强制必填
- `OrderDataValidator`：修复 `validateAndFillMasterForOrder` 与草稿版本 hospitalScope 校验不对称问题
- `HospitalGroupTemplateDetailVO`：`fullAreaName` 改从 `OrgEntity.areaName` 填充（不再为 null）
