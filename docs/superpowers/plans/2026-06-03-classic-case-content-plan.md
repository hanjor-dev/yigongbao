# 订单经典案例功能详细设计文档内容规划

## 规划目的

对已编写的技术设计文档进行审查，识别缺失或需要补充的内容，确保文档完整性和可实施性。

---

## 第一部分：现有文档结构评估

### 已完成章节
1. ✅ 需求概述
2. ✅ 数据库设计
3. ✅ 实体类设计
4. ✅ DTO/VO设计
5. ✅ Service层设计
6. ✅ Controller层设计
7. ✅ 文件迁移服务设计
8. ✅ 删除和修改保护设计
9. ✅ 错误码定义
10. ✅ 测试用例

### 识别的缺失内容
1. ❌ Service实现类的完整代码
2. ❌ 文件迁移的详细实现代码
3. ❌ Convert转换器设计
4. ❌ 数据库Mapper接口定义
5. ❌ 业务流程图
6. ❌ 时序图
7. ❌ 异常处理流程
8. ❌ 事务边界说明
9. ❌ 性能优化方案
10. ❌ 部署和配置说明

---

## 第二部分：需要补充的详细内容规划

### 2.1 Service实现类完整代码

**OrderClassicCaseServiceImpl**
- markAsClassicCase()完整实现
- listClassicCases()查询逻辑
- getClassicCaseDetail()详情查询
- isClassicCase()检查方法
- 日志记录规范
- 事务注解使用

**ClassicCaseFileServiceImpl**
- collectOrderFileIds()文件收集逻辑
- migrateFilesToClassicCase()迁移实现
- 批量处理策略
- 异常处理和回滚

### 2.2 Mapper接口定义

**OrderMainMapper扩展**
- 查询经典案例列表的自定义SQL
- 按条件筛选的动态SQL

**新增ClassicCaseMapper**
- 关联查询订单和经典案例信息
- 统计查询方法

### 2.3 Convert转换器设计

**ClassicCaseConvert**
- Entity转VO：toVO(OrderMainEntity)
- Entity列表转VO列表：toVOList(List<OrderMainEntity>)
- 使用BeanUtils.copyProperties实现

### 2.4 业务流程图

**标记为经典案例流程**
```
开始 → 验证订单状态 → 检查是否已标记 → 更新订单表 
→ 收集文件列表 → 迁移文件 → 更新文件路径 → 记录日志 → 结束
```

**删除保护流程**
```
删除请求 → 查询订单 → 检查is_classic_case → 是？抛出异常 : 执行删除
```

### 2.5 时序图

**标记经典案例时序图**
- Controller → Service → FileService → OSS
- 展示各层交互和事务边界

### 2.6 异常处理流程

**需要处理的异常场景**
- 订单不存在：抛出DATA_NOT_FOUND
- 订单未完成：抛出CLASSIC_CASE_ORDER_NOT_COMPLETED
- 重复标记：抛出CLASSIC_CASE_ALREADY_MARKED
- 文件迁移失败：回滚事务，抛出CLASSIC_CASE_FILE_MIGRATE_FAILED
- 尝试删除/修改：抛出CLASSIC_CASE_PROTECTED

### 2.7 事务边界说明

**事务范围**
- markAsClassicCase()方法使用@Transactional
- 包含：更新订单表 + 迁移文件 + 更新文件表
- 失败回滚策略：保留原文件，回滚数据库修改

**事务隔离级别**
- 使用默认隔离级别READ_COMMITTED
- 考虑并发标记场景

### 2.8 性能优化方案

**查询优化**
- 索引：idx_order_classic_case(is_classic_case, create_time DESC)
- 分页查询必须使用LIMIT
- 避免全表扫描

**文件迁移优化**
- 批量处理：每批50-100个文件
- 异步处理：使用@Async避免接口超时
- 进度反馈：提供查询接口

**缓存策略**
- 考虑经典案例列表Redis缓存
- TTL设置为1小时
- 标记时清除缓存

### 2.9 部署和配置说明

**数据库脚本执行顺序**
1. 执行ALTER TABLE添加字段
2. 执行CREATE INDEX创建索引
3. 验证字段和索引

**配置文件修改**
- 无需修改application.yml
- 文件存储配置保持不变

**权限配置**
- 标记经典案例需要管理员权限
- 增加权限码：order:classic-case:mark

---

## 第三部分：文档补充建议

### 3.1 优先级P0（必须补充）

1. **Service实现类完整代码** - 核心业务逻辑
2. **文件迁移服务完整代码** - 关键功能
3. **Convert转换器代码** - 数据转换
4. **删除保护方法完整代码** - 保护机制

### 3.2 优先级P1（建议补充）

1. **业务流程图** - 提升可读性
2. **事务边界说明** - 明确事务范围
3. **异常处理流程** - 完善错误处理

### 3.3 优先级P2（可选补充）

1. **时序图** - 辅助理解
2. **性能优化方案** - 后期优化参考
3. **部署配置说明** - 部署指导

---

## 第四部分：规划总结

### 当前文档完成度：60%

**已完成**：
- 基础设计（数据库、实体、DTO/VO）
- 接口定义（Service、Controller）
- 测试用例框架

**待补充**：
- 完整实现代码
- 业务流程图
- 详细的异常处理和事务说明

### 建议下一步行动

**选项1：补充完整代码**
- 优点：可以直接实施
- 工作量：4-5小时

**选项2：补充流程图+关键代码**
- 优点：平衡可读性和实施性
- 工作量：2-3小时

**选项3：保持现状，开始编码**
- 优点：快速进入开发
- 风险：可能遗漏细节

---

**规划文档完成**
