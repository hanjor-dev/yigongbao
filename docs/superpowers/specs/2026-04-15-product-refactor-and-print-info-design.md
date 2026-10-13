# 产品体系重构 + 打印信息管理 设计规范

## 背景

原 `product` 表为单层扁平结构（产品名+规格合并存储），与 `design_product.spec_id` 的外键设计意图不符，且不支持一个产品多规格的场景。本次重构将产品拆分为两层（产品主体 + 规格），同时实现设计阶段打印信息管理功能。

---

## 概念说明

| 概念 | 所属模块 | 说明 |
|---|---|---|
| `product` | basic | 产品型号主数据（全局目录，管理员维护） |
| `product_spec` | basic | 产品规格（属于某产品，含注册证关联） |
| `design_product` | design | 某订单某数据包的一条打印任务记录（设计师填写） |

`design_product.product_id → product.id`，`design_product.spec_id → product_spec.id`。两者不是同一概念。

---

## 一、数据库变更

### 1.1 删除旧 product 表，新建两张表

```sql
-- 产品主表
DROP TABLE IF EXISTS product;
CREATE TABLE product (
    id            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_name  VARCHAR(128)   NOT NULL COMMENT '产品名称',
    category      VARCHAR(20)    NOT NULL COMMENT '产品大类 dict_code（如 17.1=模型类）',
    category_name VARCHAR(64)    DEFAULT NULL COMMENT '产品大类名称（冗余）',
    status        TINYINT        DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark        VARCHAR(512)   DEFAULT NULL COMMENT '备注',
    create_time   DATETIME       DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by     BIGINT         DEFAULT NULL,
    update_by     BIGINT         DEFAULT NULL,
    is_deleted    TINYINT        DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_category (category),
    KEY idx_product_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品主表';
CREATE UNIQUE INDEX uk_product_name ON product ((CASE WHEN is_deleted=0 THEN product_name ELSE NULL END));

-- 产品规格表
DROP TABLE IF EXISTS product_spec;
CREATE TABLE product_spec (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id  BIGINT        NOT NULL COMMENT '关联产品ID',
    spec_name   VARCHAR(128)  NOT NULL COMMENT '规格名称（如 47号、A型）',
    cert_id     BIGINT        DEFAULT NULL COMMENT '关联注册证ID',
    cert_no     VARCHAR(64)   DEFAULT NULL COMMENT '注册证号（冗余）',
    sort        INT           DEFAULT 0 COMMENT '排序',
    status      TINYINT       DEFAULT 1 COMMENT '状态（0=禁用，1=正常）',
    remark      VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT        DEFAULT NULL,
    update_by   BIGINT        DEFAULT NULL,
    is_deleted  TINYINT       DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_product_spec_product_id (product_id),
    KEY idx_product_spec_cert_id (cert_id),
    KEY idx_product_spec_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品规格表';
CREATE UNIQUE INDEX uk_product_spec_name ON product_spec ((CASE WHEN is_deleted=0 THEN CONCAT_WS('|', product_id, spec_name) ELSE NULL END));
```

### 1.2 颜色字典重构（init.sql）

删除旧 `dict_code=16` 两级数据（id 90-94），改为三级结构：

```
16（打印颜色）
  16.1（模型类颜色，dict_value='17.1'）
    16.1.1 白色
    16.1.2 肤色
  16.2（导板类颜色，dict_value='17.2'）
    16.2.1 透明
    16.2.2 蓝色
  16.3（假体类颜色，dict_value='17.3'）
    16.3.1 白色
```

二级节点 `dict_value` 存对应产品大类 `dict_code`，供接口层按 `category` 匹配颜色分组。

---

## 二、basic 模块：产品体系重构

### 2.1 文件结构

