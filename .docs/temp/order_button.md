# 订单管理按钮权限清单

## 一、文档概述

本文档整理订单管理模块中所有按钮的功能名称与功能编码，供权限配置使用。

功能编码格式：`模块名:操作名`，模块名统一为 `order`，操作名取自按钮动作。

---

## 二、按钮清单

### 2.1 订单管理主入口（order.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 我的草稿 | `order:TabDraft` | Tab 栏 | 切换至"我的草稿" |
| 订单列表 | `order:TabOrderList` | Tab 栏 | 切换至"订单列表" |
| 审核列表 | `order:TabModifyAudit` | Tab 栏 | 切换至"修改审核" |
| 我的修改申请 | `order:TabMyModifyApply` | Tab 栏 | 切换至"我的修改申请" |

### 2.2 订单列表（orderList.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 新建订单 | `order:Add` | 筛选栏（订单列表 Tab） | 打开新建订单弹窗 |
| 批量导出 | `order:BatchExport` | 订单列表顶部工具栏 | 批量导出选中的订单 |
| 查看详情 | `order:View` | 订单列表操作列下拉 | 查看订单详情 |
| 修改 | `order:Modify` | 订单列表操作列下拉 | 执行已通过审核的修改申请（14.1/14.2/14.3） |
| 修改历史 | `order:ModifyHistory` | 订单列表操作列下拉 | 查看订单修改记录 |
| 影像调阅 | `order:ImageView` | 订单列表操作列下拉  | 打开外部的影像调阅链接 | 

### 2.3 草稿列表（draft.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 编辑草稿 | `draft:Edit` | 草稿列表操作列 | 打开草稿编辑弹窗 |
| 查看草稿 | `draft:View` | 草稿列表操作列 | 查看草稿详情 |
| 删除草稿 | `draft:Delete` | 草稿列表操作列 | 删除指定草稿 |



### 2.6 订单详情（orderDetailDialog.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 申请修改 | `order:ApplyModify` | 订单详情底部 | 从详情页发起修改申请 |
| 审核通过 | `order:Approve` | 订单详情底部  | 从详情页审核 |
| 审核驳回 | `order:Reject` | 订单详情底部 | 从详情页审核  |

### 2.7 修改审核（dataAudit.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 查看 | `order:AuditView` | 修改审核列表操作列 | 查看申请详情并执行审核 |

### 2.8 修改申请详情 / 审核弹窗（modifyApplyAuditDialog.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 审核通过 | `order:Approve` | 修改申请详情底部 | 审核通过修改申请 |
| 审核驳回 | `order:Reject` | 修改申请详情底部 | 驳回修改申请（需填写驳回原因） |

### 2.9 我的修改申请（modifyApply.vue）

| 功能名称 | 功能编码 | 所在位置 | 说明 |
|----------|----------|----------|------|
| 查看 | `order:MyApplyView` | 我的修改申请列表操作列 | 查看我发起的修改申请详情 |
| 撤回 | `order:MyApplyWithdraw` | 我的修改申请列表操作列 | 撤回待审核的修改申请 |



## 三、权限编码速查表

| 编码 | 功能名称 | 所属页面 |
|------|----------|----------|
| `order:TabDraft` | 我的草稿 | 订单管理主入口 |
| `draft:Edit` | 编辑草稿 | 草稿列表 |
| `draft:View` | 查看草稿 | 草稿列表 |
| `draft:Delete` | 删除草稿 | 草稿列表 |

| `order:TabOrderList` | 订单列表 | 订单管理主入口 |
| `order:Add` | 新建订单 | 订单列表 |
| `order:BatchExport` | 批量导出 | 订单列表 |
| `order:View` | 查看详情 | 订单列表 |
| `order:ImageView` | 影像调阅 | 订单列表 | 
| `order:Modify` | 修改 | 订单列表 |
| `order:ModifyHistory` | 修改历史 | 订单列表 |
| `order:ApplyModify` | 申请修改 | 订单详情 |
| `order:Approve` | 审核通过 | 订单详情 |
| `order:Reject` | 审核驳回 | 订单详情 |

| `order:TabModifyAudit` | 待审核列表 | 订单管理主入口 |
| `audit:View` | 查看详情 | 待审核列表 |
| `audit:Approve` | 审核通过 | 审核详情 |
| `audit:Reject` | 审核驳回 | 审核详情 |

| `order:TabMyModifyApply` | 我的修改申请 | 订单管理主入口 |
| `apply:MyApplyView` | 查看 | 我的修改申请 |
| `apply:MyApplyWithdraw` | 撤回 | 我的修改申请 |




