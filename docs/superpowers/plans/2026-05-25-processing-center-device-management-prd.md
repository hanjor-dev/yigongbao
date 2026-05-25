# 加工中心与设备管理模块产品需求规格说明书（PRD）

**文档版本**：v1.1  
**创建日期**：2026-05-25  
**最后更新**：2026-05-25  
**作者**：Kiro AI Agent  
**项目**：医工宝系统 - 基础模块扩展

---

## 一、文档概述

### 1.1 文档目的

本文档描述医工宝系统**加工中心管理**和**设备管理**两个新增功能模块的详细产品需求，包括功能定义、数据模型、接口设计、技术实现方案等。

### 1.2 模块定位

这两个模块属于**基础模块（yigongbao-module-basic）**的扩展，为生产管理模块提供基础数据支撑：

- **加工中心管理**：配置和管理各个生产加工中心的基本信息和设备分配范围
- **设备管理**：管理生产关键设备，实时接收各加工中心推送的设备状态信息

### 1.3 与生产模块的关系

```
基础模块（本文档）                    生产模块
├── 加工中心管理                      ├── 生产流转卡管理
│   └── 配置中心信息                  │   └── 关联加工中心
│   └── 配置设备ID范围                │   └── 分配到具体中心
├── 设备管理                          ├── 工序管理
│   └── 设备基础信息                  │   └── 选择设备
│   └── 设备状态（实时）              │   └── 记录设备使用
│   └── WebSocket接收状态推送         │   └── 检查设备可用性
```

**核心价值**：
- 加工中心管理提供生产任务分配的基础数据
- 设备管理提供设备可用性的实时信息
- 支持多加工中心协同生产的业务场景

---

## 二、需求背景

### 2.1 业务背景

医工宝系统当前支持单一生产中心的生产管理，随着业务扩展，需要支持**多加工中心协同生产**的场景：

1. **多地生产**：公司在不同城市设立多个加工中心（如武汉、上海、北京）
2. **设备分布**：每个加工中心拥有不同数量和类型的生产设备
3. **任务分配**：订单需要根据加工中心的设备资源和负载情况进行分配
4. **状态同步**：各加工中心的设备状态需要实时同步到云端系统

### 2.2 现有问题

1. **缺少加工中心概念**：系统中没有加工中心的数据模型，无法区分不同生产地点
2. **设备管理缺失**：生产模块中设备信息不完整，缺少设备类型、状态等关键信息
3. **状态同步缺失**：设备状态依赖手动更新，无法实时反映设备可用性
4. **任务分配困难**：无法根据加工中心的设备资源进行智能任务分配

### 2.3 解决方案

新增两个功能模块：

1. **加工中心管理**：
   - 配置各加工中心的基本信息（名称、地址、联系方式等）
   - 为每个加工中心分配可用的设备ID范围
   - 支持加工中心的启用/禁用管理

2. **设备管理**：
   - 管理所有生产设备的基础信息（设备编号、类型、所属中心等）
   - 通过WebSocket接收各加工中心推送的设备状态
   - 提供设备状态查询接口（空闲/占用、在线/离线）
   - 支持设备状态变更历史追溯

---

## 三、功能需求

### 3.1 加工中心管理

#### 3.1.1 功能概述

提供加工中心的增删改查功能，支持配置每个加工中心的可用设备ID范围。

#### 3.1.2 功能列表

| 功能编号 | 功能名称 | 功能描述 | 优先级 |
|---------|---------|---------|:------:|
| F-PC-001 | 查询加工中心列表 | 分页查询加工中心，支持按名称、状态筛选 | P0 |
| F-PC-002 | 查询加工中心详情 | 根据ID查询加工中心详细信息 | P0 |
| F-PC-003 | 创建加工中心 | 创建新的加工中心记录 | P0 |
| F-PC-004 | 更新加工中心 | 更新加工中心信息 | P0 |
| F-PC-005 | 删除加工中心 | 逻辑删除加工中心（软删除） | P0 |
| F-PC-006 | 查询所有加工中心 | 不分页查询所有启用的加工中心（下拉选择用） | P0 |
| F-PC-007 | 配置设备ID范围 | 为加工中心配置可用的设备ID范围 | P0 |
| F-PC-008 | 校验设备ID范围 | 校验设备ID是否在加工中心的可用范围内 | P1 |