```
product/
├── entity/
│   ├── ProductEntity.java          # 新结构：productName + category
│   └── ProductSpecEntity.java      # 新增
├── mapper/
│   ├── ProductMapper.java
│   └── ProductSpecMapper.java
├── service/
│   ├── ProductService.java
│   ├── ProductSpecService.java
│   └── impl/
│       ├── ProductServiceImpl.java
│       └── ProductSpecServiceImpl.java
├── controller/
│   └── ProductController.java
├── dto/
│   ├── ProductQueryDTO.java        # 分页+列表合并（含 pageNum/pageSize 可选）
│   ├── CreateProductDTO.java
│   ├── UpdateProductDTO.java
│   ├── CreateProductSpecDTO.java
│   └── UpdateProductSpecDTO.java
├── vo/
│   ├── ProductVO.java              # 含 specs 列表
│   └── ProductSpecVO.java
└── convert/
    └── ProductConvert.java
```

### 2.2 ProductEntity 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| productName | String | 产品名称 |
| category | String | 产品大类 dict_code（如 17.1） |
| categoryName | String | 大类名称（冗余） |
| status | Integer | 0=禁用，1=正常 |
| remark | String | 备注 |

### 2.3 ProductSpecEntity 字段

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| productId | Long | 关联产品ID |
| specName | String | 规格名称 |
| certId | Long | 关联注册证ID（可空） |
| certNo | String | 注册证号（冗余） |
| sort | Integer | 排序 |
| status | Integer | 0=禁用，1=正常 |

### 2.4 接口清单

| 方法 | URL | 说明 |
|---|---|---|
| POST | `/basic/product/page` | 分页查询产品 |
| POST | `/basic/product/list` | 列表查询产品（不分页） |
| GET | `/basic/product/{id}` | 查询产品详情（含 specs 列表） |
| POST | `/basic/product` | 创建产品 |
| PUT | `/basic/product/{id}` | 更新产品 |
| DELETE | `/basic/product/{id}` | 删除产品（有规格时拒绝删除） |
| POST | `/basic/product/list-by-category` | 按大类查询（含 specs），请求体复用 `ProductQueryDTO`（仅用 `category` 字段） |
| POST | `/basic/product/{id}/spec` | 创建规格 |
| GET | `/basic/product/{id}/specs` | 查询规格列表 |
| PUT | `/basic/product/spec/{specId}` | 更新规格 |
| DELETE | `/basic/product/spec/{specId}` | 删除规格 |

删除产品时：若该产品下存在未删除的规格，拒绝删除并返回 `PRODUCT_HAS_SPECS` 错误。
删除规格时：若该规格已被 `design_product`（`is_deleted=0`）引用，拒绝删除并返回 `PRODUCT_SPEC_IN_USE` 错误。已软删除的历史记录不计入引用范围。

### 2.5 ErrorCode 新增

```java
// 现有（保留）：
// PRODUCT_NOT_FOUND(648, ...)
// PRODUCT_EXISTS(649, ...)  ← 改语义为产品名称重复

// 新增：
PRODUCT_HAS_SPECS(656, "产品下存在规格，无法删除"),
PRODUCT_SPEC_NOT_FOUND(657, "产品规格不存在"),
PRODUCT_SPEC_EXISTS(658, "同一产品下规格名称已存在"),
PRODUCT_SPEC_IN_USE(659, "规格已被打印信息引用，无法删除"),
```

---

## 三、design 模块：打印信息管理

### 3.0 design_product 表现有字段（不变更）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| order_id | BIGINT | 订单ID |
| package_id | BIGINT | 数据包ID |
| product_id | BIGINT | 产品ID → product.id |
| product_name | VARCHAR(128) | 产品名称（冗余） |
| spec_id | BIGINT | 规格ID → product_spec.id |
| spec_name | VARCHAR(128) | 规格名称（冗余） |
| cert_no | VARCHAR(64) | 注册证号（冗余，后端从 spec 自动带出，忽略前端传值） |
| material_id | VARCHAR(20) | 材质 dict_code（如 15.1） |
| material_name | VARCHAR(64) | 材质名称（冗余） |
| color_id | VARCHAR(20) | 颜色 dict_code（如 16.1.1） |
| color_name | VARCHAR(64) | 颜色名称（冗余） |
| quantity | INT | 数量 |
| pack_quantity | INT | 包装数量（可空） |
| timeliness | VARCHAR(64) | 时效（可空） |
| product_mark | VARCHAR(128) | 产品标识（可空） |
| package_file_id | BIGINT | 数据包内文件ID → design_package_file.id |
| package_file_name | VARCHAR(256) | 文件名（冗余） |
| sort_order | INT | 排序 |

