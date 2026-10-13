# 打印信息（指令单录入）重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将打印信息数据模型从"以文件为锚点的单行扁平结构"重构为"以产品+规格为主体、文件一对多关联"的主从表结构，同步调整指令单/图纸生成逻辑，使录入体验与业务语义一致。

**Architecture:** 新增 `design_product_file` 关联表存储产品-文件的一对多关系；`design_package` 表承接包级字段（产品标识、包装数量、备注）；`design_product` 表移除文件字段和包级字段，新增 `is_urgent` 行级字段替换自由文本 `timeliness`；指令单/图纸生成时 JOIN 文件行展开，每个文件对应输出一行。

**Tech Stack:** Java 21, Spring Boot 3, MyBatis-Plus 3.5.8, Apache POI 5.2.5, JUnit 5 + Mockito

---

## 文件改动总览

| 文件 | 操作 | 说明 |
|---|---|---|
| `sql/ddl.sql` | 改 | design_package 加字段；design_product 字段重构；新增 design_product_file |
| `sql/init.sql` | 改 | 颜色字典（16.x）dict_value 补产品大类码 |
| `yigongbao-module-design/src/main/java/.../entity/DesignPackageEntity.java` | 改 | 新增 productMark、packQuantity、remark |
| `yigongbao-module-design/src/main/java/.../entity/DesignProductEntity.java` | 改 | 移除 packageFileId/packageFileName/productMark/packQuantity/timeliness；新增 isUrgent |
| `yigongbao-module-design/src/main/java/.../entity/DesignProductFileEntity.java` | 新建 | 产品-文件关联实体 |
| `yigongbao-module-design/src/main/java/.../mapper/DesignProductFileMapper.java` | 新建 | 基础 CRUD Mapper |
| `yigongbao-module-design/src/main/java/.../service/DesignProductFileService.java` | 新建 | Service 接口 |
| `yigongbao-module-design/src/main/java/.../service/impl/DesignProductFileServiceImpl.java` | 新建 | Service 实现 |
| `yigongbao-module-design/src/main/java/.../dto/SavePrintInfoDTO.java` | 改 | 新增 productMark（必填）、packQuantity、remark |
| `yigongbao-module-design/src/main/java/.../dto/SavePrintInfoItemDTO.java` | 改 | packageFileId→List<Long> packageFileIds；移除 packQuantity/timeliness/productMark；新增 isUrgent |
| `yigongbao-module-design/src/main/java/.../vo/DesignProductVO.java` | 改 | 新增 files 列表；移除单文件字段；新增 isUrgent；移除 timeliness/productMark/packQuantity |
| `yigongbao-module-design/src/main/java/.../vo/PrintInfoOptionsVO.java` | 改 | 新增 packQuantity、productMark、remark 包级回显字段 |
| `yigongbao-module-design/src/main/java/.../vo/ColorGroupVO.java` | 确认 | 已有 categoryCode 字段，无需改动 |
| `yigongbao-module-design/src/main/java/.../service/impl/DesignPrintInfoServiceImpl.java` | 改 | 全面重构：保存同步文件表；查询 JOIN 文件；getOptions 颜色分组逻辑确认 |
| `yigongbao-module-design/src/main/java/.../controller/DesignPrintInfoController.java` | 改 | getOptions 路径补 packageId |
| `yigongbao-module-design/src/main/java/.../service/impl/DesignDocServiceImpl.java` | 改 | 展开文件行；is_urgent 从产品行读取 |
| `yigongbao-module-design/src/main/java/.../helper/InstructionExcelBuilder.java` | 改 | BuildContext.isUrgent 移至行级；接收展开后行列表 |
| `yigongbao-module-design/src/main/java/.../helper/DrawingExcelBuilder.java` | 改 | 文件名从展开行读取，不再从产品行读 |
| `yigongbao-module-design/src/test/java/.../DesignPrintInfoServiceImplTest.java` | 改 | 更新所有测试用例适配新 DTO/逻辑 |
| `yigongbao-module-design/src/test/java/.../DesignDocServiceImplTest.java` | 改 | 更新指令单生成测试 |
| `yigongbao-module-design/src/test/java/.../DesignProductFileServiceImplTest.java` | 新建 | 新 Service 单元测试 |

---

