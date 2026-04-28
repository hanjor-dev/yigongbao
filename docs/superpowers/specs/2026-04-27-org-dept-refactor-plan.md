# 机构/部门/医院重构实现计划

**设计文档：** `docs/superpowers/specs/2026-04-27-org-dept-refactor-design.md`  
**分支：** `feature/yigongbao-module-design`  
**执行顺序：** Phase 1 → 2 → 3 → 4，每个 Phase 完成后独立编译验证

---

## Phase 1：基础数据层

> 先改 SQL 和公共枚举，后续所有 Phase 依赖此基础。

### 1.1 yigongbao-common 枚举变更

**文件：** `yigongbao-common/src/main/java/com/yigongbao/common/enums/FileBizTypeEnum.java`
- 将 `HOSPITAL_CERT("hospital_cert", "10.12", "医院资质文件", null)` 改为 `ORG_CERT("org_cert", "10.12", "机构资质文件", "org.cert")`

**文件：** `yigongbao-common/src/main/java/com/yigongbao/common/enums/SystemConfigKeyEnum.java`
- 新增枚举值：
  - `ORG_CERT_ALLOWED_EXTENSIONS("org.cert.allowed_extensions", "资质文件允许格式")`
  - `ORG_CERT_MAX_SIZE_MB("org.cert.max_size_mb", "资质文件最大大小(MB)")`
  - `MANUFACTURER_ORG_ID("manufacturer.org.id", "生产企业机构ID")`

### 1.2 主 SQL 文件变更

**文件：** `sql/ddl.sql`

`sys_org` 表：
- 删除 `agent_area` 字段
- 将 `business_license` 重命名为 `qualification_file`
- 将 `agent_product_line VARCHAR` 改为 `qualification_type TINYINT COMMENT '1=医疗器械,2=非医疗器械'`

`sys_dept` 表：
- 删除 `org_id` 字段及其索引 `idx_dept_org_id`
- 新增 `dept_type TINYINT NOT NULL COMMENT '1=内部,2=外部'`

`sys_user` 表：
- 新增 `employee_no VARCHAR(32) COMMENT '工号（内部用户必填）'`

`doctor` 表：
- 删除 `hospital_dept_id` 字段及其索引 `idx_doctor_dept`

新增表：
```sql
CREATE TABLE sys_org_hospital (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    distributor_org_id BIGINT NOT NULL COMMENT '经销商机构ID',
    hospital_org_id    BIGINT NOT NULL COMMENT '医疗机构org_id',
    create_time        DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_distributor_hospital (distributor_org_id, hospital_org_id),
    KEY idx_hospital (hospital_org_id)
) COMMENT='经销商-医疗机构关联表';

CREATE TABLE sys_dept_org (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id     BIGINT NOT NULL COMMENT '部门ID',
    org_id      BIGINT NOT NULL COMMENT '机构ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_org (dept_id, org_id),
    KEY idx_org (org_id)
) COMMENT='部门-机构关联表';
```

删除表：`hospital`、`hospital_dept`

**文件：** `sql/init.sql`
- 字典 `dict_code=1`：将 `1.1 生产企业` 的 `disabled` 设为 1；删除 `1.4 其他`
- 插入生产企业预设机构记录（`org_type='1.1'`，固定 id=1）
- 新增 sys_config 配置项：`org.cert.allowed_extensions=zip,rar,tar,7z`、`org.cert.max_size_mb=500`、`manufacturer.org.id=1`

### 1.3 各模块 test schema.sql 同步

涉及文件（5个）：
- `yigongbao-module-system/src/test/resources/schema.sql`
- `yigongbao-module-basic/src/test/resources/schema.sql`
- `yigongbao-module-order/src/test/resources/schema.sql`
- `yigongbao-module-design/src/test/resources/schema.sql`（如有）
- `yigongbao-boot/src/test/resources/schema.sql`

每个文件同步以下变更：
- `sys_org`：字段重命名/删除
- `sys_dept`：删除 `org_id`，新增 `dept_type`
- `sys_user`：新增 `employee_no`
- `doctor`：删除 `hospital_dept_id`
- 新增 `sys_org_hospital`、`sys_dept_org` 表（H2 兼容写法，无函数索引）
- 删除 `hospital`、`hospital_dept` 表定义

**验证：** `mvn clean package -DskipTests` 编译通过

---

## Phase 2：module-system 重构

> 依赖 Phase 1 完成。

### 2.1 新增 Mapper/Entity

新增 `OrgHospitalEntity`（`sys_org_hospital`）和 `OrgHospitalMapper`：
- 路径：`yigongbao-module-system/.../org/entity/OrgHospitalEntity.java`
- 路径：`yigongbao-module-system/.../org/mapper/OrgHospitalMapper.java`
- 方法：`deleteByDistributorOrgId(Long distributorOrgId)`、`insertBatch(List<OrgHospitalEntity>)`