#### 3.1.3 详细功能说明

**F-PC-001：查询加工中心列表**

- **输入**：
  - 分页参数：current（当前页）、size（每页大小）
  - 筛选条件：centerName（中心名称，模糊查询）、status（状态）
  
- **输出**：
  - 分页结果：records（记录列表）、total（总数）、current、size、pages
  - 每条记录包含：id、centerCode、centerName、contactPerson、contactPhone、address、status、deviceIdRanges、createTime、updateTime

- **业务规则**：
  - 默认按创建时间倒序排列
  - 支持按中心名称模糊查询
  - 支持按状态筛选（启用/禁用）

**F-PC-003：创建加工中心**

- **输入**：
  - centerCode：中心编码（必填，唯一）
  - centerName：中心名称（必填，唯一）
  - contactPerson：联系人（可选）
  - contactPhone：联系电话（可选）
  - address：地址（可选）
  - deviceIdRanges：设备ID范围配置（JSON格式，可选）
  - remark：备注（可选）

- **输出**：
  - 成功：返回创建的加工中心ID
  - 失败：返回错误信息

- **业务规则**：
  - centerCode必须唯一（数据库唯一索引约束）
  - centerCode格式：大写字母+数字，如"WH001"（武汉1号中心）
  - 创建时默认状态为"启用"
  - deviceIdRanges格式见下文

**设备ID范围配置格式**：

```json
{
  "ranges": [
    {
      "prefix": "SLA",
      "start": 1,
      "end": 100,
      "description": "光固化3D打印机"
    },
    {
      "prefix": "DLP",
      "start": 1,
      "end": 50,
      "description": "DLP打印机"
    },
    {
      "prefix": "UV",
      "start": 1,
      "end": 20,
      "description": "UV固化机"
    }
  ]
}
```

**说明**：
- prefix：设备编号前缀（如SLA、DLP、UV、WASH、DRY、SEAL等）
- start：起始编号
- end：结束编号
- description：设备类型描述
- 设备编号格式：`{prefix}-{number}`，如"SLA-001"、"UV-015"

#### 3.1.4 权限要求

| 功能 | 权限码 | 说明 |
|------|--------|------|
| 查询列表 | processingcenter:List | 查看加工中心列表 |
| 查询详情 | processingcenter:View | 查看加工中心详情 |
| 创建 | processingcenter:Add | 创建加工中心 |
| 更新 | processingcenter:Edit | 更新加工中心 |
| 删除 | processingcenter:Delete | 删除加工中心 |

---

### 3.2 设备管理

#### 3.2.1 功能概述

管理生产关键设备的基础信息和实时状态，通过WebSocket接收各加工中心推送的设备状态更新。

#### 3.2.2 设备类型定义

**重要说明**：加工中心通过WebSocket只推送**打印机设备**的状态信息，其他工序设备（如UV固化机、清洗机等）不通过WebSocket推送。

系统支持的设备类型：

| 设备类型代码 | 设备类型名称 | 对应工序 | 说明 |
|------------|------------|---------|------|
| PRINTER_SLA | 光固化3D打印机 | 3D打印成型 | 使用SLA技术的打印机（WebSocket推送） |
| WASH_CONTAINER | 酒精容器 | 酒精初洗 | 用于酒精清洗的容器 |
| UV_CURING | UV固化机 | UV固化 | UV光固化设备 |
| ULTRASONIC_CLEANER | 超声清洗机 | 超声清洗 | 超声波清洗设备 |
| AIR_COMPRESSOR | 空气压缩机 | 干燥 | 用于空气干燥 |
| DRYER | 烘干设备 | 干燥 | 用于加热烘干 |
| SEALING_MACHINE | 封口机 | 包装 | 用于产品封口包装 |

**备注**：当前版本仅支持光固化3D打印机（PRINTER_SLA）的实时状态推送。

#### 3.2.3 功能列表