## Task 1: DDL 和字典数据变更

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/init.sql`

- [ ] **Step 1: 修改 ddl.sql — design_package 新增三个字段**

在 `design_package` 表定义的 `upload_time` 字段之后，`create_time` 之前插入：

```sql
product_mark    VARCHAR(100)    DEFAULT NULL COMMENT '产品标识（必填，数据包级别）',
pack_quantity   INT             DEFAULT NULL COMMENT '包装数量（数据包级别统计值）',
remark          VARCHAR(500)    DEFAULT NULL COMMENT '备注',
```

- [ ] **Step 2: 修改 ddl.sql — design_product 字段重构**

将 `design_product` 表中以下字段：
- 移除：`pack_quantity`、`timeliness`、`product_mark`、`package_file_id`、`package_file_name`
- 新增（在 `quantity` 字段之后）：

```sql
is_urgent       TINYINT         DEFAULT 0   COMMENT '是否加急（0=普通，1=加急），默认从订单带出，可修改',
```

同时移除 `package_file_id` 上的索引 `idx_design_product_package_file_id`。

- [ ] **Step 3: 修改 ddl.sql — 新增 design_product_file 表**

在 `design_product` 表定义之后添加：

```sql
-- ----------------------------
-- 打印产品关联文件表
-- ----------------------------
CREATE TABLE IF NOT EXISTS design_product_file (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    design_product_id   BIGINT          NOT NULL COMMENT '关联 design_product.id',
    package_file_id     BIGINT          NOT NULL COMMENT '关联 design_package_file.id',
    package_file_name   VARCHAR(256)    DEFAULT NULL COMMENT '文件名（冗余）',
    sort_order          INT             DEFAULT 0 COMMENT '排序',

    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by           BIGINT          DEFAULT NULL,
    update_by           BIGINT          DEFAULT NULL,
    is_deleted          TINYINT         DEFAULT 0,

    PRIMARY KEY (id),
    KEY idx_dpf_design_product_id (design_product_id),
    KEY idx_dpf_package_file_id (package_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打印产品关联文件表';
```

- [ ] **Step 4: 修改 init.sql — 颜色字典补充 categoryCode 亲和值**

`getOptions` 的颜色分组逻辑依赖颜色二级字典（dict_code=16.x）的 `dict_value` 字段存储对应的产品大类码（dict_code=17.x）。

将 init.sql 中颜色字典的插入语句更新（找到 dict_code=16 的子条目）：

```sql
-- 颜色字典（dict_code=16），dict_value 存对应产品大类码供前端过滤
-- 原语句：(91, 90, '16.1', '白色', 'white', ...)
-- 改为：
(91, 90, '16.1', '白色', '17.1', 2, 1, 1),    -- 模型类（白色）
(92, 90, '16.2', '透明', '17.2', 2, 2, 1),    -- 导板类（透明）
(93, 90, '16.3', '肤色', '17.1', 2, 3, 1),    -- 模型类（肤色）
(94, 90, '16.4', '蓝色', null,   2, 4, 1),    -- 通用（不限分类）
```

> 注意：`dict_value` 原值（如 'white'）无其他地方引用，可直接替换为分类码。若将来需要保留原值，可在三级字典中另存。

- [ ] **Step 5: 提交**

```bash
cd D:/01_Project/02_Personal/医工宝
git add sql/ddl.sql sql/init.sql
git commit -m "feat(design): DDL重构——design_product文件字段拆出，新增design_product_file表，design_package补包级字段"
```

---

## Task 2: Entity 层改造

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/DesignPackageEntity.java`
- Modify: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/DesignProductEntity.java`
- Create: `yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/DesignProductFileEntity.java`

- [ ] **Step 1: 修改 DesignPackageEntity — 新增三个字段**

在 `uploadTime` 字段之后添加：

```java
/** 产品标识（必填，数据包级别） */
@TableField("product_mark")
private String productMark;

/** 包装数量（数据包级别统计值） */
@TableField("pack_quantity")
private Integer packQuantity;

/** 备注 */
@TableField("remark")
private String remark;
```

- [ ] **Step 2: 修改 DesignProductEntity — 字段重构**

移除以下字段（删除整个字段声明）：
- `packageFileId`
- `packageFileName`
- `productMark`
- `packQuantity`
- `timeliness`
- `remark`

新增以下字段（在 `quantity` 字段之后）：

```java
/** 是否加急（0=普通，1=加急），默认从订单带出，允许修改 */
@TableField("is_urgent")
private Integer isUrgent;
```

- [ ] **Step 3: 新建 DesignProductFileEntity**

```java
package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打印产品关联文件实体
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("design_product_file")
public class DesignProductFileEntity extends BaseEntity {

    /** 关联 design_product.id */
    private Long designProductId;

    /** 关联 design_package_file.id */
    private Long packageFileId;

    /** 文件名（冗余） */
    private String packageFileName;

    /** 排序 */
    private Integer sortOrder;
}
```

- [ ] **Step 4: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/entity/
git commit -m "feat(design): Entity层重构——DesignProductEntity字段拆分，新增DesignProductFileEntity"
```

---

## Task 3: Mapper 和 Service 层（DesignProductFile）

**Files:**
- Create: `.../mapper/DesignProductFileMapper.java`
- Create: `.../service/DesignProductFileService.java`
- Create: `.../service/impl/DesignProductFileServiceImpl.java`

- [ ] **Step 1: 新建 DesignProductFileMapper**

```java
package com.yigongbao.module.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印产品关联文件 Mapper
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Mapper
public interface DesignProductFileMapper extends BaseMapper<DesignProductFileEntity> {
}
```

- [ ] **Step 2: 新建 DesignProductFileService**

```java
package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignProductFileEntity;

import java.util.List;

/**
 * 打印产品关联文件 Service
 *
 * @author hanjor
 * @date 2026-04-17
 */
public interface DesignProductFileService extends IService<DesignProductFileEntity> {

    /**
     * 查询指定产品行的所有关联文件（按 sort_order 升序）
     *
     * @param designProductId design_product.id
     * @return 文件列表
     */
    List<DesignProductFileEntity> listByProductId(Long designProductId);

    /**
     * 批量查询多个产品行的关联文件
     *
     * @param designProductIds 产品行 ID 列表
     * @return 文件列表
     */
    List<DesignProductFileEntity> listByProductIds(List<Long> designProductIds);

    /**
     * 删除指定产品行的所有关联文件（逻辑删除）
     *
     * @param designProductId design_product.id
     */
    void removeByProductId(Long designProductId);

    /**
     * 批量删除多个产品行的关联文件（逻辑删除）
     *
     * @param designProductIds 产品行 ID 列表
     */
    void removeByProductIds(List<Long> designProductIds);
}
```

- [ ] **Step 3: 新建 DesignProductFileServiceImpl**

```java
package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import com.yigongbao.module.design.service.DesignProductFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 打印产品关联文件 Service 实现类
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Slf4j
@Service
public class DesignProductFileServiceImpl
        extends ServiceImpl<DesignProductFileMapper, DesignProductFileEntity>
        implements DesignProductFileService {

    @Override
    public List<DesignProductFileEntity> listByProductId(Long designProductId) {
        return list(new LambdaQueryWrapper<DesignProductFileEntity>()
                .eq(DesignProductFileEntity::getDesignProductId, designProductId)
                .orderByAsc(DesignProductFileEntity::getSortOrder));
    }

    @Override
    public List<DesignProductFileEntity> listByProductIds(List<Long> designProductIds) {
        if (designProductIds == null || designProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DesignProductFileEntity>()
                .in(DesignProductFileEntity::getDesignProductId, designProductIds)
                .orderByAsc(DesignProductFileEntity::getDesignProductId)
                .orderByAsc(DesignProductFileEntity::getSortOrder));
    }

    @Override
    public void removeByProductId(Long designProductId) {
        remove(new LambdaQueryWrapper<DesignProductFileEntity>()
                .eq(DesignProductFileEntity::getDesignProductId, designProductId));
    }

    @Override
    public void removeByProductIds(List<Long> designProductIds) {
        if (designProductIds == null || designProductIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<DesignProductFileEntity>()
                .in(DesignProductFileEntity::getDesignProductId, designProductIds));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/mapper/DesignProductFileMapper.java \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignProductFileService.java \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignProductFileServiceImpl.java
git commit -m "feat(design): 新增DesignProductFileMapper/Service/ServiceImpl"
```

---

## Task 4: DesignProductFileService 单元测试

**Files:**
- Create: `.../test/java/.../service/impl/DesignProductFileServiceImplTest.java`

- [ ] **Step 1: 新建测试文件**

```java
package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DesignProductFileServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignProductFileService 单元测试")
class DesignProductFileServiceImplTest {

    @Mock
    private DesignProductFileMapper productFileMapper;

    @InjectMocks
    private DesignProductFileServiceImpl productFileService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productFileService, productFileMapper);
    }

    @Test
    @DisplayName("listByProductId: 空结果时返回空列表")
    void listByProductId_noData_returnsEmpty() {
        when(productFileMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        List<DesignProductFileEntity> result = productFileService.listByProductId(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("listByProductId: 返回对应记录")
    void listByProductId_withData_returnsFiles() {
        DesignProductFileEntity file = new DesignProductFileEntity();
        file.setDesignProductId(1L);
        file.setPackageFileId(100L);
        file.setPackageFileName("左髋骨.stl");
        when(productFileMapper.selectList(any(Wrapper.class))).thenReturn(List.of(file));

        List<DesignProductFileEntity> result = productFileService.listByProductId(1L);
        assertEquals(1, result.size());
        assertEquals("左髋骨.stl", result.get(0).getPackageFileName());
    }

    @Test
    @DisplayName("listByProductIds: 空入参直接返回空列表，不查数据库")
    void listByProductIds_emptyInput_returnsEmpty() {
        List<DesignProductFileEntity> result = productFileService.listByProductIds(List.of());
        assertTrue(result.isEmpty());
        verify(productFileMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("removeByProductIds: 空入参直接跳过，不调用数据库")
    void removeByProductIds_emptyInput_skipsDb() {
        productFileService.removeByProductIds(List.of());
        verify(productFileMapper, never()).delete(any());
    }

    @Test
    @DisplayName("removeByProductId: 正常调用 remove")
    void removeByProductId_callsRemove() {
        when(productFileMapper.delete(any(Wrapper.class))).thenReturn(1);
        productFileService.removeByProductId(1L);
        verify(productFileMapper, times(1)).delete(any(Wrapper.class));
    }
}
```

- [ ] **Step 2: 运行测试，确认通过**

```bash
cd D:/01_Project/02_Personal/医工宝/yigongbao-parent
mvn test -pl yigongbao-module-design -Dtest=DesignProductFileServiceImplTest
```

预期：所有用例 PASS。

- [ ] **Step 3: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignProductFileServiceImplTest.java
git commit -m "test(design): 新增DesignProductFileServiceImplTest"
```

---

## Task 5: DTO / VO 层重构

**Files:**
- Modify: `.../dto/SavePrintInfoDTO.java`
- Modify: `.../dto/SavePrintInfoItemDTO.java`
- Modify: `.../vo/DesignProductVO.java`
- Modify: `.../vo/PrintInfoOptionsVO.java`

- [ ] **Step 1: 重构 SavePrintInfoDTO — 新增包级字段**

在 `items` 字段之前添加：

```java
/** 产品标识（数据包级别，必填） */
@NotBlank(message = "产品标识不能为空")
private String productMark;

/** 包装数量（数据包级别统计值，选填） */
private Integer packQuantity;

/** 备注（选填） */
private String remark;
```

同时补充 import `jakarta.validation.constraints.NotBlank`。

- [ ] **Step 2: 重构 SavePrintInfoItemDTO**

移除以下字段：
- `packageFileId`（单个）
- `packageFileName`
- `packQuantity`
- `timeliness`
- `productMark`

新增 / 修改：

```java
/** 关联文件ID列表（至少1个） */
@NotEmpty(message = "至少关联一个数据文件")
private List<Long> packageFileIds;

/** 是否加急（0=普通，1=加急） */
private Integer isUrgent;
```

同时补充 import `jakarta.validation.constraints.NotEmpty` 和 `java.util.List`。

- [ ] **Step 3: 重构 DesignProductVO**

移除以下字段：`packageFileId`、`packageFileName`、`packQuantity`、`timeliness`、`productMark`

新增：

```java
/** 是否加急（0=普通，1=加急） */
private Integer isUrgent;

/** 关联文件列表 */
private List<ProductFileVO> files;

/**
 * 产品关联文件 VO
 */
@Data
public static class ProductFileVO {
    /** design_product_file.id */
    private Long id;
    /** design_package_file.id */
    private Long packageFileId;
    /** 文件名 */
    private String packageFileName;
}
```

补充 import `java.util.List` 和 `lombok.Data`。

- [ ] **Step 4: 修改 PrintInfoOptionsVO — 新增包级回显字段**

新增以下字段（用于编辑页初始化时回显已保存的包级数据）：

```java
/** 产品标识（已保存值，供编辑页回显） */
private String productMark;

/** 包装数量（已保存值，供编辑页回显） */
private Integer packQuantity;

/** 备注（已保存值，供编辑页回显） */
private String remark;
```

> 这三个字段在 `getOptions` 中从 `design_package` 查询后回填。

- [ ] **Step 5: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/dto/ \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignProductVO.java \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/PrintInfoOptionsVO.java
git commit -m "feat(design): DTO/VO重构——SavePrintInfoDTO包级字段，文件改多选，DesignProductVO新增files"
```

---

## Task 6: DesignPrintInfoServiceImpl 全面重构

**Files:**
- Modify: `.../service/impl/DesignPrintInfoServiceImpl.java`

关键变更点：
1. 注入 `DesignProductFileService`
2. `getOptions`：从 `design_package` 读取包级字段回填 VO；颜色分组已有 `dictValue→categoryCode` 映射逻辑，确认正确
3. `listPrintInfo`：查出产品行后，批量查关联文件，组装 `DesignProductVO.files`
4. `savePrintInfo`：校验 `productMark` 非空；文件校验改为多文件 ID；保存产品行后同步插入文件关联行；更新 `design_package` 的包级字段
5. `deletePrintInfo`：删产品行前先删其关联文件行

- [ ] **Step 1: 修改 getOptions**

在方法开头查出 `design_package` 的包级字段并填入 VO（`packageId` 通过 `orderId` 查第一个包，或由调用方传入）：

> 注意：`getOptions` 当前入参只有 `orderId`，包级字段需要 `packageId` 才能定位。需将接口签名改为 `getOptions(Long orderId, Long packageId)`，Controller 同步修改。

修改接口：
```java
// DesignPrintInfoService.java
PrintInfoOptionsVO getOptions(Long orderId, Long packageId);
```

修改实现（在返回前查 package 并回填）：
```java
DesignPackageEntity pkg = packageService.getById(packageId);
if (pkg != null) {
    vo.setProductMark(pkg.getProductMark());
    vo.setPackQuantity(pkg.getPackQuantity());
    vo.setRemark(pkg.getRemark());
}
```

- [ ] **Step 2: 修改 listPrintInfo — 附加关联文件**

```java
@Override
public List<DesignProductVO> listPrintInfo(Long orderId, Long packageId) {
    log.info("查询打印信息列表，orderId={}, packageId={}", orderId, packageId);
    validatePackage(orderId, packageId);
    // 查产品行
    List<DesignProductEntity> entities = designProductService.list(
            new LambdaQueryWrapper<DesignProductEntity>()
                    .eq(DesignProductEntity::getPackageId, packageId)
                    .orderByAsc(DesignProductEntity::getSortOrder));
    if (entities.isEmpty()) return Collections.emptyList();
    // 批量查关联文件
    List<Long> productIds = entities.stream().map(DesignProductEntity::getId).toList();
    List<DesignProductFileEntity> allFiles = productFileService.listByProductIds(productIds);
    Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
            .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));
    // 组装 VO
    return entities.stream().map(e -> {
        DesignProductVO vo = toVO(e);
        List<DesignProductFileEntity> files = fileMap.getOrDefault(e.getId(), List.of());
        vo.setFiles(files.stream().map(f -> {
            DesignProductVO.ProductFileVO fvo = new DesignProductVO.ProductFileVO();
            fvo.setId(f.getId());
            fvo.setPackageFileId(f.getPackageFileId());
            fvo.setPackageFileName(f.getPackageFileName());
            return fvo;
        }).toList());
        return vo;
    }).toList();
}
```

- [ ] **Step 3: 修改 savePrintInfo — 包级字段 + 多文件关联**

核心逻辑流程：
1. 权限校验（不变）
2. 校验 `dto.productMark` 非空（新增）
3. 收集所有 `packageFileIds` 的并集，校验均属于该 `packageId`（原单文件校验→改为多文件批量校验）
4. 校验产品/规格（不变）
5. 删除旧产品行的关联文件：先查旧产品行 ID → `productFileService.removeByProductIds(oldIds)`
6. 删除旧产品行：`designProductService.remove(...)`（不变）
7. 批量插入新产品行
8. 批量插入关联文件行（遍历每个产品行及其 `packageFileIds`）
9. 更新 `design_package` 包级字段（productMark、packQuantity、remark）

关键新增代码段：

```java
// 步骤5：删旧文件关联（在 remove 产品行之前）
List<Long> oldProductIds = designProductService.list(
        new LambdaQueryWrapper<DesignProductEntity>()
                .eq(DesignProductEntity::getPackageId, packageId)
                .select(DesignProductEntity::getId))
        .stream().map(DesignProductEntity::getId).toList();
if (!oldProductIds.isEmpty()) {
    productFileService.removeByProductIds(oldProductIds);
}

// 步骤8：插入文件关联行
List<DesignProductFileEntity> fileEntities = new ArrayList<>();
for (int i = 0; i < savedEntities.size(); i++) {
    DesignProductEntity saved = savedEntities.get(i);
    List<Long> fileIds = dto.getItems().get(i).getPackageFileIds();
    for (int j = 0; j < fileIds.size(); j++) {
        Long fileId = fileIds.get(j);
        // 从 packageFileService 查出文件名
        DesignPackageFileEntity pf = packageFileService.getById(fileId);
        DesignProductFileEntity dpf = new DesignProductFileEntity();
        dpf.setDesignProductId(saved.getId());
        dpf.setPackageFileId(fileId);
        dpf.setPackageFileName(pf != null ? pf.getFileName() : null);
        dpf.setSortOrder(j);
        fileEntities.add(dpf);
    }
}
productFileService.saveBatch(fileEntities);

// 步骤9：更新 design_package 包级字段
DesignPackageEntity pkgUpdate = new DesignPackageEntity();
pkgUpdate.setId(packageId);
pkgUpdate.setProductMark(dto.getProductMark());
pkgUpdate.setPackQuantity(dto.getPackQuantity());
pkgUpdate.setRemark(dto.getRemark());
packageService.updateById(pkgUpdate);
```

- [ ] **Step 4: 修改 deletePrintInfo — 先删文件关联**

在 `designProductService.removeById(printInfoId)` 之前添加：
```java
productFileService.removeByProductId(printInfoId);
```

- [ ] **Step 5: 修改 Controller — getOptions 路径补 packageId**

```java
@GetMapping("/{orderId}/package/{packageId}/print-info/options")
public Result<PrintInfoOptionsVO> getOptions(@PathVariable Long orderId,
                                              @PathVariable Long packageId) {
    return Result.success(printInfoService.getOptions(orderId, packageId));
}
```

> 原路径 `/{orderId}/print-info/options` 改为 `/{orderId}/package/{packageId}/print-info/options`。

- [ ] **Step 6: 编译确认无报错**

```bash
cd D:/01_Project/02_Personal/医工宝/yigongbao-parent
mvn compile -pl yigongbao-module-design
```

预期：BUILD SUCCESS，无编译错误。

- [ ] **Step 7: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignPrintInfoServiceImpl.java \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignPrintInfoService.java \
        yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignPrintInfoController.java
git commit -m "feat(design): DesignPrintInfoServiceImpl重构——包级字段、多文件关联、getOptions带packageId"
```

---

## Task 7: 指令单/图纸生成逻辑重构

**Files:**
- Modify: `.../service/impl/DesignDocServiceImpl.java`
- Modify: `.../helper/InstructionExcelBuilder.java`
- Modify: `.../helper/DrawingExcelBuilder.java`

### 7.1 DesignDocServiceImpl

- [ ] **Step 1: 注入 DesignProductFileService**

在 `DesignDocServiceImpl` 的依赖注入中新增：
```java
private final DesignProductFileService productFileService;
```

- [ ] **Step 2: 重构 generateInstruction — 展开文件行**

新增内部方法（替换原直接传 products 给 Builder 的方式）：

```java
/**
 * 将产品行 + 文件行展开为指令单行列表（一个文件=一行）
 */
private List<InstructionExcelBuilder.ProductRow> expandProductRows(
        List<DesignProductEntity> products) {
    if (products.isEmpty()) return List.of();
    List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
    List<DesignProductFileEntity> allFiles = productFileService.listByProductIds(productIds);
    Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
            .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));

    List<InstructionExcelBuilder.ProductRow> rows = new ArrayList<>();
    for (DesignProductEntity p : products) {
        List<DesignProductFileEntity> files = fileMap.getOrDefault(p.getId(), List.of());
        for (DesignProductFileEntity f : files) {
            InstructionExcelBuilder.ProductRow row = new InstructionExcelBuilder.ProductRow();
            row.setCertNo(p.getCertNo());
            row.setProductName(p.getProductName());
            row.setPackageFileName(f.getPackageFileName());
            row.setSpecName(p.getSpecName());
            row.setMaterialName(p.getMaterialName());
            row.setQuantity(p.getQuantity());
            row.setIsUrgent(p.getIsUrgent()); // 行级 is_urgent
            row.setColorName(p.getColorName());
            rows.add(row);
        }
    }
    return rows;
}
```

在 `generateInstruction` 中，将原来的 `products` 替换为展开后的 `rows`：
```java
// 旧：InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, products, version, now);
// 新：
List<InstructionExcelBuilder.ProductRow> rows = expandProductRows(products);
InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, rows, version, now);
```

同理，`buildInstructionContext` 签名改为接收 `List<ProductRow>`。

- [ ] **Step 3: 从 design_package 读取包级字段**

在 `generateInstruction` 和 `generateDrawing` 中，`productMark`、`packQuantity`、`remark` 改从 `pkg`（`DesignPackageEntity`）读取：

```java
ctx.setProductMark(pkg.getProductMark());
// packQuantity 用于指令单底部包装数量行
ctx.setPackQuantity(pkg.getPackQuantity());
ctx.setRemark(pkg.getRemark());
```

同时移除从 `DesignProductEntity` 读取这些字段的逻辑。

### 7.2 InstructionExcelBuilder 重构

- [ ] **Step 4: 新建 ProductRow 内部类替换直接使用 DesignProductEntity**

```java
@Data
public static class ProductRow {
    private String certNo;
    private String productName;
    private String packageFileName;  // 已去后缀
    private String specName;
    private String materialName;
    private Integer quantity;
    private Integer isUrgent;       // 0=普通，1=加急（行级）
    private String colorName;
}
```

- [ ] **Step 5: 修改 BuildContext**

移除 `List<DesignProductEntity> products`，改为：
```java
private List<ProductRow> rows;       // 展开后的产品×文件行
private String productMark;          // 产品标识（从 design_package 读）
private Integer packQuantity;        // 包装数量（从 design_package 读）
private String remark;               // 备注
```

移除 `isUrgent` 和 `getIsUrgentValue()`（已移至行级）。

- [ ] **Step 6: 修改 build() — 遍历 rows 而非 products**

将步骤 5（遍历产品数据）中的 `ctx.getProducts()` 改为 `ctx.getRows()`，`DesignProductEntity p` 改为 `ProductRow p`，字段访问对应调整。

加急加粗逻辑改为：
```java
if (StatusConstants.YES == (row.getIsUrgent() != null ? row.getIsUrgent() : StatusConstants.NO)) {
    setBold(wb, sheet, rowIdx, 7);
}
```

底部区域的 `productMark` 从 `ctx.getProductMark()` 读取（原从 products stream 拼接改为直接读）；`packQuantity` 从 `ctx.getPackQuantity()` 读取。

### 7.3 DrawingExcelBuilder 重构

- [ ] **Step 7: 修改 BuildContext — products 改为 rows**

移除 `List<DesignProductEntity> products`，改为：
```java
@Data
public static class ProductRow {
    private String packageFileName;  // 文件名（已去后缀）
    private String productName;
}

