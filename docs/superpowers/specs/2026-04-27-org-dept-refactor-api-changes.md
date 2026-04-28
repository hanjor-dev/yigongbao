# 机构/部门/医院重构 - 接口变更说明

> 本文档列出本次重构涉及的所有接口变更，供前端调用方参考。
> **（改动）** 标识表示该接口有变更，需要前端同步更新。

---

## 一、机构管理（/system/org）

### 创建机构 POST /system/org **（改动）**
**请求体变更：**
- 删除字段：`businessLicense`、`agentArea`、`agentProductLine`
- 新增字段：`qualificationFile`（String，资质文件路径）、`qualificationType`（Integer，1=医疗器械/2=非医疗器械）、`hospitalOrgIds`（List<Long>，经销商类型时可传，关联医疗机构ID列表）
- **新增校验**：`orgType` 不允许传生产企业类型（1.1）；`qualificationType=1` 时 `qualificationFile` 必填

### 更新机构 PUT /system/org/{id} **（改动）**
**请求体变更**：同创建机构

### 查询机构详情 GET /system/org/{id} **（改动）**
**响应体变更：**
- 删除字段：`businessLicense`、`agentArea`、`agentProductLine`、`agentProductLineNames`
- 新增字段：`qualificationFile`、`qualificationType`、`hospitalOrgIds`、`hospitalOrgNames`

### 分页查询机构列表 POST /system/org/list **（改动）**
**响应体变更**：同查询机构详情（列表项字段变更）

---

## 二、部门管理（/system/dept）

### 创建部门 POST /system/dept **（改动）**
**请求体变更：**
- 删除字段：`orgId`
- 新增字段：`deptType`（Integer，必填，1=内部/2=外部）、`orgIds`（List<Long>，关联机构ID列表）

### 更新部门 PUT /system/dept/{id} **（改动）**
**请求体变更**：同创建部门

### 查询部门详情 GET /system/dept/{id} **（改动）**
**响应体变更：**
- 删除字段：`orgId`、`orgName`
- 新增字段：`deptType`、`orgIds`、`orgNames`

### 全量查询部门列表 GET /system/dept/list **（改动）**
**语义变更**：`orgId` 参数含义从"查询该机构下的部门"变更为"查询关联了该机构的部门"

---

## 三、用户管理（/system/user）

### 创建用户 POST /system/user **（改动）**
**请求体变更：**
- 新增字段：`employeeNo`（String，内部部门用户必填）、`hospitalOrgIds`（List<Long>，外部部门用户可传，关联医疗机构）
- **新增校验**：外部部门（deptType=2）用户必须传 `orgId`

### 更新用户 PUT /system/user/{id} **（改动）**
**请求体变更**：新增 `employeeNo` 字段

### 查询用户详情/列表 **（改动）**
**响应体变更**：新增 `employeeNo` 字段

---

## 四、用户医院关联（/system/user/{userId}/hospitals）

### 查询用户医院列表 GET /system/user/{userId}/hospitals **（改动）**
**响应体变更**：返回类型从 `HospitalVO` 改为 `OrgVO`（字段名变化：`hospitalName`→`orgName`，`hospitalCode`→`orgCode` 等）

### 获取可分配医院列表 POST /system/user/{userId}/hospitals/options **（改动）**
**响应体变更**：同上

### 获取当前用户可操作医院 GET /system/hospital-scope/my-hospitals **（改动）**
**响应体变更**：返回类型从 `HospitalVO` 改为 `OrgVO`

---

## 五、医生管理（路由变更）**（改动）**

**路由变更**：`/basic/doctor/**` → `/system/doctor/**`

接口功能不变，仅路由前缀变更。

**请求体变更（所有涉及医生的接口）：**
- 删除字段：`hospitalDeptId`、`hospitalDeptName`（科室概念废弃）

---

## 六、医院分组模板（路由变更）**（改动）**

**路由变更**：`/basic/hospital-group-template/**` → `/system/hospital-group-template/**`

### 查询模板详情 GET /system/hospital-group-template/{id} **（改动）**
**响应体变更（明细列表）：**
- 新增字段：`assigned`（Boolean，该医疗机构是否已被至少一个用户关联）
- `hospitalId` 语义变更：从 `hospital.id` 改为 `sys_org.id`（医疗机构类型）

---

## 七、订单管理（/order）

### 创建订单/草稿 **（改动）**
**请求体变更：**
- 删除字段：`hospitalDeptId`、`hospitalDeptName`
- **新增校验**：`orderType` 受用户所属机构 `qualificationType` 限制（`qualificationType=2` 时 `orderType` 只能为 2）

### 订单列表/详情 **（改动）**
**响应体变更：**
- 删除字段：`hospitalDeptId`、`hospitalDeptName`
- `hospitalId` 语义变更：从 `hospital.id` 改为 `sys_org.id`

---

## 八、废弃接口

以下接口已废弃，不再提供：
- `GET/POST /basic/hospital/**`（医院管理模块已废弃，统一使用机构管理）
- `GET/POST /basic/hospital-dept/**`（科室管理模块已废弃）
