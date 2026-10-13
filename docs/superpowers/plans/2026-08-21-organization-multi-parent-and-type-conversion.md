# 组织多父级与机构类型转换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 扩展组织权限方案和演示原型，使其支持多父级、全部实际机构源数据，以及保留机构 ID 的经销商/服务商类型转换。

**Architecture:** 组织关系使用有向无环图，多条父级路径共同参与可见范围与操作权限计算；源数据提供类型、集合和实际机构三种建模方式。机构类型转换采用稳定机构主体、有效期历史和发布前影响预检，历史订单保留创建时快照，当前权限按新类型重算。

**Tech Stack:** Markdown、单文件 HTML/CSS/JavaScript、Node.js 静态语法校验。

---

### Task 1: 更新方案规则

**Files:**
- Modify: `docs/superpowers/specs/2026-08-21-organization-relation-data-permission-design.md`

- [x] 将单父级限制改为多父级 DAG，并定义多路径权限合并规则。
- [x] 增加“实际部门 + 全部实际机构”第三种源数据方案及适用边界。
- [x] 增加机构类型转换的数据、流程、影响预检、历史处理和审计设计。
- [x] 检查数据库草案、测试场景、风险和待确认项是否同步。

### Task 2: 更新交互原型

**Files:**
- Modify: `docs/prototypes/iam-relation-workbench.html`

- [x] 增加第三种方案按钮、机构搜索筛选和实际机构源数据。
- [x] 移除单父级阻断，保留重复连线和循环校验，并展示多个直接父级。
- [x] 在权限预览中展示多条命中路径与去重结果。
- [x] 增加机构类型转换入口、影响预检、冲突清单和确认转换演示。

### Task 3: 验证

**Files:**
- Test: `docs/prototypes/iam-relation-workbench.html`
- Test: `docs/superpowers/specs/2026-08-21-organization-relation-data-permission-design.md`

- [x] 使用 Node.js 解析嵌入式 JavaScript，期望无语法错误。
- [x] 检查三种源数据、多父级、循环阻断、路径合并和类型转换标记齐全。
- [x] 检查设计文档不存在“仅允许一个直接父级”等旧规则残留。
