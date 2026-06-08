# 收费模板功能实施计划

**创建时间**：2026-06-08  
**作者**：Kiro  
**模块**：yigongbao-module-basic  
**关联需求**：基于重建项目的收费模板管理功能

---

## ⚠️ 实施前必读

### 强制要求

**在开始任何代码编写前，必须完整阅读以下文档**：

📖 **项目编码规范**：`.docs/技术实现/java-coding-standards.mdc`

### 关键编码规范强调

实施过程中必须严格遵守以下规范，违反将导致代码审查不通过：

#### 1. 注释规范
- **类注释**：所有类必须添加功能说明、作者、创建时间
- **方法注释**：ServiceImpl 中所有公共方法必须添加 Javadoc 注释（功能、参数、返回值、异常）
- **行内注释**：ServiceImpl 中关键业务逻辑必须添加行内注释说明意图

#### 2. 日志规范
- **Controller 层禁止记录日志**：所有日志由 ServiceImpl 记录
- **ServiceImpl 必须记录日志**：
  - 方法入参（注意脱敏）
  - 关键业务节点
  - 异常信息（包含完整堆栈）
  - 成功/失败标识
- 使用 `log.info/warn/error`，禁止 `System.out.println`

#### 3. 魔法值规范
- **禁止使用数字魔法值**：状态值必须使用 `StatusConstants.NORMAL/DISABLED`
- **禁止使用字符串魔法值**：编码规则必须使用 `CodeRuleConstants` 常量
- **枚举优先**：业务状态使用枚举类型

#### 4. 异常处理规范
- **优先使用 ErrorCodeEnum**：`throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND)`
- **Controller 禁止 try-catch**：异常由 GlobalExceptionHandler 统一处理
- **ServiceImpl 异常传递**：业务异常直接抛出，系统异常记录日志后抛出

#### 5. 数据库规范
- **逻辑删除与唯一索引**：凡是同时有 `is_deleted` 字段和唯一约束的表，必须使用函数索引
- **索引命名必须带表名前缀**：`idx_charging_template_status` 而非 `idx_status`
- **禁止手动定义 is_deleted 索引**：MyBatis-Plus 会自动创建

#### 6. 测试规范
- **ServiceImpl 单元测试**：必须覆盖所有业务方法的成功/失败场景
- **Controller 接口测试**：必须使用 MockMvc 验证参数绑定、校验、响应格式
- **反射注入 baseMapper**：ServiceImpl 测试必须在 @BeforeEach 中注入
- **test profile 配置**：禁用 SaToken 拦截器，使用 H2 内存库

---

## 一、功能概述

### 1.1 业务背景

在医工宝系统中，不同的业务账号需要使用不同的收费标准。为了灵活管理各个重建项目的收费价格，需要引入"收费模板"功能：

- 收费模板是重建项目价格的集合
- 每个模板包含所有重建项目对应的收费价格
- 创建"业务"类型账号时必须绑定一个收费模板
- 模板创建后，重建项目可能新增或删除，模板需要展示差异信息

### 1.2 核心功能

1. **收费模板 CRUD**：创建、更新、删除、查询收费模板
2. **模板明细管理**：批量维护重建项目的收费价格
3. **名称唯一性校验**：模板名称不能重复
4. **差异检测**：查询时自动计算模板与当前重建项目列表的差异
5. **账号绑定**：业务账号创建时绑定收费模板

### 1.3 关键设计决策

#### 决策 1：删除重建项目时，模板明细如何处理？

**决策**：保留模板明细记录，不物理删除

**原因**：
- 历史数据审计：直接删除会导致无法追溯历史订单使用的费用标准
- 合规要求：保留完整的费用变更历史
- 实现方式：查询时 JOIN rebuild_project 表判断项目是否已删除（is_deleted=1）

**优势**：
- 单一数据源，项目恢复时模板自动生效
- 无需在明细表冗余 project_status 字段

#### 决策 2：差异检测机制

**实时计算方案**：查询模板详情时动态计算差异

- **缺失项目（missing）**：当前活跃项目中存在，但模板明细中没有录入
- **失效项目（obsolete）**：模板明细中存在，但重建项目已删除

**计算公式**：
```
missingCount = 当前活跃项目数 - 模板已录入的活跃项目数
obsoleteCount = 模板中关联的已删除项目数
```

---

