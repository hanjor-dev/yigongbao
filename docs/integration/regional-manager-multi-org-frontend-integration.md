# 区域管理员多机构数据权限——前端对接文档

> 版本：1.0  
> 日期：2026-08-22  
> 适用范围：账户管理、订单提单、订单列表/详情、区域管理员工作台  
> 后端对应提交：`1a98c499 feat: 支持区域管理员账户级多机构数据权限`

## 1. 对接结论

本次不新增“账户—机构权限配置”独立页面，直接改造现有账户创建/编辑页面。

区域管理员的数据权限模型为：

```text
有效机构 effectiveOrgIds = 主所属机构 orgId + 额外管理机构 managedOrgIds
```

- `orgId`：主所属机构，必填，只允许正常状态的经销商或服务商。
- `managedOrgIds`：额外管理机构，多选、选填，只允许正常状态的经销商或服务商。
- 区域管理员只能以 `orgId` 对应的主机构提单。
- 区域管理员可以查看主机构和全部额外管理机构下的订单。
- 提单时可选医院来自全部有效机构关联医院的并集。
- 医疗机构不能作为区域管理员的主机构或额外管理机构。
- 其他角色的页面字段和数据权限逻辑不变。

前端识别区域管理员时，应使用：

```ts
role.roleCode === 'regional-manager'
```

不要仅通过角色名称“区域管理员”判断。`dataScopeType === 'user_orgs'` 可作为辅助判断，但角色编码是本功能的业务身份标识。

## 2. 前端影响范围

| 页面/功能 | 是否必须修改 | 修改内容 |
|---|---:|---|
| 账户创建页/弹窗 | 是 | 区域管理员显示“主所属机构”和“额外管理机构”多选 |
| 账户编辑页/弹窗 | 是 | 回显 `managedOrgIds`，提交覆盖后的完整集合 |
| 账户详情页 | 是 | 展示主机构、额外管理机构、实际生效机构 |
| 账户列表页 | 建议 | 无需新增必显列；可增加“管理机构数”或详情入口 |
| 角色选择组件 | 是 | 通过 `roleCode` 控制区域管理员专属字段 |
| 订单创建/草稿页 | 是 | 区域管理员允许提单；医院下拉改为当前账户可操作医院接口 |
| 订单列表、详情、导出 | 原则上无需改接口 | 后端自动按有效机构集合过滤；前端需正确展示空列表和无权错误 |
| 区域管理员工作台 | 原则上无需改接口 | 后端统计范围已改为有效机构集合 |
| 其他角色账户表单 | 否 | 保持现有部门、机构、医院范围等逻辑 |

## 3. 账户创建/编辑页面

### 3.1 表单布局

当所选角色 `roleCode === 'regional-manager'` 时，显示以下字段：

| 字段 | 控件 | 必填 | 说明 |
|---|---|---:|---|
| 所属部门 `deptId` | 单选下拉 | 是 | 沿用现有业务部门选择逻辑 |
| 主所属机构 `orgId` | 单选下拉 | 是 | 只能选择当前部门关联的正常经销商/服务商 |
| 额外管理机构 `managedOrgIds` | 可搜索多选 | 否 | 可选择全公司正常经销商/服务商，不包含主机构 |

建议文案：

- 字段名：`额外管理机构`
- 占位提示：`请选择该区域管理员可额外管理的经销商或服务商（选填）`
- 辅助说明：`账户可查看主所属机构及额外管理机构下的全部订单；提单仍归属主所属机构。`

### 3.2 主所属机构候选数据

先选择部门，再查询该部门关联机构：

```http
GET /system/dept/{deptId}/orgs
```

前端保留：

```ts
['1.2', '1.4'].includes(org.orgType)
```

机构类型约定：

| `orgType` | 含义 | 是否允许 |
|---|---|---:|
| `1.2` | 经销商 | 是 |
| `1.4` | 服务商 | 是 |
| `1.3` | 医疗机构 | 否 |
| 其他 | 生产企业等 | 否 |

`GET /system/dept/{deptId}/orgs` 返回的简要机构对象：

```json
{
  "id": 1001,
  "orgName": "华东经销商甲",
  "orgCode": "D-EAST-001",
  "orgType": "1.2"
}
```

