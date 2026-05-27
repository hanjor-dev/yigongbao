# 生产管理模块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建生产管理模块，覆盖从设计审核通过到产品入库的完整生产链条，支持医疗器械和非医疗器械两种订单类型的差异化流程。

**Architecture:** 新建 yigongbao-module-production 模块，与 order/design 模块平级，依赖 design 模块（传递引入 order/flow/basic/system）。采用批次级别管理（流转卡）+ 产品级别追溯（产品编号/UDI码）的双层架构。通过 FlowFacade 驱动状态流转，WebSocket 监听设备状态自动触发打印开始/完成。

**⚠️ 架构约束（必读）：**

1. **Flow 模块以 orderId 为唯一单一状态**：`FlowFacade.executeFlow(orderId, action, operator)` 驱动 `order_main.phase/status` 变更，一个订单在任意时刻只有一个状态，不支持并行。

2. **一订单可有多张流转卡（一包一卡）**：一个订单可有多个设计数据包，每个数据包对应一张流转卡，多张流转卡可并行生产。**因此生产阶段不能在单张流转卡完成时直接触发 Flow**，否则会导致订单状态错误推进。

3. **聚合触发原则（方案 A）**：Flow 状态流转只在**同一订单下所有活跃流转卡都达到某阶段**时触发一次。流转卡自己维护 `production_record.status` 内部状态，不触发 Flow；只有聚合条件满足时，才统一触发 Flow 并回写 `order_main`。

4. **聚合触发条件**：

   | 触发时机 | 聚合条件 | FlowActionEnum |
   |---|---|---|
   | 某张流转卡打印完成 | 所有活跃流转卡 status = `print_completed` | `COMPLETE_PRINT` |
   | 某张流转卡后处理完成（进入质检） | 所有活跃流转卡 status = `qc_in_progress` | `COMPLETE_POST_PROCESSING` |
   | 某张流转卡质检完成流转包装 | 所有活跃流转卡 status = `packing` | `QC_PASS` |
   | 某张流转卡包装完成 | 所有活跃流转卡 status = `warehouse_in` | `COMPLETE_WAREHOUSE_IN` |

5. **活跃流转卡定义**：status 不在 `[print_failed, abandoned]` 中的流转卡。

6. **每次调用 FlowFacade 后必须回写订单状态**：`TransitionResult` 不自动更新 order_main，调用方必须手动 `orderMainMapper.updateById()`。统一封装为公共方法 `triggerFlowIfAllReach(orderId, requiredStatus, action)`。

7. **FlowFacade 正确调用方式**：`flowFacade.executeFlow(orderId, FlowActionEnum.XXX, FlowOperator.of(userId, userName))`，不存在 `FlowContext.setAction()` 写法。

8. **三层状态粒度设计（重要）**：

   | 粒度 | 存储位置 | 展示场景 | 示例值 |
   |---|---|---|---|
   | **粗粒度（订单级）** | `order_main.phase/status` | 订单列表、订单详情页顶部进度条 | 打印中、后处理中、质检中 |
   | **中粒度（流转卡级）** | `production_record.status` | 生产管理列表、流转卡详情页主状态 | 打印中、打印完成、后处理中、质检中、包装中 |
   | **细粒度（工序级）** | `production_process.status` + `production_process.process_type` | 流转卡详情页"后处理"阶段内的补充展示（如"当前工序：UV固化"） | 酒精初洗-已完成、UV固化-进行中 |

   **规则**：
   - 订单级和流转卡级状态**直接展示给前端**，是主要的状态展示依据
   - 工序级状态**仅在后处理阶段内作为补充信息展示**（前端在流转卡详情的"后处理"区块内展示工序进度）
   - 每个工序的质检结果（`production_process_product_result`）**不直接展示给前端**，仅用于内部质检流程驱动
   - `production_record.current_process` 字段记录当前所在工序，供前端在后处理阶段展示"当前工序"

9. **流转卡状态与订单状态的对应关系**：

   | 流转卡 status | 对应订单 phase/status（聚合后） |
   |---|---|
   | `pending_print` | 打印阶段 / 待打印 |
   | `printing` | 打印阶段 / 打印中（WebSocket 触发，仅更新流转卡，不触发 Flow） |
   | `print_completed` | 打印阶段 / 打印完成（全部完成后触发 COMPLETE_PRINT） |
   | `post_processing` | 后处理阶段 / 后处理中 |
   | `qc_in_progress` | 质检阶段 / 质检中（全部进入后触发 COMPLETE_POST_PROCESSING） |
   | `packing` | 仓储阶段 / 包装中（全部进入后触发 QC_PASS） |
   | `warehouse_in` | 仓储阶段 / 入库中（全部进入后触发 COMPLETE_WAREHOUSE_IN） |
   | `completed` | 完成阶段 |

10. **部分合格状态展示**：流转卡 `status` 停留在 `qc_in_progress` 直到所有产品 pass；`has_redo_product` 标志位区分"全部质检中"与"存在待重做产品"两种子状态，前端据此在流转卡详情页展示不同提示。

11. **移除活跃流转卡唯一性校验**：原约束"一订单同时只允许一张活跃流转卡"已废除，也不需要 `PRODUCTION_RECORD_ALREADY_EXISTS(807)` 错误码。

12. **流转卡自动创建（无手动创建接口）**：`downloadDataPackage()` 在触发 `START_PRINT` 后，自动为该数据包创建流转卡（含产品记录和工序记录），状态为 `pending_print`，`printDeviceId` 留空。前端无需手动调用创建接口，`POST /production/record/create` 接口不存在。幂等保护：仅当 `START_PRINT` 在可用动作列表中时才触发（即订单处于 DESIGN_REVIEW_PASSED 状态），否则直接返回成功。

13. **打印机分配接口**：`POST /production/record/assign-device/{recordId}` 接收 `deviceId` 和 `material`，校验设备在线且未被占用，更新流转卡的 `printDeviceId`/`printDeviceCode`/`printDeviceName`/`material` 字段。`GET /production/record/printers` 返回所有打印机列表（含在线状态）。

13. **工序列表不包含 DESIGN**：`createProcessRecords()` 中不应创建 `DESIGN` 工序（设计已在 design 模块完成，生产阶段无对应操作）。工序从 `PRINT` 开始：医疗器械 `[PRINT, WASH, CURE, CLEAN_DRY, PACK]`，非医疗器械 `[PRINT, PACK]`。

14. **所有订单类型都需要 PACK 工序**：非医疗器械打印完成后直接进入 PACK，不是直接入库。

15. **`post_processing` 状态必须在中间工序流转时更新**：`transferToNext()` 中，当 `fromProcess` 为 `WASH` 或 `CURE` 时，需更新流转卡 `status = post_processing`，同时更新 `current_process` 为下一工序类型，供前端展示当前后处理工序进度。

16. **`DeviceStatusListener` 需注入 `IProductionRecordService`**：`handlePrintComplete()` 调用 `recordService.triggerFlowIfAllReach()`，必须在依赖字段中添加 `private final IProductionRecordService recordService;`。

17. **`triggerFlowIfAllReach` 必须在接口中声明**：该方法被跨服务调用（Task 7/8/8.5 的 ServiceImpl 均需调用），必须在 `IProductionRecordService` 接口中声明，否则只能注入实现类，违反依赖倒置原则。

18. **逻辑删除必须通过 `deleteById`**：Task 13 中废弃产品时，禁止手动 `setIsDeleted(1)` + `updateById()`，应改为 `productMapper.deleteById(p.getId())`，由 MyBatis-Plus `@TableLogic` 自动处理。

19. **`PRODUCTION_RECORD_ALREADY_EXISTS(807)` 从 ErrorCodeEnum 中删除**：约束第 11 条已废除该校验，Task 3 Step 7 的错误码列表中不应包含此项。

20. **`ProcessConfigController` 移除 `@Slf4j`**：Controller 层禁止日志输出，JSON 解析异常的 `log.warn()` 应移至 Service 层处理。

**Tech Stack:** Spring Boot 3.2.5, MyBatis Plus 3.5.8, SaToken 1.37.0, Hutool 5.8.26, ZXing 3.5.3 (二维码), Flow Engine (状态机)

---

## 🚨 编码规范要求（必读）

**【强制】实施前必须阅读并严格遵守 `.claude/rules/java-coding-standards.md` 中的所有规范**，包括但不限于：

1. **注释规范**：
   - 所有类必须添加类注释（功能说明、作者、创建时间）
   - 所有公共方法必须添加 Javadoc 注释（功能、参数、返回值、异常）
   - ServiceImpl 必须添加方法级注释和关键行级注释

2. **日志规范**：
   - Controller 层禁止输出日志
   - ServiceImpl 必须记录日志（入参、关键节点、异常）
   - 使用 `log.info/warn/error` 记录业务操作和异常

3. **异常处理**：
   - 优先使用 `ErrorCodeEnum` 抛出业务异常
   - Controller 禁止 try-catch，由 GlobalExceptionHandler 统一处理

4. **数据库规范**：
   - 所有 Entity 继承 `BaseEntity`
   - 逻辑删除表的唯一索引必须使用函数索引
   - 禁止在 Mapper XML 中写 SQL，全部使用 MyBatis-Plus 代码操作

5. **命名规范**：
   - 禁止使用魔法值（0/1），必须使用 `StatusConstants` 常量
   - 调用编码生成器必须使用 `CodeRuleConstants` 常量

**违反编码规范的代码将被要求重写。**

---

## 文件结构概览

```
yigongbao-module-production/
├── pom.xml
└── src/main/java/com/yigongbao/module/production/
    ├── record/              # 生产流转卡
    │   ├── entity/ProductionRecordEntity.java
    │   ├── mapper/ProductionRecordMapper.java
    │   ├── service/IProductionRecordService.java
    │   ├── service/impl/ProductionRecordServiceImpl.java
    │   ├── controller/ProductionRecordController.java
    │   ├── vo/ProductionRecordVO.java
    │   └── convert/ProductionRecordConvert.java
    ├── product/             # 生产产品记录
    │   ├── entity/ProductionProductEntity.java
    │   ├── mapper/ProductionProductMapper.java
    │   ├── service/IProductionProductService.java
    │   └── service/impl/ProductionProductServiceImpl.java
    ├── process/             # 工序记录
    │   ├── entity/ProductionProcessEntity.java
    │   ├── mapper/ProductionProcessMapper.java
    │   ├── service/IProductionProcessService.java
    │   ├── service/impl/ProductionProcessServiceImpl.java
    │   ├── controller/ProductionProcessController.java
    │   ├── vo/ProcessVO.java
    │   └── dto/FillProcessDTO.java
    ├── qc/                  # 质检管理
    │   ├── entity/ProductionProcessProductResultEntity.java
    │   ├── mapper/ProductionProcessProductResultMapper.java
    │   ├── service/IProductionQcService.java
    │   ├── service/impl/ProductionQcServiceImpl.java
    │   ├── controller/ProductionQcController.java
    │   └── dto/QcProductDTO.java
    ├── transfer/            # 工序流转
    │   ├── entity/ProductionProcessTransferEntity.java
    │   └── mapper/ProductionProcessTransferMapper.java
    ├── enums/
    │   ├── ProcessTypeEnum.java
    │   ├── ProductStatusEnum.java
    │   └── ProcessStatusEnum.java
    └── constants/
        └── ProductionConstants.java
```

---

## Task 1: 模块脚手架搭建

**Files:**
- Create: `yigongbao-parent/yigongbao-module-production/pom.xml`
- Modify: `yigongbao-parent/pom.xml` (添加 module 和 dependencyManagement)
- Modify: `yigongbao-parent/yigongbao-boot/pom.xml` (添加 production 依赖)

- [ ] **Step 1: 创建 production 模块 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-parent</artifactId>
        <version>1.0.0</version>
    </parent>
    <artifactId>yigongbao-module-production</artifactId>

    <dependencies>
        <!-- design：设计数据包查询（传递引入 order/flow/basic/system） -->
        <dependency>
            <groupId>com.yigongbao</groupId>
            <artifactId>yigongbao-module-design</artifactId>
        </dependency>

        <!-- ZXing：二维码生成 -->
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 修改父 pom.xml 添加 module**

