# Findings & Decisions - 医工宝订单模块详细设计方案

> 更新日期：2026-03-31
> 版本：v2.1
> 目标：整合所有补充需求后的详细方案设计

---

## 零、版本记录

| 版本 | 日期 | 修改内容 | 作者 |
|-----|------|---------|------|
| v1.0 | 2026-03-31 | 初始版本：订单状态机设计方案 | hanjor |
| v2.0 | 2026-03-31 | 整合所有补充需求：草稿独立表设计、35+字段分析、10分钟权限、字典化、文件关联 | hanjor |
| v2.1 | 2026-03-31 | 修正字典化方案：使用 dict_code 格式（如 11.1）替代字符串值；更新 file_category 为 dict_code 格式（如 10.1） | hanjor |
| **v2.3** | **2026-03-31** | **新增多模块架构策略：枚举迁移到common、设计/生产阶段独立模块、OpenFeign跨模块调用方案、循环依赖规避策略** | **hanjor** |
| **v2.2** | **2026-03-31** | **修正修改申请表（移除JSON字段，按类型粒度设计）；FileBizTypeEnum复用FileBizTypeEnum；新增order.image.required系统配置；表名改为order（移除_main后缀）；project_estimated_hours改为DECIMAL(8,2)** | **hanjor** |

---

|| **v2.3** | **2026-03-31** | **新增多模块架构策略：枚举迁移到common、设计/生产阶段独立模块、OpenFeign跨模块调用方案、循环依赖规避策略** | **hanjor** |
|| **v2.4** | **2026-03-31** | **代码库现状调研：CodeRuleConstants已含ORDER_NO/ORDER_ITEM_NO；FileBizTypeEnum已含10.1/10.2/10.3；ErrorCodeEnum已定义到674；DictCodeConstants只定义到5；SystemConfigKeyEnum只有6个配置键** | **hanjor** |

---

## 零点五、代码库现状（v2.4 新增）

> 调研时间：2026-03-31
> 通过读取现有代码文件，确认以下资源已存在或不存在。

### 0.5.1 CodeRuleConstants（已包含订单相关）

| 常量 | 值 | 状态 |
|------|------|------|
| ORDER_NO | "ORDER_NO" | ✅ 已存在 |
| ORDER_ITEM_NO | "ORDER_ITEM_NO" | ✅ 已存在 |
| DATA_PACKAGE_NO | "DATA_PACKAGE_NO" | ✅ 已存在 |
| INSTRUCTION_NO | "INSTRUCTION_NO" | ✅ 已存在 |

**结论**：无需重复定义订单编码常量。

### 0.5.2 FileBizTypeEnum（已包含订单相关文件类别）

| 枚举 | dictCode | 状态 |
|------|----------|------|
| IMAGE_DATA | "10.1" | ✅ 已存在 |
| IMAGE_REPORT | "10.2" | ✅ 已存在 |
| ORDER_ATTACHMENT | "10.3" | ✅ 已存在 |
| PRINT_PACKAGE | "10.4" | ✅ 已存在 |
| DESIGN_REPORT | "10.5" | ✅ 已存在 |
| VISUAL_MODEL | "10.6" | ✅ 已存在 |
| DRAWING_FILE | "10.7" | ✅ 已存在 |
| INSTRUCTION_FILE | "10.8" | ✅ 已存在 |

**结论**：
1. 无需创建 `OrderFileCategoryEnum`，直接复用 `FileBizTypeEnum`
2. 草稿提交校验时，使用 `FileBizTypeEnum.IMAGE_DATA.getDictCode()` 获取 "10.1"
3. 文件上传时 bizType 传入枚举 code（如 "image_data"），dictCode（"10.1"）存储到 file_detail.object_type

### 0.5.3 ErrorCodeEnum（当前最大值为 674）

```java
// 当前定义到 674
DOCTOR_EXISTS(674, "医生编码已存在");
```

**结论**：新增 675-701 段位给订单相关错误码，无冲突。

### 0.5.4 DictCodeConstants（当前只定义到 5）

| 常量 | 值 | 状态 |
|------|------|------|
| ORG_TYPE | "1" | ✅ 已存在 |
| AGENT_PRODUCT_LINE | "5" | ✅ 已存在 |

**结论**：新增以下常量：

```java
// ==================== 订单业务类型 ====================
public static final String ORDER_BUSINESS_TYPE = "11";
public static final String ORDER_BUSINESS_TYPE_BUSINESS = "11.1";
public static final String ORDER_BUSINESS_TYPE_TEST = "11.2";
public static final String ORDER_BUSINESS_TYPE_TRIAL = "11.3";
public static final String ORDER_BUSINESS_TYPE_AGENT = "11.4";

// ==================== 患者性别 ====================
public static final String PATIENT_GENDER = "12";
public static final String PATIENT_GENDER_MALE = "12.1";
public static final String PATIENT_GENDER_FEMALE = "12.2";
```

### 0.5.5 SystemConfigKeyEnum（当前只有 6 个）

| 配置键 | 说明 |
|--------|------|
| DEFAULT_PASSWORD | 默认密码 |
| LOGIN_MAX_FAILURES | 最大登录失败次数 |
| LOGIN_LOCK_DURATION | 登录锁定时长 |
| SMS_SEND_INTERVAL | 短信发送间隔 |
| SYSTEM_NAME | 系统名称 |
| MAX_UPLOAD_SIZE | 文件上传最大大小 |

**结论**：新增以下配置键：

```java
// ==================== 订单配置 ====================
ORDER_IMAGE_REQUIRED("order.image.required", "提交订单是否必须上传影像文件"),
ORDER_DRAFT_EXPIRE_DAYS("order.draft.expire.days", "草稿自动过期天数"),
ORDER_MODIFY_WINDOW_MINUTES("order.modify.window.minutes", "订单提交后修改窗口期（分钟）");
```

### 0.5.6 common 层文件结构

