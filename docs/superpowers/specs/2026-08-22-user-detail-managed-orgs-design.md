# 账户详情额外管理机构对象返回设计

## 背景

区域管理员账户详情当前返回 `managedOrgIds`，前端只能获得额外管理机构 ID，无法直接回显机构名称。若前端再调用机构接口自行匹配，会增加请求和状态同步成本。

## 目标

在不破坏现有创建、编辑和详情调用方的前提下，让账户详情直接返回额外管理机构的 ID 与名称。

## 接口设计

账户详情接口保持现有路径和请求参数不变。`UserVO` 保留已有字段：

```json
{
  "managedOrgIds": [1001, 1002],
  "effectiveOrgIds": [1000, 1001, 1002]
}
```

新增只读响应字段 `managedOrgs`：

```json
{
  "managedOrgs": [
    { "id": 1001, "orgName": "华东经销商" },
    { "id": 1002, "orgName": "华南服务商" }
  ]
}
```

`managedOrgs` 的最终 API 字段名固定为 `managedOrgs`，元素使用专用类型
`ManagedOrgSimpleVO`，只包含：

- `id`：机构 ID。
- `orgName`：机构名称。

不直接复用完整 `OrgVO`，避免把机构编码、联系人、资质等无关字段耦合到账户详情契约。

## 返回规则

- `getUserById` 在角色判断前把 `managedOrgs` 初始化为 `[]`，区域管理员再用查询结果覆盖；因此详情接口对其他角色也明确返回 `[]`。
- 不在共享 `UserVO` 字段声明处设置默认值，避免账户列表、登录信息等其他使用 `UserVO` 的响应意外新增空字段。
- `managedOrgs` 对应额外管理机构，不包含主所属机构；主机构继续由 `orgId`、`orgName` 表示。
- 详情响应中的 `managedOrgIds` 与 `managedOrgs` 从同一份有效机构快照派生，二者严格一一对应且顺序一致。
- 历史关系中包含主机构时，同时从详情响应的 `managedOrgIds` 和 `managedOrgs` 排除；这只规范详情契约，不修改关系表，也不改变独立 `getManagedOrgIds` 方法的既有语义。
- 有效额外机构必须同时满足：机构存在、`isDeleted=0`、`status=1`，且类型为经销商 `1.2` 或服务商 `1.4`。无效关系不进入详情响应的 `managedOrgIds`、`managedOrgs` 或 `effectiveOrgIds`。
- 机构名称为 `null`、空串或纯空白时仍保留对象及原始 `orgName` 值；名称完整性属于机构主数据问题，不应导致合法机构权限从详情中消失。
- 无额外管理机构时返回 `[]`。

## 查询与代码边界

- 新增 `ManagedOrgSimpleVO { Long id; String orgName; }`，作为账户详情中额外管理机构的精简响应类型。
- 新增 `ManagedOrgScopeVO { List<Long> managedOrgIds; List<ManagedOrgSimpleVO> managedOrgs; List<Long> effectiveOrgIds; }`，作为一次详情查询内的统一快照。
- `UserManagedOrgService` 增加 `ManagedOrgScopeVO getManagedOrgScope(Long userId, Long primaryOrgId)`。该方法最多查询一次有效关系 ID、一次机构集合，并从结果统一派生三个字段；机构批量查询返回乱序时，按照关系 ID 顺序重建对象列表。
- `UserServiceImpl#getUserById` 已经持有用户及主机构 ID，区域管理员分支调用一次 `getManagedOrgScope`，同时填充 `managedOrgIds`、`managedOrgs` 和 `effectiveOrgIds`，不再为三个字段分别查询。
- 主机构有效时，`effectiveOrgIds = [primaryOrgId] + managedOrgIds`；主机构无效时沿用现有安全规则，`effectiveOrgIds=[]`，但仍返回有效的额外管理机构对象供管理员修正账户配置。
- 创建和编辑请求仍使用 `managedOrgIds: number[]`，不接受 `managedOrgs`，防止前端提交名称覆盖机构主数据。
- 本次只调整账户详情返回，不扩大到账户列表，避免列表页产生额外查询和无明确展示需求的数据负载。

## 兼容性

- 保留 `managedOrgIds`，现有前端无需立即修改。
- 新前端使用 `managedOrgs` 直接回显标签，提交时从对象中提取 ID 或继续使用原有 `managedOrgIds`。
- 数据库结构与历史数据无需迁移。

## 测试

- 区域管理员存在额外管理机构时，详情返回对应 `id + orgName` 对象，且 `managedOrgs[i].id == managedOrgIds[i]`。
- 无额外管理机构时返回空数组。
- 非区域管理员详情 JSON 中存在 `"managedOrgs":[]`，并且不调用管理机构快照查询。
- 机构批量查询结果乱序时，详情仍按关系顺序返回。
- 已删除、禁用、错误机构类型和不存在的机构同时从三个快照字段中过滤。
- 历史关系包含主机构时，同时从详情的 `managedOrgIds` 与 `managedOrgs` 排除。
- `orgName` 为 `null`、空串或纯空白时保留对象和原值。
- 一次区域管理员详情最多调用一次关系查询和一次机构集合查询。
- 账户列表不填充 `managedOrgs`，避免扩大既有列表响应。
- 原有创建、编辑请求中的 `managedOrgIds` 语义保持不变。
