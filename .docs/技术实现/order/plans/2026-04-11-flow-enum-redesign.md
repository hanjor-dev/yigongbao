# Flow 枚举编码体系重设计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 FlowPhaseEnum/FlowStatusEnum 的整数值体系从"顺序值"重设计为"Phase 间隔10、Status = phase×100+序号(1-9)"，同时历史表新增 phase_name/status_name 快照字段，从根本上解决生产环境阶段/状态插入扩展问题。

**Architecture:** 枚举值变更是纯整数替换，业务逻辑不变；`belongsTo()` 判断规则从范围算法改为整除；历史表增加两个 VARCHAR 快照字段，写入时由 `FlowStatusHistoryServiceImpl` 补充。所有变更按"底层→枚举→规则→服务→测试→文档"顺序推进，每层变更后立即运行对应测试。

**Tech Stack:** Java 21, MyBatis-Plus 3.5.8, JUnit 5 + Mockito, H2（测试）, MySQL 8（生产 DDL）

---

## 新旧值对照表（实施参考）

| 枚举常量 | 旧 Phase 值 | 新 Phase 值 |
|---------|------------|------------|
| ORDER | 1 | 10 |
| DESIGN | 2 | 20 |
| PRINT | 3 | 30 |
| POST_PROCESSING | 4 | 40 |
| QC | 5 | 50 |
| WAREHOUSE | 6 | 60 |
| CONFIRM | 7 | 70 |
| COMPLETED | 8 | 80 |

| 枚举常量 | 旧 Status 值 | 新 Status 值 |
|---------|-------------|-------------|
| DRAFT | 10 | 1001 |
| PENDING_DATA_AUDIT | 11 | 1002 |
| DATA_AUDIT_PASSED | 12 | 1003 |
| DATA_AUDIT_REJECTED | 13 | 1004 |
| PENDING_DESIGN | 21 | 2001 |
| DESIGN_IN_PROGRESS | 22 | 2002 |
| DESIGN_COMPLETED | 23 | 2003 |
| DESIGN_REVIEWING | 24 | 2004 |
| DESIGN_REVIEW_PASSED | 25 | 2005 |
| DESIGN_REVIEW_REJECTED | 26 | 2006 |
| PENDING_PRINT | 31 | 3001 |
| PRINTING | 32 | 3002 |
| PRINT_COMPLETED | 33 | 3003 |
| POST_PROCESSING | 41 | 4001 |
| QC_IN_PROGRESS | 51 | 5001 |
| QC_PASSED | 52 | 5002 |
| QC_FAILED | 53 | 5003 |
| REWORK | 54 | 5004 |
| WAREHOUSE_IN | 61 | 6001 |
| WAREHOUSED | 62 | 6002 |
| AWAITING_CONFIRM | 71 | 7001 |
| COMPLETED | 80 | 8001 |

**新 belongsTo() 规则：** `statusValue / 100 == phase.getValue()`
例：5001 / 100 = 50 == QC.getValue()

---

## 影响文件清单