| 功能编号 | 功能名称 | 功能描述 | 优先级 |
|---------|---------|---------|:------:|
| F-DV-001 | 查询设备列表 | 分页查询设备，支持按中心、类型、状态筛选 | P0 |
| F-DV-002 | 查询设备详情 | 根据ID查询设备详细信息 | P0 |
| F-DV-003 | 手动创建设备 | 手动创建设备记录（管理员功能） | P1 |
| F-DV-004 | 手动更新设备状态 | 手动修改设备状态（管理员功能） | P1 |
| F-DV-005 | 查询在线设备 | 查询所有在线状态的设备 | P0 |
| F-DV-006 | 查询空闲设备 | 查询空闲且在线的设备（用于任务分配） | P0 |
| F-DV-007 | WebSocket接收状态推送 | 接收加工中心推送的设备状态 | P0 |
| F-DV-008 | 自动创建设备 | 首次推送时自动创建设备记录 | P0 |
| F-DV-009 | 批量更新设备状态 | 批量更新设备状态（WebSocket触发） | P0 |
| F-DV-010 | 设备离线检测 | 检测长时间未心跳的设备并标记为离线 | P1 |
| F-DV-011 | 查询设备状态历史 | 查询设备状态变更历史记录 | P2 |

#### 3.2.4 详细功能说明

**F-DV-001：查询设备列表**

- **输入**：
  - 分页参数：current、size
  - 筛选条件：
    - centerId：加工中心ID
    - deviceType：设备类型
    - state：设备状态（0=空闲，1=占用）
    - connectionStatus：连接状态（0=离线，1=在线）
    - deviceId：设备编号（模糊查询）

- **输出**：
  - 分页结果，每条记录包含：
    - id、deviceId、deviceName、deviceType、centerId、centerName
    - state、connectionStatus、lastHeartbeat
    - createTime、updateTime

- **业务规则**：
  - 默认按更新时间倒序排列
  - 支持多条件组合筛选

**F-DV-006：查询空闲设备**

- **输入**：
  - centerId：加工中心ID（可选，不传则查询所有中心）
  - deviceType：设备类型（可选，不传则查询所有类型）

- **输出**：
  - 设备列表（不分页）
  - 每条记录包含：deviceId、deviceName、deviceType、centerName

- **业务规则**：
  - 只返回 state=0（空闲）且 connectionStatus=1（在线）的设备
  - 用于生产任务分配时选择可用设备

**F-DV-007：WebSocket接收状态推送**

- **连接URL**：`ws://ip:port/api/basic/ws/device`
  - 无需URL参数，加工中心信息通过消息体传递

- **消息格式**：
```json
{
  "center_name": "武汉嘉一",
  "devices": [
    {"id": "SLA-001", "state": 1},
    {"id": "SLA-002", "state": 0}
  ]
}
```

- **字段说明**：
  - center_name：加工中心名称（必填，用于识别推送来源）
  - devices：打印机设备状态数组（必填，仅包含打印机）
    - id：设备编号（必填）
    - state：设备状态（必填，0=空闲，1=占用）

- **业务规则**：
  - 首次连接时发送所有打印机设备状态（全量推送）
  - 后续保持连接时只发送变化的设备状态（增量推送）
  - 系统根据center_name查询加工中心信息
  - 如果设备不存在，自动创建设备记录（设备类型默认为PRINTER_SLA）
  - 如果设备存在，更新设备状态和心跳时间
  - 连接断开时，将该中心所有设备标记为离线

**F-DV-009：批量更新设备状态**

- **触发时机**：WebSocket收到设备状态推送消息

- **处理逻辑**：
  1. 解析消息，提取center_name和devices数组
  2. 根据center_name查询加工中心信息
  3. 遍历devices数组：
     - 根据deviceId查询设备记录
     - 如果设备不存在：自动创建设备记录（设备类型默认为PRINTER_SLA）
     - 如果设备存在：更新state、connectionStatus、lastHeartbeat
     - 如果状态发生变化：记录状态变更日志
  4. 更新加工中心的最后心跳时间

- **业务规则**：
  - 自动创建的设备：deviceName默认为deviceId，deviceType固定为PRINTER_SLA
  - 状态变更日志：记录oldState、newState、changeTime
  - 心跳时间：每次收到推送都更新lastHeartbeat

**F-DV-010：设备离线检测**

- **检测规则**：
  - 如果设备的lastHeartbeat超过5分钟未更新，标记为离线
  - 定时任务每分钟执行一次检测

- **处理逻辑**：
  1. 查询所有connectionStatus=1（在线）的设备
  2. 检查lastHeartbeat是否超过5分钟
  3. 超时的设备更新connectionStatus=0（离线）
  4. 记录离线日志