说明：该接口返回对象当前没有 `status` 字段，最终合法性由保存接口再次校验。若候选中存在已停用机构，保存时会返回错误；后续可再单独增强该接口的状态字段。

### 3.3 额外管理机构候选数据

沿用现有机构全量下拉接口：

```http
GET /system/org/all
```

前端过滤规则：

```ts
const managedOrgOptions = allOrgs.filter(org =>
  org.status === 1 &&
  ['1.2', '1.4'].includes(org.orgType) &&
  org.id !== form.orgId
)
```

多选建议支持：

- 按机构名称、机构编码搜索。
- 标签中显示机构类型，例如“华东经销商甲（经销商）”。
- 已选项去重。
- 禁止选择主机构。
- 选项较多时启用虚拟滚动或远程搜索，避免一次渲染数百个 DOM 节点。

当前后端也提供分页接口：

```http
POST /system/org/list
Content-Type: application/json

{
  "pageNum": 1,
  "pageSize": 50,
  "orgName": "华东",
  "orgType": "1.2",
  "status": 1
}
```

但 `orgType` 当前只支持单值。若采用远程分页搜索，经销商 `1.2` 和服务商 `1.4` 需要分别请求后合并；第一版建议沿用 `/system/org/all` 并在前端过滤。

### 3.4 表单联动规则

#### 选择区域管理员角色

1. 显示 `managedOrgIds` 多选。
2. 主机构候选允许经销商和服务商。
3. `accountType` 应为业务账户 `6.2`，并与角色返回的 `accountType` 保持一致。

#### 区域管理员切换为其他角色

1. 隐藏额外管理机构字段。
2. 清空前端本地 `managedOrgIds`。
3. 更新请求中不要提交非空 `managedOrgIds`；建议省略该字段。
4. 后端在角色切换成功后会自动清理历史额外机构关系。

#### 修改部门

1. 清空主机构 `orgId`。
2. 重新调用 `/system/dept/{deptId}/orgs`。
3. `managedOrgIds` 可保留，因为额外管理机构不要求属于主部门；但仍需排除新主机构。

#### 修改主机构

1. 从 `managedOrgIds` 中自动删除新的主机构 ID。
2. 重新校验主机构是否属于当前部门。
3. 编辑提交时建议始终发送当前完整 `managedOrgIds`，避免依赖“未传表示保持不变”的增量语义。

## 4. 接口对接

所有响应沿用统一格式：

```ts
interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
  priority?: number | null
}
```

`code === 200` 表示成功。失败时优先向用户展示后端 `message`。

### 4.1 获取角色列表

```http
GET /system/role/all
```

前端需要使用的角色字段：

```ts
interface RoleVO {
  id: number
  roleName: string
  roleCode: string
  accountType: '6.1' | '6.2'
  dataScopeType: string
  status: 0 | 1
}
```

区域管理员示例：

```json
{
  "id": 15,
  "roleName": "区域管理员",
  "roleCode": "regional-manager",
  "accountType": "6.2",
  "dataScopeType": "user_orgs",
  "status": 1
}
```

### 4.2 创建区域管理员账户

```http
POST /system/user
Content-Type: application/json
```

在原 `CreateUserDTO` 基础上新增：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `managedOrgIds` | `number[]` | 否 | 额外管理机构 ID；不包含主机构 |

完整请求示例：

```json
{
  "realName": "张三",
  "phone": "13800000001",
  "email": "zhangsan@example.com",
  "accountType": "6.2",
  "deptId": 201,
  "orgId": 1001,
  "roleId": 15,
  "managedOrgIds": [1002, 1003],
  "employeeNo": "YX-001",
  "chargingTemplateId": 8,
  "remark": "华东区域管理员"
}
```

没有额外管理机构时可传：

```json
"managedOrgIds": []
```