```
yigongbao-common/src/main/java/com/yigongbao/common/
├── enums/
│   ├── ErrorCodeEnum.java        ✅
│   ├── FileBizTypeEnum.java      ✅
│   ├── SystemConfigKeyEnum.java  ✅
│   ├── CodeResetTypeEnum.java    ✅
│   ├── OperationTypeEnum.java     ✅
│   ├── YesNoEnum.java            ✅
│   └── ResourceTypeEnum.java     ✅
├── constant/
│   ├── CodeRuleConstants.java    ✅
│   └── DictCodeConstants.java    ✅
└── ...
```

**需要新建**：
- `enums/order/OrderTypeEnum.java`
- `enums/order/OrderPhaseEnum.java`
- `enums/order/OrderStatusEnum.java`
- `enums/order/OrderActionEnum.java`
- `enums/order/OrderBusinessTypeEnum.java`
- `enums/order/OrderPatientGenderEnum.java`
- `rules/PhaseTransitionRule.java`
- `rules/OrderPhaseTransitionRules.java`
- `rules/OrderStatusTransitionRules.java`

---

## 一、需求补充分析与决策

### 1.1 草稿功能详细设计

**需求回顾**：
1. 草稿保存后仅自己的账号可查看
2. 不分配和占用业务订单流水号（订单编号）
3. 草稿状态的订单信息可以直接多次修改直到提交
4. 草稿30天自动过期删除
5. 需要有草稿数据相关的增删改查接口能力

#### 方案对比

| 方案 | 描述 | 优点 | 缺点 | 决策 |
|------|------|------|------|------|
| **方案A** | 复用 order_main 表，phase=1, status=10 为草稿 | 与状态机设计一致，无需新建表 | 查询时需额外过滤条件；草稿大量数据影响 order_main 表性能 | 放弃 |
| **方案B** | 新建 order_draft 草稿表，提交后转为正式订单 | 草稿与正式订单完全隔离；order_main 表更精简；权限控制简单 | 需维护数据转换逻辑 | **采用** |

#### 方案B详细设计

**核心思路**：草稿数据独立存储，提交时整体转换

```
┌──────────────────────────────────────────────────────────────┐
│                      草稿阶段                                │
│                                                              │
│  order_draft         order_item_draft      file_detail      │
│  ├── 基础信息         ├── 重建项目明细      ├── 影像数据    │
│  ├── 机构信息         └── ...              ├── 影像报告    │
│  ├── 医生/患者信息                            └── ...         │
│  └── ...                                                    │
│                                                              │
│  订单编号：无（null）                                        │
│  有效期：30天（expires_at）                                  │
└──────────────────────────────────────────────────────────────┘
                            ↓ 提交订单
                            ↓
┌──────────────────────────────────────────────────────────────┐
│                      正式订单阶段                            │
│                                                              │
│  order_main          order_item           file_detail        │
│  ├── 基础信息         ├── 重建项目明细      └── 重新关联     │
│  ├── 机构信息                                                      │
│  ├── 医生/患者信息                                              │
│  └── ...                                                     │
│                                                              │
│  订单编号：ORD-20260331-0001                                │
│  phase=1, status=10 (DRAFT)                                │
└──────────────────────────────────────────────────────────────┘
```

**order_draft 表设计（草稿表）**：

```sql
CREATE TABLE `order_draft` (
    -- ==================== 主键与冗余字段 ====================
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作员ID（创建人）',

    -- ==================== 订单类型（固定不变） ====================
    `order_type` TINYINT NOT NULL COMMENT '1-医疗器械，2-非医疗器械，3-服务',
    `business_type` VARCHAR(20) NOT NULL COMMENT '业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）',

    -- ==================== 机构信息 ====================
    `org_id` BIGINT NOT NULL COMMENT '提单机构ID',
    `org_name` VARCHAR(200) COMMENT '提单机构名称',
    `operator_name` VARCHAR(100) COMMENT '操作员姓名',
    `operator_phone` VARCHAR(20) COMMENT '操作员电话',

    -- ==================== 医院与科室 ====================
    `hospital_id` BIGINT COMMENT '医院ID',
    `hospital_name` VARCHAR(200) COMMENT '医院名称',
    `dept_id` BIGINT COMMENT '科室ID',
    `dept_name` VARCHAR(100) COMMENT '科室名称',

    -- ==================== 医生/患者信息 ====================
    `doctor_id` BIGINT COMMENT '医生ID',
    `doctor_name` VARCHAR(100) COMMENT '医生姓名',
    `doctor_phone` VARCHAR(20) COMMENT '医生电话',
    `patient_name` VARCHAR(100) COMMENT '患者姓名',
    `patient_age` INT COMMENT '患者年龄',
    `patient_gender` VARCHAR(20) COMMENT '患者性别（字典 dict_code：12.1-男，12.2-女）',

    -- ==================== 业务信息 ====================
    `is_urgent` TINYINT DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    `is_postal` TINYINT DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    `postal_address` TEXT COMMENT '邮寄地址',

    -- ==================== 时效信息 ====================
    `expected_delivery_date` DATETIME COMMENT '期望交付时间',

    -- ==================== 有效期管理 ====================
    `expires_at` DATETIME NOT NULL COMMENT '过期时间（创建时间+30天）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-有效，2-已提交，3-已过期',

    -- ==================== 公共字段（BaseEntity） ====================
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT,
    `update_by` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_order_draft_operator_id` (`operator_id`),
    KEY `idx_order_draft_status` (`status`),
    KEY `idx_order_draft_expires_at` (`expires_at`),
    KEY `idx_order_draft_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单草稿表';
```

**设计要点**：
1. **无订单编号字段**：`order_code` 不存在于草稿表，提交时由 `CodeGeneratorService` 生成
2. **无 phase/status 字段**：草稿阶段不需要状态机，提交后转入正式订单
3. **expires_at 字段**：用于30天过期管理
4. **operator_id 字段**：索引字段，用于查询"我的草稿"
5. **status 字段**：区分有效/已提交/已过期，便于清理

**order_item_draft 表设计（草稿明细表）**：