#### 3.2.5 权限要求

| 功能 | 权限码 | 说明 |
|------|--------|------|
| 查询列表 | device:List | 查看设备列表 |
| 查询详情 | device:View | 查看设备详情 |
| 手动创建 | device:Add | 手动创建设备 |
| 手动更新状态 | device:UpdateState | 手动更新设备状态 |
| 查询在线设备 | device:List | 查看在线设备 |
| 查询空闲设备 | device:List | 查看空闲设备 |


---

## 四、数据模型设计

### 4.1 加工中心表（processing_center）

| 字段名 | 类型 | 长度 | 说明 | 约束 |
|--------|------|------|------|------|
| id | BIGINT | - | 主键 | PRIMARY KEY, AUTO_INCREMENT |
| center_code | VARCHAR | 50 | 中心编码 | NOT NULL, UNIQUE |
| center_name | VARCHAR | 100 | 中心名称 | NOT NULL |
| contact_person | VARCHAR | 50 | 联系人 | NULL |
| contact_phone | VARCHAR | 20 | 联系电话 | NULL |
| address | VARCHAR | 200 | 地址 | NULL |
| device_id_ranges | TEXT | - | 可用设备ID范围（JSON） | NULL |
| status | TINYINT | - | 状态（0=禁用，1=启用） | NOT NULL, DEFAULT 1 |
| remark | VARCHAR | 500 | 备注 | NULL |
| create_time | DATETIME | - | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | - | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| create_by | BIGINT | - | 创建人ID | NULL |
| update_by | BIGINT | - | 更新人ID | NULL |
| is_deleted | TINYINT | - | 是否删除（0=否，1=是） | NOT NULL, DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE INDEX uk_center_code ON processing_center ((CASE WHEN is_deleted = 0 THEN center_code ELSE NULL END))
- INDEX idx_status (status)
- INDEX idx_center_name (center_name)

**建表SQL**：
```sql
CREATE TABLE processing_center (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    center_code VARCHAR(50) NOT NULL COMMENT '中心编码',
    center_name VARCHAR(100) NOT NULL COMMENT '中心名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    device_id_ranges TEXT COMMENT '可用设备ID范围（JSON数组）',
    status TINYINT DEFAULT 1 COMMENT '状态（0=禁用，1=启用）',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_status (status),
    KEY idx_center_name (center_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加工中心表';

CREATE UNIQUE INDEX uk_center_code
    ON processing_center ((CASE WHEN is_deleted = 0 THEN center_code ELSE NULL END));
```

### 4.2 设备表（device）

| 字段名 | 类型 | 长度 | 说明 | 约束 |
|--------|------|------|------|------|
| id | BIGINT | - | 主键 | PRIMARY KEY, AUTO_INCREMENT |
| device_id | VARCHAR | 50 | 设备编号（如SLA-001） | NOT NULL, UNIQUE |
| device_name | VARCHAR | 100 | 设备名称 | NULL |
| device_type | VARCHAR | 50 | 设备类型 | NULL |
| center_id | BIGINT | - | 所属加工中心ID | NULL |
| center_name | VARCHAR | 100 | 所属加工中心名称（冗余） | NULL |
| state | TINYINT | - | 设备状态（0=空闲，1=占用） | NOT NULL, DEFAULT 0 |
| connection_status | TINYINT | - | 连接状态（0=离线，1=在线） | NOT NULL, DEFAULT 0 |
| last_heartbeat | DATETIME | - | 最后心跳时间 | NULL |
| remark | VARCHAR | 500 | 备注 | NULL |
| create_time | DATETIME | - | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | DATETIME | - | 更新时间 | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP |
| create_by | BIGINT | - | 创建人ID | NULL |
| update_by | BIGINT | - | 更新人ID | NULL |
| is_deleted | TINYINT | - | 是否删除（0=否，1=是） | NOT NULL, DEFAULT 0 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE INDEX uk_device_id ON device ((CASE WHEN is_deleted = 0 THEN device_id ELSE NULL END))
- INDEX idx_center_id (center_id)
- INDEX idx_device_type (device_type)
- INDEX idx_state (state)
- INDEX idx_connection_status (connection_status)