在 `yigongbao-parent/pom.xml` 的 `<modules>` 中添加：
```xml
<module>yigongbao-module-production</module>
```

在 `<dependencyManagement>` 中添加：
```xml
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-production</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 3: 修改 boot 模块 pom.xml**

在 `yigongbao-boot/pom.xml` 的 `<dependencies>` 中添加：
```xml
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-production</artifactId>
</dependency>
```

- [ ] **Step 4: 创建目录结构**

```bash
cd yigongbao-parent/yigongbao-module-production
mkdir -p src/main/java/com/yigongbao/module/production/{record,product,process,qc,transfer,enums,constants}/{entity,mapper,service/impl,controller,vo,dto,convert}
mkdir -p src/main/resources
mkdir -p src/test/java/com/yigongbao/module/production
mkdir -p src/test/resources
```

- [ ] **Step 5: 验证模块编译**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/pom.xml yigongbao-parent/yigongbao-boot/pom.xml yigongbao-parent/yigongbao-module-production/
git commit -m "feat: 创建生产管理模块脚手架"
```

---

## Task 2: 数据库表结构创建

**Files:**
- Create: `sql/production_ddl.sql`

- [ ] **Step 1: 创建 production_record 表（生产流转卡）**

```sql
CREATE TABLE production_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    record_no VARCHAR(50) NOT NULL COMMENT '流转卡编号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_code VARCHAR(50) COMMENT '订单编号（冗余字段，便于查询展示）',
    order_type TINYINT NOT NULL COMMENT '订单类型（1=医疗器械，2=非医疗器械）',
    design_package_id BIGINT NOT NULL COMMENT '设计数据包ID',
    design_package_code VARCHAR(50) NOT NULL COMMENT '设计数据包编号',
    production_batch_no VARCHAR(50) NOT NULL COMMENT '生产批号',
    version_no VARCHAR(20) COMMENT '版本号',
    material VARCHAR(100) COMMENT '材质',
    -- ==================== 订单冗余信息（避免跨表查询）====================
    hospital_name VARCHAR(200) COMMENT '医院名称（冗余）',
    hospital_dept_name VARCHAR(100) COMMENT '科室名称（冗余）',
    doctor_name VARCHAR(100) COMMENT '医生姓名（冗余）',
    patient_name VARCHAR(100) COMMENT '患者姓名（冗余）',
    is_urgent TINYINT NOT NULL DEFAULT 0 COMMENT '是否加急（0=否，1=是）',
    is_postal TINYINT NOT NULL DEFAULT 0 COMMENT '是否邮寄（0=否，1=是）',
    expected_delivery_date DATETIME COMMENT '期望交付时间（冗余）',
    processing_center_id BIGINT COMMENT '加工中心ID',
    processing_center_name VARCHAR(100) COMMENT '加工中心名称（冗余字段）',
    print_device_id BIGINT COMMENT '分配的打印机ID',
    print_device_code VARCHAR(50) COMMENT '打印机编号',
    print_device_name VARCHAR(100) COMMENT '打印机名称（冗余字段）',
    total_product_count INT NOT NULL DEFAULT 0 COMMENT '产品总数',
    qualified_count INT NOT NULL DEFAULT 0 COMMENT '合格数量',
    unqualified_count INT NOT NULL DEFAULT 0 COMMENT '不合格数量',
    has_redo_product TINYINT NOT NULL DEFAULT 0 COMMENT '是否存在待重做产品（0=否，1=是）',
    status VARCHAR(50) NOT NULL COMMENT '当前状态',
    current_process VARCHAR(50) COMMENT '当前工序',
    qr_code_url VARCHAR(255) COMMENT '流转卡二维码URL',
    pack_device_id BIGINT COMMENT '包装设备ID',
    pack_device_no VARCHAR(50) COMMENT '包装设备编号',
    pack_seal_temperature DECIMAL(5,2) COMMENT '热封温度（℃）',
    pack_seal_time INT COMMENT '热封时间（秒）',
    pack_sterilization_method VARCHAR(100) COMMENT '灭菌方式',
    pack_sterilization_batch_no VARCHAR(50) COMMENT '灭菌批号',
    pack_operator_id BIGINT COMMENT '包装操作人ID',
    pack_operator_name VARCHAR(50) COMMENT '包装操作人姓名',
    pack_time DATETIME COMMENT '包装完成时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_order_id (order_id),
    KEY idx_design_package_id (design_package_id),
    KEY idx_status (status),
    KEY idx_production_batch_no (production_batch_no),
    KEY idx_processing_center_id (processing_center_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产流转卡表';

CREATE UNIQUE INDEX uk_record_no 
    ON production_record ((CASE WHEN is_deleted = 0 THEN record_no ELSE NULL END));
```

- [ ] **Step 2: 创建 production_product 表（生产产品记录）**

```sql
CREATE TABLE production_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    print_file_id BIGINT NOT NULL COMMENT '打印文件ID',
    product_no VARCHAR(50) NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称',
    file_name VARCHAR(255) COMMENT '打印文件名',
    udi_code VARCHAR(200) COMMENT 'UDI码（质检合格后生成，仅医疗器械）',
    udi_di VARCHAR(100) COMMENT 'UDI-DI（设备标识符，来自注册证）',
    udi_pi VARCHAR(100) COMMENT 'UDI-PI（生产标识符：批号+序列号）',
    udi_generate_time DATETIME COMMENT 'UDI生成时间',
    status VARCHAR(50) NOT NULL COMMENT '产品状态（in_process/redo/pass/completed）',
    current_process_type VARCHAR(50) COMMENT '当前所在工序',
    qc_result VARCHAR(50) COMMENT '质检结果（pass/redo）',
    qc_remark VARCHAR(500) COMMENT '质检不合格原因',
    qc_time DATETIME COMMENT '质检时间',
    qc_user_id BIGINT COMMENT '质检员ID',
    redo_process_type VARCHAR(50) COMMENT '指定的重做工序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_production_record_id (production_record_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产产品记录表';

CREATE UNIQUE INDEX uk_product_no 
    ON production_product ((CASE WHEN is_deleted = 0 THEN product_no ELSE NULL END));
```

- [ ] **Step 3: 创建 production_process 表（工序记录）**

```sql
CREATE TABLE production_process (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    process_type VARCHAR(50) NOT NULL COMMENT '工序类型',
    process_name VARCHAR(100) NOT NULL COMMENT '工序名称',
    process_order INT NOT NULL COMMENT '工序顺序',
    device_type VARCHAR(100) COMMENT '关键设备类型',
    device_id BIGINT COMMENT '设备ID',
    device_no VARCHAR(50) COMMENT '设备编号',
    process_params JSON COMMENT '关键参数（JSON格式）',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    operator_id BIGINT COMMENT '操作人员ID',
    operator_name VARCHAR(50) COMMENT '操作人员姓名',
    has_redo TINYINT DEFAULT 0 COMMENT '本工序是否有重做',
    redo_remark VARCHAR(500) COMMENT '重做记录',
    inspection_result VARCHAR(50) COMMENT '工序整体结果',
    inspector_id BIGINT COMMENT '检验员ID',
    inspector_name VARCHAR(50) COMMENT '检验员姓名',
    status VARCHAR(50) NOT NULL COMMENT '工序状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_production_record_id (production_record_id),
    KEY idx_process_type (process_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序记录表';
```

- [ ] **Step 4: 创建 production_process_product_result 表（质检产品记录）**

```sql
CREATE TABLE production_process_product_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_process_id BIGINT NOT NULL COMMENT '工序记录ID',
    production_product_id BIGINT NOT NULL COMMENT '产品ID',
    result VARCHAR(50) NOT NULL COMMENT '检验结果（qualified/unqualified）',
    remark VARCHAR(500) COMMENT '不合格原因',
    attempt_no INT NOT NULL DEFAULT 1 COMMENT '尝试次数',
    is_latest TINYINT NOT NULL DEFAULT 1 COMMENT '是否最新记录',
    inspector_id BIGINT COMMENT '检验员ID',
    inspect_time DATETIME COMMENT '检验时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_process_id (production_process_id),
    KEY idx_product_id (production_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检产品记录表';
```

- [ ] **Step 5: 创建 production_process_transfer 表（工序流转记录）**

```sql
CREATE TABLE production_process_transfer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    production_record_id BIGINT NOT NULL COMMENT '流转卡ID',
    from_process_type VARCHAR(50) NOT NULL COMMENT '来源工序代码',
    to_process_type VARCHAR(50) NOT NULL COMMENT '目标工序代码',
    transfer_time DATETIME NOT NULL COMMENT '流转时间',
    scan_user_id BIGINT NOT NULL COMMENT '扫码人ID',
    scan_user_name VARCHAR(50) NOT NULL COMMENT '扫码人姓名',
    handover_user_id BIGINT COMMENT '交接人ID',
    handover_user_name VARCHAR(50) COMMENT '交接人姓名',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_production_record_id (production_record_id),
    KEY idx_transfer_time (transfer_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序流转记录表';
```

- [ ] **Step 6: 执行 DDL 并验证**

```bash
mysql -h localhost -P 3307 -u root -p yigongbao < sql/production_ddl.sql
```

Expected: 表创建成功

- [ ] **Step 5: Commit**

```bash
git add sql/production_ddl.sql
git commit -m "feat: 创建生产管理模块数据库表结构"
```

---

## Task 3: 枚举类和常量创建

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/ProcessTypeEnum.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/ProductStatusEnum.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/ProcessStatusEnum.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/RecordStatusEnum.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/QcResultEnum.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/constants/ProductionConstants.java`
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`

- [ ] **Step 1: 创建 ProcessTypeEnum**

```java
package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessTypeEnum {
    DESIGN("design", "设计", 1),
    PRINT("print", "3D打印成型", 2),
    WASH("wash", "酒精初洗（含打磨）", 3),
    CURE("cure", "UV固化", 4),
    CLEAN_DRY("clean_dry", "超声清洗+干燥", 5),
    PACK("pack", "包装", 6);

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
    private final Integer order;
}
```

- [ ] **Step 2: 创建 ProductStatusEnum**