`SavePrintInfoItemDTO` 的所有字段均与上表映射。`certNo` 字段：**后端保存时从 `product_spec.cert_no` 读取覆盖**，不使用前端传入值，防止数据篡改。

### 3.1 接口：获取选项数据

**URL**：`GET /api/design/workorder/{orderId}/print-info/options`

**权限**：已登录即可（不限操作人）

**响应 VO（PrintInfoOptionsVO）**：

```java
public class PrintInfoOptionsVO {
    private Integer designMode;           // 来自 order_main.design_mode
    private List<PrintInfoProductVO> products; // 产品树（含 specs）
    private List<DictOptionVO> materials; // 材质列表，含 isDefault 标记
    private List<ColorGroupVO> colorGroups; // 颜色分组
}

public class PrintInfoProductVO {
    private Long id;
    private String productName;
    private String category;         // dict_code，如 17.1
    private String categoryName;
    private List<PrintInfoSpecVO> specs;
}

public class PrintInfoSpecVO {
    private Long id;
    private String specName;
    private Long certId;
    private String certNo;
}

public class ColorGroupVO {
    private String categoryCode;     // 如 17.1（来自二级节点 dict_value）
    private String categoryName;     // 如 模型类
    private List<DictOptionVO> colors;
}

public class DictOptionVO {
    private String code;      // dict_code，如 15.1、16.1.1（选项的编码，非字典类型编码）
    private String name;      // dict_name，如 树脂、白色
    private Boolean isDefault; // 仅 materials 使用，15.1=树脂 为默认
}
```

**后端逻辑**：
1. 查订单取 `designMode`（不校验权限，任何人查选项）
2. `productService.listAllWithSpecs()` — 查所有 status=1 的产品，每个产品只返回 status=1 的规格；若某产品所有规格均 status=0 或无规格，仍返回该产品但 specs 为空列表（前端应对空 specs 做禁用处理）
3. `dictService.listByTypeCode("15")` — 材质，`15.1`（树脂）标 `isDefault=true`
4. `dictService.listTreeByTypeCode("16")` — 颜色三级树，二级节点 `dict_value` 是 categoryCode（如 `17.1`），三级节点是颜色选项

### 3.2 接口：查询打印信息列表

**URL**：`GET /api/design/workorder/{orderId}/package/{packageId}/print-info`

**权限**：已登录即可

**响应**：`List<DesignProductVO>`，返回该数据包下所有 `is_deleted=0` 的 `design_product` 记录，按 `sort_order` 升序。供设计师重新打开表单时回显已填写内容。

**DesignProductVO 字段**（与 `design_product` 表一一对应，冗余字段直接返回，不做二次关联查询）：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| orderId | Long | 订单ID |
| packageId | Long | 数据包ID |
| productId | Long | 产品ID |
| productName | String | 产品名称 |
| specId | Long | 规格ID |
| specName | String | 规格名称 |
| certNo | String | 注册证号 |
| materialId | String | 材质 dict_code |
| materialName | String | 材质名称 |
| colorId | String | 颜色 dict_code |
| colorName | String | 颜色名称 |
| quantity | Integer | 数量 |
| packQuantity | Integer | 包装数量 |
| timeliness | String | 时效 |
| productMark | String | 产品标识 |
| packageFileId | Long | 数据包内文件ID |
| packageFileName | String | 文件名 |
| sortOrder | Integer | 排序 |

### 3.3 接口：保存打印信息

**URL**：`POST /api/design/workorder/{orderId}/package/{packageId}/print-info`

**策略**：整包替换（删除该 packageId 下所有旧记录，批量插入新记录）

**空列表语义**：`items` 允许为空列表（`[]`），表示清空该数据包的所有打印信息，此为合法操作。`@NotNull` 校验列表本身不为 null，但不校验非空。

**权限**：当前登录用户必须是该订单的设计师，订单状态必须为 `DESIGN_IN_PROGRESS` 或 `DESIGN_REVIEW_REJECTED`