**建表SQL**：
```sql
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(50) NOT NULL COMMENT '设备编号（如SLA-001）',
    device_name VARCHAR(100) COMMENT '设备名称',
    device_type VARCHAR(50) COMMENT '设备类型',
    center_id BIGINT COMMENT '所属加工中心ID',
    center_name VARCHAR(100) COMMENT '所属加工中心名称（冗余字段）',
    state TINYINT DEFAULT 0 COMMENT '设备状态（0=空闲，1=占用）',
    connection_status TINYINT DEFAULT 0 COMMENT '连接状态（0=离线，1=在线）',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除（0=否，1=是）',
    KEY idx_center_id (center_id),
    KEY idx_device_type (device_type),
    KEY idx_state (state),
    KEY idx_connection_status (connection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE UNIQUE INDEX uk_device_id
    ON device ((CASE WHEN is_deleted = 0 THEN device_id ELSE NULL END));
```

### 4.3 设备状态变更日志表（device_state_log）

| 字段名 | 类型 | 长度 | 说明 | 约束 |
|--------|------|------|------|------|
| id | BIGINT | - | 主键 | PRIMARY KEY, AUTO_INCREMENT |
| device_id | VARCHAR | 50 | 设备编号 | NOT NULL |
| old_state | TINYINT | - | 旧状态 | NULL |
| new_state | TINYINT | - | 新状态 | NOT NULL |
| change_time | DATETIME | - | 变更时间 | DEFAULT CURRENT_TIMESTAMP |
| change_type | VARCHAR | 20 | 变更类型（auto/manual） | NULL |
| operator_id | BIGINT | - | 操作人ID（手动变更时） | NULL |

**索引**：
- PRIMARY KEY (id)
- INDEX idx_device_id (device_id)
- INDEX idx_change_time (change_time)

**建表SQL**：
```sql
CREATE TABLE device_state_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id VARCHAR(50) NOT NULL COMMENT '设备编号',
    old_state TINYINT COMMENT '旧状态',
    new_state TINYINT NOT NULL COMMENT '新状态',
    change_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    change_type VARCHAR(20) COMMENT '变更类型（auto=自动，manual=手动）',
    operator_id BIGINT COMMENT '操作人ID（手动变更时）',
    KEY idx_device_id (device_id),
    KEY idx_change_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备状态变更日志';
```

### 4.4 表关系说明

```
processing_center（加工中心）
  └─ 1:N ─ device（设备）
      └─ 1:N ─ device_state_log（状态变更日志）
```

**关系说明**：
- 一个加工中心可以有多个设备
- 一个设备只能属于一个加工中心
- 一个设备可以有多条状态变更日志
- device.center_name是冗余字段，用于快速查询，避免JOIN


---

## 五、接口设计

### 5.1 加工中心管理接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询列表 | POST | /api/basic/processing-center/list | 分页查询 |
| 查询详情 | GET | /api/basic/processing-center/{id} | 根据ID查询 |
| 创建 | POST | /api/basic/processing-center | 创建加工中心 |
| 更新 | PUT | /api/basic/processing-center/{id} | 更新加工中心 |
| 删除 | DELETE | /api/basic/processing-center/{id} | 删除加工中心 |
| 查询所有 | GET | /api/basic/processing-center/all | 下拉选择用 |

### 5.2 设备管理接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询列表 | POST | /api/basic/device/list | 分页查询 |
| 查询详情 | GET | /api/basic/device/{id} | 根据ID查询 |
| 手动创建 | POST | /api/basic/device | 手动创建设备 |
| 手动更新状态 | PUT | /api/basic/device/{id}/state | 手动更新状态 |
| 查询空闲设备 | GET | /api/basic/device/idle | 查询空闲设备 |
| 查询状态历史 | GET | /api/basic/device/{deviceId}/state-log | 查询状态历史 |

### 5.3 WebSocket接口

| 接口 | 协议 | 路径 | 说明 |
|------|------|------|------|
| 设备状态推送 | WebSocket | /api/basic/ws/device | 接收打印机设备状态推送 |

---

## 六、WebSocket技术实现方案

### 6.1 技术选型

**方案**：使用Spring WebSocket（原生WebSocket，不使用STOMP）

**理由**：
- 客户端是加工中心系统，可能不支持STOMP协议
- 原生WebSocket更灵活，消息格式自定义
- 实现简单，性能更好

### 6.2 核心组件设计