| 文件路径 | 变更类型 | 说明 |
|---------|---------|------|
| `sql/ddl.sql` | 修改 | phase/status/from_status/to_status 列类型 TINYINT→INT；order_main 注释更新；history 表新增 phase_name/status_name 字段 |
| `yigongbao-module-order/src/test/resources/schema.sql` | 修改 | 同 ddl.sql，H2 版本，phase/status 列注释更新；history 表新增快照字段 |
| `yigongbao-common/.../FlowStatusHistoryEntity.java` | 修改 | 新增 phaseName/statusName 字段 |
| `yigongbao-module-flow/.../FlowPhaseEnum.java` | 修改 | 8 个 value 值全部更新，类注释更新编码规则说明 |
| `yigongbao-module-flow/.../FlowStatusEnum.java` | 修改 | 22 个 value 值全部更新，belongsTo() 算法修改，注释更新 |
| `yigongbao-module-flow/.../FlowStatusHistoryServiceImpl.java` | 修改 | recordTransition() 新增 phaseName/statusName 参数查找与写入 |
| `yigongbao-module-flow/.../FlowStatusHistoryService.java` | 修改 | recordTransition() 接口签名无需改（内部实现变化），或视情况更新 |
| `yigongbao-module-flow/.../FlowStateMachineServiceImpl.java` | 修改 | 调用 recordTransition 处补充 phaseName/statusName 参数 |
| `yigongbao-module-flow/.../FlowPhaseTransitionRules.java` | 修改 | 注释中的旧状态值更新（如 DATA_AUDIT_PASSED(12)→(1003)），逻辑本身依赖枚举不变 |
| `yigongbao-module-flow/.../FlowStatusTransitionRules.java` | 修改 | 注释中的旧状态值更新，逻辑本身依赖枚举不变 |
| `yigongbao-module-flow/src/test/.../FlowStatusTransitionRulesTest.java` | 修改 | 所有硬编码整数值全部替换为新值 |
| `yigongbao-module-flow/src/test/.../FlowPhaseTransitionRulesTest.java` | 修改 | 所有硬编码整数值全部替换为新值 |
| `yigongbao-module-flow/src/test/.../FlowStateMachineServiceImplTest.java` | 修改 | 所有硬编码整数值全部替换为新值 |
| `yigongbao-module-flow/src/test/.../FlowContextTest.java` | 修改 | 检查是否有硬编码整数值并替换 |
| `yigongbao-module-order/src/test/.../` | 修改 | 订单测试中引用的 phase/status 硬编码值替换 |
| `.docs/技术实现/order/00_订单状态机设计方案.md` | 修改 | 枚举值表格全部更新，编码规则说明更新 |
| `.docs/技术实现/order/01_订单流程实现方案.md` | 修改 | 枚举值引用全部更新 |
| `.docs/测试文档/订单流转状态机接口测试文档.md` | 修改 | 状态值引用全部更新 |
| `.docs/接口文档/19_订单模块接口文档.md` | 修改 | 状态值枚举说明全部更新 |

---

## Task 1: SQL 层——DDL 更新（ddl.sql）

**Files:**
- Modify: `sql/ddl.sql:919-1116`

- [ ] **Step 1: 修改 order_main 表的 phase/status 列类型和注释**

  找到 `sql/ddl.sql` 中 order_main 表定义（约第 973-974 行），将：
  ```sql
  phase           TINYINT         NOT NULL DEFAULT 1 COMMENT '当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成',
  status          TINYINT         NOT NULL DEFAULT 10 COMMENT '当前状态',
  ```
  改为：
  ```sql
  phase           INT             NOT NULL DEFAULT 10 COMMENT '当前阶段：10-订单，20-设计，30-打印，40-后处理，50-质检，60-仓储，70-确认，80-完成（间隔10，支持扩展插入）',
  status          INT             NOT NULL DEFAULT 1001 COMMENT '当前状态（格式：phase×100+序号，如1001=订单草稿，2001=待设计）',
  ```

- [ ] **Step 2: 修改 order_flow_status_history 表，列类型 + 新增快照字段**

  找到 `sql/ddl.sql` 中 order_flow_status_history 表定义（约第 1089-1116 行），将：
  ```sql
  phase           TINYINT         COMMENT '变更时阶段',
  from_status     TINYINT         COMMENT '变更前状态',
  to_status       TINYINT         COMMENT '变更后状态',
  ```
  改为：
  ```sql
  phase           INT             COMMENT '变更时阶段（FlowPhaseEnum.value）',
  phase_name      VARCHAR(50)     COMMENT '变更时阶段名称（快照，防止枚举改名后历史展示错误）',
  from_status     INT             COMMENT '变更前状态（FlowStatusEnum.value）',
  from_status_name VARCHAR(50)    COMMENT '变更前状态名称（快照）',
  to_status       INT             COMMENT '变更后状态（FlowStatusEnum.value）',
  to_status_name  VARCHAR(50)     COMMENT '变更后状态名称（快照）',
  ```

---

## Task 2: SQL 层——H2 测试 Schema 更新（schema.sql）

**Files:**
- Modify: `yigongbao-module-order/src/test/resources/schema.sql:96-97,154-171`

- [ ] **Step 1: 修改 order_main 的 phase/status 列注释和默认值**

  找到 `schema.sql` 中 order_main 表（约第 96-97 行），将：
  ```sql
  phase INT DEFAULT 1 COMMENT '当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成',
  status INT DEFAULT 20 COMMENT '当前状态',
  ```
  改为：
  ```sql
  phase INT DEFAULT 10 COMMENT '当前阶段：10-订单，20-设计，30-打印，40-后处理，50-质检，60-仓储，70-确认，80-完成',
  status INT DEFAULT 1001 COMMENT '当前状态（格式：phase×100+序号）',
  ```