创建成功：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1787370000000
}
```

后端处理规则：

- 自动去除 `null`、重复 ID 和与 `orgId` 相同的 ID。
- 非区域管理员提交非空 `managedOrgIds` 会拒绝。
- 主机构或额外机构不是正常经销商/服务商时会拒绝。
- 主机构不属于所选部门时会拒绝。

### 4.3 获取账户详情与编辑回显

```http
GET /system/user/{userId}
```

区域管理员返回新增字段：

| 字段 | 类型 | 含义 |
|---|---|---|
| `managedOrgIds` | `number[]` | 额外管理机构，不含主机构 |
| `effectiveOrgIds` | `number[]` | 实际生效机构，包含主机构和有效额外机构 |

示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 501,
    "realName": "张三",
    "accountType": "6.2",
    "deptId": 201,
    "deptName": "市场部",
    "orgId": 1001,
    "orgName": "华东经销商甲",
    "roleId": 15,
    "roleName": "区域管理员",
    "roleCode": "regional-manager",
    "dataScopeType": "user_orgs",
    "managedOrgIds": [1002, 1003],
    "effectiveOrgIds": [1001, 1002, 1003]
  },
  "timestamp": 1787370000000
}
```

回显规则：

- 编辑多选值使用 `managedOrgIds`，不要使用 `effectiveOrgIds`。
- `effectiveOrgIds` 只用于只读展示和问题排查。
- 机构名称不随 `managedOrgIds` 返回。前端应从 `/system/org/all` 建立 `id -> OrgVO` 映射后显示名称。
- 非区域管理员可能不返回这两个字段，前端按空数组兼容。

### 4.4 修改区域管理员账户

```http
PUT /system/user/{userId}
Content-Type: application/json
```

注意：`UpdateUserDTO.accountType` 当前为必填字段，编辑提交时必须保留发送。

建议前端按完整表单提交：

```json
{
  "realName": "张三",
  "phone": "13800000001",
  "accountType": "6.2",
  "deptId": 201,
  "orgId": 1001,
  "roleId": 15,
  "managedOrgIds": [1002, 1004],
  "chargingTemplateId": 8,
  "remark": "调整管理范围"
}
```

`managedOrgIds` 在修改接口中的语义必须严格区分：

| 提交值 | 后端语义 |
|---|---|
| 字段省略或 `null` | 本次不修改原额外机构集合 |
| `[]` | 清空全部额外机构，只保留主机构权限 |
| `[1002, 1004]` | 用该数组全量覆盖原额外机构集合 |

前端账户编辑页面属于完整表单，推荐总是发送数组：有选择时发送完整 ID 数组，无选择时发送 `[]`。

### 4.5 获取当前登录用户可提单医院

订单创建页和草稿页统一调用：

```http
GET /system/hospital-scope/my-hospitals
```

不要由前端根据 `managedOrgIds` 自行查机构关联医院并求并集，也不要从本地账户详情推算。后端会实时根据当前账户有效机构集合返回可操作医院。

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 3001,
      "orgName": "第一人民医院",
      "orgCode": "H-001",
      "orgType": "1.3",
      "orgTypeName": "医疗机构",
      "status": 1
    },
    {
      "id": 3002,
      "orgName": "中心医院",
      "orgCode": "H-002",
      "orgType": "1.3",
      "orgTypeName": "医疗机构",
      "status": 1
    }
  ],
  "timestamp": 1787370000000
}
```

管理员查看指定账户医院范围时可使用：

```http
GET /system/hospital-scope/my-hospitals/{userId}
```

订单页面不要使用带 `userId` 的接口代替当前登录用户接口。

## 5. 订单页面修改

### 5.1 提单入口

若前端当前只允许业务员显示“新建订单”，需将区域管理员加入允许提单角色：

```ts
const canCreateOrder = [
  'salesman',
  'salesman-self',
  'regional-manager'
].includes(currentUser.roleCode)
```

实际角色编码列表应在现有权限组件基础上追加 `regional-manager`，不要替换原有判断。

### 5.2 提单机构

区域管理员提单时：

- `orgId` 固定为当前登录账户的主机构 `currentUser.orgId`。
- `orgName` 显示当前登录账户主机构名称。
- 不提供额外管理机构切换器。
- 不允许以 `managedOrgIds` 中的机构名义提单。

直提订单接口仍为：

```http
POST /order
```

草稿保存接口仍为：

```http
POST /order/draft
```

两类请求中的 `orgId` 都必须等于当前登录账户的主机构 ID。后端会再次校验，传入额外管理机构 ID 将返回 `612 没有权限执行该操作`。

### 5.3 医院下拉

页面进入、重新打开草稿或用户主动刷新时，重新请求：

```http
GET /system/hospital-scope/my-hospitals
```

不要长期缓存医院列表。管理员调整账户额外管理机构、机构停用或医院关联关系变化后，后端结果会实时变化。

提交订单、修改医院、草稿转正式订单时，后端都会再次验证医院权限。若原草稿医院已不在当前范围内：

- 前端应标记原选择已失效。
- 要求用户重新选择医院后再提交。
- 后端可能返回 `710 无权操作该医院的订单`。

### 5.4 订单列表与详情

订单列表、详情和工作台接口参数无需增加机构数组。前端不能把 `effectiveOrgIds` 作为查询参数传给后端。

后端根据当前登录用户实时过滤：

```text
order.orgId IN currentUser.effectiveOrgIds
```

当管理员移除某个额外管理机构后：

- 该机构订单会在下一次查询时立即从列表消失。
- 已打开的订单详情再次请求时可能返回“订单不存在”或无权限提示。
- 历史订单本身不会被修改或转移机构。

前端在收到无权/不存在响应时，应关闭失效详情页并刷新列表，不要继续使用页面缓存数据。

## 6. TypeScript 类型与表单示例

```ts
type OrgType = '1.2' | '1.3' | '1.4' | string