## 二、数据库设计

### 2.1 收费模板主表（charging_template）

```sql
DROP TABLE IF EXISTS charging_template;
CREATE TABLE charging_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_name   VARCHAR(100)    NOT NULL COMMENT '模板名称',
    remark          VARCHAR(512)    DEFAULT NULL COMMENT '备注说明',
    status          TINYINT         DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    
    -- 通用字段
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by       BIGINT          DEFAULT NULL COMMENT '更新人ID',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    
    PRIMARY KEY (id),
    KEY idx_charging_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收费模板表';

-- 函数唯一索引（支持逻辑删除）
CREATE UNIQUE INDEX uk_charging_template_name
    ON charging_template ((CASE WHEN is_deleted = 0 THEN template_name ELSE NULL END));
```

**设计要点**：
- 使用函数索引确保未删除记录的名称唯一性
- status 字段使用 StatusConstants.NORMAL/DISABLED
- 索引名带表名前缀 `idx_charging_template_`

### 2.2 收费模板明细表（charging_template_item）

```sql
DROP TABLE IF EXISTS charging_template_item;
CREATE TABLE charging_template_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id         BIGINT          NOT NULL COMMENT '模板ID（关联charging_template表）',
    rebuild_project_id  BIGINT          NOT NULL COMMENT '重建项目ID（关联rebuild_project表）',
    price               DECIMAL(10,2)   NOT NULL COMMENT '收费价格（元）',
    
    -- 通用字段
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT          DEFAULT NULL COMMENT '创建人ID',
    update_by           BIGINT          DEFAULT NULL COMMENT '更新人ID',
    
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_project (template_id, rebuild_project_id),
    KEY idx_charging_template_item_template_id (template_id),
    KEY idx_charging_template_item_project_id (rebuild_project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收费模板明细表';
```

**设计要点**：
- **不使用逻辑删除**：明细表无 is_deleted 字段
- 联合唯一索引确保同一模板下项目不重复
- 通过 JOIN rebuild_project 判断项目是否失效

### 2.3 用户表扩展

```sql
ALTER TABLE sys_user
ADD COLUMN charging_template_id BIGINT DEFAULT NULL COMMENT '收费模板ID（业务账户必填，关联charging_template表）';

ALTER TABLE sys_user 
ADD KEY idx_user_template_id (charging_template_id);
```

---

## 三、实施步骤

### 阶段 P0：核心 CRUD（必须完成）

**目标**：实现收费模板的基础增删改查功能

#### 任务 1：数据库设计与初始化
- 在 `sql/ddl.sql` 中添加表结构定义
- 在 `sql/init.sql` 中添加测试数据（可选）
- 在 `sys_user` 表中添加 `charging_template_id` 字段

#### 任务 2：Entity 层
创建实体类：
- `ChargingTemplateEntity`：继承 BaseEntity
- `ChargingTemplateItemEntity`：**不继承 BaseEntity**（无 is_deleted）

**关键点**：
- 使用 `@TableName` 指定表名
- 使用 `@TableField` 映射字段
- ChargingTemplateEntity 继承 BaseEntity 自动获得公共字段
- ChargingTemplateItemEntity 手动定义所有字段（不继承 BaseEntity）

#### 任务 3：DTO/VO 层
创建数据传输对象：
- `CreateChargingTemplateDTO`：创建模板（包含明细列表）
- `UpdateChargingTemplateDTO`：更新模板（包含明细列表）
- `ChargingTemplateVO`：模板视图对象
- `ChargingTemplateDetailVO`：模板详情（含差异统计）
- `ChargingTemplateItemDTO`：明细 DTO
- `ChargingTemplateItemVO`：明细 VO（含 isObsolete 标记）

**校验规则**：
```java
// CreateChargingTemplateDTO
@NotBlank(message = "模板名称不能为空")
@Size(max = 100, message = "模板名称长度不能超过100")
private String templateName;

@NotNull(message = "模板明细不能为空")
@Size(min = 1, message = "至少包含一个收费项目")
private List<ChargingTemplateItemDTO> items;

// ChargingTemplateItemDTO
@NotNull(message = "重建项目ID不能为空")
private Long rebuildProjectId;

@NotNull(message = "收费价格不能为空")
@DecimalMin(value = "0.01", message = "收费价格必须大于0")
@DecimalMax(value = "9999999.99", message = "收费价格不能超过9999999.99")
private BigDecimal price;
```