- [ ] **Step 2: 修改 order_flow_status_history 表，新增快照字段**

  找到 `schema.sql` 中 order_flow_status_history 表（约第 154-171 行），将：
  ```sql
  phase INT COMMENT '变更时阶段',
  from_status INT COMMENT '变更前状态',
  to_status INT COMMENT '变更后状态',
  ```
  改为：
  ```sql
  phase INT COMMENT '变更时阶段',
  phase_name VARCHAR(50) COMMENT '变更时阶段名称（快照）',
  from_status INT COMMENT '变更前状态',
  from_status_name VARCHAR(50) COMMENT '变更前状态名称（快照）',
  to_status INT COMMENT '变更后状态',
  to_status_name VARCHAR(50) COMMENT '变更后状态名称（快照）',
  ```

---

## Task 3: Entity 层——FlowStatusHistoryEntity 新增快照字段

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/entity/FlowStatusHistoryEntity.java`

- [ ] **Step 1: 新增三个快照字段**

  在 `phase` 字段后新增 `phaseName`，在 `fromStatus` 后新增 `fromStatusName`，在 `toStatus` 后新增 `toStatusName`：

  ```java
  /**
   * 变更时阶段
   */
  private Integer phase;

  /**
   * 变更时阶段名称（快照，历史展示用）
   */
  private String phaseName;

  /**
   * 变更前状态
   */
  private Integer fromStatus;

  /**
   * 变更前状态名称（快照，历史展示用）
   */
  private String fromStatusName;

  /**
   * 变更后状态
   */
  private Integer toStatus;

  /**
   * 变更后状态名称（快照，历史展示用）
   */
  private String toStatusName;
  ```

---

## Task 4: 枚举层——FlowPhaseEnum 值更新

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowPhaseEnum.java`

- [ ] **Step 1: 更新 8 个枚举常量的 value，并更新类注释**

  更新类级 Javadoc，说明新编码规则，然后将 8 个枚举值全部替换：

  ```java
  /**
   * 流转阶段枚举
   * 定义业务流转的阶段
   *
   * 【编码规则】
   * - Phase value 使用间隔10的整数：10, 20, 30...
   * - 间隔设计允许在任意两个已有阶段之间插入新阶段，不影响已有值和历史数据
   * - 例如：在 DESIGN(20) 和 PRINT(30) 之间可插入 LAYOUT(25)
   *
   * @author hanjor
   * @date 2026-03-31
   */
  ```

  枚举常量改为：
  ```java
  ORDER(10, "订单阶段"),
  DESIGN(20, "设计阶段"),
  PRINT(30, "打印阶段"),
  POST_PROCESSING(40, "后处理阶段"),
  QC(50, "质检阶段"),
  WAREHOUSE(60, "仓储阶段"),
  CONFIRM(70, "确认阶段"),
  COMPLETED(80, "已完成");
  ```

---