interface OrgOption {
  id: number
  orgName: string
  orgCode: string
  orgType: OrgType
  orgTypeName?: string
  status?: 0 | 1
}

interface UserForm {
  realName: string
  phone: string
  accountType: '6.1' | '6.2'
  deptId?: number
  orgId?: number
  roleId?: number
  managedOrgIds: number[]
  chargingTemplateId?: number
  remark?: string
}

interface UserDetail {
  id: number
  roleId: number
  roleCode: string
  dataScopeType: string
  deptId?: number
  orgId: number
  orgName: string
  managedOrgIds?: number[]
  effectiveOrgIds?: number[]
}

const isRegionalManager = (role?: RoleVO) =>
  role?.roleCode === 'regional-manager'

function normalizeRegionalManagerForm(form: UserForm, role?: RoleVO) {
  if (!isRegionalManager(role)) {
    form.managedOrgIds = []
    return
  }

  form.managedOrgIds = [...new Set(form.managedOrgIds)]
    .filter(Boolean)
    .filter(id => id !== form.orgId)
}
```

编辑加载示例：

```ts
async function loadUserForEdit(userId: number) {
  const [userResult, roleResult, orgResult] = await Promise.all([
    api.get<ApiResult<UserDetail>>(`/system/user/${userId}`),
    api.get<ApiResult<RoleVO[]>>('/system/role/all'),
    api.get<ApiResult<OrgOption[]>>('/system/org/all')
  ])

  const user = userResult.data.data
  form.managedOrgIds = user.managedOrgIds ?? []
  // orgResult 用于把 ID 映射为机构名称。
}
```

## 7. 错误处理

| 场景 | 典型响应 | 前端处理建议 |
|---|---|---|
| 未选主机构 | `410 缺少参数：主所属机构` | 定位并提示主机构字段 |
| 主机构不是正常经销商/服务商 | `410 参数无效：区域管理员主机构只能选择正常的经销商或服务商` | 刷新机构选项并要求重选 |
| 额外机构包含非法/停用机构 | `410 参数无效：额外管理机构只能选择正常的经销商或服务商` | 刷新候选列表，保留仍有效选项 |
| 非区域管理员提交非空额外机构 | `410 参数无效：仅区域管理员可配置额外管理机构` | 检查角色切换时是否清空字段 |
| 主机构不属于部门 | `623 所选机构不属于该部门` | 清空主机构并重新加载部门关联机构 |
| 以额外机构提单 | `612 没有权限执行该操作` | 将订单 `orgId` 重置为当前账户主机构 |
| 医院权限已失效 | `710 无权操作该医院的订单` | 刷新医院列表并要求重新选择 |

不要只依赖 HTTP 状态码；项目业务错误通过统一响应体的 `code` 和 `message` 返回。

## 8. 数据变更后的页面行为

### 8.1 管理员调整区域管理员的机构集合

- 保存成功后，关闭编辑弹窗并重新查询账户详情/列表。
- 如果被修改的是当前登录账户，下一次订单、医院、工作台请求即按新范围生效，无需前端维护权限缓存。
- 已加载的订单列表和医院下拉不会自动更新，页面应在重新进入、刷新或关键操作后重新请求。

### 8.2 机构停用、删除或类型变化

- 无效额外机构会被后端实时排除出 `effectiveOrgIds`。
- 主机构失效时，区域管理员的有效机构集合为空，订单和医院数据将不可访问，需管理员先修复账户主机构。
- 账户编辑页若发现 `managedOrgIds` 中有 ID 无法在候选机构中找到，应显示“已失效机构”标记，不要静默替换为其他机构；保存时仅提交用户确认后的有效集合。

### 8.3 历史数据

- 修改机构权限只影响后续可见性，不修改历史订单的 `orgId`。
- 重新获得某机构权限后，该机构历史订单会重新可见。
- 前端无需执行历史数据迁移或重新绑定。

## 9. 无需修改或禁止实现的内容

- 不新增独立的“区域管理员机构授权”页面。
- 不修改其他角色的 `hospitalIds`、部门权限或机构权限表单逻辑。
- 不允许医疗机构进入 `managedOrgIds`。
- 不允许区域管理员选择额外机构作为提单机构。
- 不在订单分页请求中传 `managedOrgIds` 或 `effectiveOrgIds`。
- 不根据机构下业务员列表自行计算订单权限。
- 不在浏览器长期缓存有效机构集合或医院集合。
- 不要求前端初始化现有区域管理员的额外机构；由管理员逐个编辑账户配置。

## 10. 联调验收清单

### 10.1 账户创建

- [ ] 选择区域管理员角色后显示额外管理机构多选。
- [ ] 主机构只显示当前部门关联的经销商、服务商。
- [ ] 额外机构只显示正常经销商、服务商。
- [ ] 主机构不会出现在额外机构候选和提交数组中。
- [ ] 不选择额外机构也能成功创建。
- [ ] 选择两家额外机构后，详情正确返回 `managedOrgIds` 和 `effectiveOrgIds`。

### 10.2 账户编辑

- [ ] 编辑时使用 `managedOrgIds` 正确回显，不把主机构重复选中。
- [ ] 提交 `[]` 后额外机构被全部清空，主机构仍在 `effectiveOrgIds`。
- [ ] 修改主机构后自动从额外集合排除新主机构。
- [ ] 区域管理员切换为其他角色后，额外机构字段隐藏且历史关系清理。
- [ ] 其他角色提交时不会携带非空 `managedOrgIds`。

### 10.3 提单与医院

- [ ] 区域管理员能进入创建订单和草稿页面。
- [ ] 提单机构固定为账户主机构，不能切换为额外管理机构。
- [ ] 医院下拉包含主机构和全部额外机构关联医院的并集。
- [ ] 多家机构关联同一医院时，下拉只出现一次。
- [ ] 移除额外机构后刷新页面，该机构独有医院不再可选。
- [ ] 原草稿医院权限失效后，提交被拦截并提示重新选择。

### 10.4 订单数据权限

- [ ] 区域管理员能看到主机构订单。
- [ ] 能看到全部额外管理机构订单。
- [ ] 看不到未授权机构订单。
- [ ] 移除某机构权限后，该机构订单从列表、详情和工作台同步消失。
- [ ] 重新授权后，该机构历史订单重新可见。
- [ ] 其他角色的订单范围与改造前一致。

## 11. 前后端职责边界

前端负责：

- 按角色显示和隐藏字段。
- 提供正确的候选机构并做好表单联动。
- 正确区分 `managedOrgIds` 的空数组和未提交语义。
- 订单页从后端医院范围接口取数。
- 展示后端错误并刷新失效页面数据。

后端负责：

- 校验区域管理员、部门、主机构和额外机构合法性。
- 保存账户额外管理机构集合。
- 计算 `effectiveOrgIds`。
- 按有效机构过滤订单、详情、操作权限和工作台统计。
- 计算可提单医院并集并在提交时再次鉴权。
- 保证区域管理员只能以主机构提单。