private List<ProductRow> rows;
```

- [ ] **Step 8: 修改 build() — 填槽逻辑使用 rows**

```java
if (productIdx < to) {
    ProductRow row = rows.get(productIdx);
    setCell(sheet, coord[0], coord[1], stripExtension(row.getPackageFileName()));
    setCell(sheet, coord[2], coord[3], strOrEmpty(row.getProductName()));
}
```

- [ ] **Step 9: DesignDocServiceImpl — 新增 expandDrawingRows 方法并接入 generateDrawing**

```java
private List<DrawingExcelBuilder.ProductRow> expandDrawingRows(
        List<DesignProductEntity> products) {
    if (products.isEmpty()) return List.of();
    List<Long> productIds = products.stream().map(DesignProductEntity::getId).toList();
    List<DesignProductFileEntity> allFiles = productFileService.listByProductIds(productIds);
    Map<Long, List<DesignProductFileEntity>> fileMap = allFiles.stream()
            .collect(Collectors.groupingBy(DesignProductFileEntity::getDesignProductId));

    List<DrawingExcelBuilder.ProductRow> rows = new ArrayList<>();
    for (DesignProductEntity p : products) {
        List<DesignProductFileEntity> files = fileMap.getOrDefault(p.getId(), List.of());
        for (DesignProductFileEntity f : files) {
            DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
            row.setPackageFileName(f.getPackageFileName());
            row.setProductName(p.getProductName());
            rows.add(row);
        }
    }
    return rows;
}
```

在 `generateDrawing` 中，将原来的 `products` 替换为展开后的 `rows`：
```java
// 旧：DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, products, now);
// 新：
List<DrawingExcelBuilder.ProductRow> drawingRows = expandDrawingRows(products);
DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, drawingRows, now);
```

同理，`buildDrawingContext` 签名改为接收 `List<DrawingExcelBuilder.ProductRow>`。

- [ ] **Step 10: 编译确认**

```bash
mvn compile -pl yigongbao-module-design
```

预期：BUILD SUCCESS。

- [ ] **Step 11: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/main/java/com/yigongbao/module/design/
git commit -m "feat(design): 指令单/图纸生成重构——展开产品×文件行，行级isUrgent，包级字段从design_package读取"
```