```sql
CREATE TABLE `order_item_draft` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `draft_id` BIGINT NOT NULL COMMENT '草稿ID',

    -- ==================== 重建项目信息 ====================
    `body_part_id` BIGINT COMMENT '部位ID',
    `body_part_name` VARCHAR(100) COMMENT '部位名称',
    `project_id` BIGINT COMMENT '重建项目ID',
    `project_name` VARCHAR(200) COMMENT '重建项目名称',
    `project_estimated_hours` INT COMMENT '预计耗时（工作日）',

    -- ==================== 用户填写内容 ====================
    `project_desc` TEXT COMMENT '项目说明',
    `forming_requirement` TEXT COMMENT '成形需求',
    `other_requirement` TEXT COMMENT '其他要求',

    -- ==================== 序号 ====================
    `sort_order` INT DEFAULT 1 COMMENT '排序序号',

    -- ==================== 公共字段 ====================
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT,
    `update_by` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_draft_id` (`draft_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单草稿明细表';
```

**文件关联设计**：

| 阶段 | objectType | objectId | 说明 |
|------|-----------|----------|------|
| 草稿阶段 | `order_draft` | order_draft.id | 影像文件关联到草稿 |
| 正式订单 | `order_main` | order_main.id | 提交后文件重新关联 |

**文件类别（file_category）**：

| 类别码 | 类别名 | 说明 | 必填 |
|--------|--------|------|------|
| 1 | IMAGE_DATA | 影像数据（CT/MRI等） | **提交时必填** |
| 2 | IMAGE_REPORT | 影像报告 | **提交时必填** |
| 3 | PRINT_FILE | 打印文件包 | 设计阶段上传 |
| 4 | DESIGN_REPORT | 设计报告 | 设计阶段上传 |
| 5 | VISUAL_MODEL | 可视化模型 | 设计阶段上传 |
| 6 | DRAWING | 图纸 | 生产阶段上传 |
| 7 | OTHER | 其他附件 | 可选 |

**30天过期清理实现**：

1. **方案一：定时任务（推荐）**

```java
@Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
public void cleanupExpiredDrafts() {
    log.info("开始清理过期草稿订单");
    LocalDateTime now = LocalDateTime.now();
    // 1. 删除过期草稿的明细
    LambdaQueryWrapper<OrderItemDraftEntity> itemWrapper = new LambdaQueryWrapper<>();
    itemWrapper.inSql(OrderItemDraftEntity::getDraftId,
        "SELECT id FROM order_draft WHERE status = 3 AND expires_at < NOW()");
    orderItemDraftMapper.delete(itemWrapper);
    // 2. 删除过期草稿（文件由 file_detail 的逻辑删除处理）
    LambdaQueryWrapper<OrderDraftEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(OrderDraftEntity::getStatus, 3)
           .lt(OrderDraftEntity::getExpiresAt, now);
    int count = orderDraftMapper.delete(wrapper);
    log.info("清理过期草稿订单完成，删除数量={}", count);
}
```

2. **方案二：懒清理（简化实现）**

在查询草稿列表时，自动清理过期草稿：

```java
public IPage<OrderDraftVO> listDrafts(Integer pageNum, Integer pageSize) {
    // 查询前先清理过期草稿
    cleanupExpiredDrafts();
    // 执行查询...
}
```

---

### 1.2 订单主表（order_main）字段设计分析

**需求回顾**：
1. 订单主表改名为 `order_main` 是否更清晰？
2. business_type 字段使用字典编码值，而非写死的字符串值
3. patient_gender 使用字典
4. 分析35+字段的合理性

#### 35+字段设计合理性分析

**字段分组**：

| 分组 | 字段数 | 说明 | 是否必填 |
|------|--------|------|---------|
| 基础信息 | 3 | order_code, order_type, business_type | 必填 |
| 机构信息 | 7 | org/orgName/operator*/hospital*/dept* | 必填 |
| 医生/患者 | 6 | doctor*/patient* | 必填 |
| 业务信息 | 3 | isUrgent/isPostal/postalAddress | 条件必填 |
| 时效信息 | 6 | delivery/designStart/designSubmit/confirm/actualCompleteTime | 可选 |
| 阶段+状态 | 2 | phase, status | 必填 |
| 处理人 | 4 | currentHandler*/designer*/producer* | 可选 |
| 审核信息 | 3 | audit*/designReview*/version | 可选 |
| **合计** | **34** | | |

**结论**：34个字段在 MySQL 中完全合理，单行数据量预估：

| 字段类型 | 预估大小 |
|----------|---------|
| VARCHAR(200) × 10 | ~2KB |
| VARCHAR(100) × 8 | ~800B |
| DATETIME × 5 | ~20B |
| BIGINT × 15 | ~120B |
| TINYINT × 8 | ~8B |
| TEXT × 5 | ~200B（平均） |
| **总计** | **~3.2KB/行** |

即使10万条订单，总存储约 320MB，完全可行。

#### 订单主表设计（order_main）

```sql
CREATE TABLE `order_main` (
    -- ==================== 主键与编码 ====================
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',

    -- ==================== 订单类型 ====================
    `order_type` TINYINT NOT NULL COMMENT '1-医疗器械，2-非医疗器械，3-服务',
    `business_type` VARCHAR(20) NOT NULL COMMENT '业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）',

    -- ==================== 机构信息 ====================
    `org_id` BIGINT NOT NULL COMMENT '提单机构ID',
    `org_name` VARCHAR(200) COMMENT '提单机构名称（冗余）',
    `operator_id` BIGINT COMMENT '操作员ID（创建人）',
    `operator_name` VARCHAR(100) COMMENT '操作员姓名（冗余）',
    `operator_phone` VARCHAR(20) COMMENT '操作员电话',

    -- ==================== 医院与科室 ====================
    `hospital_id` BIGINT COMMENT '医院ID',
    `hospital_name` VARCHAR(200) COMMENT '医院名称（冗余）',
    `dept_id` BIGINT COMMENT '科室ID',
    `dept_name` VARCHAR(100) COMMENT '科室名称（冗余）',

    -- ==================== 医生/患者信息 ====================
    `doctor_id` BIGINT COMMENT '医生ID',
    `doctor_name` VARCHAR(100) COMMENT '医生姓名',
    `doctor_phone` VARCHAR(20) COMMENT '医生电话',
    `patient_name` VARCHAR(100) COMMENT '患者姓名',
    `patient_age` INT COMMENT '患者年龄',
    `patient_gender` VARCHAR(20) COMMENT '患者性别（字典 dict_code：12.1-男，12.2-女）',

    -- ==================== 业务信息 ====================
    `is_urgent` TINYINT DEFAULT 0 COMMENT '是否加急：0-否，1-是',
    `is_postal` TINYINT DEFAULT 0 COMMENT '是否邮寄：0-否，1-是',
    `postal_address` TEXT COMMENT '邮寄地址',

    -- ==================== 时效信息 ====================
    `expected_delivery_date` DATETIME COMMENT '期望交付时间',
    `design_start_time` DATETIME COMMENT '设计开始时间',
    `design_submit_time` DATETIME COMMENT '设计提交时间',
    `user_confirm_time` DATETIME COMMENT '用户确认时间（服务订单）',
    `actual_complete_time` DATETIME COMMENT '实际完成时间',

    -- ==================== 【核心】阶段 + 状态 ====================
    `phase` TINYINT NOT NULL DEFAULT 1 COMMENT '当前阶段：1-订单，2-设计，...',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '当前状态',

    -- ==================== 当前处理人 ====================
    `current_handler_id` BIGINT COMMENT '当前处理人ID',
    `current_handler_name` VARCHAR(100) COMMENT '当前处理人姓名',
    `designer_id` BIGINT COMMENT '设计师ID',
    `producer_id` BIGINT COMMENT '生产员ID',

    -- ==================== 审核信息 ====================
    `audit_remark` TEXT COMMENT '审核备注（驳回原因等）',
    `design_review_remark` TEXT COMMENT '设计审核备注',

    -- ==================== 乐观锁 ====================
    `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',

    -- ==================== 公共字段（BaseEntity） ====================
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT,
    `update_by` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_main_code` (`order_code`),
    KEY `idx_order_main_org_id` (`org_id`),
    KEY `idx_order_main_hospital_id` (`hospital_id`),
    KEY `idx_order_main_operator_id` (`operator_id`),
    KEY `idx_order_main_phase_status` (`phase`, `status`),
    KEY `idx_order_main_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';
```

**设计要点**：
1. **表名改为 order_main**：与 order_draft 对应，更清晰
2. **business_type 使用字典值**：business/test/trial/agent
3. **patient_gender 使用字典值**：male/female（替代 TINYINT 0/1）
4. **version 字段**：乐观锁，处理并发更新
5. **索引前缀**：所有索引使用 `idx_order_main_` 前缀，避免与 order_draft 等表冲突

---

### 1.3 10分钟修改权限与修改申请流程

**需求回顾**：
1. 提单的业务人员和具有其数据范围权限的区域管理人员具有提交后10分钟内可修改订单信息的能力
2. 如果涉及到修改重建项目或者影像文件的，需要提交修改申请
3. 相关管理员同意该申请后方可修改订单对应信息
4. 公司管理员角色的用户可以直接修改订单全部信息（需要记录留痕）

#### 权限模型

| 角色 | 10分钟内修改 | 修改重建项目/影像 | 修改申请 | 直接修改全部 |
|------|------------|-------------------|---------|-------------|
| 业务员（提交人） | ✓ | ✓（需申请） | ✓（发起人） | ✗ |
| 区域管理员 | ✓ | ✓（需申请） | ✓（可发起） | ✗ |
| 公司管理员 | ✗ | ✗ | ✗ | ✓（记录留痕） |

#### 10分钟修改判断逻辑

```java
/**
 * 判断订单是否在提交后10分钟内
 */
public boolean isWithinModifyWindow(OrderMainEntity order) {
    if (order == null) return false;
    // 提交时间 + 10分钟 > 当前时间
    return order.getCreateTime()
        .plusMinutes(10)
        .isAfter(LocalDateTime.now());
}
```

#### 修改申请流程

```
┌─────────────────┐
│  业务员发起申请  │
│  选择修改字段    │
│  填写修改原因    │
└────────┬────────┘
         ↓
┌─────────────────┐
│  提交申请       │
│  申请状态=PENDING│
└────────┬────────┘
         ↓
┌─────────────────┐
│  管理员审核     │
│  同意/拒绝      │
└────────┬────────┘
         ↓
    ┌───┴───┐
    ↓       ↓
  同意    拒绝
    ↓       ↓
┌─────────┐ ┌─────────┐
│ 业务员  │ │ 申请结束│
│ 修改订单│ │
│ 留痕记录│ │
└─────────┘ └─────────┘
```

**order_modify_apply 表设计**：

> **设计说明**：修改申请只区分修改类型（info/image/item），不需要精确到具体字段。

```sql
CREATE TABLE `order_modify_apply` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',

    -- ==================== 申请信息 ====================
    `apply_type` VARCHAR(20) NOT NULL COMMENT '申请类型：info-基础信息，image-影像文件，item-重建项目',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-待审核，approved-已同意，rejected-已拒绝',
    `apply_reason` TEXT COMMENT '申请原因',
    `reject_reason` TEXT COMMENT '驳回原因',

    -- ==================== 操作人 ====================
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(100) COMMENT '申请人姓名',
    `auditor_id` BIGINT COMMENT '审核人ID',
    `auditor_name` VARCHAR(100) COMMENT '审核人姓名',
    `audit_time` DATETIME COMMENT '审核时间',

    -- ==================== 公共字段 ====================
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT,
    `update_by` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息修改申请表';
```

**apply_type 枚举**：

| 值 | 说明 | 涉及修改内容 |
|----|------|-------------|
| `info` | 基础信息修改 | 医生/患者/邮寄等基础信息 |
| `image` | 影像文件修改 | 影像数据/影像报告 |
| `item` | 重建项目修改 | 重建项目明细（order_item） |

---

### 1.4 字典化设计

> **重要说明**：本项目字典系统使用 `dict_code` 作为存储值，而非 `dict_value`。
> - 字典编码格式：如 `11.1`、`11.2`、`12.1` 等层级编码
> - 前端通过 `GET /system/select/biz-type-list` 获取可选值
> - 存储时使用 `dict_code` 值（如 `11.1`）而非英文字符串

#### business_type 字典化

**dict_code 规划**（使用 `11` 作为父级编码）：

| dict_code | dict_name | dict_value | 说明 |
|-----------|-----------|------------|------|
| 11 | 订单业务类型 | - | 父节点 |
| 11.1 | 业务 | business | 业务订单 |
| 11.2 | 测试 | test | 测试订单 |
| 11.3 | 试用 | trial | 试用订单 |
| 11.4 | 代理 | agent | 代理订单 |

**DictCodeConstants 新增**：

```java
/**
 * 字典编码常量
 * 订单模块相关字典编码
 */
public class DictCodeConstants {

    // ==================== 订单业务类型 ====================
    /** 订单业务类型（父节点 dict_code） */
    public static final String ORDER_BUSINESS_TYPE = "11";

    // 订单业务类型子节点（dict_code）
    public static final String ORDER_BUSINESS_TYPE_BUSINESS = "11.1";  // 业务
    public static final String ORDER_BUSINESS_TYPE_TEST = "11.2";      // 测试
    public static final String ORDER_BUSINESS_TYPE_TRIAL = "11.3";     // 试用
    public static final String ORDER_BUSINESS_TYPE_AGENT = "11.4";     // 代理
}
```

**sys_dict 初始化数据**：

```sql
-- ==================== 订单业务类型字典（dict_code=11） ====================
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(100, 0, '11', '订单业务类型', NULL, 1, 11, 1),
-- 子节点
(101, 100, '11.1', '业务', 'business', 2, 1, 1),
(102, 100, '11.2', '测试', 'test', 2, 2, 1),
(103, 100, '11.3', '试用', 'trial', 2, 3, 1),
(104, 100, '11.4', '代理', 'agent', 2, 4, 1);
```

#### patient_gender 字典化

**dict_code 规划**（使用 `12` 作为父级编码）：

| dict_code | dict_name | dict_value | 说明 |
|-----------|-----------|------------|------|
| 12 | 患者性别 | - | 父节点 |
| 12.1 | 男 | male | 男性 |
| 12.2 | 女 | female | 女性 |

**DictCodeConstants 新增**：

```java
public class DictCodeConstants {

    // ==================== 患者性别 ====================
    /** 患者性别（父节点 dict_code） */
    public static final String PATIENT_GENDER = "12";

    // 患者性别子节点（dict_code）
    public static final String PATIENT_GENDER_MALE = "12.1";    // 男
    public static final String PATIENT_GENDER_FEMALE = "12.2";  // 女
}
```

**sys_dict 初始化数据**：

```sql
-- ==================== 患者性别字典（dict_code=12） ====================
INSERT INTO sys_dict (id, parent_id, dict_code, dict_name, dict_value, level, sort, status) VALUES
-- 父节点
(110, 0, '12', '患者性别', NULL, 1, 12, 1),
-- 子节点
(111, 110, '12.1', '男', 'male', 2, 1, 1),
(112, 110, '12.2', '女', 'female', 2, 2, 1);
```

---

### 1.5 文件上传与订单关联方案

**需求回顾**：
1. 复用现有 FileService / FileController
2. 文件存储在 file_detail 表
3. 通过 bizType + bizId 关联订单
4. **bizType 使用字典 dict_code（如 `10.1`、`10.4`）**

#### bizType 字典化（订单相关）

**dict_code 规划**（使用 `10.x` 扩展已有文件业务类型）：

| dict_code | dict_name | dict_value | 说明 |
|-----------|-----------|------------|------|
| 10.1 | 影像数据 | 10.1 | CT/MRI 等医学影像（**提交必填**） |
| 10.2 | 影像报告 | 10.2 | 影像检查报告（**提交必填**） |
| 10.3 | 订单其他附件 | 10.3 | 订单相关其他附件 |

**sys_dict 扩展数据**（在已有 10.x 基础上补充）：

```sql
-- 已有 10.1、10.2、10.3，继续使用原有的 dict_code
-- 订单草稿阶段使用：bizType=10.1/10.2/10.3 + bizId=order_draft.id
-- 正式订单使用：bizType=10.1/10.2/10.3 + bizId=order_main.id
```

#### order_file 表设计（业务关联索引表）

> **设计说明**：
> - `file_detail`：存储文件元数据（x-file-storage 框架标准），`object_type` 存储 dict_code
> - `order_file`：作为业务层索引表，关联 `file_detail.id` + 订单ID
> - 提供文件类别细分、数据包编号、订单明细关联能力

```sql
CREATE TABLE `order_file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID（order_main.id）',
    `order_code` VARCHAR(50) NOT NULL COMMENT '订单编号',

    -- ==================== 文件关联 ====================
    `file_id` VARCHAR(32) NOT NULL COMMENT '文件ID（file_detail.id）',
    `file_category` VARCHAR(20) NOT NULL COMMENT '文件类别（字典 dict_code：10.1-影像数据，10.2-影像报告...）',
    `package_no` VARCHAR(50) COMMENT '数据包编号',

    -- ==================== 关联明细 ====================
    `order_item_id` BIGINT COMMENT '关联的订单明细ID',

    -- ==================== 公共字段 ====================
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by` BIGINT,
    `update_by` BIGINT,
    `is_deleted` TINYINT DEFAULT 0,

    PRIMARY KEY (`id`),
    KEY `idx_order_file_order_id` (`order_id`),
    KEY `idx_order_file_file_id` (`file_id`),
    KEY `idx_order_file_category` (`file_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单文件关联表';
```

#### order_file.file_category 字典化

**dict_code 规划**：

| dict_code | dict_name | dict_value | 提交时必填 | 说明 |
|-----------|-----------|------------|------------|------|
| 10.1 | 影像数据 | 10.1 | **是** | CT/MRI 等医学影像 |
| 10.2 | 影像报告 | 10.2 | **是** | 影像检查报告 |
| 10.3 | 订单其他附件 | 10.3 | 否 | 订单相关其他附件 |
| 10.4 | 打印文件包 | 10.4 | 否 | 设计阶段上传 |
| 10.5 | 设计报告 | 10.5 | 否 | 设计阶段上传 |
| 10.6 | 可视化模型 | 10.6 | 否 | 设计阶段上传 |
| 10.7 | 图纸文件 | 10.7 | 否 | 生产阶段上传 |
| 10.8 | 指令单文件 | 10.8 | 否 | 生产阶段上传 |

> **注意**：file_category 复用已有文件业务类型字典（dict_code=10.x），无需新增字典数据

#### 复用说明

> **重要**：`FileBizTypeEnum` 不需要创建，直接复用 `common.enums.FileBizTypeEnum`。
> 该枚举已定义所有文件业务类型，包括 dict_code 和 name。

```java
// 订单模块中直接使用 FileBizTypeEnum
import com.yigongbao.common.enums.FileBizTypeEnum;

// 使用示例
FileBizTypeEnum.IMAGE_DATA      // dictCode = "10.1"
FileBizTypeEnum.IMAGE_REPORT    // dictCode = "10.2"
FileBizTypeEnum.ORDER_ATTACHMENT  // dictCode = "10.3"
```

#### 提交前文件校验逻辑（基于系统配置）

> **重要**：影像文件是否必填由系统配置 `order.image.required` 控制。

```java
/**
 * 校验订单提交前置条件
 * 检查必填文件是否已上传
 */
public void validateSubmitPreconditions(Long orderId) {
    // 查询订单关联的所有文件
    List<OrderFileEntity> files = orderFileMapper.selectList(
        new LambdaQueryWrapper<OrderFileEntity>()
            .eq(OrderFileEntity::getOrderId, orderId)
    );

    // 校验影像数据（dict_code=10.1）
    boolean hasImageData = files.stream()
        .anyMatch(f -> FileBizTypeEnum.IMAGE_DATA.getDictCode().equals(f.getFileCategory()));
    if (!hasImageData) {
        throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "请上传影像数据（CT/MRI等）");
    }

    // 校验影像报告（dict_code=10.2）
    boolean hasImageReport = files.stream()
        .anyMatch(f -> FileBizTypeEnum.IMAGE_REPORT.getDictCode().equals(f.getFileCategory()));
    if (!hasImageReport) {
        throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "请上传影像报告");
    }
}
```

> **设计说明**：
> - `file_detail` 存储文件元数据（x-file-storage 框架标准）
> - `order_file` 作为业务层索引表，关联 file_detail.id + order_id
> - 提交时校验 order_file 中是否存在 file_category=1 和 file_category=2 的记录

**提交前文件校验逻辑**：

```java
/**
 * 校验订单提交前置条件
 */
public void validateSubmitPreconditions(Long orderId) {
    // 1. 校验必填文件
    List<OrderFileEntity> files = orderFileMapper.selectList(
        new LambdaQueryWrapper<OrderFileEntity>()
            .eq(OrderFileEntity::getOrderId, orderId)
    );

    boolean hasImageData = files.stream()
        .anyMatch(f -> f.getFileCategory() == FileBizTypeEnum.IMAGE_DATA.getDictCode());
    boolean hasImageReport = files.stream()
        .anyMatch(f -> f.getFileCategory() == FileBizTypeEnum.IMAGE_REPORT.getDictCode());

    if (!hasImageData) {
        throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED,
            "请上传影像数据（CT/MRI等）");
    }
    if (!hasImageReport) {
        throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED,
            "请上传影像报告");
    }
}
```

---

## 二、综合数据库表设计

### 2.1 第一期核心表（订单阶段）

| 序号 | 表名 | 说明 | 优先级 |
|------|------|------|--------|
| 1 | order_draft | 订单草稿表 | P0 |
| 2 | order_item_draft | 草稿明细表 | P0 |
| 3 | order_main | 订单主表 | P0 |
| 4 | order_item | 订单明细表 | P1 |
| 5 | order_file | 订单文件关联表 | P1 |
| 6 | order_status_history | 状态历史表 | P0 |
| 7 | order_modify_apply | 修改申请表 | P2 |

> **说明**：
> - order_draft/order_item_draft：草稿阶段独立存储，提交后转入正式订单
> - order_main/order_item/order_file：正式订单存储
> - order_status_history：状态变更历史，用于追溯
> - order_modify_apply：修改申请表（P2 实现）

### 2.2 草稿到正式订单的转换流程

```
1. 草稿保存（order_draft + order_item_draft + file_detail）
      ↓
2. 用户点击"提交订单"
      ↓
3. 校验前置条件
   ├── 校验必填字段（org_id, hospital_id, patient_name 等）
   ├── 校验 order_item_draft 至少1条
   └── 校验 order_file 中存在影像数据和影像报告
      ↓
4. 生成订单编号（CodeGeneratorService）
      ↓
5. 开启事务
   ├── 创建 order_main（从 order_draft 复制）
   ├── 创建 order_item（从 order_item_draft 批量复制）
   ├── 创建/更新 order_file（关联到 order_main）
   ├── 更新 file_detail（object_type 从 order_draft 改为 order_main）
   ├── 更新 order_draft.status = 2（已提交）
   ├── 记录状态历史（CREATE 动作）
   └── 删除 order_item_draft（可选，保留便于审计）
      ↓
6. 提交事务
      ↓
7. 返回订单ID和订单编号
```

---

## 三、ErrorCode 设计

```java
// ==================== 订单相关 675-699 ====================

// 订单不存在
ORDER_NOT_FOUND(675, "订单不存在"),
ORDER_DRAFT_NOT_FOUND(676, "草稿不存在"),

// 状态错误
ORDER_STATUS_ERROR(677, "订单状态不合法"),
ORDER_STATUS_TRANSITION_ERROR(678, "订单状态转换不合法"),
ORDER_NOT_DRAFT(679, "只有草稿状态的订单才能操作"),
ORDER_CANNOT_DELETE(680, "只有草稿状态的订单才能删除"),
ORDER_WITHDRAW_NOT_ALLOWED(681, "当前状态不允许撤回"),
ORDER_RESUBMIT_NOT_ALLOWED(682, "当前状态不允许重新提交"),
ORDER_ALREADY_SUBMITTED(683, "订单已提交，不能重复提交"),
ORDER_ALREADY_AUDITED(684, "订单已审核，不能重复操作"),
ORDER_NOT_WITHIN_WINDOW(685, "订单已超过10分钟修改窗口期"),

// 草稿相关
ORDER_DRAFT_EXPIRED(686, "草稿已过期，请重新创建"),
ORDER_DRAFT_NOT_FOUND(687, "草稿不存在或已过期"),
ORDER_DRAFT_NOT_YOURS(688, "只能查看自己的草稿"),

// 文件相关
ORDER_FILE_NOT_UPLOADED(689, "订单文件未上传"),
ORDER_FILE_REQUIRED(690, "请上传必需的文件：%s"),
ORDER_FILE_CATEGORY_ERROR(691, "文件类别不合法"),

// 明细相关
ORDER_ITEM_NOT_FOUND(692, "订单明细不存在"),
ORDER_ITEM_REQUIRED(693, "请至少添加一个重建项目"),
ORDER_ITEM_EMPTY(694, "重建项目明细不能为空"),

// 类型相关
ORDER_TYPE_NOT_FOUND(695, "订单类型不存在"),
ORDER_BUSINESS_TYPE_INVALID(696, "业务类型不合法"),
ORDER_PATIENT_GENDER_INVALID(697, "患者性别不合法"),

// 审核相关
ORDER_AUDIT_REMARK_REQUIRED(698, "审核驳回时必须填写驳回原因"),

// 修改申请相关
ORDER_MODIFY_APPLY_NOT_FOUND(699, "修改申请不存在"),
ORDER_MODIFY_APPLY_STATUS_ERROR(700, "修改申请状态不合法"),
ORDER_MODIFY_APPLY_ALREADY_PROCESSED(701, "该修改申请已处理");
```

---

## 四、CodeRule 设计

```java
// ==================== 订单相关编码 ====================

/**
 * 订单编号
 * 格式：ORD-yyyyMMdd-nnnnnn
 * 示例：ORD-20260331-000001
 */
ORDER_NO("ORDER_NO", "订单编号"),

/**
 * 草稿编号（预留，但草稿阶段不生成编号）
 */
ORDER_DRAFT_NO("ORDER_DRAFT_NO", "草稿编号（预留）");
```

---

## 五、接口设计（更新版）

### 5.1 草稿管理接口

| 接口 | 方法 | 路径 | 说明 | 优先级 |
|------|------|------|------|--------|
| 保存草稿 | POST | /api/order/draft | 创建或更新草稿 | P0 |
| 查询我的草稿列表 | GET | /api/order/draft/list | 分页查询当前用户的草稿 | P0 |
| 查询草稿详情 | GET | /api/order/draft/{id} | 查询草稿详情 | P0 |
| 删除草稿 | DELETE | /api/order/draft/{id} | 删除草稿（仅创建者可删除） | P0 |
| 提交草稿 | POST | /api/order/draft/{id}/submit | 提交草稿转为正式订单 | P0 |

### 5.2 订单管理接口

| 接口 | 方法 | 路径 | 说明 | 优先级 |
|------|------|------|------|--------|
| 查询订单列表 | GET | /api/order/list | 分页查询订单 | P0 |
| 查询订单详情 | GET | /api/order/{id} | 查询订单详情 | P0 |
| 更新订单信息 | PUT | /api/order/{id} | 更新订单（公司管理员/10分钟内） | P0 |
| 删除订单 | DELETE | /api/order/{id} | 删除订单（仅草稿） | P0 |

### 5.3 状态操作接口

| 接口 | 方法 | 路径 | 说明 | 优先级 |
|------|------|------|------|--------|
| 提交订单 | PUT | /api/order/{id}/submit | 提交审核 | P0 |
| 撤回订单 | PUT | /api/order/{id}/withdraw | 撤回重审 | P0 |
| 重新提交 | PUT | /api/order/{id}/resubmit | 审核驳回后重新提交 | P0 |
| 审核通过 | PUT | /api/order/{id}/audit/pass | 数据审核通过 | P0 |
| 审核驳回 | PUT | /api/order/{id}/audit/reject | 数据审核驳回 | P0 |
| 查询状态历史 | GET | /api/order/{id}/history | 查询状态变更历史 | P0 |
| 查询可执行操作 | GET | /api/order/{id}/actions | 查询当前可执行操作 | P0 |

### 5.4 文件接口

| 接口 | 方法 | 路径 | 说明 | 优先级 |
|------|------|------|------|--------|
| 上传文件 | POST | /api/order/{id}/files | 上传订单文件 | P0 |
| 查询文件列表 | GET | /api/order/{id}/files | 查询订单文件列表 | P0 |
| 删除文件 | DELETE | /api/order/{id}/files/{fileId} | 删除订单文件 | P0 |

### 5.5 修改申请接口

| 接口 | 方法 | 路径 | 说明 | 优先级 |
|------|------|------|------|--------|
| 发起修改申请 | POST | /api/order/{id}/modify-apply | 发起修改申请 | P2 |
| 查询我的申请 | GET | /api/order/modify-apply/my | 查询我发起的申请 | P2 |
| 待审核申请 | GET | /api/order/modify-apply/pending | 查询待审核申请 | P2 |
| 审核申请 | PUT | /api/order/modify-apply/{id}/audit | 审核申请（同意/拒绝） | P2 |

---

## 六、关键技术难点

### 6.1 草稿提交事务一致性

**场景**：草稿提交时需要同时完成多个操作：
1. 创建 order_main
2. 创建 order_item
3. 更新 order_file
4. 更新 file_detail
5. 更新 order_draft

**解决方案**：使用 `@Transactional(rollbackFor = Exception.class)` 保证原子性

```java
@Transactional(rollbackFor = Exception.class)
public OrderSubmitVO submitDraft(Long draftId) {
    // 1. 校验前置条件
    validateSubmitPreconditions(draftId);
    // 2. 生成订单编号
    String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);
    // 3. 创建订单主表
    OrderMainEntity orderMain = createOrderMainFromDraft(draftId, orderCode);
    // 4. 创建订单明细
    List<OrderItemEntity> items = createOrderItemsFromDraft(draftId, orderMain.getId());
    // 5. 更新文件关联
    updateFileReferences(draftId, orderMain);
    // 6. 标记草稿已提交
    updateDraftStatus(draftId, 2);
    // 7. 记录状态历史
    orderStatusHistoryService.recordTransition(orderMain, null, OrderActionEnum.CREATE, "提交订单");
    return new OrderSubmitVO(orderMain.getId(), orderCode);
}
```

### 6.2 10分钟修改窗口期判断

**场景**：需要准确判断当前时间是否在"提交时间 + 10分钟"内

**解决方案**：
1. 数据库层面：创建时间 `create_time` 字段记录提交时间
2. 业务层面：在 OrderService 中判断

```java
public boolean canModify(Long orderId, Long currentUserId) {
    OrderMainEntity order = orderMapper.selectById(orderId);
    if (order == null) return false;
    // 1. 公司管理员可以直接修改
    if (hasAdminRole(currentUserId)) return true;
    // 2. 提单人或区域管理员，且在10分钟内
    if (isSubmitterOrRegionalManager(order, currentUserId)) {
        return order.getCreateTime().plusMinutes(10).isAfter(LocalDateTime.now());
    }
    return false;
}
```

### 6.3 修改留痕记录

**场景**：公司管理员修改订单时需要记录修改前后值

**解决方案**：

```java
/**
 * 记录修改留痕
 */
public void recordModification(Long orderId, Long modifierId, Map<String, Object> beforeValues,
                              Map<String, Object> afterValues, String modifyReason) {
    OrderModificationLogEntity log = new OrderModificationLogEntity();
    log.setOrderId(orderId);
    log.setModifierId(modifierId);
    log.setBeforeValues(JSONUtil.toJsonStr(beforeValues));
    log.setAfterValues(JSONUtil.toJsonStr(afterValues));
    log.setModifyReason(modifyReason);
    orderModificationLogMapper.insert(log);
}
```

**order_modification_log 表**：

```sql
CREATE TABLE `order_modification_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `modifier_id` BIGINT NOT NULL COMMENT '修改人ID',
    `modifier_name` VARCHAR(100) COMMENT '修改人姓名',
    `before_values` TEXT COMMENT '修改前值（JSON）',
    `after_values` TEXT COMMENT '修改后值（JSON）',
    `modify_reason` TEXT COMMENT '修改原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单修改留痕表';
```

---

## 七、索引设计规范

### 7.1 索引名前缀规范

为避免多表索引名冲突，所有 order 相关表的索引统一使用表名前缀：

| 表名 | 索引前缀 | 示例 |
|------|---------|------|
| order_draft | `idx_order_draft_` | `idx_order_draft_operator_id` |
| order_item_draft | `idx_order_item_draft_` | `idx_order_item_draft_draft_id` |
| order_main | `idx_order_main_` | `idx_order_main_phase_status` |
| order_item | `idx_order_item_` | `idx_order_item_order_id` |
| order_file | `idx_order_file_` | `idx_order_file_order_id` |
| order_status_history | `idx_order_status_history_` | `idx_order_status_history_order_id` |
| order_modify_apply | `idx_order_modify_apply_` | `idx_order_modify_apply_order_id` |

### 7.2 H2 测试数据库索引规范

**重要**：H2 测试数据库中禁止定义 `is_deleted` 字段的索引（MyBatis-Plus @TableLogic 会自动创建）

---

## 八、多模块架构策略

### 8.1 背景

当前只实现订单阶段（ORDER Phase），但系统中还存在设计阶段（DESIGN）和生产阶段（PRODUCTION）。为避免未来模块臃肿，采用**按阶段拆分独立模块**的策略。

### 8.2 模块规划

| 模块 | 说明 | 优先级 |
|------|------|--------|
| `yigongbao-module-order` | 订单阶段（当前开发） | P0 |
| `yigongbao-module-design` | 设计阶段（未来） | P1 |
| `yigongbao-module-production` | 生产阶段（未来） | P2 |

### 8.3 枚举迁移到 common

| 枚举 | 存放位置 | 原因 |
|------|----------|------|
| `OrderPhaseEnum` | `yigongbao-common` | 所有阶段模块都需引用 |
| `OrderStatusEnum` | `yigongbao-common` | 各阶段都有状态 |
| `OrderActionEnum` | `yigongbao-common` | 各阶段都有动作 |
| `OrderBusinessTypeEnum` | `yigongbao-common` | 字典编码可复用 |
| `OrderPatientGenderEnum` | `yigongbao-common` | 字典编码可复用 |
| `OrderCodeRuleConstants` | `yigongbao-common` | 编码常量可复用 |

### 8.4 跨模块依赖分析

**场景**：设计阶段需要查询订单信息（order_code、患者姓名等）。

**方案**：使用 OpenFeign 远程调用

```java
// yigongbao-module-design 中
@FeignClient("yigongbao-module-order")
public interface OrderFeignClient {
    @GetMapping("/api/order/{orderCode}")
    OrderMainVO getByOrderCode(@PathVariable String orderCode);
}
```

### 8.5 避免循环依赖

**原则**：
- 所有业务模块都只依赖 `yigongbao-common`
- 模块之间通过 Feign/MQ 解耦，不直接依赖
- 禁止：order → design 且 design → order

```
✅ 正确：order → common ← design
✅ 正确：order 通过 Feign 调用 design
❌ 错误：order → design 且 design → order
```

---

## 九、版本历史

| 版本 | 日期 | 修改内容 | 作者 |
|-----|------|---------|------|
| v1.0 | 2026-03-31 | 初始版本：订单状态机设计方案 | hanjor |
| v2.0 | 2026-03-31 | 整合所有补充需求：草稿独立表设计、35+字段分析、10分钟权限、字典化、文件关联 | hanjor |