## Task 5: 枚举层——FlowStatusEnum 值更新 + belongsTo() 修改

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/enums/FlowStatusEnum.java`

- [ ] **Step 1: 更新类注释，说明新编码规则**

  更新类级 Javadoc：
  ```java
  /**
   * 流转状态枚举
   * 按阶段分段的完整状态码体系
   *
   * 【编码规则】
   * - Status value = phase.value × 100 + 序号(1-9)
   * - 例：ORDER(10) 的状态从 1001 开始：DRAFT=1001, PENDING_DATA_AUDIT=1002...
   * - 每阶段最多9个状态（序号1-9）；每阶段间有间隔，支持插入新阶段
   * - belongsTo(phase) 判断：statusValue / 100 == phase.getValue()
   *
   * @author hanjor
   * @date 2026-03-31
   */
  ```

- [ ] **Step 2: 替换全部 22 个枚举值，更新各分区注释头**

  ```java
  // ==================== 订单阶段（1001-1009）====================
  DRAFT(1001, "草稿"),
  PENDING_DATA_AUDIT(1002, "数据待审核"),
  DATA_AUDIT_PASSED(1003, "数据审核通过"),
  DATA_AUDIT_REJECTED(1004, "数据审核不通过"),

  // ==================== 设计阶段（2001-2009）====================
  PENDING_DESIGN(2001, "待设计"),
  DESIGN_IN_PROGRESS(2002, "设计中"),
  DESIGN_COMPLETED(2003, "设计完成"),
  DESIGN_REVIEWING(2004, "设计审核中"),
  DESIGN_REVIEW_PASSED(2005, "设计审核通过"),
  DESIGN_REVIEW_REJECTED(2006, "设计审核不通过"),

  // ==================== 打印阶段（3001-3009）====================
  PENDING_PRINT(3001, "待打印"),
  PRINTING(3002, "打印中"),
  PRINT_COMPLETED(3003, "打印完成"),

  // ==================== 后处理阶段（4001-4009）====================
  POST_PROCESSING(4001, "后处理中"),

  // ==================== 质检阶段（5001-5009）====================
  QC_IN_PROGRESS(5001, "质检中"),
  QC_PASSED(5002, "质检合格"),
  QC_FAILED(5003, "质检不合格"),
  REWORK(5004, "返工"),

  // ==================== 仓储阶段（6001-6009）====================
  WAREHOUSE_IN(6001, "入库中"),
  WAREHOUSED(6002, "已入库"),

  // ==================== 确认阶段（7001-7009）====================
  AWAITING_CONFIRM(7001, "待客户确认"),

  // ==================== 已完成（8001）====================
  COMPLETED(8001, "已完成");
  ```

- [ ] **Step 3: 修改 belongsTo() 方法**

  将现有算法：
  ```java
  return statusValue >= phaseValue && statusValue < phaseValue + 10;
  ```
  改为：
  ```java
  return statusValue / 100 == phase.getValue();
  ```

  同时更新 Javadoc 注释，说明新的判断规则。

---

## Task 6: 规则层——注释更新（FlowPhaseTransitionRules + FlowStatusTransitionRules）

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowPhaseTransitionRules.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/rules/FlowStatusTransitionRules.java`

> **说明：** 两个规则类的核心逻辑全部依赖 FlowStatusEnum/FlowPhaseEnum 枚举引用，不含硬编码整数值，枚举值改变后自动生效。只需更新注释中提到的旧整数值。

- [ ] **Step 1: 更新 FlowPhaseTransitionRules.java 中的注释**

  找到 `decideNextPhaseAndStatus()` 方法的完整 Javadoc（约第 128-145 行），注意第 130 行有笔误 `DESIGNING` 需同步更正为 `PENDING_DESIGN`。将整个注释块：
  ```
  * 【核心修复 P1-3】：阶段推进时，必须同时确定初始可见状态
  * - DATA_AUDIT_PASSED → DESIGN + DESIGNING
  * - DESIGN_REVIEW_PASSED → PRINT + PENDING_PRINT 或 CONFIRM + AWAITING_CONFIRM
  *
  * 阶段推进规则：
  * - DATA_AUDIT_PASSED(12) → 进入 DESIGN(2)，status 变为 DESIGNING(21)
  * - DESIGN_REVIEW_PASSED(24) → 进入 PRINT(3) 或 CONFIRM(7)
  * - PRINT_COMPLETED(33) → 进入 POST_PROCESSING(4)
  * - QC_PASSED(52) → 进入 WAREHOUSE(6)
  * - WAREHOUSED(62) → 进入 COMPLETED(8)
  * - COMPLETED(80) → 进入 COMPLETED(8)
  ```
  改为：
  ```
  * 【核心修复 P1-3】：阶段推进时，必须同时确定初始可见状态
  * - DATA_AUDIT_PASSED → DESIGN + PENDING_DESIGN
  * - DESIGN_REVIEW_PASSED → PRINT + PENDING_PRINT 或 CONFIRM + AWAITING_CONFIRM
  *
  * 阶段推进规则：
  * - DATA_AUDIT_PASSED(1003) → 进入 DESIGN(20)，status 变为 PENDING_DESIGN(2001)
  * - DESIGN_REVIEW_PASSED(2005) → 进入 PRINT(30) 或 CONFIRM(70)
  * - PRINT_COMPLETED(3003) → 进入 POST_PROCESSING(40)
  * - QC_PASSED(5002) → 进入 WAREHOUSE(60)
  * - WAREHOUSED(6002) → 进入 COMPLETED(80)
  * - COMPLETED(8001) → 进入 COMPLETED(80)
  ```