#### 任务 4：Mapper 层
创建 Mapper 接口：
- `ChargingTemplateMapper extends BaseMapper<ChargingTemplateEntity>`
- `ChargingTemplateItemMapper extends BaseMapper<ChargingTemplateItemEntity>`

**关键点**：
- 禁止编写 XML SQL
- 所有数据库操作使用 MyBatis-Plus 代码方式

#### 任务 5：Service 层

**接口定义**：
```java
public interface ChargingTemplateService extends IService<ChargingTemplateEntity> {
    
    /**
     * 分页查询收费模板列表
     */
    IPage<ChargingTemplateVO> listPage(Integer pageNum, Integer pageSize, String templateName);
    
    /**
     * 根据ID查询模板详情（含差异统计）
     */
    ChargingTemplateDetailVO getDetailById(Long id);
    
    /**
     * 创建收费模板
     */
    Long create(CreateChargingTemplateDTO dto);
    
    /**
     * 更新收费模板
     */
    void update(Long id, UpdateChargingTemplateDTO dto);
    
    /**
     * 删除收费模板
     */
    void remove(Long id);
}
```

**ServiceImpl 实现关键点**：

1. **创建模板**：
   - 校验模板名称唯一性
   - 使用 `@Transactional(rollbackFor = Exception.class)`
   - 先插入主表，再批量插入明细表
   - 记录详细日志

2. **更新模板**：
   - 校验模板是否存在
   - 校验名称唯一性（排除当前ID）
   - 事务中：更新主表 + 删除旧明细 + 插入新明细
   - 使用 `LambdaQueryWrapper<ChargingTemplateItemEntity>().eq(ChargingTemplateItemEntity::getTemplateId, id)`删除

3. **删除模板**：
   - 校验模板是否被账号引用
   - 逻辑删除主表（MyBatis-Plus 自动处理）
   - **明细表物理删除**（无 is_deleted 字段）
   - 事务控制

4. **查询详情（含差异统计）**：
```java
@Override
public ChargingTemplateDetailVO getDetailById(Long id) {
    // 查询模板主表
    ChargingTemplateEntity template = getById(id);
    if (template == null) {
        throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
    }
    
    // 查询模板明细
    List<ChargingTemplateItemEntity> items = itemMapper.selectList(
        new LambdaQueryWrapper<ChargingTemplateItemEntity>()
            .eq(ChargingTemplateItemEntity::getTemplateId, id)
    );
    
    // 查询所有活跃的重建项目
    List<RebuildProjectEntity> activeProjects = rebuildProjectMapper.selectList(
        new LambdaQueryWrapper<RebuildProjectEntity>()
            .eq(RebuildProjectEntity::getStatus, StatusConstants.NORMAL)
            .eq(RebuildProjectEntity::getIsDeleted, StatusConstants.NOT_DELETED)
    );
    
    // 计算差异
    Set<Long> activeProjectIds = activeProjects.stream()
        .map(RebuildProjectEntity::getId)
        .collect(Collectors.toSet());
    
    Set<Long> templateProjectIds = items.stream()
        .map(ChargingTemplateItemEntity::getRebuildProjectId)
        .collect(Collectors.toSet());
    
    // 缺失项目：活跃项目中存在但模板中没有
    int missingCount = (int) activeProjectIds.stream()
        .filter(id -> !templateProjectIds.contains(id))
        .count();
    
    // 失效项目：模板中存在但项目已删除
    int obsoleteCount = 0;
    for (ChargingTemplateItemEntity item : items) {
        if (!activeProjectIds.contains(item.getRebuildProjectId())) {
            obsoleteCount++;
        }
    }
    
    // 构造返回 VO
    ChargingTemplateDetailVO vo = new ChargingTemplateDetailVO();
    BeanUtil.copyProperties(template, vo);
    vo.setMissingCount(missingCount);
    vo.setObsoleteCount(obsoleteCount);
    vo.setTotalActiveProjects(activeProjects.size());
    vo.setItems(convertToItemVOs(items, activeProjectIds));
    
    return vo;
}
```

#### 任务 6：Controller 层

