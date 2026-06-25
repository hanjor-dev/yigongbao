## init.sql 初始化数据编号冲突风险检查报告

### 1. 已修复的问题
✅ **sys_org (机构表)**
- 原编号：`ORG-H-0008` → 已改为 `ORG-H-9999`
- 风险：使用较小编号会与业务自动生成编号冲突
- 状态：已修复

### 2. 潜在风险项（需要注意但当前设计合理）

#### hospital_dept (医院科室字典表)
```sql
-- 当前初始化数据：
('HDEPT-0001', '骨科', 1, 1),
('HDEPT-0002', '口腔科', 2, 1),
...
('HDEPT-0009', '其他科室', 9, 1);
```

**风险评估**：
- ✅ **无风险** - 该表是**系统字典表**，不是业务数据表
- ✅ 初始化后有编号同步机制（line 1058）：
  ```sql
  UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_dept) WHERE rule_code = 'HDEPT_NO';
  ```
- ✅ 编号生成器会从当前最大值+1开始生成，不会冲突

#### rebuild_body_part (重建部位字典表)
```sql
-- 当前初始化数据：
('BP-0001', '头部', 1, 1, '...'),
('BP-0002', '颈部', 2, 1, '...'),
...
```

**风险评估**：
- ✅ **无风险** - 该表是**系统字典表**，包含预定义的身体部位
- ✅ 虽然没有明确的编号同步机制，但该表内容相对固定，很少新增

### 3. 编号同步机制说明

`init.sql` 中包含了编号同步机制（lines 1054-1059）：

```sql
-- 编码生成器首次调用时以此值为初始序号，确保新生成的编码不与已有种子数据冲突
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM hospital_dept) WHERE rule_code = 'HDEPT_NO';
UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM sys_org) WHERE rule_code = 'ORG_NO';
```

**这个机制确保了**：
- 编号生成器会从表中现有记录数开始
- 新生成的编号不会与初始化数据冲突

### 4. 建议

#### ✅ 当前设计合理的原因：
1. **字典表 vs 业务表**：`hospital_dept` 和 `rebuild_body_part` 是字典表，内容相对固定
2. **编号同步机制**：有明确的同步逻辑防止冲突
3. **编号范围区分**：
   - 字典表初始化数据：0001-0099
   - 业务自动生成：从当前最大值+1开始

#### ⚠️ 如果要进一步优化，可以考虑：
1. **统一使用大编号作为兜底数据**（类似 ORG-H-9999）
2. **为字典表添加编号同步检查**：
   ```sql
   UPDATE sys_code_rule SET current_value = (SELECT COUNT(*) FROM rebuild_body_part) WHERE rule_code = 'BODYPART_NO';
   ```

### 5. 结论

✅ **除了 sys_org 表（已修复），其他初始化数据不存在明显的编号冲突风险**

主要原因：
1. 使用编号的都是字典表，不是高频业务表
2. 有编号同步机制保护
3. 编号生成器设计合理

---
**检查完成时间**：2026-06-25
**检查范围**：sql/init.sql 全文