---

## Task 8: 更新单元测试

**Files:**
- Modify: `.../test/java/.../service/impl/DesignPrintInfoServiceImplTest.java`
- Modify: `.../test/java/.../service/impl/DesignDocServiceImplTest.java`

### 8.1 DesignPrintInfoServiceImplTest

- [ ] **Step 1: 新增 Mock — DesignProductFileService**

```java
@Mock private DesignProductFileService productFileService;
```

- [ ] **Step 2: 修改 buildSavePrintInfoDTO 辅助方法**

```java
private SavePrintInfoDTO buildSavePrintInfoDTO() {
    SavePrintInfoItemDTO item = new SavePrintInfoItemDTO();
    item.setPackageFileIds(List.of(FILE_ID));   // 改为多文件ID列表
    item.setProductId(PRODUCT_ID);
    item.setSpecId(SPEC_ID);
    item.setQuantity(1);
    item.setIsUrgent(0);
    item.setSortOrder(1);

    SavePrintInfoDTO dto = new SavePrintInfoDTO();
    dto.setProductMark("LGC");    // 新增必填字段
    dto.setItems(List.of(item));
    return dto;
}
```

- [ ] **Step 3: 更新 savePrintInfo 成功测试**

新增 Mock 依赖：
```java
when(productFileService.listByProductIds(any())).thenReturn(List.of());
when(productFileService.removeByProductIds(any())).thenCallRealMethod(); // or doNothing
when(productFileService.saveBatch(any())).thenReturn(true);
```