```java
@RestController
@RequestMapping("/api/basic/charging-template")
@RequiredArgsConstructor
public class ChargingTemplateController {

    private final ChargingTemplateService chargingTemplateService;

    /**
     * 分页查询收费模板列表
     */
    @PostMapping("/list")
    public Result<IPage<ChargingTemplateVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName) {
        IPage<ChargingTemplateVO> page = chargingTemplateService.listPage(pageNum, pageSize, templateName);
        return Result.success(page);
    }

    /**
     * 根据ID查询模板详情
     */
    @GetMapping("/{id}")
    public Result<ChargingTemplateDetailVO> getById(@PathVariable Long id) {
        return Result.success(chargingTemplateService.getDetailById(id));
    }

    /**
     * 创建收费模板
     */
    @PostMapping
    public Result<Long> create(@Validated @RequestBody CreateChargingTemplateDTO dto) {
        Long id = chargingTemplateService.create(dto);
        return Result.success(id);
    }

    /**
     * 更新收费模板
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, 
                               @Validated @RequestBody UpdateChargingTemplateDTO dto) {
        chargingTemplateService.update(id, dto);
        return Result.success();
    }

    /**
     * 删除收费模板
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chargingTemplateService.remove(id);
        return Result.success();
    }
}
```

**关键点**：
- Controller 禁止记录日志
- 使用 `@Validated` 触发参数校验
- 禁止 try-catch，异常由 GlobalExceptionHandler 处理
- 返回统一的 `Result` 格式

---

### 阶段 P1：差异计算优化（重要）

**目标**：优化差异计算逻辑，提供更详细的差异信息

#### 任务 7：详细差异信息
在 `ChargingTemplateDetailVO` 中增加字段：
```java
/**
 * 缺失项目列表（未录入模板的活跃项目）
 */
private List<MissingProjectVO> missingProjects;

/**
 * 失效项目列表（模板中关联的已删除项目）
 */
private List<ObsoleteProjectVO> obsoleteProjects;
```

---

### 阶段 P2：账号绑定校验（必须完成）

**目标**：创建业务账号时绑定收费模板，并进行校验

#### 任务 8：用户 Service 扩展

修改 `UserServiceImpl.createUser` 方法：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void createUser(CreateUserDTO dto) {
    // 校验业务账号必须绑定收费模板
    if ("6.2".equals(dto.getAccountType())) {
        if (dto.getChargingTemplateId() == null) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "收费模板");
        }
        
        // 校验模板是否存在且状态正常
        ChargingTemplateEntity template = chargingTemplateMapper.selectById(dto.getChargingTemplateId());
        if (template == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        if (template.getStatus() != StatusConstants.NORMAL) {
            throw new BusinessException(400, "收费模板已禁用，无法绑定");
        }
    }
    
    // ... 其他逻辑
}
```

---

### 阶段 P3：测试用例（必须完成）

#### 任务 9：ServiceImpl 单元测试

创建 `ChargingTemplateServiceImplTest`：

**必须覆盖的场景**：
1. `create_shouldSuccess`：创建成功
2. `create_whenDuplicateName_shouldThrowException`：名称重复
3. `update_shouldSuccess`：更新成功
4. `update_whenNotExists_shouldThrowException`：模板不存在
5. `remove_shouldSuccess`：删除成功
6. `remove_whenReferencedByUser_shouldThrowException`：被账号引用
7. `getDetailById_shouldReturnWithDiffStats`：查询详情含差异统计
8. `listPage_shouldReturnPagedData`：分页查询

**关键点**：
- 使用 `@ExtendWith(MockitoExtension.class)`
- 反射注入 baseMapper
- Mock 参数使用 `any()` 而非 `any(具体类.class)`
- 验证 Mock 调用次数

#### 任务 10：Controller 接口测试

创建 `ChargingTemplateControllerTest`：

**必须覆盖的场景**：
1. `list_shouldReturnPagedData`：分页查询成功
2. `getById_shouldReturnDetail`：查询详情成功
3. `getById_whenNotExists_shouldReturn404`：数据不存在
4. `create_shouldSuccess`：创建成功
5. `create_whenValidationFailed_shouldReturn400`：参数校验失败
6. `update_shouldSuccess`：更新成功
7. `delete_shouldSuccess`：删除成功

**关键点**：
- 使用 `@SpringBootTest` + `@AutoConfigureMockMvc`
- 配置 `@ActiveProfiles("test")`
- 禁用 SaToken 拦截器（application-test.yml）
- 使用 H2 内存库
- 断言 `code`、`message`、`data`

---

## 四、关键技术要点

### 4.1 事务控制

所有修改操作必须添加事务注解：
```java
@Transactional(rollbackFor = Exception.class)
public void create(CreateChargingTemplateDTO dto) {
    // ...
}
```

### 4.2 BeanUtils.copyProperties 使用

排除嵌套集合字段：
```java
ChargingTemplateEntity entity = new ChargingTemplateEntity();
BeanUtils.copyProperties(dto, entity, "items");  // 排除 items 字段
```

### 4.3 批量插入优化

使用 MyBatis-Plus 的 `saveBatch` 方法：
```java
// 批量插入明细
List<ChargingTemplateItemEntity> itemEntities = dto.getItems().stream()
    .map(itemDTO -> {
        ChargingTemplateItemEntity item = new ChargingTemplateItemEntity();
        BeanUtil.copyProperties(itemDTO, item);
        item.setTemplateId(templateId);
        return item;
    })
    .collect(Collectors.toList());