#### 6.2.1 WebSocket配置类

```java
@Configuration
@EnableWebSocket
public class DeviceWebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private DeviceWebSocketHandler deviceWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deviceWebSocketHandler, "/api/basic/ws/device")
                .setAllowedOrigins("*");
    }
}
```

#### 6.2.2 WebSocket处理器

负责处理WebSocket连接、消息接收、连接断开等事件。

#### 6.2.3 连接管理器

维护WebSocket连接和心跳时间，支持连接查询和心跳更新。

### 6.3 设备类型处理

**设备类型默认规则**：
- WebSocket推送的设备统一默认为 `PRINTER_SLA`（光固化3D打印机）
- 无需根据设备编号前缀进行类型推断
- 如需支持其他设备类型，可通过管理后台手动修改

### 6.4 设备离线检测

**检测规则**：
- 如果设备的lastHeartbeat超过5分钟未更新，标记为离线
- 定时任务每分钟执行一次检测

**实现方式**：
- 使用Spring @Scheduled注解实现定时任务
- cron表达式：`0 * * * * ?`（每分钟执行一次）


---

## 七、代码结构设计

### 7.1 模块目录结构

```
yigongbao-module-basic/
├── src/main/java/com/yigongbao/module/basic/
│   ├── processingCenter/              # 加工中心管理模块
│   │   ├── controller/
│   │   │   └── ProcessingCenterController.java
│   │   ├── service/
│   │   │   ├── IProcessingCenterService.java
│   │   │   └── impl/
│   │   │       └── ProcessingCenterServiceImpl.java
│   │   ├── mapper/
│   │   │   └── ProcessingCenterMapper.java
│   │   ├── entity/
│   │   │   └── ProcessingCenterEntity.java
│   │   ├── dto/
│   │   │   ├── CreateProcessingCenterDTO.java
│   │   │   ├── UpdateProcessingCenterDTO.java
│   │   │   └── ProcessingCenterPageDTO.java
│   │   ├── vo/
│   │   │   └── ProcessingCenterVO.java
│   │   └── convert/
│   │       └── ProcessingCenterConvert.java
│   │
│   └── device/                        # 设备管理模块
│       ├── controller/
│       │   └── DeviceController.java
│       ├── service/
│       │   ├── IDeviceService.java
│       │   ├── IDeviceStateLogService.java
│       │   └── impl/
│       │       ├── DeviceServiceImpl.java
│       │       └── DeviceStateLogServiceImpl.java
│       ├── mapper/
│       │   ├── DeviceMapper.java
│       │   └── DeviceStateLogMapper.java
│       ├── entity/
│       │   ├── DeviceEntity.java
│       │   └── DeviceStateLogEntity.java
│       ├── dto/
│       │   ├── CreateDeviceDTO.java
│       │   ├── UpdateDeviceStateDTO.java
│       │   ├── DevicePageDTO.java
│       │   └── DeviceStatusPushDTO.java
│       ├── vo/
│       │   ├── DeviceVO.java
│       │   └── DeviceStateLogVO.java
│       ├── convert/
│       │   ├── DeviceConvert.java
│       │   └── DeviceStateLogConvert.java
│       ├── enums/
│       │   └── DeviceTypeEnum.java
│       ├── websocket/
│       │   ├── DeviceWebSocketConfig.java
│       │   ├── DeviceWebSocketHandler.java
│       │   └── DeviceConnectionManager.java
│       └── task/
│           └── DeviceOfflineDetectionTask.java
```

### 7.2 关键类说明

#### 7.2.1 加工中心管理

**ProcessingCenterController**：
- 提供加工中心的CRUD接口
- 使用`@RequirePermission`注解控制权限
- 返回统一的`Result<T>`格式

**ProcessingCenterServiceImpl**：
- 实现加工中心的业务逻辑
- 继承`ServiceImpl<ProcessingCenterMapper, ProcessingCenterEntity>`
- 实现`IProcessingCenterService`接口
- 记录操作日志

**ProcessingCenterEntity**：
- 继承`BaseEntity`
- 使用`@TableName("processing_center")`注解
- deviceIdRanges字段存储JSON格式的设备ID范围配置

#### 7.2.2 设备管理

**DeviceController**：
- 提供设备的查询和管理接口
- 支持按中心、类型、状态筛选
- 提供空闲设备查询接口