更新文件校验 Mock（packageFileService.count 改为批量校验逻辑，视具体实现调整）。

- [ ] **Step 4: 新增 productMark 必填校验测试**

```java
@Test
@DisplayName("savePrintInfo: productMark 为空时抛出异常")
void savePrintInfo_emptyProductMark_shouldThrow() {
    try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
        stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
        when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
        when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

        SavePrintInfoDTO dto = buildSavePrintInfoDTO();
        dto.setProductMark(null);  // 清空必填字段

        assertThrows(BusinessException.class,
                () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
    }
}
```

- [ ] **Step 5: 更新 listPrintInfo 测试 — 附加文件 Mock**

```java
@Test
@DisplayName("查询打印信息列表成功，附带关联文件")
void listPrintInfo_shouldReturnWithFiles() {
    when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

    DesignProductEntity entity = new DesignProductEntity();
    entity.setId(1L);
    entity.setPackageId(PACKAGE_ID);
    when(designProductService.list(any(Wrapper.class))).thenReturn(List.of(entity));

    DesignProductFileEntity fileEntity = new DesignProductFileEntity();
    fileEntity.setDesignProductId(1L);
    fileEntity.setPackageFileId(FILE_ID);
    fileEntity.setPackageFileName("左髋骨.stl");
    when(productFileService.listByProductIds(List.of(1L))).thenReturn(List.of(fileEntity));

    List<DesignProductVO> result = printInfoService.listPrintInfo(ORDER_ID, PACKAGE_ID);

    assertEquals(1, result.size());
    assertEquals(1, result.get(0).getFiles().size());
    assertEquals("左髋骨.stl", result.get(0).getFiles().get(0).getPackageFileName());
}
```