新增 `DeptOrgEntity`（`sys_dept_org`）和 `DeptOrgMapper`：
- 路径：`yigongbao-module-system/.../dept/entity/DeptOrgEntity.java`
- 路径：`yigongbao-module-system/.../dept/mapper/DeptOrgMapper.java`
- 方法：`deleteByDeptId(Long deptId)`、`selectOrgIdsByDeptId(Long deptId)`、`selectDeptIdsByOrgId(Long orgId)`

### 2.2 OrgEntity / OrgVO / OrgDTO 变更

**OrgEntity：**
- 删除 `businessLicense`、`agentArea`、`agentProductLine`
- 新增 `qualificationFile`（String）、`qualificationType`（Integer）

**OrgVO：**
- 同上 + 新增 `hospitalOrgIds`（`List<Long>`）、`hospitalOrgNames`（`List<String>`）

**CreateOrgDTO / UpdateOrgDTO：**
- 同上 + 新增 `hospitalOrgIds`（`List<Long>`，经销商类型时可传）

### 2.3 OrgServiceImpl 变更

- `createOrg()`：
  - 禁止 `orgType=1.1`（抛 `BusinessException`）
  - `qualificationType=1` 时校验 `qualificationFile` 非空
  - `orgType=1.2` 时，将 `hospitalOrgIds` 写入 `sys_org_hospital`（先删后插）
- `updateOrg()`：同上逻辑
- `getOrgById()`：填充 `hospitalOrgIds` 和 `hospitalOrgNames`（从 `sys_org_hospital` + `sys_org` 取）

### 2.4 DeptEntity / DeptVO / DeptDTO 变更

**DeptEntity：**
- 删除 `orgId`
- 新增 `deptType`（Integer）

**DeptVO：**
- 删除 `orgId`、`orgName`
- 新增 `deptType`、`orgIds`（`List<Long>`）、`orgNames`（`List<String>`）

**CreateDeptDTO：**
- 删除 `orgId`
- 新增 `deptType`（必填）、`orgIds`（`List<Long>`）

**DeptPageDTO：**
- 删除 `orgId`，新增 `deptType` 过滤条件

### 2.5 DeptServiceImpl 重写

- `createDept()`：
  - 校验 `deptType` 必填
  - 内部部门（1）：`orgIds` 中的机构必须 `orgType=1.1`
  - 外部部门（2）：`orgIds` 中的机构必须 `orgType=1.2`
  - 部门名称唯一性改为全局唯一（去掉 `orgId` 维度）
  - 写入 `sys_dept_org`
- `updateDept()`：同上，先删 `sys_dept_org` 再插
- `listAllDept(Long orgId)`：改为 `SELECT dept_id FROM sys_dept_org WHERE org_id=?` 再批量查部门
- `getDeptById()`：填充 `orgIds`、`orgNames`

### 2.6 UserEntity / UserDTO / UserVO 变更

**UserEntity：**
- 新增 `employeeNo`（String）

**CreateUserDTO：**
- 新增 `employeeNo`（内部用户必填）
- 新增 `hospitalOrgIds`（`List<Long>`，外部用户可传）
- `orgId` 保留（外部用户必传，内部用户后端自动填充）

**UpdateUserDTO / UserVO：**
- 新增 `employeeNo`

### 2.7 UserServiceImpl 变更

- 移除 `HospitalService` 注入
- `createUser()`：
  - 查询 `deptEntity.getDeptType()`
  - 内部（1）：`orgId` 强制覆盖为 `configService.getConfigValue(MANUFACTURER_ORG_ID)`；校验 `employeeNo` 非空
  - 外部（2）：校验 `orgId` 存在于 `sys_dept_org`（该机构属于所选部门）且 `orgType=1.2`；将 `hospitalOrgIds` 写入 `sys_user_hospital`
- `validateHospitalScope()`：改为查 `sys_org`（`orgType=1.3`）校验 `hospitalIds` 合法性
- `toVOWithNames()`：医院名称改从 `sys_org.org_name` 批量取（按 `sys_user_hospital` 中的 `hospital_id` 查 `sys_org`）

### 2.8 UserHospitalServiceImpl 变更

- `getHospitalIdsByUserId()`：逻辑不变，返回值语义改为 `sys_org.id`（注释更新）
- `hasPermissionOnHospital()`：校验时改查 `sys_org`（`orgType=1.3`）确认目标是医疗机构

### 2.9 测试更新

- `DeptServiceImplTest`：更新所有 `orgId` 相关测试用例，改用 `deptType` + `orgIds`
- `UserServiceImplTest`：新增 `dept_type=1/2` 分支测试
- `UserHospitalServiceImplTest`：更新 mock 数据（`hospital_id` 改为 `sys_org.id`）

**验证：** `mvn test -pl yigongbao-module-system`

---

## Phase 3：module-basic 重构

> 依赖 Phase 1、2 完成（需要 `OrgService` 可用）。