- [ ] **Step 2: 更新 FlowStatusTransitionRules.java 中的注释**

  找到类顶部注释块中的不可见状态说明（约第 17-22 行）：
  ```
  * - DESIGN_REVIEW_PASSED(24) 为不可见状态
  ```
  改为：
  ```
  * - DESIGN_REVIEW_PASSED(2005) 为不可见状态
  ```

  找到各状态转换分区的注释头，将旧的范围说明更新：
  - `// ==================== 订单阶段状态转换（10-19）====================` → `（1001-1009）`
  - `// ==================== 设计阶段状态转换（20-29）====================` → `（2001-2009）`
  - `// ==================== 打印阶段状态转换（30-39）====================` → `（3001-3009）`
  - `// ==================== 后处理阶段状态转换（40-49）====================` → `（4001-4009）`
  - `// ==================== 质检阶段状态转换（50-59）====================` → `（5001-5009）`
  - `// ==================== 仓储阶段状态转换（60-69）====================` → `（6001-6009）`
  - `// ==================== 确认阶段状态转换（70-79，服务订单专用）====================` → `（7001-7009）`

  找到 `getAvailableActions()` 内部注释：
  ```
  // PRINT_COMPLETED 为过渡状态，不会出现在 phase=PRINT 的订单中（自动推进）
  // DESIGN_REVIEW_PASSED 为不可见状态，不会出现在 phase=DESIGN 的订单中
  // QC_PASSED 为过渡状态，不会出现在 phase=QC 的订单中（自动推进）
  ```
  （这些注释描述的是枚举常量名，无整数值，保持不变即可）

---

## Task 7: Service 层——FlowStatusHistoryService 接口 + Impl 更新

**Files:**
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/FlowStatusHistoryService.java`
- Modify: `yigongbao-module-flow/src/main/java/com/yigongbao/flow/service/impl/FlowStatusHistoryServiceImpl.java`

- [ ] **Step 1: 检查 FlowStatusHistoryService 接口的 recordTransition 签名**

  读取接口定义，确认参数列表是否需要新增 phaseName/fromStatusName/toStatusName，或者由 Impl 内部查找枚举 name。
  
  **推荐方案：Impl 内部查找（不改接口）**——recordTransition 收到 phase(Integer) 和 fromStatus/toStatus(Integer) 后，内部通过 `FlowPhaseEnum.getByValue(phase).getName()` 和 `FlowStatusEnum.getByValue(status).getName()` 获取名称并写入实体。这样调用方（FlowStateMachineServiceImpl）无需任何改动。

- [ ] **Step 2: 修改 FlowStatusHistoryServiceImpl.recordTransition()**

  在构建 entity 时新增三行：
  ```java
  // 快照阶段和状态名称，防止枚举后续改名导致历史展示错误
  FlowPhaseEnum phaseEnum = FlowPhaseEnum.getByValue(phase);
  entity.setPhaseName(phaseEnum != null ? phaseEnum.getName() : null);

  FlowStatusEnum fromStatusEnum = FlowStatusEnum.getByValue(fromStatus);
  entity.setFromStatusName(fromStatusEnum != null ? fromStatusEnum.getName() : null);

  FlowStatusEnum toStatusEnum = FlowStatusEnum.getByValue(toStatus);
  entity.setToStatusName(toStatusEnum != null ? toStatusEnum.getName() : null);
  ```

  同时更新 log.info 中的记录说明（保持格式一致即可）。

---

## Task 8: 运行全部 Flow 模块测试（第一次验证）

- [ ] **Step 1: 运行 flow 模块全部测试**

  ```bash
  cd yigongbao-parent
  mvn test -pl yigongbao-module-flow
  ```
  Expected: 所有测试通过（当前枚举依赖测试中的整数值，此步骤会暴露所有需要更新的测试用例）

- [ ] **Step 2: 记录所有失败的测试类和行号**（预期此处测试会批量失败，这是正常的——Task 9 来修复）

---

## Task 9: 测试层——Flow 模块测试更新

**Files:**
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowStatusTransitionRulesTest.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/rules/FlowPhaseTransitionRulesTest.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/service/FlowStateMachineServiceImplTest.java`
- Modify: `yigongbao-module-flow/src/test/java/com/yigongbao/flow/context/FlowContextTest.java`

> **批量替换策略：** 测试文件中的整数值已无法通过枚举常量推导，必须全量替换。使用 Edit 工具精确替换每个硬编码整数。替换对照表见本文档顶部"新旧值对照表"。