### 8.2 DesignDocServiceImplTest

- [ ] **Step 6: 更新测试 — 注入 productFileService Mock 并 stub**

在测试中新增 `@Mock private DesignProductFileService productFileService`，并在指令单/图纸生成测试中 stub：
```java
when(productFileService.listByProductIds(any())).thenReturn(List.of(/* 测试文件实体 */));
```

- [ ] **Step 7: 运行全部设计模块测试**

```bash
mvn test -pl yigongbao-module-design
```

预期：全部 PASS，无编译错误。

- [ ] **Step 8: 提交**

```bash
git add yigongbao-parent/yigongbao-module-design/src/test/
git commit -m "test(design): 更新DesignPrintInfoServiceImplTest和DesignDocServiceImplTest适配重构"
```

---

## Task 9: H2 测试 Schema 补充

**Files:**
- Create/Modify: `yigongbao-parent/yigongbao-module-design/src/test/resources/schema.sql`

目前 test/resources 中只有模板 xlsx 文件，没有 schema.sql。若有 Controller 集成测试需要 H2，需补充 design 模块相关表定义。

- [ ] **Step 1: 检查是否存在 schema.sql**

```bash
find D:/01_Project/02_Personal/医工宝/yigongbao-parent -name "schema.sql"
```

- [ ] **Step 2: 若无 schema.sql，检查是否有集成测试**

