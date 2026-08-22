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

`managedOrgs` 使用专用精简对象，只包含：

- `id`：机构 ID。
- `orgName`：机构名称。

不直接复用完整 `OrgVO`，避免把机构编码、联系人、资质等无关字段耦合到账户详情契约。

## 返回规则

- 仅区域管理员填充 `managedOrgs`；其他角色返回空数组，避免 `null` 判断。
- `managedOrgs` 对应额外管理机构，不包含主所属机构；主机构继续由 `orgId`、`orgName` 表示。
- 对象顺序与 `managedOrgIds` 的有效返回顺序保持一致。
- 关系指向的机构不存在时，不返回该对象；`managedOrgIds` 保持原有语义，不借此变更数据权限规则。
- 无额外管理机构时返回 `[]`。

## 查询与代码边界

- `UserManagedOrgService` 增加查询额外管理机构精简信息的方法，由服务层一次性批量读取机构，避免详情转换层逐个查询。
- `UserServiceImpl#getUserById` 的区域管理员分支同时填充 `managedOrgIds`、`managedOrgs` 和 `effectiveOrgIds`。
- 创建和编辑请求仍使用 `managedOrgIds: number[]`，不接受 `managedOrgs`，防止前端提交名称覆盖机构主数据。
- 本次只调整账户详情返回，不扩大到账户列表，避免列表页产生额外查询和无明确展示需求的数据负载。

## 兼容性

- 保留 `managedOrgIds`，现有前端无需立即修改。
- 新前端使用 `managedOrgs` 直接回显标签，提交时从对象中提取 ID 或继续使用原有 `managedOrgIds`。
- 数据库结构与历史数据无需迁移。

## 测试

- 区域管理员存在额外管理机构时，详情返回对应 `id + orgName` 对象。
- 无额外管理机构时返回空数组。
- 非区域管理员不查询额外管理机构详情，并返回空数组。
- 存在无效机构关系时，忽略无法加载的机构对象。
- 原有 `managedOrgIds`、`effectiveOrgIds` 返回行为保持不变。