```java
package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatusEnum {
    IN_PROCESS("in_process", "生产中"),
    REDO("redo", "待重做"),
    PASS("pass", "质检合格"),
    COMPLETED("completed", "已完成入库");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 3: 创建 ProcessStatusEnum**

```java
package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessStatusEnum {
    PENDING("pending", "待开始"),
    IN_PROGRESS("in_progress", "进行中"),
    COMPLETED("completed", "已完成");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 4: 创建 RecordStatusEnum（流转卡状态）**

```java
package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecordStatusEnum {
    PENDING_PRINT("pending_print", "待打印"),
    PRINTING("printing", "打印中"),
    PRINT_COMPLETED("print_completed", "打印完成"),
    POST_PROCESSING("post_processing", "后处理中"),
    QC_IN_PROGRESS("qc_in_progress", "质检中"),
    PACKING("packing", "包装中"),
    WAREHOUSE_IN("warehouse_in", "入库中"),
    COMPLETED("completed", "已完成"),
    PRINT_FAILED("print_failed", "打印失败"),
    ABANDONED("abandoned", "已废弃");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 5: 创建 QcResultEnum（质检结果）**

```java
package com.yigongbao.module.production.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QcResultEnum {
    PASS("pass", "合格"),
    REDO("redo", "不合格");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
}
```

- [ ] **Step 6: 创建 ProductionConstants**

```java
package com.yigongbao.module.production.constants;

public class ProductionConstants {
    // 编码生成器规则常量（对应 sys_code_rule.rule_code 字段值，需在数据库中预置）
    public static final String PRODUCTION_RECORD_NO = "PRODUCTION_RECORD_NO";
    public static final String PRODUCTION_BATCH_NO = "PRODUCTION_BATCH_NO";
    public static final String PRODUCT_NO = "PRODUCT_NO";
    public static final String UDI_CODE = "UDI_CODE";
    
    // 订单类型常量（对应 order.orderType 字段值）
    public static final Integer ORDER_TYPE_MEDICAL = 1;      // 医疗器械
    public static final Integer ORDER_TYPE_NON_MEDICAL = 2;  // 非医疗器械
    
    // 设备状态常量
    public static final Integer DEVICE_STATE_IDLE = 0;       // 空闲
    public static final Integer DEVICE_STATE_BUSY = 1;       // 占用
    
    private ProductionConstants() {}
}
```

> **⚠️ 注意**：`CodeRuleConstants` 文件在 common 模块中**不存在**，生产模块的编码规则常量统一定义在 `ProductionConstants` 中。Task 6 的 ServiceImpl 中所有 `CodeRuleConstants.PRODUCTION_RECORD_NO` 等引用均应改为 `ProductionConstants.PRODUCTION_RECORD_NO`。

- [ ] **Step 7: 扩展 ErrorCodeEnum（在 yigongbao-common 模块）**

在 `ErrorCodeEnum` 中添加生产模块专用错误码（800-808范围）：

```java
// ==================== 生产模块错误码 (800-808) ====================
PRODUCTION_RECORD_NOT_FOUND(800, "生产流转卡不存在", 3),
PRINT_DEVICE_NOT_FOUND(801, "打印设备不存在", 3),
PROCESSING_CENTER_NOT_FOUND(802, "加工中心不存在", 3),
PACK_DEVICE_NOT_FOUND(803, "包装设备不存在", 3),
PRODUCT_NOT_ALL_PASS(804, "存在未通过质检的产品，无法流转", 3),
PACK_INFO_NOT_FILLED(805, "请先填写包装信息", 3),
DEVICE_NOT_AVAILABLE(806, "设备不可用", 3),
SYSTEM_ERROR(808, "系统内部错误", 3),
// 注意：807 已废除，不添加 PRODUCTION_RECORD_ALREADY_EXISTS
```

**复用已存在的错误码（无需新建）：**
- `ORDER_NOT_FOUND(677)` - 订单不存在
- `DESIGN_PACKAGE_NOT_FOUND(758)` - 设计数据包不存在
- `PRODUCT_NOT_FOUND(648)` - 产品不存在

- [ ] **Step 8: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/enums/
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/constants/
git add yigongbao-parent/yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java
git commit -m "feat: 创建生产模块枚举、常量和专用错误码"
```

---

## Task 4: 实体类创建

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/entity/ProductionRecordEntity.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/entity/ProductionProductEntity.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/entity/ProductionProcessEntity.java`

- [ ] **Step 1: 创建 ProductionRecordEntity**

```java
package com.yigongbao.module.production.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_record")
public class ProductionRecordEntity extends BaseEntity {
    private String recordNo;
    private Long orderId;
    private String orderCode;
    private Integer orderType;
    private Long designPackageId;
    private String designPackageCode;
    private String productionBatchNo;
    private String versionNo;
    private String material;
    private Long processingCenterId;
    private String processingCenterName;
    private Long printDeviceId;
    private String printDeviceCode;
    private String printDeviceName;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer hasRedoProduct;  // 是否存在待重做产品（0=否，1=是）
    private String status;
    private String currentProcess;
    private String qrCodeUrl;
    private Long packDeviceId;
    private String packDeviceNo;
    private BigDecimal packSealTemperature;
    private Integer packSealTime;
    private String packSterilizationMethod;
    private String packSterilizationBatchNo;
    private Long packOperatorId;
    private String packOperatorName;
    private LocalDateTime packTime;
}
```

- [ ] **Step 2: 创建 ProductionProductEntity**

```java
package com.yigongbao.module.production.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_product")
public class ProductionProductEntity extends BaseEntity {
    private Long productionRecordId;
    private Long printFileId;
    private String productNo;
    private String productName;
    private String fileName;
    private String udiCode;
    private LocalDateTime udiGenerateTime;
    private String status;
    private String currentProcessType;
    private String qcResult;
    private String qcRemark;
    private LocalDateTime qcTime;
    private Long qcUserId;
    private String redoProcessType;
}
```

- [ ] **Step 3: 创建 ProductionProcessEntity**

```java
package com.yigongbao.module.production.process.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process")
public class ProductionProcessEntity extends BaseEntity {
    private Long productionRecordId;
    private String processType;
    private String processName;
    private Integer processOrder;
    private String deviceType;
    private Long deviceId;
    private String deviceNo;
    private String processParams;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long operatorId;
    private String operatorName;
    private Integer hasRedo;
    private String redoRemark;
    private String inspectionResult;
    private Long inspectorId;
    private String inspectorName;
    private String status;
}
```

- [ ] **Step 4: 创建 ProductionProcessProductResultEntity**

```java
package com.yigongbao.module.production.qc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process_product_result")
public class ProductionProcessProductResultEntity extends BaseEntity {
    private Long productionProcessId;
    private Long productionProductId;
    private String result;
    private String remark;
    private Integer attemptNo;
    private Integer isLatest;
    private Long inspectorId;
    private LocalDateTime inspectTime;
}
```

- [ ] **Step 5: 创建 ProductionProcessTransferEntity**

```java
package com.yigongbao.module.production.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_process_transfer")
public class ProductionProcessTransferEntity extends BaseEntity {
    private Long productionRecordId;
    private String fromProcessType;
    private String toProcessType;
    private LocalDateTime transferTime;
    private Long scanUserId;
    private String scanUserName;
    private Long handoverUserId;
    private String handoverUserName;
    private String remark;
}
```

- [ ] **Step 6: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/*/entity/
git commit -m "feat: 创建生产模块实体类"
```

---

## Task 5: Mapper 接口创建

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/mapper/ProductionRecordMapper.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/mapper/ProductionProductMapper.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/mapper/ProductionProcessMapper.java`

- [ ] **Step 1: 创建 ProductionRecordMapper**

```java
package com.yigongbao.module.production.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductionRecordMapper extends BaseMapper<ProductionRecordEntity> {
}
```

- [ ] **Step 2: 创建 ProductionProductMapper**

```java
package com.yigongbao.module.production.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductionProductMapper extends BaseMapper<ProductionProductEntity> {
}
```

- [ ] **Step 3: 创建 ProductionProcessMapper**

```java
package com.yigongbao.module.production.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductionProcessMapper extends BaseMapper<ProductionProcessEntity> {
}
```

- [ ] **Step 4: 创建 ProductionProcessProductResultMapper**

```java
package com.yigongbao.module.production.qc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.qc.entity.ProductionProcessProductResultEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductionProcessProductResultMapper extends BaseMapper<ProductionProcessProductResultEntity> {
}
```

- [ ] **Step 5: 创建 ProductionProcessTransferMapper**

```java
package com.yigongbao.module.production.transfer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.production.transfer.entity.ProductionProcessTransferEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductionProcessTransferMapper extends BaseMapper<ProductionProcessTransferEntity> {
}
```

- [ ] **Step 6: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/*/mapper/
git commit -m "feat: 创建生产模块 Mapper 接口"
```

---

## Task 6: 生产流转卡服务层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/IProductionRecordService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/vo/ProductionRecordVO.java`

- [ ] **Step 1: 创建 IProductionRecordService 接口**

```java
package com.yigongbao.module.production.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;

public interface IProductionRecordService extends IService<ProductionRecordEntity> {
    ProductionRecordVO getRecordDetail(Long id);
    ProductionRecordVO getByRecordNo(String recordNo);
    String getQrCodeUrl(Long id);
    IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto);
    /** 下载设计数据包，触发 START_PRINT 并自动创建流转卡（含产品记录和工序记录） */
    void downloadDataPackage(Long designPackageId);
    /** 分配打印机，更新流转卡 printDeviceId/material */
    void assignDevice(Long recordId, AssignDeviceDTO dto);
    /** 查询打印机列表（按加工中心分组，生产员仅看自己绑定的加工中心） */
    List<ProcessingCenterPrintersVO> getPrinters();
    /** 聚合触发：同订单所有活跃流转卡均达到 requiredStatus 时触发 Flow 并回写 order_main */
    void triggerFlowIfAllReach(Long orderId, String requiredStatus, FlowActionEnum action);
}
```

- [ ] **Step 2: 创建 AssignDeviceDTO 和 PrinterVO**

```java
package com.yigongbao.module.production.record.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class AssignDeviceDTO {
    @NotNull(message = "打印机ID不能为空")
    private Long deviceId;
    private String material;
}
```

```java
package com.yigongbao.module.production.record.vo;

import lombok.Data;

@Data
public class PrinterVO {
    private Long id;
    private String deviceNo;
    private String deviceName;
    private Integer status;      // 0=离线，1=可使用，2=使用中
    private String statusName;
}

@Data
public class ProcessingCenterPrintersVO {
    private Long centerId;
    private String centerName;
    private List<PrinterVO> printers;
}
```

- [ ] **Step 3: 创建 ProductionRecordVO 和相关 DTO**

```java
package com.yigongbao.module.production.record.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductionRecordVO {
    private Long id;
    private String recordNo;
    private Integer orderType;
    private String designPackageCode;
    private String productionBatchNo;
    private Integer totalProductCount;
    private Integer qualifiedCount;
    private Integer unqualifiedCount;
    private Integer hasRedoProduct;
    private String status;
    private String qrCodeUrl;
    private LocalDateTime createTime;
    private List<ProductionProductVO> products;
}
```

```java
package com.yigongbao.module.production.product.vo;

import lombok.Data;

@Data
public class ProductionProductVO {
    private Long id;
    private String productNo;
    private String productName;
    private String status;
    private String qcResult;
    private String udiCode;
}
```

```java
package com.yigongbao.module.production.record.dto;

import lombok.Data;

@Data
public class ProductionRecordPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String recordNo;
    private String status;
    private Long processingCenterId;
}
```

- [ ] **Step 4: 创建 ProductionRecordServiceImpl（核心逻辑）**

> **⚠️ 实现前必读**：
> - `CodeRuleConstants` 不存在，编码常量全部使用 `ProductionConstants.PRODUCTION_RECORD_NO` 等
> - `DesignPrintFileMapper.selectByPackageId()` 需确认 design 模块是否已有此方法，若无需先在 design 模块补充
> - `DesignDataPackageEntity` 无 `versionNo` 字段，实现时跳过该字段赋值
> - 创建前必须校验同一订单是否已有活跃流转卡

```java
package com.yigongbao.module.production.record.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.datapackage.entity.DesignDataPackageEntity;
import com.yigongbao.module.design.datapackage.mapper.DesignDataPackageMapper;
import com.yigongbao.module.design.printfile.entity.DesignPrintFileEntity;
import com.yigongbao.module.design.printfile.mapper.DesignPrintFileMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.*;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.production.util.QrCodeUtil;
import com.yigongbao.module.system.codegenerator.service.ICodeGeneratorService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 生产流转卡服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionRecordServiceImpl extends ServiceImpl<ProductionRecordMapper, ProductionRecordEntity>
        implements IProductionRecordService {

    private final ICodeGeneratorService codeGeneratorService;
    private final DesignDataPackageMapper designPackageMapper;
    private final DesignPrintFileMapper printFileMapper;
    private final OrderMainMapper orderMainMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final FlowFacade flowFacade;

    /**
     * 根据订单类型自动创建工序记录
     * 医疗器械：print/wash/cure/clean_dry/pack（5个工序）
     * 非医疗器械：print/pack（2个工序，打印完成后直接包装）
     * 注意：不创建 DESIGN 工序（设计已在 design 模块完成）
     */
    private void createProcessRecords(Long recordId, Integer orderType) {
        List<ProcessTypeEnum> processTypes = new ArrayList<>();
        processTypes.add(ProcessTypeEnum.PRINT);
        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(orderType)) {
            processTypes.addAll(Arrays.asList(ProcessTypeEnum.WASH, ProcessTypeEnum.CURE, ProcessTypeEnum.CLEAN_DRY));
        }
        processTypes.add(ProcessTypeEnum.PACK);  // 所有订单类型都需要包装工序
        for (int i = 0; i < processTypes.size(); i++) {
            ProcessTypeEnum pt = processTypes.get(i);
            ProductionProcessEntity process = new ProductionProcessEntity();
            process.setProductionRecordId(recordId);
            process.setProcessType(pt.getCode());
            process.setProcessName(pt.getDesc());
            process.setProcessOrder(i + 1);
            process.setStatus(ProcessStatusEnum.PENDING.getCode());
            processMapper.insert(process);
        }
        log.info("自动创建工序记录: recordId={}, orderType={}, processCount={}", recordId, orderType, processTypes.size());
    }

    @Override
    public ProductionRecordVO getRecordDetail(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        ProductionRecordVO vo = new ProductionRecordVO();
        vo.setId(record.getId());
        vo.setRecordNo(record.getRecordNo());
        vo.setStatus(record.getStatus());
        vo.setQrCodeUrl(record.getQrCodeUrl());
        return vo;
    }

    @Override
    public ProductionRecordVO getByRecordNo(String recordNo) {
        ProductionRecordEntity record = getOne(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getRecordNo, recordNo));
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return getRecordDetail(record.getId());
    }

    @Override
    public String getQrCodeUrl(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return record.getQrCodeUrl();
    }

    @Override
    public IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto) {
        // 简单分页，按需扩展查询条件
        Page<ProductionRecordEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (dto.getStatus() != null) {
            wrapper.eq(ProductionRecordEntity::getStatus, dto.getStatus());
        }
        if (dto.getRecordNo() != null) {
            wrapper.like(ProductionRecordEntity::getRecordNo, dto.getRecordNo());
        }
        Page<ProductionRecordEntity> result = page(page, wrapper);
        return result.convert(e -> getRecordDetail(e.getId()));
    }

    /**
     * 下载设计数据包，触发订单状态流转 design_approved → pending_print，并自动创建流转卡
     * 幂等保护：仅当订单当前处于 design_approved 阶段时触发，否则直接返回
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadDataPackage(Long designPackageId) {
        DesignDataPackageEntity designPackage = designPackageMapper.selectById(designPackageId);
        if (designPackage == null) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        OrderMainEntity order = orderMainMapper.selectById(designPackage.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 幂等：已推进过则跳过
        List<String> availableActions = flowFacade.getAvailableActions(order.getId());
        if (!availableActions.contains(FlowActionEnum.START_PRINT.name())) {
            log.info("下载数据包幂等跳过，订单已推进: orderId={}, designPackageId={}", order.getId(), designPackageId);
            return;
        }
        triggerFlowAndSync(order.getId(), FlowActionEnum.START_PRINT);

        // 自动创建流转卡（printDeviceId 留空，后续通过 assign-device 接口分配）
        String recordNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_RECORD_NO);
        String batchNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO);
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setRecordNo(recordNo);
        record.setOrderId(order.getId());
        record.setOrderCode(order.getOrderCode());
        record.setOrderType(order.getOrderType());
        record.setDesignPackageId(designPackage.getId());
        record.setDesignPackageCode(designPackage.getPackageCode());
        record.setProductionBatchNo(batchNo);
        record.setStatus(RecordStatusEnum.PENDING_PRINT.getCode());
        save(record);

        List<DesignPrintFileEntity> printFiles = printFileMapper.selectByPackageId(designPackage.getId());
        int totalCount = 0;
        for (DesignPrintFileEntity file : printFiles) {
            int printCount = file.getPrintCount() != null ? file.getPrintCount() : 1;
            for (int i = 0; i < printCount; i++) {
                ProductionProductEntity product = new ProductionProductEntity();
                product.setProductionRecordId(record.getId());
                product.setPrintFileId(file.getId());
                product.setProductNo(codeGeneratorService.generate(ProductionConstants.PRODUCT_NO));
                product.setProductName(file.getFileName());
                product.setFileName(file.getFileName());
                product.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
                productMapper.insert(product);
                totalCount++;
            }
        }
        record.setTotalProductCount(totalCount);
        createProcessRecords(record.getId(), order.getOrderType());
        String qrContent = String.format("RECORD:%s|BATCH:%s", recordNo, batchNo);
        record.setQrCodeUrl("data:image/png;base64," + QrCodeUtil.generateQrCodeBase64(qrContent));
        updateById(record);
        log.info("下载设计数据包，自动创建流转卡: orderId={}, designPackageId={}, recordId={}, productCount={}",
                order.getId(), designPackageId, record.getId(), totalCount);
    }

    /**
     * 分配打印机，更新流转卡 printDeviceId/printDeviceCode/printDeviceName/material
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDevice(Long recordId, AssignDeviceDTO dto) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        DeviceEntity device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND);
        }
        if (!StatusConstants.YES.equals(device.getConnectionStatus())) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_OFFLINE);
        }
        if (StatusConstants.YES.equals(device.getState())) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_BUSY);
        }
        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        if (dto.getMaterial() != null) {
            record.setMaterial(dto.getMaterial());
        }
        updateById(record);
        log.info("分配打印机: recordId={}, deviceId={}, deviceNo={}", recordId, device.getId(), device.getDeviceId());
    }

    /**
     * 查询打印机列表（按加工中心分组）
     * 权限控制：生产员只能看到自己绑定的加工中心下的设备
     */
    @Override
    public List<ProcessingCenterPrintersVO> getPrinters() {
        // 1. 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(userId);
        
        // 2. 查询设备列表（根据角色权限过滤）
        List<DeviceEntity> devices;
        if (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(currentUser.getRoleCode())) {
            // 生产员：只查询自己绑定的加工中心下的设备
            if (currentUser.getCenterId() == null) {
                log.warn("生产员未绑定加工中心: userId={}", userId);
                return Collections.emptyList();
            }
            devices = deviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceType, ProductionConstants.DEVICE_TYPE_PRINTER)
                .eq(DeviceEntity::getCenterId, currentUser.getCenterId())
                .eq(DeviceEntity::getIsDeleted, StatusConstants.NO));
        } else {
            // 其他角色：查询所有打印机
            devices = deviceMapper.selectByType(ProductionConstants.DEVICE_TYPE_PRINTER);
        }
        
        // 3. 转换为 PrinterVO 并按加工中心分组
        Map<Long, List<PrinterVO>> centerPrintersMap = devices.stream()
            .map(d -> {
                PrinterVO vo = new PrinterVO();
                vo.setId(d.getId());
                vo.setDeviceNo(d.getDeviceId());
                vo.setDeviceName(d.getDeviceName());
                int status = !StatusConstants.YES.equals(d.getConnectionStatus()) ? 0
                        : StatusConstants.YES.equals(d.getState()) ? 2 : 1;
                vo.setStatus(status);
                vo.setStatusName(status == 0 ? "离线" : status == 1 ? "可使用" : "使用中");
                return new Object[]{d.getCenterId(), d.getCenterName(), vo};
            })
            .collect(Collectors.groupingBy(
                arr -> (Long) arr[0],
                Collectors.mapping(arr -> (PrinterVO) arr[2], Collectors.toList())
            ));
        
        // 4. 构建返回结果（保留加工中心名称）
        Map<Long, String> centerNameMap = devices.stream()
            .collect(Collectors.toMap(
                DeviceEntity::getCenterId,
                DeviceEntity::getCenterName,
                (v1, v2) -> v1
            ));
        
        return centerPrintersMap.entrySet().stream()
            .map(entry -> {
                ProcessingCenterPrintersVO vo = new ProcessingCenterPrintersVO();
                vo.setCenterId(entry.getKey());
                vo.setCenterName(centerNameMap.get(entry.getKey()));
                vo.setPrinters(entry.getValue());
                return vo;
            })
            .sorted(Comparator.comparing(ProcessingCenterPrintersVO::getCenterId))
            .collect(Collectors.toList());
    }

    /**
     * 聚合触发：当同一订单下所有活跃流转卡都达到 requiredStatus 时，才触发 Flow 状态流转并回写 order_main
     * 活跃流转卡 = status 不在 [print_failed, abandoned] 中的流转卡
     *
     * @param orderId        订单ID
     * @param requiredStatus 需要所有活跃流转卡都达到的状态
     * @param action         满足条件时触发的 Flow 动作
     */
    public void triggerFlowIfAllReach(Long orderId, String requiredStatus, FlowActionEnum action) {
        long totalActive = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .notIn(ProductionRecordEntity::getStatus,
                        RecordStatusEnum.PRINT_FAILED.getCode(),
                        RecordStatusEnum.ABANDONED.getCode()));
        long reachedCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .eq(ProductionRecordEntity::getStatus, requiredStatus));
        if (totalActive > 0 && totalActive == reachedCount) {
            triggerFlowAndSync(orderId, action);
            log.info("聚合条件满足，触发Flow: orderId={}, requiredStatus={}, action=", orderId, requiredStatus, action);
        } else {
            log.info("聚合条件未满足，暂不触发Flow: orderId={}, requiredStatus={}, active={}, reached={}",
                    orderId, requiredStatus, totalActive, reachedCount);
        }
    }

    /**
     * 调用 FlowFacade 执行状态流转，并将结果回写到 order_main 表
     */
    private void triggerFlowAndSync(Long orderId, FlowActionEnum action) {
        FlowOperator operator = FlowOperator.of(StpUtil.getLoginIdAsLong(),
                StpUtil.getSession().get("username", "system").toString());
        TransitionResult result = flowFacade.executeFlow(orderId, action, operator);
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setPhase(result.getTargetPhase());
        order.setStatus(result.getFinalStatus());
        orderMainMapper.updateById(order);
        log.info("Flow状态流转完成: orderId={}, action={}, targetPhase={}, targetStatus={}",
                orderId, action, result.getTargetPhase(), result.getFinalStatus());
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/
git commit -m "feat: 创建生产流转卡服务层"
```

---

## Task 7: 工序操作服务层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/IProductionProcessService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/dto/FillProcessDTO.java`

- [ ] **Step 1: 创建 IProductionProcessService 接口**

```java
package com.yigongbao.module.production.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.dto.FillProcessDTO;

public interface IProductionProcessService extends IService<ProductionProcessEntity> {
    void fillProcess(Long processId, FillProcessDTO dto);
    void transferToNext(Long recordId, String fromProcess, String toProcess);
}
```

- [ ] **Step 2: 创建 FillProcessDTO**

```java
package com.yigongbao.module.production.process.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class FillProcessDTO {
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    private String processParams;
    private Integer hasRedo;
    private String redoRemark;
}
```

- [ ] **Step 3: 创建 ProductionProcessServiceImpl**

```java
package com.yigongbao.module.production.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.transfer.entity.ProductionProcessTransferEntity;
import com.yigongbao.module.production.transfer.mapper.ProductionProcessTransferMapper;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序操作服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionProcessServiceImpl extends ServiceImpl<ProductionProcessMapper, ProductionProcessEntity>
        implements IProductionProcessService {

    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessTransferMapper transferMapper;
    private final OrderMainMapper orderMainMapper;
    private final FlowFacade flowFacade;
    private final IProductionRecordService recordService;

    /**
     * 填写工序信息，完成后检查是否有 redo 产品在此工序重做，自动恢复为 in_process
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillProcess(Long processId, FillProcessDTO dto) {
        ProductionProcessEntity process = getById(processId);
        if (process == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        process.setDeviceId(dto.getDeviceId());
        process.setProcessParams(dto.getProcessParams());
        process.setHasRedo(dto.getHasRedo());
        process.setRedoRemark(dto.getRedoRemark());
        process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
        updateById(process);

        // redo 产品重做完成后自动恢复为 in_process
        List<ProductionProductEntity> redoProducts = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                        .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode())
                        .eq(ProductionProductEntity::getRedoProcessType, process.getProcessType()));
        if (!redoProducts.isEmpty()) {
            redoProducts.forEach(p -> {
                p.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
                p.setRedoProcessType(null);
                productMapper.updateById(p);
            });
            // 清除流转卡的 has_redo_product 标志（若所有 redo 产品都已恢复）
            long remainRedo = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                    .eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
                    .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode()));
            if (remainRedo == 0) {
                ProductionRecordEntity record = new ProductionRecordEntity();
                record.setId(process.getProductionRecordId());
                record.setHasRedoProduct(0);
                recordMapper.updateById(record);
            }
            log.info("redo产品重做完成，状态恢复为in_process: processId={}, processType={}, productCount={}",
                    processId, process.getProcessType(), redoProducts.size());
        }

        log.info("填写工序信息: processId={}, deviceId=", processId, dto.getDeviceId());
    }

    /**
     * 工序流转：记录流转日志，更新流转卡状态，并聚合判断是否触发 Flow
     * 聚合规则：
     *   print 完成 → 流转卡状态改为 print_completed → 若同订单所有活跃卡均 print_completed → COMPLETE_PRINT
     *   clean_dry 完成 → 流转卡状态改为 qc_in_progress → 若同订单所有活跃卡均 qc_in_progress → COMPLETE_POST_PROCESSING
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToNext(Long recordId, String fromProcess, String toProcess) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        ProductionProcessTransferEntity transfer = new ProductionProcessTransferEntity();
        transfer.setProductionRecordId(recordId);
        transfer.setFromProcessType(fromProcess);
        transfer.setToProcessType(toProcess);
        transfer.setTransferTime(LocalDateTime.now());
        transfer.setScanUserId(StpUtil.getLoginIdAsLong());
        transfer.setScanUserName(StpUtil.getSession().get("username", "").toString());
        transferMapper.insert(transfer);

        // 更新流转卡内部状态，并聚合判断是否触发 Flow
        if (ProcessTypeEnum.PRINT.getCode().equals(fromProcess)) {
            record.setStatus(RecordStatusEnum.PRINT_COMPLETED.getCode());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    RecordStatusEnum.PRINT_COMPLETED.getCode(), FlowActionEnum.COMPLETE_PRINT);
        } else if (ProcessTypeEnum.WASH.getCode().equals(fromProcess)
                || ProcessTypeEnum.CURE.getCode().equals(fromProcess)) {
            // 中间后处理工序：更新流转卡为后处理中，记录当前工序供前端展示
            record.setStatus(RecordStatusEnum.POST_PROCESSING.getCode());
            record.setCurrentProcess(toProcess);
            recordMapper.updateById(record);
        } else if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(fromProcess)) {
            record.setStatus(RecordStatusEnum.QC_IN_PROGRESS.getCode());
            record.setCurrentProcess(null);
            recordMapper.updateById(record);
            recordService.triggerFlowIfAllReach(record.getOrderId(),
                    RecordStatusEnum.QC_IN_PROGRESS.getCode(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        }
        // 注意：PACK 工序完成不在此处处理，统一由 ProductionPackServiceImpl.transferToWarehouse() 负责

        log.info("工序流转: recordId={}, recordNo={}, {} -> {}, scanUser={}",
                recordId, record.getRecordNo(), fromProcess, toProcess, transfer.getScanUserName());
    }

}
```

> **⚠️ 注意**：`ProductionProcessServiceImpl` 需注入 `IProductionRecordService`（而非 `ProductionRecordMapper`）以调用 `triggerFlowIfAllReach()`。同时需注入 `ProductionRecordMapper` 用于更新流转卡状态。

- [ ] **Step 4: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/
git commit -m "feat: 创建工序操作服务层"
```

---

## Task 8: 质检服务层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/IProductionQcService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/service/impl/ProductionQcServiceImpl.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/dto/QcProductDTO.java`

- [ ] **Step 1: 创建 IProductionQcService 接口**

```java
package com.yigongbao.module.production.qc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;

import java.util.List;

public interface IProductionQcService {
    void markProductPass(Long productId);
    void markProductRedo(Long productId, String reason);
    void assignRedoProcess(Long productId, String processType);
    void transferToPacking(Long recordId);
    List<ProductionProductVO> listProductsByRecordId(Long recordId);
    IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto);
    IPage<ProductionProductVO> listRedoProducts(ProductionRedoPageDTO dto);
}
```

```java
package com.yigongbao.module.production.qc.dto;

import lombok.Data;

@Data
public class ProductionQcPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String status;
}
```

```java
package com.yigongbao.module.production.qc.dto;

import lombok.Data;

@Data
public class ProductionRedoPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Long recordId;
}
```

- [ ] **Step 2: 创建 QcProductDTO**

```java
package com.yigongbao.module.production.qc.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class QcProductDTO {
    @NotNull(message = "产品ID不能为空")
    private Long productId;
    private String result;
    private String remark;
}
```

- [ ] **Step 3: 创建 ProductionQcServiceImpl**

```java
package com.yigongbao.module.production.qc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.qc.dto.ProductionRedoPageDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.system.codegenerator.service.ICodeGeneratorService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 质检服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionQcServiceImpl implements IProductionQcService {

    private final ProductionProductMapper productMapper;
    private final ProductionRecordMapper recordMapper;
    private final OrderMainMapper orderMainMapper;
    private final ICodeGeneratorService codeGeneratorService;
    private final FlowFacade flowFacade;
    private final IProductionRecordService recordService;

    /**
     * 标记产品质检合格；医疗器械订单同步生成 UDI 码；同步更新流转卡合格计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductPass(Long productId) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        ProductionRecordEntity record = recordMapper.selectById(product.getProductionRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        product.setStatus(ProductStatusEnum.PASS.getCode());
        product.setQcResult(QcResultEnum.PASS.getCode());
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());

        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
            String udiCode = codeGeneratorService.generate(ProductionConstants.UDI_CODE);
            product.setUdiCode(udiCode);
            product.setUdiGenerateTime(LocalDateTime.now());
            log.info("生成UDI码: productId={}, productNo={}, udiCode={}", productId, product.getProductNo(), udiCode);
        }
        productMapper.updateById(product);

        // 回写流转卡合格计数
        record.setQualifiedCount(record.getQualifiedCount() + 1);
        recordMapper.updateById(record);

        log.info("标记产品质检合格: productId={}, productNo={}, orderType={}, hasUDI={}",
                productId, product.getProductNo(), record.getOrderType(), product.getUdiCode() != null);
    }

    /**
     * 标记产品质检不合格（redo），同步更新流转卡 has_redo_product 标志和不合格计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductRedo(Long productId, String reason) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        product.setStatus(ProductStatusEnum.REDO.getCode());
        product.setQcResult(QcResultEnum.REDO.getCode());
        product.setQcRemark(reason);
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());
        productMapper.updateById(product);

        // 回写流转卡不合格计数和 has_redo_product 标志
        ProductionRecordEntity record = recordMapper.selectById(product.getProductionRecordId());
        if (record != null) {
            record.setUnqualifiedCount(record.getUnqualifiedCount() + 1);
            record.setHasRedoProduct(1);
            recordMapper.updateById(record);
        }

        log.info("标记产品质检不合格: productId={}, productNo={}, reason={}", productId, product.getProductNo(), reason);
    }

    /**
     * 指定 redo 产品的重做工序
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRedoProcess(Long productId, String processType) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        product.setRedoProcessType(processType);
        productMapper.updateById(product);
        log.info("指定产品重做工序: productId={}, processType={}", productId, processType);
    }

    /**
     * 质检完成，流转到包装
     * 校验本张流转卡所有产品均已 pass → 更新流转卡状态为 packing
     * 聚合判断：若同订单所有活跃流转卡均为 packing → 触发 Flow QC_PASS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToPacking(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        long notPassCount = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .ne(ProductionProductEntity::getStatus, ProductStatusEnum.PASS.getCode()));
        if (notPassCount > 0) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_ALL_PASS);
        }

        // 更新流转卡状态为 packing
        record.setStatus(RecordStatusEnum.PACKING.getCode());
        recordMapper.updateById(record);

        // 聚合触发：所有活跃流转卡均为 packing 时才触发 Flow
        recordService.triggerFlowIfAllReach(record.getOrderId(),
                RecordStatusEnum.PACKING.getCode(), FlowActionEnum.QC_PASS);

        log.info("质检完成，流转到包装: recordId={}, recordNo={}, orderId={}",
                recordId, record.getRecordNo(), record.getOrderId());
    }

    @Override
    public List<ProductionProductVO> listProductsByRecordId(Long recordId) {
        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId)
                        .orderByAsc(ProductionProductEntity::getId));
        return products.stream().map(p -> BeanUtil.copyProperties(p, ProductionProductVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto) {
        Page<ProductionRecordEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getStatus, RecordStatusEnum.QC_IN_PROGRESS.getCode());
        Page<ProductionRecordEntity> result = recordMapper.selectPage(page, wrapper);
        return result.convert(e -> BeanUtil.copyProperties(e, ProductionRecordVO.class));
    }

    @Override
    public IPage<ProductionProductVO> listRedoProducts(ProductionRedoPageDTO dto) {
        Page<ProductionProductEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode());
        if (dto.getRecordId() != null) {
            wrapper.eq(ProductionProductEntity::getProductionRecordId, dto.getRecordId());
        }
        Page<ProductionProductEntity> result = productMapper.selectPage(page, wrapper);
        return result.convert(p -> BeanUtil.copyProperties(p, ProductionProductVO.class));
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/
git commit -m "feat: 创建质检服务层"
```

---

## Task 9: Controller 层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/controller/ProductionRecordController.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProductionProcessController.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/qc/controller/ProductionQcController.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProcessConfigController.java`

- [ ] **Step 1: 创建 ProductionRecordController**

```java
package com.yigongbao.module.production.record.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.production.record.vo.PrinterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@Tag(name = "生产流转卡管理")
@RestController
@RequestMapping("/production/record")
@RequiredArgsConstructor
public class ProductionRecordController {

    private final IProductionRecordService recordService;

    @Operation(summary = "分页查询生产列表")
    @PostMapping("/list")
    public Result<IPage<ProductionRecordVO>> list(@RequestBody ProductionRecordPageDTO dto) {
        IPage<ProductionRecordVO> page = recordService.pageRecords(dto);
        return Result.success(page);
    }

    @Operation(summary = "扫码查询流转卡")
    @PostMapping("/scan")
    public Result<ProductionRecordVO> scanRecord(@RequestParam String recordNo) {
        ProductionRecordVO vo = recordService.getByRecordNo(recordNo);
        return Result.success(vo);
    }

    @Operation(summary = "查询流转卡详情")
    @GetMapping("/{id}")
    public Result<ProductionRecordVO> getRecordDetail(@PathVariable Long id) {
        ProductionRecordVO vo = recordService.getRecordDetail(id);
        return Result.success(vo);
    }

    @Operation(summary = "下载设计数据包")
    @PostMapping("/{designPackageId}/download-package")
    public Result<Void> downloadDataPackage(@PathVariable Long designPackageId) {
        recordService.downloadDataPackage(designPackageId);
        return Result.success();
    }

    @Operation(summary = "获取打印机列表（按加工中心分组）")
    @GetMapping("/printers")
    public Result<List<ProcessingCenterPrintersVO>> getPrinters() {
        return Result.success(recordService.getPrinters());
    }

    @Operation(summary = "分配打印机")
    @PostMapping("/assign-device/{recordId}")
    public Result<Void> assignDevice(@PathVariable Long recordId, @Valid @RequestBody AssignDeviceDTO dto) {
        recordService.assignDevice(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "获取流转卡二维码")
    @GetMapping("/{id}/qr-code")
    public Result<String> getQrCode(@PathVariable Long id) {
        String qrCodeUrl = recordService.getQrCodeUrl(id);
        return Result.success(qrCodeUrl);
    }
}
```

- [ ] **Step 2: 创建 ProductionProcessController**

```java
package com.yigongbao.module.production.process.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.process.service.IProductionProcessService;
import com.yigongbao.module.production.process.dto.FillProcessDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@Tag(name = "工序操作管理")
@RestController
@RequestMapping("/production/process")
@RequiredArgsConstructor
public class ProductionProcessController {

    private final IProductionProcessService processService;

    @Operation(summary = "填写工序信息")
    @PutMapping("/{id}/fill")
    public Result<Void> fillProcess(@PathVariable Long id, @Valid @RequestBody FillProcessDTO dto) {
        processService.fillProcess(id, dto);
        return Result.success();
    }

    @Operation(summary = "工序流转")
    @PostMapping("/{recordId}/transfer")
    public Result<Void> transferToNext(@PathVariable Long recordId, 
                                       @RequestParam String fromProcess,
                                       @RequestParam String toProcess) {
        processService.transferToNext(recordId, fromProcess, toProcess);
        return Result.success();
    }
}
```

- [ ] **Step 3: 创建 ProductionQcController**

```java
package com.yigongbao.module.production.qc.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "质检管理")
@RestController
@RequestMapping("/production/qc")
@RequiredArgsConstructor
public class ProductionQcController {

    private final IProductionQcService qcService;

    @Operation(summary = "质检列表")
    @PostMapping("/list")
    public Result<IPage<ProductionRecordVO>> list(@RequestBody ProductionQcPageDTO dto) {
        IPage<ProductionRecordVO> page = qcService.listQcRecords(dto);
        return Result.success(page);
    }

    @Operation(summary = "获取待质检产品列表")
    @GetMapping("/{recordId}/products")
    public Result<List<ProductionProductVO>> getProducts(@PathVariable Long recordId) {
        List<ProductionProductVO> products = qcService.listProductsByRecordId(recordId);
        return Result.success(products);
    }

    @Operation(summary = "标记产品质检合格")
    @PostMapping("/product/{productId}/pass")
    public Result<Void> markProductPass(@PathVariable Long productId) {
        qcService.markProductPass(productId);
        return Result.success();
    }

    @Operation(summary = "标记产品质检不合格")
    @PostMapping("/product/{productId}/redo")
    public Result<Void> markProductRedo(@PathVariable Long productId, @RequestParam String reason) {
        qcService.markProductRedo(productId, reason);
        return Result.success();
    }

    @Operation(summary = "质检完成，流转到包装")
    @PostMapping("/{recordId}/transfer-to-pack")
    public Result<Void> transferToPacking(@PathVariable Long recordId) {
        qcService.transferToPacking(recordId);
        return Result.success();
    }

    @Operation(summary = "redo产品列表（生产管理员）")
    @PostMapping("/redo/list")
    public Result<IPage<ProductionProductVO>> listRedoProducts(@RequestBody ProductionRedoPageDTO dto) {
        IPage<ProductionProductVO> page = qcService.listRedoProducts(dto);
        return Result.success(page);
    }

    @Operation(summary = "指定redo重做工序")
    @PostMapping("/redo/{productId}/assign")
    public Result<Void> assignRedoProcess(@PathVariable Long productId, @RequestParam String processType) {
        qcService.assignRedoProcess(productId, processType);
        return Result.success();
    }
}
```

- [ ] **Step 4: 创建 ProcessConfigController（工序步骤定义 + 参数配置字典）**

```java
package com.yigongbao.module.production.process.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.system.config.service.IConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工序配置接口
 * 提供工序步骤定义和参数字段配置，供前端动态渲染流程导航和参数表单
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Tag(name = "工序配置")
@RestController
@RequestMapping("/production/process-config")
@RequiredArgsConstructor
public class ProcessConfigController {

    private final IProcessConfigService processConfigService;  // JSON解析逻辑移至Service层

    /** 工序步骤定义（固定，不依赖数据库，过滤掉 DESIGN 工序） */
    private static final List<Map<String, Object>> PROCESS_STEPS = Arrays.stream(ProcessTypeEnum.values())
            .filter(e -> e != ProcessTypeEnum.DESIGN)
            .map(e -> {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("processType", e.getCode());
                step.put("processName", e.getDesc());
                step.put("processSeq", e.getOrder() - 1);
                return step;
            })
            .toList();

    @Operation(summary = "获取工序步骤定义", description = "返回标准工序列表，前端用于渲染流程导航栏")
    @GetMapping("/steps")
    public Result<List<Map<String, Object>>> getProcessSteps() {
        return Result.success(PROCESS_STEPS);
    }

    @Operation(summary = "获取工序参数配置字典", description = "从sys_config读取各工序参数字段定义，前端据此动态渲染参数表单")
    @GetMapping("/params")
    public Result<Object> getProcessParamsConfig() {
        String json = configService.getConfigValue(
            SystemConfigKeyEnum.PRODUCTION_PROCESS_PARAMS_CONFIG.getKey());
        try {
            return Result.success(objectMapper.readValue(json, new TypeReference<Object>() {}));
        } catch (Exception e) {
            return Result.success(json);  // 解析失败返回原始JSON
        }
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/*/controller/
git commit -m "feat: 创建生产模块 Controller 层"
```

---

## Task 10: Flow 引擎集成

> **此任务已合并到 Task 6/7/8/8.5 的 ServiceImpl 中**，不再单独实现。
>
> 各服务层已内置 `triggerFlow(orderId, action)` 私有方法，统一处理 FlowFacade 调用 + order_main 回写。
>
> **FlowAction 映射关系（已在各 ServiceImpl 中实现）：**
>
> | 触发时机 | FlowActionEnum | 调用位置 |
> |---|---|---|
> | 下载设计数据包（幂等） | `START_PRINT` | `ProductionRecordServiceImpl.downloadDataPackage()` |
> | 打印完成（WebSocket） | `COMPLETE_PRINT` | `DeviceStatusListener.handlePrintComplete()` + 聚合 |
> | 工序流转：print → 下一工序 | `COMPLETE_PRINT` | `ProductionProcessServiceImpl.transferToNext()` + 聚合 |
> | 工序流转：clean_dry → 质检 | `COMPLETE_POST_PROCESSING` | `ProductionProcessServiceImpl.transferToNext()` |
> | 质检全部合格，流转到包装 | `QC_PASS` | `ProductionQcServiceImpl.transferToPacking()` |
> | 包装完成，流转到入库 | `COMPLETE_WAREHOUSE_IN` | `ProductionPackServiceImpl.transferToWarehouse()` |
>
> **⚠️ 注意**：`START_PRINT` 在 Flow 模块中触发阶段推进：DESIGN 阶段的 DESIGN_REVIEW_PASSED 状态 → PRINT 阶段的 PENDING_PRINT 状态。FlowPhaseTransitionRules 已配置此规则。

---

## Task 11: WebSocket 设备状态监听集成

**说明**：本任务不创建新的 WebSocket 端点，而是在现有的 `com.yigongbao.module.basic.device.websocket.DeviceWebSocketHandler` 中增加对生产流转卡状态的同步更新逻辑。

**Files:**
- Modify: `yigongbao-module-basic/src/main/java/com/yigongbao/module/basic/device/websocket/DeviceWebSocketHandler.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/DeviceStatusListener.java`

- [ ] **Step 1: 创建 DeviceStatusListener 监听设备状态变更事件**

```java
package com.yigongbao.module.production.listener;

import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 设备状态监听器
 * 监听打印设备状态变更，通过FlowFacade触发订单状态流转
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceStatusListener {

    private final ProductionRecordMapper recordMapper;
    private final OrderMainMapper orderMainMapper;
    private final FlowFacade flowFacade;
    private final IProductionRecordService recordService;  // 用于调用 triggerFlowIfAllReach

    /**
     * 监听设备状态变更事件
     * 当打印设备状态从 0（空闲）变为 1（占用）时，触发"打印开始"状态流转
     * 当打印设备状态从 1（占用）变为 0（空闲）时，触发"打印完成"状态流转
     */
    @EventListener
    public void onDeviceStateChange(DeviceStateChangeEvent event) {
        Long deviceId = event.getDeviceId();
        Integer newState = event.getNewState();
        Integer oldState = event.getOldState();
        
        // 查询该设备当前分配的生产流转卡
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductionRecordEntity::getPrintDeviceId, deviceId)
               .in(ProductionRecordEntity::getStatus, 
                   RecordStatusEnum.PENDING_PRINT.getCode(), 
                   RecordStatusEnum.PRINTING.getCode());
        ProductionRecordEntity record = recordMapper.selectOne(wrapper);
        
        if (record == null) {
            log.debug("设备状态变更，但未找到关联的生产流转卡: deviceId={}", deviceId);
            return;
        }
        
        // 打印开始：空闲 → 占用
        if (ProductionConstants.DEVICE_STATE_IDLE.equals(oldState) 
                && ProductionConstants.DEVICE_STATE_BUSY.equals(newState)) {
            handlePrintStart(record);
        }
        // 打印完成：占用 → 空闲
        else if (ProductionConstants.DEVICE_STATE_BUSY.equals(oldState) 
                && ProductionConstants.DEVICE_STATE_IDLE.equals(newState)) {
            handlePrintComplete(record);
        }
    }

    /**
     * 处理打印开始：更新流转卡状态为 printing
     * 注意：START_PRINT 是订单级操作（DESIGN_REVIEW_PASSED → PENDING_PRINT），由 downloadDataPackage 触发
     * 打印开始时订单已在 PENDING_PRINT 状态，此处仅更新流转卡内部状态，不触发 Flow
     */
    private void handlePrintStart(ProductionRecordEntity record) {
        record.setStatus(RecordStatusEnum.PRINTING.getCode());
        recordMapper.updateById(record);
        log.info("设备状态变更触发打印开始，更新流转卡状态: recordId={}, recordNo={}, deviceId={}",
            record.getId(), record.getRecordNo(), record.getPrintDeviceId());
    }

    /**
     * 处理打印完成：更新流转卡状态为 print_completed
     * 聚合判断：若同订单所有活跃流转卡均为 print_completed → 触发 Flow COMPLETE_PRINT
     */
    private void handlePrintComplete(ProductionRecordEntity record) {
        record.setStatus(RecordStatusEnum.PRINT_COMPLETED.getCode());
        recordMapper.updateById(record);
        recordService.triggerFlowIfAllReach(record.getOrderId(),
                RecordStatusEnum.PRINT_COMPLETED.getCode(), FlowActionEnum.COMPLETE_PRINT);
        log.info("设备状态变更触发打印完成，更新流转卡状态: recordId={}, recordNo={}, deviceId={}",
            record.getId(), record.getRecordNo(), record.getPrintDeviceId());
    }
}
```

- [ ] **Step 2: 创建 DeviceStateChangeEvent 事件类**

```java
package com.yigongbao.module.production.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 设备状态变更事件
 */
@Getter
public class DeviceStateChangeEvent extends ApplicationEvent {
    private final Long deviceId;
    private final Integer oldState;
    private final Integer newState;

    public DeviceStateChangeEvent(Object source, Long deviceId, Integer oldState, Integer newState) {
        super(source);
        this.deviceId = deviceId;
        this.oldState = oldState;
        this.newState = newState;
    }
}
```

- [ ] **Step 3: 在 DeviceWebSocketHandler 中发布设备状态变更事件**

在 `yigongbao-module-basic` 的 `DeviceWebSocketHandler.handleDeviceStatusUpdate()` 方法中，当检测到设备状态变更时，发布 `DeviceStateChangeEvent` 事件：

```java
// 在 DeviceWebSocketHandler 中添加
@Autowired
private ApplicationEventPublisher eventPublisher;

// 在设备状态更新逻辑中添加事件发布
if (!oldState.equals(newState)) {
    eventPublisher.publishEvent(new DeviceStateChangeEvent(this, deviceId, oldState, newState));
}
```

- [ ] **Step 4: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/
git commit -m "feat: 集成设备状态监听，自动同步打印状态"
```

---

## Task 12: 单元测试

**Files:**
- Create: `yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`
- Create: `yigongbao-module-production/src/test/resources/application-test.yml`

- [ ] **Step 1: 创建测试配置文件**

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
  h2:
    console:
      enabled: true

satoken:
  interceptor:
    enable: false

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 2: 创建 ProductionRecordServiceImplTest**

```java
package com.yigongbao.module.production.record.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionRecordServiceImplTest {

    @Mock
    private ProductionRecordMapper recordMapper;

    @InjectMocks
    private ProductionRecordServiceImpl recordService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(recordService, recordMapper);
    }

    @Test
    void testDownloadDataPackage_AutoCreatesRecord() {
        // 验证 downloadDataPackage 触发 Flow 并自动创建流转卡
        // 具体 mock 逻辑由实现时补充
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-production -Dtest=ProductionRecordServiceImplTest
```

Expected: Tests run: 1, Failures: 0

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/test/
git commit -m "test: 添加生产模块单元测试"
```

---

## 执行计划总结

本实施计划包含 12 个任务，覆盖生产管理模块的完整实现：

1. ✅ 模块脚手架搭建
2. ✅ 数据库表结构创建（6张核心表）
3. ✅ 枚举类和常量创建
4. ✅ 实体类创建
5. ✅ Mapper 接口创建
6. ✅ 生产流转卡服务层
7. ✅ 工序操作服务层
8. ✅ 质检服务层
9. ✅ Controller 层
10. ✅ Flow 引擎集成
11. ✅ WebSocket 设备状态监听
12. ✅ 单元测试

**关键特性**：
- 批次级别管理（流转卡）+ 产品级别追溯（产品编号/UDI码）
- 医疗器械/非医疗器械订单类型分叉处理
- Flow 引擎驱动状态流转
- WebSocket 自动监听设备状态
- 二维码扫码流转
- 质检 redo 机制

**下一步**：选择执行方式

---

## 执行方式选择

计划已完成并保存至 `docs/superpowers/plans/2026-05-27-production-module.md`。

**两种执行方式：**

**1. Subagent-Driven（推荐）** - 每个任务派发独立子代理，任务间审查，快速迭代

**2. Inline Execution** - 在当前会话中使用 executing-plans 技能执行，批量执行带检查点

**请选择执行方式？**

---

## Task 3.5: 二维码生成工具类

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/util/QrCodeUtil.java`

- [ ] **Step 1: 创建QrCodeUtil工具类**

```java
package com.yigongbao.module.production.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 二维码生成工具类
 * 使用ZXing库生成二维码图片
 */
@Slf4j
public class QrCodeUtil {
    
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    
    /**
     * 生成二维码（Base64格式）
     * 
     * @param content 二维码内容
     * @return Base64编码的PNG图片字符串
     */
    public static String generateQrCodeBase64(String content) {
        return generateQrCodeBase64(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    /**
     * 生成二维码（Base64格式，自定义尺寸）
     * 
     * @param content 二维码内容
     * @param width 宽度（像素）
     * @param height 高度（像素）
     * @return Base64编码的PNG图片字符串
     */
    public static String generateQrCodeBase64(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            log.debug("生成二维码成功: contentLength={}, imageSize={}bytes", content.length(), outputStream.size());
            
            return base64;
        } catch (Exception e) {
            log.error("生成二维码失败: content={}", content, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);  // 800+8，见 Task 3 Step 7
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/util/
git commit -m "feat: 创建二维码生成工具类"
```


---

## Task 6.5: 产品服务层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/service/IProductionProductService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/service/impl/ProductionProductServiceImpl.java`

- [ ] **Step 1: 创建IProductionProductService接口**

```java
package com.yigongbao.module.production.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import java.util.List;

/**
 * 生产产品服务接口
 */
public interface IProductionProductService extends IService<ProductionProductEntity> {
    
    /**
     * 根据流转卡ID查询产品列表
     */
    List<ProductionProductEntity> listByRecordId(Long recordId);
    
    /**
     * 根据产品编号查询产品
     */
    ProductionProductEntity getByProductNo(String productNo);
    
    /**
     * 更新产品状态
     */
    void updateStatus(Long productId, String status);
}
```

- [ ] **Step 2: 创建ProductionProductServiceImpl实现类**

```java
package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 生产产品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionProductServiceImpl extends ServiceImpl<ProductionProductMapper, ProductionProductEntity> 
        implements IProductionProductService {

    /**
     * 根据流转卡ID查询产品列表
     */
    @Override
    public List<ProductionProductEntity> listByRecordId(Long recordId) {
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductionProductEntity::getProductionRecordId, recordId)
               .orderByAsc(ProductionProductEntity::getId);
        return list(wrapper);
    }

    /**
     * 根据产品编号查询产品
     */
    @Override
    public ProductionProductEntity getByProductNo(String productNo) {
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductionProductEntity::getProductNo, productNo);
        ProductionProductEntity product = getOne(wrapper);
        
        if (product == null) {
            log.warn("产品不存在: productNo={}", productNo);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        
        return product;
    }

    /**
     * 更新产品状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long productId, String status) {
        ProductionProductEntity product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        
        String oldStatus = product.getStatus();
        product.setStatus(status);
        updateById(product);
        
        log.info("更新产品状态: productId={}, productNo={}, {} -> {}", 
            productId, product.getProductNo(), oldStatus, status);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/service/
git commit -m "feat: 创建产品服务层"
```


---

## Task 8.5: 包装服务层

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/service/IProductionPackService.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/service/impl/ProductionPackServiceImpl.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/dto/FillPackDTO.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/controller/ProductionPackController.java`

- [ ] **Step 1: 创建IProductionPackService接口**

```java
package com.yigongbao.module.production.pack.service;

import com.yigongbao.module.production.pack.dto.FillPackDTO;

/**
 * 包装服务接口
 */
public interface IProductionPackService {
    
    /**
     * 填写包装信息
     */
    void fillPackInfo(Long recordId, FillPackDTO dto);
    
    /**
     * 包装完成，流转到入库
     */
    void transferToWarehouse(Long recordId);
}
```

- [ ] **Step 2: 创建FillPackDTO**

```java
package com.yigongbao.module.production.pack.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 填写包装信息DTO
 */
@Data
public class FillPackDTO {
    @NotNull(message = "包装设备ID不能为空")
    private Long packDeviceId;
    
    private BigDecimal packSealTemperature;
    private Integer packSealTime;
    private String packSterilizationMethod;
    private String packSterilizationBatchNo;
}
```

- [ ] **Step 3: 创建ProductionPackServiceImpl实现类**

```java
package com.yigongbao.module.production.pack.service.impl;

import com.yigongbao.module.production.pack.service.IProductionPackService;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/**
 * 包装服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionPackServiceImpl implements IProductionPackService {

    private final ProductionRecordMapper recordMapper;
    private final DeviceMapper deviceMapper;
    private final OrderMainMapper orderMainMapper;
    private final FlowFacade flowFacade;
    private final IProductionRecordService recordService;

    /**
     * 填写包装信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillPackInfo(Long recordId, FillPackDTO dto) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        
        // 查询包装设备信息
        DeviceEntity device = deviceMapper.selectById(dto.getPackDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.PACK_DEVICE_NOT_FOUND);
        }
        
        // 填写包装信息
        record.setPackDeviceId(dto.getPackDeviceId());
        record.setPackDeviceNo(device.getDeviceId());
        record.setPackSealTemperature(dto.getPackSealTemperature());
        record.setPackSealTime(dto.getPackSealTime());
        record.setPackSterilizationMethod(dto.getPackSterilizationMethod());
        record.setPackSterilizationBatchNo(dto.getPackSterilizationBatchNo());
        record.setPackOperatorId(StpUtil.getLoginIdAsLong());
        record.setPackTime(LocalDateTime.now());
        
        recordMapper.updateById(record);
        
        log.info("填写包装信息: recordId={}, recordNo={}, packDeviceId={}, packDeviceNo={}", 
            recordId, record.getRecordNo(), device.getId(), device.getDeviceId());
    }

    /**
     * 包装完成,流转到入库
     * 更新流转卡状态为 warehouse_in
     * 聚合判断：若同订单所有活跃流转卡均为 warehouse_in → 触发 Flow COMPLETE_WAREHOUSE_IN
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToWarehouse(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        
        if (record.getPackDeviceId() == null) {
            throw new BusinessException(ErrorCodeEnum.PACK_INFO_NOT_FILLED);
        }
        
        record.setStatus(RecordStatusEnum.WAREHOUSE_IN.getCode());
        recordMapper.updateById(record);

        recordService.triggerFlowIfAllReach(record.getOrderId(),
                RecordStatusEnum.WAREHOUSE_IN.getCode(), FlowActionEnum.COMPLETE_WAREHOUSE_IN);
        
        log.info("包装完成，流转到入库: recordId={}, recordNo={}, orderId={}",
                recordId, record.getRecordNo(), record.getOrderId());
    }
}
```

- [ ] **Step 4: 创建ProductionPackController**

```java
package com.yigongbao.module.production.pack.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.production.pack.service.IProductionPackService;
import com.yigongbao.module.production.pack.dto.FillPackDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * 包装管理
 */
@Tag(name = "包装管理")
@RestController
@RequestMapping("/production/pack")
@RequiredArgsConstructor
public class ProductionPackController {

    private final IProductionPackService packService;

    @Operation(summary = "填写包装信息")
    @PutMapping("/{recordId}/fill")
    public Result<Void> fillPackInfo(@PathVariable Long recordId, @Valid @RequestBody FillPackDTO dto) {
        packService.fillPackInfo(recordId, dto);
        return Result.success();
    }

    @Operation(summary = "包装完成，流转到入库")
    @PostMapping("/{recordId}/transfer")
    public Result<Void> transferToWarehouse(@PathVariable Long recordId) {
        packService.transferToWarehouse(recordId);
        return Result.success();
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/pack/
git commit -m "feat: 创建包装服务层"
```


---

## Task 6.8: 转换器类创建

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/convert/ProductionRecordConvert.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/product/convert/ProductionProductConvert.java`
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/convert/ProductionProcessConvert.java`

- [ ] **Step 1: 创建ProductionRecordConvert**

```java
package com.yigongbao.module.production.record.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.production.record.dto.CreateRecordDTO;

/**
 * 生产流转卡转换器
 */
public class ProductionRecordConvert {

    /**
     * Entity转VO
     */
    public static ProductionRecordVO toVO(ProductionRecordEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductionRecordVO vo = new ProductionRecordVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    /**
     * DTO转Entity
     */
    public static ProductionRecordEntity toEntity(CreateRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        ProductionRecordEntity entity = new ProductionRecordEntity();
        BeanUtil.copyProperties(dto, entity);
        return entity;
    }
}
```

- [ ] **Step 2: 创建ProductionProductConvert**

```java
package com.yigongbao.module.production.product.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.vo.ProductionProductVO;

/**
 * 生产产品转换器
 */
public class ProductionProductConvert {

    /**
     * Entity转VO
     */
    public static ProductionProductVO toVO(ProductionProductEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductionProductVO vo = new ProductionProductVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
```

- [ ] **Step 3: 创建ProductionProcessConvert**

```java
package com.yigongbao.module.production.process.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.vo.ProductionProcessVO;

/**
 * 工序记录转换器
 */
public class ProductionProcessConvert {

    /**
     * Entity转VO
     */
    public static ProductionProcessVO toVO(ProductionProcessEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductionProcessVO vo = new ProductionProcessVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/*/convert/
git commit -m "feat: 创建转换器类"
```

---

## Task 13: 打印失败/检验不合格处理逻辑（P0修复）

**需求来源**：需求分析§9.2 打印失败处理、§9.2.1 打印工序检验不合格

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/IProductionProcessService.java`
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/controller/ProductionProcessController.java`

- [ ] **Step 1: 在 IProductionProcessService 中添加接口方法**

```java
/**
 * 打印失败处理
 * @param recordId 流转卡ID
 * @param failureReason 失败原因
 * @param recreate true=废弃原卡创建新卡（方式B），false=修复后继续（方式A，仅记录日志）
 * @return 新流转卡ID（recreate=true时返回），否则返回null
 */
Long handlePrintFailure(Long recordId, String failureReason, boolean recreate);

/**
 * 打印检验不合格处理（打印完成后检验发现问题）
 * @param recordId 流转卡ID
 * @param failureReason 不合格原因
 * @param recreate true=废弃原卡创建新卡（方式B），false=重打后继续（方式A）
 * @return 新流转卡ID（recreate=true时返回），否则返回null
 */
Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate);
```

- [ ] **Step 2: 在 ProductionProcessServiceImpl 中实现**

```java
/**
 * 打印失败处理
 * 方式A：修复后继续，原流转卡不变，仅记录日志
 * 方式B：废弃原流转卡（标记 print_failed），原产品编号作废，返回null（由前端重新调用createRecord创建新卡）
 */
@Override
@Transactional(rollbackFor = Exception.class)
public Long handlePrintFailure(Long recordId, String failureReason, boolean recreate) {
    ProductionRecordEntity record = recordMapper.selectById(recordId);
    if (record == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
    }

    if (!recreate) {
        // 方式A：修复后继续，无需系统操作
        log.info("打印失败-修复后继续: recordId={}, recordNo={}, reason={}", 
            recordId, record.getRecordNo(), failureReason);
        return null;
    }

    // 方式B：废弃原流转卡
    record.setStatus(RecordStatusEnum.PRINT_FAILED.getCode());
    recordMapper.updateById(record);

    // 逻辑删除原产品编号（作废）
    LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ProductionProductEntity::getProductionRecordId, recordId);
    List<ProductionProductEntity> products = productMapper.selectList(wrapper);
    products.forEach(p -> productMapper.deleteById(p.getId()));  // 通过 @TableLogic 逻辑删除

    log.info("打印失败-废弃流转卡: recordId={}, recordNo={}, reason={}, voidedProductCount={}", 
        recordId, record.getRecordNo(), failureReason, products.size());
    return null;
}

/**
 * 打印检验不合格处理
 * 方式A：重打后继续，原流转卡不变
 * 方式B：废弃原流转卡（标记 abandoned），原产品编号作废
 */
@Override
@Transactional(rollbackFor = Exception.class)
public Long handlePrintInspectionFail(Long recordId, String failureReason, boolean recreate) {
    ProductionRecordEntity record = recordMapper.selectById(recordId);
    if (record == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
    }

    if (!recreate) {
        log.info("打印检验不合格-重打后继续: recordId={}, recordNo={}, reason={}", 
            recordId, record.getRecordNo(), failureReason);
        return null;
    }

    // 方式B：废弃原流转卡（abandoned）
    record.setStatus(RecordStatusEnum.ABANDONED.getCode());
    recordMapper.updateById(record);

    LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ProductionProductEntity::getProductionRecordId, recordId);
    List<ProductionProductEntity> products = productMapper.selectList(wrapper);
    products.forEach(p -> productMapper.deleteById(p.getId()));  // 通过 @TableLogic 逻辑删除

    log.info("打印检验不合格-废弃流转卡: recordId={}, recordNo={}, reason={}, voidedProductCount={}", 
        recordId, record.getRecordNo(), failureReason, products.size());
    return null;
}
```

- [ ] **Step 3: 在 ProductionProcessController 中添加接口**

```java
@Operation(summary = "打印失败处理")
@PostMapping("/{recordId}/print-failure")
public Result<Void> handlePrintFailure(@PathVariable Long recordId,
                                       @RequestParam String failureReason,
                                       @RequestParam boolean recreate) {
    processService.handlePrintFailure(recordId, failureReason, recreate);
    return Result.success();
}

@Operation(summary = "打印检验不合格处理")
@PostMapping("/{recordId}/print-inspection-fail")
public Result<Void> handlePrintInspectionFail(@PathVariable Long recordId,
                                              @RequestParam String failureReason,
                                              @RequestParam boolean recreate) {
    processService.handlePrintInspectionFail(recordId, failureReason, recreate);
    return Result.success();
}
```

- [ ] **Step 4: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/
git commit -m "feat: 实现打印失败和检验不合格处理逻辑"
```

---

## Task 14: 质检redo完成后状态自动恢复（P1修复）

**需求来源**：需求分析§5.5步骤6、§8.8 BR-703

**Files:**
- Modify: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java`

- [ ] **Step 1: 在 fillProcess() 方法末尾追加redo状态恢复逻辑**

在 `ProductionProcessServiceImpl.fillProcess()` 方法中，工序填写完成后检查是否有redo产品在此工序重做：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void fillProcess(Long processId, FillProcessDTO dto) {
    ProductionProcessEntity process = getById(processId);
    if (process == null) {
        throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
    }
    process.setDeviceId(dto.getDeviceId());
    process.setProcessParams(dto.getProcessParams());
    process.setHasRedo(dto.getHasRedo());
    process.setRedoRemark(dto.getRedoRemark());
    process.setStatus(ProcessStatusEnum.COMPLETED.getCode());
    updateById(process);

    // 检查是否有redo产品指定在此工序重做，重做完成后自动恢复为 in_process
    LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ProductionProductEntity::getProductionRecordId, process.getProductionRecordId())
           .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode())
           .eq(ProductionProductEntity::getRedoProcessType, process.getProcessType());
    List<ProductionProductEntity> redoProducts = productMapper.selectList(wrapper);

    if (!redoProducts.isEmpty()) {
        redoProducts.forEach(p -> {
            p.setStatus(ProductStatusEnum.IN_PROCESS.getCode());
            p.setRedoProcessType(null);
            productMapper.updateById(p);
        });
        log.info("redo产品重做完成，状态恢复为in_process: processId={}, processType={}, productCount={}", 
            processId, process.getProcessType(), redoProducts.size());
    }

    log.info("填写工序信息: processId={}, deviceId={}", processId, dto.getDeviceId());
}
```

- [ ] **Step 2: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/process/service/impl/ProductionProcessServiceImpl.java
git commit -m "feat: 质检redo完成后自动恢复产品状态为in_process"
```

---

## Task 15: 设备状态推送超时提醒（P1修复）

**需求来源**：需求分析§5.2步骤10-11、§5.3步骤3、PRD §7.5

**Files:**
- Create: `yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/PrintTimeoutChecker.java`
- Modify: `yigongbao-parent/yigongbao-module-system/src/main/resources/sql/init.sql`（新增生产模块配置初始数据）

- [ ] **Step 1: 在 init.sql 中新增生产模块超时配置初始数据**

在 `sys_config` 初始数据末尾追加：

```sql
-- 生产管理配置
INSERT INTO sys_config (config_key, config_name, config_value, config_type, config_group, config_desc, is_system, is_public, sort, status)
VALUES
('production.pending.print.timeout.minutes', '待打印超时阈值（分钟）', '10', 'number', 'production', '分配设备后超过此时间未收到打印开始推送，触发超时提醒', 1, 0, 100, 1),
('production.printing.timeout.minutes', '打印中超时阈值（分钟）', '240', 'number', 'production', '打印开始后超过此时间未收到打印完成推送，触发超时提醒', 1, 0, 101, 1),
('production.process.params.config', '工序参数配置字典（JSON）', '{"print":[{"key":"layerThickness","label":"层厚","type":"select","value":"100","options":[{"label":"50μm","value":"50"},{"label":"100μm","value":"100"},{"label":"200μm","value":"200"}],"unit":"μm"},{"key":"laserPower","label":"激光器功率","type":"number","value":50,"unit":"mW"}],"wash":[{"key":"alcoholBatchNo","label":"酒精批号","type":"text"},{"key":"soakMode","label":"浸泡程度","type":"select","value":"complete","options":[{"label":"完全浸泡","value":"complete"},{"label":"部分浸泡","value":"partial"}]},{"key":"dryTemp","label":"干燥温度","type":"number","unit":"℃"},{"key":"dryTime","label":"干燥时长","type":"number","unit":"分钟"}],"cure":[{"key":"cureMode","label":"固化模式","type":"select","value":"HIGH","options":[{"label":"HIGH","value":"HIGH"},{"label":"LOW","value":"LOW"}]}],"clean_dry":[{"key":"alcoholBatchNo","label":"酒精批号","type":"text"},{"key":"cleanMode","label":"清洗模式","type":"select","value":"variable_wave","options":[{"label":"变波","value":"variable_wave"},{"label":"脱气","value":"degas"}]},{"key":"heating","label":"加热","type":"switch","value":false},{"key":"dryMethod","label":"干燥方式","type":"select","value":"air_dry","options":[{"label":"空气干燥","value":"air_dry"},{"label":"烘干","value":"oven"}]}],"pack":[{"key":"sealTemperature","label":"热封温度","type":"number","value":180,"unit":"℃"},{"key":"sealTime","label":"热封时间","type":"number","value":3,"unit":"秒"},{"key":"sterilizationMethod","label":"灭菌方式","type":"text"},{"key":"sterilizationBatchNo","label":"灭菌批号","type":"text"}]}', 'json', 'production', '各工序参数字段定义，前端据此动态渲染参数表单', 1, 0, 102, 1);
```

- [ ] **Step 2: 创建 PrintTimeoutChecker 定时任务**

```java
package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.system.config.service.IConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印超时检查器
 * 定期检查打印状态超时的流转卡，记录警告日志（通知机制由后续迭代实现）
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintTimeoutChecker {

    /** 待打印超时默认值（分钟），sys_config 无配置时兜底 */
    private static final int DEFAULT_PENDING_TIMEOUT = 10;
    /** 打印中超时默认值（分钟），sys_config 无配置时兜底 */
    private static final int DEFAULT_PRINTING_TIMEOUT = 240;

    private final ProductionRecordMapper recordMapper;
    private final IConfigService configService;

    /**
     * 每分钟检查一次打印超时
     * 超时阈值从 sys_config 读取，无配置时使用默认值
     */
    @Scheduled(fixedDelay = 60000)
    public void checkPrintTimeout() {
        int pendingTimeoutMinutes = getConfigInt(
            SystemConfigKeyEnum.PRODUCTION_PENDING_PRINT_TIMEOUT_MINUTES, DEFAULT_PENDING_TIMEOUT);
        int printingTimeoutMinutes = getConfigInt(
            SystemConfigKeyEnum.PRODUCTION_PRINTING_TIMEOUT_MINUTES, DEFAULT_PRINTING_TIMEOUT);

        // 检查待打印超时（已分配设备但未开始打印）
        LocalDateTime pendingThreshold = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        LambdaQueryWrapper<ProductionRecordEntity> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(ProductionRecordEntity::getStatus, RecordStatusEnum.PENDING_PRINT.getCode())
                      .isNotNull(ProductionRecordEntity::getPrintDeviceId)
                      .lt(ProductionRecordEntity::getUpdateTime, pendingThreshold);
        List<ProductionRecordEntity> pendingTimeout = recordMapper.selectList(pendingWrapper);
        pendingTimeout.forEach(record ->
            log.warn("待打印超时提醒: recordId={}, recordNo={}, deviceId={}, 超过{}分钟未收到打印开始推送，请检查设备连接",
                record.getId(), record.getRecordNo(), record.getPrintDeviceId(), pendingTimeoutMinutes)
        );

        // 检查打印中超时
        LocalDateTime printingThreshold = LocalDateTime.now().minusMinutes(printingTimeoutMinutes);
        LambdaQueryWrapper<ProductionRecordEntity> printingWrapper = new LambdaQueryWrapper<>();
        printingWrapper.eq(ProductionRecordEntity::getStatus, RecordStatusEnum.PRINTING.getCode())
                       .lt(ProductionRecordEntity::getUpdateTime, printingThreshold);
        List<ProductionRecordEntity> printingTimeout = recordMapper.selectList(printingWrapper);
        printingTimeout.forEach(record ->
            log.warn("打印中超时提醒: recordId={}, recordNo={}, deviceId={}, 超过{}分钟未收到打印完成推送，请检查设备状态",
                record.getId(), record.getRecordNo(), record.getPrintDeviceId(), printingTimeoutMinutes)
        );
    }

    private int getConfigInt(SystemConfigKeyEnum key, int defaultValue) {
        try {
            String value = configService.getConfigValue(key.getKey());
            return Integer.parseInt(value);
        } catch (Exception e) {
            log.warn("读取配置失败，使用默认值: configKey={}, defaultValue={}", key.getKey(), defaultValue);
            return defaultValue;
        }
    }
}
```

- [ ] **Step 2: 在 yigongbao-boot 启动类或配置类中启用定时任务**

确认 `@EnableScheduling` 注解已在启动类或配置类上添加（检查现有配置，如已有则跳过）。

- [ ] **Step 3: 编译验证**

```bash
cd yigongbao-parent
mvn clean compile -pl yigongbao-module-production
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/listener/PrintTimeoutChecker.java
git commit -m "feat: 添加打印超时提醒定时任务"
```