### 3.1 删除 hospital 模块

删除以下包的全部文件：
- `yigongbao-module-basic/.../hospital/`（11个文件）
- `yigongbao-module-basic/.../hospitalDept/`（全部文件）

### 3.2 Doctor 模块变更

**DoctorEntity：**
- 删除 `hospitalDeptId` 字段

**DoctorVO / CreateDoctorDTO / UpdateDoctorDTO：**
- 删除 `hospitalDeptId`、`hospitalDeptName` 字段

**DoctorServiceImpl：**
- 移除 `HospitalService`、`HospitalDeptService` 注入，改注入 `OrgService`
- `createDoctor()`：校验 `hospitalId` 改为 `orgService.getById(hospitalId)` 并校验 `orgType=1.3`
- `updateDoctor()`：同上
- `fillExtraFieldsBatch()`：医院名称改为批量查 `sys_org`（`selectBatchIds(hospitalIds)`）取 `orgName`
- `quickAdd()`：去重条件移除 `hospitalDeptId`

### 3.3 HospitalGroupTemplate 模块变更

**HospitalGroupTemplateDetailVO：**
- 新增 `assigned`（Boolean）字段

**HospitalGroupTemplateServiceImpl：**
- 移除 `HospitalMapper`、`HospitalService` 注入，改注入 `OrgService`、`UserHospitalMapper`
- `saveDetails()`：校验 `hospitalId` 改查 `sys_org`（`orgType=1.3`）
- `getDetails()`：
  - 医院名称批量从 `sys_org.org_name` 取
  - 批量查 `SELECT hospital_id, COUNT(*) FROM sys_user_hospital WHERE hospital_id IN (?) GROUP BY hospital_id`，`count > 0` 则 `assigned=true`

### 3.4 测试更新

- `DoctorServiceImplTest`：移除 `HospitalService` mock，改 mock `OrgService`
- `HospitalDeptServiceImplTest`：删除（对应模块已删除）
- `HospitalGroupTemplateServiceImplTest`（如有）：更新 mock

**验证：** `mvn test -pl yigongbao-module-basic`

---

## Phase 4：module-order 重构

> 依赖 Phase 1、2、3 完成。

### 4.1 订单 DTO/VO/Entity 字段变更

以下文件删除 `hospitalDeptId`、`hospitalDeptName` 字段：
- `CreateOrderDTO`、`UpdateOrderDTO`、`OrderPageDTO`
- `CreateOrderDraftDTO`、`OrderDraftPageDTO`
- `OrderListVO`、`OrderDetailVO`、`OrderDraftVO`、`OrderDraftDetailVO`
- `OrderDraftEntity`（`hospital_dept_id`、`hospital_dept_name` 列）
- `OrderModifyApplyEntity`（如有科室字段）

### 4.2 OrderDataValidator 变更

- 移除 `HospitalService`、`HospitalDeptService` 注入，改注入 `OrgService`
- `lookupHospital(Long hospitalId)`：改为 `orgService.getById(hospitalId)`，校验 `orgType=1.3`，不存在抛 `ErrorCodeEnum.DATA_NOT_FOUND`
- 地区字段填充：从 `OrgEntity.areaId`、`OrgEntity.areaName` 取（替换原 `HospitalEntity` 字段）
- 删除 `lookupHospitalDept()` 方法及所有调用处
- `validateHospitalScope()`：逻辑不变，底层已由 `UserHospitalService` 适配
- 新增 `validateOrderType(Long userId, Integer orderType)`：
  ```
  查 sys_user.org_id → 查 sys_org.qualification_type
  qualification_type=2 且 orderType!=2 → 抛 BusinessException
  ```

### 4.3 OrderMainServiceImpl 变更

- 移除 `HospitalMapper` 注入，改注入 `OrgService`
- `fillAreaFromHospital()`：改为 `orgService.getById(hospitalId)` 取地区信息
- `createOrder()`/`submitOrder()`（`DIRECT`/`SUBMIT` 模式）：调用 `orderDataValidator.validateOrderType(userId, orderType)`
- 草稿保存路径：不调用 `validateOrderType`

### 4.4 OrderModifyApplyServiceImpl 变更

- 删除解析 `hospitalDeptId`/`hospitalDeptName` 变更的逻辑（科室字段已删除）

### 4.5 测试更新

- `OrderMainServiceImplListOrdersTest`：移除 `HospitalService` mock，改 mock `OrgService`；更新 `hospital_id` 测试数据语义
- `OrderQueryHelperTest`：更新 `hospitalId` 相关测试数据
- `OrderModifyApplyServiceImplTest`：移除科室字段相关断言

**验证：** `mvn test -pl yigongbao-module-order`

---

## 最终验证

```bash
cd yigongbao-parent
mvn clean package -DskipTests   # 全量编译
mvn test                         # 全量测试
```

所有测试通过后，提交本次重构变更。