**请求 DTO（SavePrintInfoDTO）**：

```java
public class SavePrintInfoDTO {
    @NotNull
    private List<SavePrintInfoItemDTO> items;
}

public class SavePrintInfoItemDTO {
    @NotNull private Long packageFileId;
    private String packageFileName;    // 冗余
    @NotNull private Long productId;
    private String productName;        // 冗余
    @NotNull private Long specId;
    private String specName;           // 冗余
    private String certNo;             // 冗余，后端从 product_spec.cert_no 自动覆盖，前端传值被忽略
    private String materialId;         // dict_code，如 15.1
    private String materialName;       // 冗余
    private String colorId;            // dict_code，如 16.1.1
    private String colorName;          // 冗余
    @NotNull @Min(1) private Integer quantity;
    private Integer packQuantity;
    private String timeliness;
    private String productMark;
    private Integer sortOrder;
}
```

**后端校验顺序**：
1. `checkOrderAndPermission(orderId)` — 状态 + 操作人（`DESIGN_OPERATOR_NOT_ALLOWED(740)`）
2. 校验 packageId 的 `orderId` 字段等于传入 orderId
3. 校验每条 `packageFileId` 属于该 packageId
4. 校验每条 `productId` 存在且 status=1；校验每条 `specId` 存在、status=1，且 `spec.productId == productId`（防止跨产品的 specId 混传导致外键不一致）
5. 删除该 packageId 下旧 `design_product` 记录（`is_deleted=0`）
6. 批量插入新记录（`saveBatch`），`certNo` 从步骤 4 查出的 spec 对象中取，不使用前端传值

### 3.4 接口：删除单条打印信息

**URL**：`DELETE /api/design/workorder/{orderId}/package/{packageId}/print-info/{printInfoId}`

**权限**：同上（`checkOrderAndPermission`）

**后端逻辑**：
1. 校验订单状态和操作人
2. 查询 `design_product`，验证 `orderId` 和 `packageId` 匹配
3. 逻辑删除

### 3.5 文件清单

```
design/
├── service/
│   ├── DesignPrintInfoService.java
│   └── impl/DesignPrintInfoServiceImpl.java
├── controller/
│   └── DesignPrintInfoController.java
├── dto/
│   ├── SavePrintInfoDTO.java
│   └── SavePrintInfoItemDTO.java
└── vo/
    ├── PrintInfoOptionsVO.java
    ├── PrintInfoProductVO.java
    ├── PrintInfoSpecVO.java
    ├── ColorGroupVO.java
    ├── DictOptionVO.java
    └── DesignProductVO.java            # 查询列表响应
```

---

## 四、测试覆盖

### basic 模块测试

**ProductServiceImplTest**：
- 创建产品成功
- 产品名重复抛 `PRODUCT_EXISTS`
- 删除有规格的产品抛 `PRODUCT_HAS_SPECS`
- 按大类查询返回正确产品（含规格）

**ProductSpecServiceImplTest**：
- 创建规格成功（自动关联注册证号）
- 同产品下规格名重复抛 `PRODUCT_SPEC_EXISTS`
- 删除被引用的规格抛 `PRODUCT_SPEC_IN_USE`

### design 模块测试

**DesignPrintInfoServiceImplTest**：
- 获取选项数据返回正确产品树、材质默认值、颜色分组
- 获取选项数据：status=0 的规格不出现在 specs 列表中
- 查询打印信息列表成功（返回已保存记录，按 sort_order 排序）
- 保存打印信息成功（整包替换：验证旧记录被删除、新记录被插入）
- 保存时 specId 不属于传入 productId 抛异常
- 保存空列表时旧记录被清空
- packageFileId 不属于该 package 抛异常
- 权限校验：非设计师操作抛 `DESIGN_OPERATOR_NOT_ALLOWED`（错误码 740）
- 删除单条成功

---

## 五、不变更范围

- `registration_cert` 全部代码（结构不变）
- `design_product` 表结构
- `DesignProductService` / `DesignProductServiceImpl`（保留简单 IService）
- `DesignFileServiceImpl` 及其测试（已完成，不受影响）
