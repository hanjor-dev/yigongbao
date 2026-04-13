# Order Column Config Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 `sql/init.sql` 中 `order.column.config` 配置值格式错误、字段不完整两个问题，使系统默认列配置真正生效。

**Architecture:** 只修改 `sql/init.sql` 一处，将旧的简单 `{field: boolean}` 映射格式替换为与 `OrderColumnConfigVO` 完全匹配的完整 JSON，包含 29 个可显示列（含操作列 `action`）。代码层无需任何改动。

**Tech Stack:** SQL（init data script）

---

### Task 1：替换 `init.sql` 中 `order.column.config` 的 config_value

**Files:**
- Modify: `sql/init.sql`（第 159 行，`order.column.config` 这一行）

**背景：** 当前值是旧格式 `{"id":true,"orderNo":true,...}`，`OrderQueryHelper.getSystemDefaultColumnConfig()` 用 Jackson 将其反序列化为 `OrderColumnConfigVO`，由于格式不匹配导致 `columns=null`，系统默认列配置完全失效。需替换为完整的结构化 JSON。

- [ ] **Step 1：定位目标行**

打开 `sql/init.sql`，找到以下这行（约第 159 行）：

```sql
('order.column.config', '订单列表默认列配置', '{"id":true,"orderNo":true,"hospitalName":true,"patientName":true,"statusName":true,"createTime":true}', 'string', 'system', '订单列表默认显示的列（JSON格式）', 1, 0, 10, 1),
```

- [ ] **Step 2：替换整行**

将上面那行替换为：

```sql
('order.column.config', '订单列表默认列配置', '{"module":"order","columns":[{"field":"orderCode","label":"订单编号","visible":true,"sort":1,"width":160,"fixed":null},{"field":"phaseName","label":"当前阶段","visible":true,"sort":2,"width":100,"fixed":null},{"field":"statusName","label":"当前状态","visible":true,"sort":3,"width":120,"fixed":null},{"field":"isUrgent","label":"加急","visible":true,"sort":4,"width":70,"fixed":null},{"field":"businessTypeName","label":"业务类型","visible":true,"sort":5,"width":90,"fixed":null},{"field":"orderTypeName","label":"订单类型","visible":true,"sort":6,"width":110,"fixed":null},{"field":"needsPhysicalDeliveryName","label":"实体交付","visible":true,"sort":7,"width":90,"fixed":null},{"field":"orgName","label":"提单机构","visible":true,"sort":8,"width":150,"fixed":null},{"field":"operatorName","label":"操作员","visible":true,"sort":9,"width":100,"fixed":null},{"field":"operatorPhone","label":"操作员电话","visible":true,"sort":10,"width":120,"fixed":null},{"field":"operatorDeptName","label":"所属部门","visible":true,"sort":11,"width":120,"fixed":null},{"field":"hospitalName","label":"医院","visible":true,"sort":12,"width":180,"fixed":null},{"field":"areaName","label":"地区","visible":true,"sort":13,"width":100,"fixed":null},{"field":"fullAreaName","label":"完整地区","visible":true,"sort":14,"width":160,"fixed":null},{"field":"hospitalDeptName","label":"科室","visible":true,"sort":15,"width":100,"fixed":null},{"field":"doctorName","label":"医生姓名","visible":true,"sort":16,"width":100,"fixed":null},{"field":"doctorPhone","label":"医生电话","visible":true,"sort":17,"width":120,"fixed":null},{"field":"patientName","label":"患者姓名","visible":true,"sort":18,"width":100,"fixed":null},{"field":"patientAge","label":"患者年龄","visible":true,"sort":19,"width":80,"fixed":null},{"field":"patientGenderName","label":"患者性别","visible":true,"sort":20,"width":80,"fixed":null},{"field":"isPostal","label":"是否邮寄","visible":true,"sort":21,"width":80,"fixed":null},{"field":"postalAddress","label":"邮寄地址","visible":true,"sort":22,"width":160,"fixed":null},{"field":"designerName","label":"设计师","visible":true,"sort":23,"width":100,"fixed":null},{"field":"expectedDeliveryDate","label":"期望交付时间","visible":true,"sort":24,"width":160,"fixed":null},{"field":"estimatedCost","label":"预估费用","visible":true,"sort":25,"width":100,"fixed":null},{"field":"dataEvaluationOpinion","label":"影像评估意见","visible":true,"sort":26,"width":160,"fixed":null},{"field":"rebuildProjectList","label":"重建项目","visible":true,"sort":27,"width":200,"fixed":null},{"field":"createTime","label":"创建时间","visible":true,"sort":28,"width":160,"fixed":null},{"field":"action","label":"操作","visible":true,"sort":29,"width":150,"fixed":null}]}', 'json', 'system', '订单列表默认显示的列（JSON格式，结构为 OrderColumnConfigVO）', 1, 0, 10, 1),
```

注意两处附带修改：
- `config_type` 从 `'string'` 改为 `'json'`（语义更准确）
- `config_desc` 从 `'订单列表默认显示的列（JSON格式）'` 改为 `'订单列表默认显示的列（JSON格式，结构为 OrderColumnConfigVO）'`

- [ ] **Step 3：验证 JSON 格式正确**

将新 `config_value` 中的 JSON 部分粘贴到任意 JSON 验证工具（如 https://jsonlint.com）确认无语法错误，或在本地运行：

```bash
echo '{"module":"order","columns":[...]}' | python3 -m json.tool
```

预期：输出格式化 JSON，无错误。

- [ ] **Step 4：commit**

```bash
git add sql/init.sql
git commit -m "fix(order): 修复 order.column.config 格式错误并补全全量字段配置"
```