若当前 design 模块只有 Mockito 单元测试（无 @SpringBootTest），则 H2 schema 暂不需要，跳过本任务。

若需要补充，在 `src/test/resources/schema.sql` 中添加以下三张表的 H2 兼容版本（去掉函数索引，去掉 `ON UPDATE CURRENT_TIMESTAMP`）：

```sql
CREATE TABLE IF NOT EXISTS design_package (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_code VARCHAR(50) NOT NULL,
    package_code VARCHAR(50) NOT NULL,
    package_seq INT NOT NULL,
    file_id VARCHAR(32) NOT NULL,
    file_name VARCHAR(256),
    file_url VARCHAR(512),
    file_size BIGINT,
    file_count INT DEFAULT 0,
    upload_time DATETIME,
    product_mark VARCHAR(100),
    pack_quantity INT,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS design_product (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128),
    spec_id BIGINT NOT NULL,
    spec_name VARCHAR(128),
    cert_no VARCHAR(64),
    material_id VARCHAR(20),
    material_name VARCHAR(64),
    color_id VARCHAR(20),
    color_name VARCHAR(64),
    quantity INT NOT NULL DEFAULT 1,
    is_urgent TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS design_product_file (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    design_product_id BIGINT NOT NULL,
    package_file_id BIGINT NOT NULL,
    package_file_name VARCHAR(256),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    create_by BIGINT,
    update_by BIGINT,
    is_deleted TINYINT DEFAULT 0
);
```