- [ ] **Step 1: 替换 FlowStatusTransitionRulesTest.java 中所有旧状态/阶段整数值**

  按新旧对照表，逐一将旧值替换为新值。重点区域：
  - `phase` 参数：1→10, 2→20, 3→30, 4→40, 5→50, 6→60, 7→70, 8→80
  - `status` 参数：全部按对照表替换（10→1001, 11→1002 ... 80→8001）
  - `assertEquals` 断言中的期望值：同上替换
  - **`@DisplayName` 字符串中的括号内整数值也需同步更新**，例如 `@DisplayName("phase=ORDER, status=DRAFT(10)")` → `@DisplayName("phase=ORDER, status=DRAFT(1001)")`，避免测试报告中显示过时的值

- [ ] **Step 2: 替换 FlowPhaseTransitionRulesTest.java 中所有旧阶段整数值**

  `phase` 相关：1→10, 2→20, 3→30, 4→40, 5→50, 6→60, 7→70, 8→80
  状态相关：按对照表替换

- [ ] **Step 3: 替换 FlowStateMachineServiceImplTest.java 中所有旧整数值**

  Mock setup 中 `order.getPhase()` 返回值：1→10, 2→20...
  Mock setup 中 `order.getStatus()` 返回值：全部按对照表替换
  断言中的期望值：同上

  **特别注意：** `verify(flowStatusHistoryService).recordTransition(...)` 调用中的 `eq(phase)` 和 `eq(status)` 参数也必须更新。例如：
  ```java
  // 旧
  verify(flowStatusHistoryService).recordTransition(
          eq(1L), eq("ORD-20260402-0001"), eq(1),   // eq(1) = phase ORDER
          eq(10), eq(10), ...                         // eq(10) = status DRAFT
  // 新
  verify(flowStatusHistoryService).recordTransition(
          eq(1L), eq("ORD-20260402-0001"), eq(10),  // eq(10) = phase ORDER
          eq(1001), eq(1001), ...                    // eq(1001) = status DRAFT
  ```

- [ ] **Step 4: 检查并替换 FlowContextTest.java**

  FlowContext 依赖 action 字符串而非整数值，预计无需修改。确认后跳过或按需更新。

- [ ] **Step 5: 运行 flow 模块全部测试**

  ```bash
  mvn test -pl yigongbao-module-flow
  ```
  Expected: 全部通过（BUILD SUCCESS）