**DeviceServiceImpl**：
- 实现设备的业务逻辑
- 提供批量更新设备状态的方法
- 提供设备离线标记的方法
- 自动创建设备时默认类型为PRINTER_SLA

**DeviceWebSocketHandler**：
- 处理WebSocket连接建立、消息接收、连接断开
- 调用DeviceService批量更新设备状态
- 连接断开时标记该中心所有设备为离线

**DeviceConnectionManager**：
- 维护WebSocket连接映射
- 维护心跳时间映射
- 提供连接查询和心跳更新方法

**DeviceOfflineDetectionTask**：
- 定时检测设备离线
- 使用`@Scheduled(cron = "0 * * * * ?")`注解
- 每分钟执行一次

#### 7.2.3 枚举类

**DeviceTypeEnum**：
```java
public enum DeviceTypeEnum {
    PRINTER_SLA("PRINTER_SLA", "光固化3D打印机"),
    WASH_CONTAINER("WASH_CONTAINER", "酒精容器"),
    UV_CURING("UV_CURING", "UV固化机"),
    ULTRASONIC_CLEANER("ULTRASONIC_CLEANER", "超声清洗机"),
    AIR_COMPRESSOR("AIR_COMPRESSOR", "空气压缩机"),
    DRYER("DRYER", "烘干设备"),
    SEALING_MACHINE("SEALING_MACHINE", "封口机");
    
    private final String code;
    private final String name;
    
    DeviceTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
}
```

**说明**：WebSocket推送的设备默认类型为PRINTER_SLA，无需前缀推断逻辑。

---

## 八、非功能性需求

### 8.1 性能要求

| 指标 | 要求 | 说明 |
|------|------|------|
| 接口响应时间 | < 500ms | 正常网络环境下 |
| WebSocket消息处理 | < 100ms | 单条消息处理时间 |
| 并发连接数 | 支持100个 | 同时连接的加工中心数量 |
| 设备状态更新 | 实时 | 收到推送后立即更新 |

### 8.2 可用性要求

| 指标 | 要求 | 说明 |
|------|------|------|
| 系统可用性 | 99% | 年度统计 |
| WebSocket重连 | 自动重连 | 客户端断线后自动重连 |
| 数据一致性 | 强一致性 | 设备状态更新保证一致性 |

### 8.3 安全性要求

| 指标 | 要求 | 说明 |
|------|------|------|
| 权限控制 | 基于角色的访问控制 | 使用@RequirePermission注解 |
| WebSocket认证 | 连接参数校验 | 校验centerCode是否存在 |
| 数据校验 | 输入参数校验 | 使用@Validated注解 |

### 8.4 可维护性要求

| 指标 | 要求 | 说明 |
|------|------|------|
| 代码规范 | 遵循项目编码规范 | 见CLAUDE.md和java-coding-standards.md |
| 日志记录 | 遵循日志规范 | 见logging-standards.md |
| 单元测试覆盖率 | > 80% | Service层和Controller层 |

---

## 九、实施计划

### 9.1 开发阶段

**第一阶段：基础功能开发（3天）**

1. **Day 1：数据模型和基础框架**
   - 创建数据库表（processing_center、device、device_state_log）
   - 创建Entity、Mapper、DTO、VO类
   - 创建Convert转换器
   - 创建枚举类（DeviceTypeEnum）

2. **Day 2：加工中心管理功能**
   - 实现ProcessingCenterService
   - 实现ProcessingCenterController
   - 实现设备ID范围校验逻辑
   - 编写单元测试

3. **Day 3：设备管理基础功能**
   - 实现DeviceService（查询、手动创建、手动更新状态）
   - 实现DeviceController
   - 实现设备类型推断逻辑
   - 编写单元测试

**第二阶段：WebSocket功能开发（2天）**

4. **Day 4：WebSocket服务端实现**
   - 实现DeviceWebSocketConfig
   - 实现DeviceWebSocketHandler
   - 实现DeviceConnectionManager
   - 实现批量更新设备状态逻辑

5. **Day 5：设备离线检测和测试**
   - 实现DeviceOfflineDetectionTask
   - 实现设备状态变更日志记录
   - WebSocket功能测试
   - 集成测试

**第三阶段：联调和优化（1天）**