- [ ] **Step 3: 提交（若有改动）**

```bash
git add yigongbao-parent/yigongbao-module-design/src/test/resources/schema.sql
git commit -m "test(design): 补充H2 schema——design_package/product/product_file"
```

---

## Task 10: 最终集成验证和文档同步

- [ ] **Step 1: 全模块编译+测试**

```bash
cd D:/01_Project/02_Personal/医工宝/yigongbao-parent
mvn clean test -pl yigongbao-module-design
```

预期：BUILD SUCCESS，所有测试 PASS。

- [ ] **Step 2: 全量构建（含依赖模块）**

```bash
mvn clean package -DskipTests
```

预期：BUILD SUCCESS。

- [ ] **Step 3: 确认接口文档与实现一致**

检查 `.docs/接口文档/` 中 print-info 相关接口文档，确认以下变更已体现：
- `getOptions` 路径变更（加 `packageId`）
- `savePrintInfo` 请求体新增包级字段
- `SavePrintInfoItemDTO` 中 `packageFileId` 改为 `packageFileIds`
- `DesignProductVO` 新增 `files` 列表

若接口文档不存在则跳过，仅确认 Swagger UI 注解与代码一致。

- [ ] **Step 4: 确认功能设计文档与实现一致**

检查 `.docs/功能设计/v1/打印信息填写方案.md`，确认文档中所有数据模型、字段名、逻辑描述与代码实现一致。

- [ ] **Step 5: 最终提交**

```bash
git add .
git commit -m "docs(design): 同步接口文档和功能设计文档——打印信息重构完成"
```

---

## 注意事项

1. **向前兼容**：`getOptions` 接口路径变更（加 packageId），前端需同步更新调用路径
2. **旧数据迁移**：若有已存在的 `design_product` 数据（含旧版文件字段），需在 DDL 脚本中评估是否需要迁移脚本（新字段 is_urgent 默认值为 0，无需迁移；旧的 packageFileId 数据若需保留，可在 Task 1 后单独写迁移 SQL）
3. **颜色字典 dict_value 变更**：`init.sql` 中颜色字典的 `dict_value` 原值（如 'white'）改为产品大类码（如 '17.1'），如有其他代码依赖原 dict_value 值需一并排查