- [ ] **Step 6: Commit**

  > **注意：** git 命令须从仓库根目录（`D:\01_Project\02_Personal\医工宝\`）执行，`sql/ddl.sql` 位于仓库根目录下，不在 `yigongbao-parent/` 内。

  ```bash
  # 在仓库根目录执行
  git add yigongbao-parent/yigongbao-module-flow/src/ \
          yigongbao-parent/yigongbao-common/src/ \
          sql/ \
          yigongbao-parent/yigongbao-module-order/src/test/resources/
  git commit -m "refactor(flow): 重设计枚举编码体系，Phase间隔10，Status=phase×100+序号，历史表新增快照字段"
  ```

---

## Task 10: 测试层——Order 模块测试更新

**Files:**
- Modify: `yigongbao-module-order/src/test/java/...` （所有引用 phase/status 整数值的测试）

- [ ] **Step 1: 搜索 order 测试目录中所有硬编码旧整数值**

  ```bash
  grep -rn "getPhase\|getStatus\|setPhase\|setStatus\|phase.*=.*[0-9]\|status.*=.*[0-9]" \
    yigongbao-module-order/src/test/
  ```
  列出所有命中位置，按对照表替换。

- [ ] **Step 2: 运行 order 模块全部测试**

  ```bash
  mvn test -pl yigongbao-module-order
  ```
  Expected: 全部通过（BUILD SUCCESS）

- [ ] **Step 3: Commit**

  > **注意：** 须从仓库根目录执行。

  ```bash
  git add yigongbao-parent/yigongbao-module-order/src/test/
  git commit -m "test(order): 同步更新测试中的 phase/status 枚举值为新编码体系"
  ```

---

## Task 11: 完整构建验证

- [ ] **Step 1: 全量编译**

  ```bash
  cd yigongbao-parent
  mvn clean package -DskipTests
  ```
  Expected: BUILD SUCCESS，无编译错误

- [ ] **Step 2: 全量测试**

  ```bash
  mvn test
  ```
  Expected: 所有模块测试通过

- [ ] **Step 3: 如有失败，定位并修复，重复 Step 2 直至全绿**

---

## Task 12: 代码审查

- [ ] **Step 1: 调用 code-review 技能对本次所有变更进行审查**

  使用 `/code-review` 检查：
  - 枚举值替换是否完整无遗漏
  - belongsTo() 新算法逻辑是否正确
  - 历史表快照字段写入逻辑是否健壮（null 防御）
  - 测试用例覆盖是否充分
  - 文档中是否还有遗漏的旧值引用

- [ ] **Step 2: 按 code-review 反馈修复问题**

- [ ] **Step 3: 修复后重新运行全量测试确认**

  ```bash
  mvn test
  ```
  Expected: BUILD SUCCESS

---

## Task 13: 文档同步更新

**Files:**
- Modify: `.docs/技术实现/order/00_订单状态机设计方案.md`
- Modify: `.docs/技术实现/order/01_订单流程实现方案.md`
- Modify: `.docs/测试文档/订单流转状态机接口测试文档.md`
- Modify: `.docs/接口文档/19_订单模块接口文档.md`

- [ ] **Step 1: 更新 00_订单状态机设计方案.md**

  - 更新文档顶部的枚举编码规则说明章节，增加"新编码体系（v2）"说明
  - 将文档中所有 FlowPhaseEnum 表格行的值更新（1→10, 2→20...）
  - 将文档中所有 FlowStatusEnum 表格行的值更新（按对照表）
  - 将所有正文描述中如"阶段1"、"状态10"、"状态值为21"等表述更新为新值
  - 将"不可见状态 DESIGN_REVIEW_PASSED(25)"等括号内的旧值更新为 (2005)
  - 在文档末尾追加变更历史：`v5.0 2026-04-11: 枚举编码体系重设计，Phase 间隔10，Status = phase×100+序号`

- [ ] **Step 2: 更新 01_订单流程实现方案.md**

  - 同样更新所有枚举整数值引用
  - 更新文档版本号和变更历史

- [ ] **Step 3: 更新 订单流转状态机接口测试文档.md**

  - 更新测试场景中的初始状态值和期望状态值
  - 更新所有"当前状态=XX"、"变更后状态=XX"等数值描述

- [ ] **Step 4: 更新 19_订单模块接口文档.md**

  - 更新接口文档中枚举值说明表格（如 phase 枚举说明、status 枚举说明）
  - 更新示例 JSON 中的 phase/status 字段值

- [ ] **Step 5: Commit 文档变更**

  ```bash
  git add .docs/
  git commit -m "docs: 同步更新文档中的 FlowPhaseEnum/FlowStatusEnum 枚举值为新编码体系"
  ```

---

## Task 14: 最终验证与收尾

- [ ] **Step 1: 运行全量测试最终确认**

  ```bash
  cd yigongbao-parent
  mvn test
  ```
  Expected: 全部模块 BUILD SUCCESS

- [ ] **Step 2: 检查是否有遗漏的旧值引用**

  ```bash
  # 从仓库根目录执行，搜索所有子模块源码
  # 搜索旧的 phase 值（1-8 作为 Phase 语义出现）
  grep -rn "phase.*=.*[1-8][^0-9]" --include="*.java" yigongbao-parent/
  # 搜索旧的 status 值（10-80 两位数）
  grep -rn "status.*=.*[1-8][0-9][^0-9]" --include="*.java" yigongbao-parent/
  ```
  如有命中，逐一确认是否为遗漏替换。

- [ ] **Step 3: 最终 Commit（如有遗漏修复）**

---

## 重要注意事项

1. **Task 1-3（SQL + Entity）必须先于 Task 4-5（枚举）完成**，否则编译时 Entity 字段与 DB 不一致。

2. **Task 4-5（枚举更新）完成后，所有测试会立即失败**（旧硬编码值不匹配），Task 9 修复测试时间窗口内保持 failing 是正常的。

3. **`belongsTo()` 新算法前提**：Status 值必须严格遵循 `phase×100+序号` 格式，枚举中不能出现不符合此规则的值。

4. **ddl.sql 的 TINYINT→INT 变更**：当前 `phase` 和 `status` 列类型为 `TINYINT`（范围 -128~127），新 status 最大值为 8001，超出 TINYINT，必须改为 `INT`。H2 的 schema.sql 已经是 `INT`，只有 `ddl.sql` 需要修改。

5. **历史表新增列均可为 NULL**，对已有测试中的 INSERT 语句无破坏性影响。
