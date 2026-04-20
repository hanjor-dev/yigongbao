# yigongbao-module-imaging 设计文档

**日期**：2026-04-20  
**状态**：已确认

---

## 一、模块定位

`yigongbao-module-imaging` 是系统通用的影像数据提供模块，**只做查询，不做写入**。

核心职责：聚合订单模块（DCM影像文件）和设计模块（3D模型文件）的数据，统一为前端阅览场景服务。未来订单阶段或其他阶段需要调阅影像/模型时，直接接入此模块，无需各业务模块自行实现。

---

## 二、接口清单

| 接口 | 维度 | 数据来源 | 说明 |
|------|------|----------|------|
| `GET /imaging/{orderId}/dcm-packages` | 订单 | `order_file`（fileCategory=10.1） | 影像数据包地址列表，前端自行解析展示 |
| `GET /imaging/{orderId}/package-model-files` | 订单 | `design_package_file` | 该订单所有打印包内模型文件列表，附颜色+透明度，按包分组 |
| `GET /imaging/design-package/{packageId}/model-files` | 数据包 | `design_package_file` | 指定打印包内模型文件列表，附颜色+透明度 |
| `GET /imaging/{orderId}/models` | 订单 | `design_model` | 可视化模型列表，附颜色+透明度 |

**说明**：
- DCM影像接口只有订单维度，不提供数据包维度
- 可视化模型（`design_model`）只关联订单，不关联数据包，故也只有订单维度
- 打印数据包内模型文件提供两个维度（订单/数据包），满足不同页面场景

---

## 三、返回数据结构

### 3.1 DCM影像包列表

```json
// GET /imaging/{orderId}/dcm-packages
// 返回：List<DcmPackageVO>
[
  {
    "fileId": "abc123",
    "fileName": "影像数据包_20260410.zip",
    "fileUrl": "https://...",
    "fileSize": 1024000,
    "packageNo": "PKG001",
    "uploadTime": "2026-04-10T10:00:00"
  }
]
```

### 3.2 打印数据包内模型文件——订单维度（按包分组）

```json
// GET /imaging/{orderId}/package-model-files
// 返回：List<PackageModelGroupVO>
[
  {
    "packageId": 1,
    "packageCode": "ORD20260410001-1",
    "files": [
      {
        "packageFileId": 101,
        "fileName": "右肺上叶.stl",
        "fileExt": "stl",
        "filePath": "models/右肺上叶.stl",
        "fileSize": 2048,
        "colorCode": "170,255,0",
        "opacity": 0.8
      }
    ]
  }
]
```

### 3.3 打印数据包内模型文件——数据包维度

```json
// GET /imaging/design-package/{packageId}/model-files
// 返回：List<PackageModelFileVO>
[
  {
    "packageFileId": 101,
    "fileName": "右肺上叶.stl",
    "fileExt": "stl",
    "filePath": "models/右肺上叶.stl",
    "fileSize": 2048,
    "colorCode": "170,255,0",
    "opacity": 0.8
  }
]
```

### 3.4 可视化模型列表

```json
// GET /imaging/{orderId}/models
// 返回：List<ModelVO>
[
  {
    "modelId": 1,
    "fileId": "def456",
    "fileName": "整体模型.stl",
    "fileUrl": "https://...",
    "fileSize": 4096,
    "colorCode": "255,0,0",
    "opacity": 0.9
  }
]
```

**颜色透明度未匹配时**：`colorCode` 和 `opacity` 返回 `null`，由前端处理默认展示逻辑。

---

## 四、模块架构

### 4.1 目录结构

```
yigongbao-module-imaging/
├── pom.xml
└── src/main/java/com/yigongbao/module/imaging/
    ├── controller/
    │   └── ImagingController.java         # 4个查询接口
    ├── service/
    │   ├── ImagingService.java
    │   └── impl/ImagingServiceImpl.java
    ├── mapper/
    │   └── PartColorMapper.java           # 查询 part_colors 表
    ├── entity/
    │   └── PartColorEntity.java           # part_colors 表映射
    └── vo/
        ├── DcmPackageVO.java
        ├── PackageModelFileVO.java
        ├── PackageModelGroupVO.java        # 订单维度分组用
        └── ModelVO.java
```

### 4.2 依赖关系

```
yigongbao-module-imaging
    ├── yigongbao-common          # Result<T>、BaseEntity、异常等
    ├── yigongbao-framework       # SaToken、AOP等
    ├── yigongbao-module-order    # 查询 order_file 表
    └── yigongbao-module-design   # 查询 design_package_file、design_model 表
```

`yigongbao-boot` 的 pom 新增对 `yigongbao-module-imaging` 的依赖。

### 4.3 颜色透明度匹配逻辑

- 文件名去掉扩展名后与 `part_colors.part_detail` 精确匹配
- 为避免 N+1 查询：先收集所有文件名（去扩展名），一次 `WHERE part_detail IN (...)` 批量查出匹配记录，在内存中组装结果

### 4.4 权限

接口需要登录认证（`@SaCheckLogin`），不做细粒度角色限制。

---

## 五、数据库变更

### 5.1 part_colors 表迁移

将 `image-3d-ai.part_colors` 迁移到医工宝主库，表结构调整如下：

```sql
CREATE TABLE part_colors
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    part_detail  VARCHAR(255) NULL COMMENT '部位名称（与模型文件名精确匹配）',
    color_code   VARCHAR(255) NULL COMMENT '颜色RGB值（如：170,255,0）',
    opacity      DECIMAL(3, 2) DEFAULT 1.00 COMMENT '透明度（0.00~1.00，1=不透明）'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部位颜色透明度配置表';
```

- DDL 写入 `sql/ddl.sql`
- 初始数据（从 `image-3d-ai` 导出）写入 `sql/init.sql`，`opacity` 统一默认 `1.00`

### 5.2 无其他表结构变更

本模块只读现有表（`order_file`、`design_package_file`、`design_model`），不修改任何现有表结构。