itemService.saveBatch(itemEntities);
```

### 4.4 流式计算差异

使用 Stream API 计算集合差异：
```java
Set<Long> missingIds = activeProjectIds.stream()
    .filter(id -> !templateProjectIds.contains(id))
    .collect(Collectors.toSet());
```

---

## 五、验收标准

### 5.1 功能验收

- [ ] 可以创建收费模板，包含多个重建项目的价格
- [ ] 可以更新模板信息和明细价格
- [ ] 可以删除模板（校验是否被账号引用）
- [ ] 查询模板详情时显示差异统计（缺失、失效）
- [ ] 模板名称唯一性校验生效
- [ ] 创建业务账号时必须绑定收费模板
- [ ] 绑定的模板必须存在且状态正常

### 5.2 代码质量验收

- [ ] 所有类和方法有完整注释
- [ ] ServiceImpl 关键位置有日志记录
- [ ] Controller 层无日志输出
- [ ] 无魔法值，使用常量或枚举
- [ ] 异常使用 ErrorCodeEnum
- [ ] 单元测试覆盖率 > 80%
- [ ] 接口测试覆盖所有 Controller 方法
- [ ] 代码通过编译，无警告

### 5.3 数据库验收

- [ ] 表结构符合规范
- [ ] 使用函数唯一索引支持逻辑删除
- [ ] 索引名带表名前缀
- [ ] 外键关联正确
- [ ] 测试数据可正常插入

---

## 六、风险与注意事项

### 6.1 数据一致性风险

**风险**：删除重建项目后，模板明细中仍保留该项目

**应对**：
- 明细表不使用逻辑删除
- 查询时 JOIN rebuild_project 判断项目状态
- 展示差异信息提示用户

### 6.2 性能风险

**风险**：差异计算涉及多表 JOIN 和集合运算

**应对**：
- 仅在详情查询时计算差异，列表查询不计算
- 使用 Stream API 优化集合运算
- 后续可考虑缓存活跃项目列表

### 6.3 并发风险

**风险**：同时创建同名模板可能绕过唯一性校验

**应对**：
- 数据库层面使用函数唯一索引强制约束
- Service 层先查询再插入（乐观策略）

---

## 七、后续优化方向

### 7.1 模板复制功能

支持基于现有模板快速创建新模板

### 7.2 模板版本管理

记录模板的历史变更，支持版本回溯

### 7.3 批量调价

支持按比例或固定金额批量调整模板价格

### 7.4 模板对比

支持两个模板的价格差异对比

---

## 八、实施检查清单

实施前：
- [ ] 已完整阅读 `.docs/技术实现/java-coding-standards.mdc`
- [ ] 了解项目分层架构和命名规范
- [ ] 了解 BaseEntity 公共字段
- [ ] 了解逻辑删除与函数索引规范

实施中：
- [ ] 每完成一个任务，自测通过
- [ ] 提交前运行 `mvn clean compile test`
- [ ] 代码格式符合规范（4空格缩进）
- [ ] 所有警告已处理

实施后：
- [ ] 所有单元测试通过
- [ ] 所有接口测试通过
- [ ] 功能验收通过
- [ ] 代码审查通过

---

**计划文档版本**：1.0  
**预计实施工时**：8-12 小时  
**优先级**：P0（核心功能）