6. **Day 6：联调和优化**
   - 与加工中心客户端联调
   - 性能测试和优化
   - 文档完善
   - 代码审查

### 9.2 测试计划

**单元测试**：
- ProcessingCenterServiceImpl测试
- DeviceServiceImpl测试
- DeviceWebSocketHandler测试

**集成测试**：
- WebSocket连接测试
- 设备状态推送测试
- 设备离线检测测试

**性能测试**：
- 并发连接测试（100个连接）
- 批量设备状态更新测试（1000个设备）
- 接口响应时间测试

### 9.3 部署计划

**部署前准备**：
1. 执行数据库DDL脚本
2. 配置WebSocket端口（确保防火墙开放）
3. 配置定时任务（设备离线检测）

**部署步骤**：
1. 部署后端服务
2. 验证WebSocket连接
3. 通知各加工中心配置连接参数
4. 监控设备状态推送

**回滚方案**：
- 保留旧版本代码
- 数据库表结构向下兼容
- WebSocket连接失败不影响其他功能

---

## 十、风险评估

### 10.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| WebSocket连接不稳定 | 高 | 中 | 实现自动重连机制，客户端定时心跳 |
| 设备状态推送延迟 | 中 | 低 | 优化消息处理逻辑，异步处理 |
| 并发连接数超限 | 高 | 低 | 限制连接数，优化资源使用 |

### 10.2 业务风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 加工中心客户端不支持WebSocket | 高 | 低 | 提供HTTP轮询备选方案 |
| 设备编号规则不统一 | 中 | 中 | 制定统一的设备编号规范 |
| 设备类型推断错误 | 中 | 低 | 支持手动修正设备类型 |

---

## 十一、附录

### 11.1 设备编号规范

**格式**：`{prefix}-{number}`

**示例**：
- SLA-001：光固化3D打印机001号
- UV-015：UV固化机015号
- WASH-003：酒精容器003号

**规则**：
- prefix：大写字母，表示设备类型
- number：3位数字，不足3位前面补0
- 中间用短横线连接

### 11.2 WebSocket客户端示例

**JavaScript示例**：
```javascript
const ws = new WebSocket('ws://ip:port/api/basic/ws/device');

ws.onopen = function() {
    console.log('WebSocket连接已建立');
    
    // 首次连接发送所有打印机设备状态
    const message = {
        center_name: '武汉嘉一',
        devices: [
            {id: 'SLA-001', state: 0},
            {id: 'SLA-002', state: 1}
        ]
    };
    ws.send(JSON.stringify(message));
};

ws.onmessage = function(event) {
    const response = JSON.parse(event.data);
    console.log('收到服务器响应:', response);
};

ws.onerror = function(error) {
    console.error('WebSocket错误:', error);
};

ws.onclose = function() {
    console.log('WebSocket连接已关闭');
    // 实现自动重连
    setTimeout(() => {
        // 重新连接
    }, 5000);
};

// 设备状态变化时发送增量更新
function sendDeviceStateChange(deviceId, state) {
    const message = {
        center_name: '武汉嘉一',
        devices: [{id: deviceId, state: state}]
    };
    ws.send(JSON.stringify(message));
}
```

**说明**：
- 连接URL无需centerCode参数
- 消息体中的center_name用于识别加工中心
- 仅推送打印机设备状态（设备ID如SLA-001、SLA-002等）

### 11.3 参考文档

- [医工宝 Java 编码规范](../../.claude/rules/java-coding-standards.md)
- [医工宝日志规范](../../.claude/rules/logging-standards.md)
- [生产管理模块需求分析](../../.docs/需求分析/v1/生产管理模块需求分析_v2.md)

---

**文档结束**

## 版本历史

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| v1.0 | 2026-05-25 | 初始版本创建 | Kiro AI Agent |
| v1.1 | 2026-05-25 | 根据实际需求优化：<br>1. WebSocket连接URL移除centerCode参数<br>2. 明确只推送打印机设备信息<br>3. 设备类型默认为PRINTER_SLA，移除前缀推断逻辑<br>4. 移除DLP打印机类型<br>5. 简化DeviceTypeEnum枚举类<br>6. 更新WebSocket客户端示例 | Kiro AI Agent |

**文档版本**：v1.1  
**创建日期**：2026-05-25  
**作者**：Kiro AI Agent  
**审核状态**：待审核

