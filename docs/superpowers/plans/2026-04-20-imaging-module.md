# yigongbao-module-imaging 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建通用影像提供模块 `yigongbao-module-imaging`，提供4个只读接口供前端阅览调用，并将 `part_colors` 表迁移至主库并新增 `opacity` 字段。

**Architecture:** 独立 Maven 子模块，依赖 order 和 design 模块的 Mapper/Entity，新建 `PartColorEntity` + `PartColorMapper` 管理部位颜色透明度配置。唯一的 `ImagingServiceImpl` 通过批量查询避免 N+1，在内存中按文件名（去扩展名）精确匹配颜色透明度后组装 VO。

**Tech Stack:** Java 21, Spring Boot 3.2.5, MyBatis Plus 3.5.8, SaToken 1.37.0, Hutool 5.8.26, JUnit 5 + Mockito

---

## 文件清单

### 新建文件

| 文件 | 职责 |
|------|------|
| `yigongbao-module-imaging/pom.xml` | 模块 Maven 配置 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/entity/PartColorEntity.java` | part_colors 表映射 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/mapper/PartColorMapper.java` | 部位颜色查询 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo/DcmPackageVO.java` | DCM影像包返回值 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo/PackageModelFileVO.java` | 数据包内模型文件返回值 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo/PackageModelGroupVO.java` | 订单维度分组返回值 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo/ModelVO.java` | 可视化模型返回值 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/service/ImagingService.java` | 服务接口 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/service/impl/ImagingServiceImpl.java` | 服务实现 |
| `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/controller/ImagingController.java` | 4个查询接口 |
| `yigongbao-module-imaging/src/test/java/com/yigongbao/module/imaging/service/impl/ImagingServiceImplTest.java` | 单元测试 |

### 修改文件

| 文件 | 变更内容 |
|------|---------|
| `yigongbao-parent/pom.xml` | modules 列表新增 `yigongbao-module-imaging` |
| `yigongbao-boot/pom.xml` | 新增对 imaging 模块的依赖 |
| `sql/ddl.sql` | 新增 `part_colors` 建表 DDL（含 opacity 字段） |
| `sql/init.sql` | 新增 part_colors 全量初始数据（从 sql/part_colors.sql 迁移） |

---

## Task 1：数据库变更——part_colors 迁移到主库

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `sql/init.sql`

- [ ] **Step 1：在 ddl.sql 末尾追加建表语句**

打开 `sql/ddl.sql`，在文件末尾追加：

```sql
-- 部位颜色透明度配置表（从 image-3d-ai 迁移，新增 opacity 字段）
CREATE TABLE part_colors
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    part_detail VARCHAR(255) NULL COMMENT '部位名称（与模型文件名精确匹配，去扩展名）',
    color_code  VARCHAR(255) NULL COMMENT '颜色RGB值（如：170,255,0）',
    opacity     DECIMAL(3, 2) DEFAULT 1.00 COMMENT '透明度（0.00~1.00，1=不透明）'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '部位颜色透明度配置表';

CREATE INDEX idx_part_colors_part_detail ON part_colors (part_detail);
```

- [ ] **Step 2：将 sql/part_colors.sql 的数据迁移到 init.sql**

打开 `sql/part_colors.sql`，将所有 INSERT 语句复制到 `sql/init.sql` 末尾，但需将数据库前缀 `` `image-3d-ai`. `` 去掉，改为不带库名前缀，并在每条 INSERT 中追加 `opacity` 列（使用默认值 `1.00`）。

示例（调整格式）：
```sql
-- part_colors 初始数据
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (1, '右肺上叶', '170,255,0', 1.00);
INSERT INTO part_colors (id, part_detail, color_code, opacity) VALUES (2, '右肺上叶尖段', '255,170,255', 1.00);
-- ... 全部575条数据，opacity 统一填 1.00
```

- [ ] **Step 3：提交**

```bash
git add sql/ddl.sql sql/init.sql
git commit -m "feat(imaging): 迁移 part_colors 表到主库，新增 opacity 字段"
```

---

## Task 2：Maven 模块骨架

**Files:**
- Create: `yigongbao-module-imaging/pom.xml`
- Modify: `yigongbao-parent/pom.xml`
- Modify: `yigongbao-boot/pom.xml`

- [ ] **Step 1：创建模块目录和 pom.xml**

创建目录 `yigongbao-parent/yigongbao-module-imaging/`，新建 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.yigongbao</groupId>
        <artifactId>yigongbao-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>yigongbao-module-imaging</artifactId>
    <packaging>jar</packaging>
    <description>影像阅览通用模块——提供DCM影像、3D模型文件查询接口</description>

    <dependencies>
        <!-- 订单模块：查询 order_file -->
        <dependency>
            <groupId>com.yigongbao</groupId>
            <artifactId>yigongbao-module-order</artifactId>
            <version>${project.version}</version>
        </dependency>
        <!-- 设计模块：查询 design_package_file、design_model -->
        <dependency>
            <groupId>com.yigongbao</groupId>
            <artifactId>yigongbao-module-design</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：在父 pom.xml 的 modules 列表中新增 imaging 模块**

打开 `yigongbao-parent/pom.xml`，找到 `<modules>` 标签，在 `yigongbao-module-design` 之后新增：

```xml
<module>yigongbao-module-imaging</module>
```

- [ ] **Step 3：在 yigongbao-boot/pom.xml 中新增依赖**

打开 `yigongbao-boot/pom.xml`，在现有模块依赖末尾新增：

```xml
<dependency>
    <groupId>com.yigongbao</groupId>
    <artifactId>yigongbao-module-imaging</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 4：创建 Java 源码目录结构**

```bash
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/controller
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/service/impl
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/mapper
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/entity
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo
mkdir -p yigongbao-parent/yigongbao-module-imaging/src/test/java/com/yigongbao/module/imaging/service/impl
```

- [ ] **Step 5：验证编译通过**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests -pl yigongbao-module-imaging -am
```

预期：BUILD SUCCESS

- [ ] **Step 6：提交**

```bash
git add yigongbao-parent/yigongbao-module-imaging/ yigongbao-parent/pom.xml yigongbao-boot/pom.xml
git commit -m "feat(imaging): 新建 yigongbao-module-imaging 模块骨架"
```

---

## Task 3：PartColorEntity 和 PartColorMapper

**Files:**
- Create: `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/entity/PartColorEntity.java`
- Create: `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/mapper/PartColorMapper.java`

- [ ] **Step 1：创建 PartColorEntity**

```java
package com.yigongbao.module.imaging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部位颜色透明度配置表
 * 文件名去扩展名后与 partDetail 精确匹配，获取颜色和透明度
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@TableName("part_colors")
public class PartColorEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 部位名称（与模型文件名精确匹配，去扩展名）
     */
    private String partDetail;

    /**
     * 颜色RGB值（如：170,255,0）
     */
    private String colorCode;

    /**
     * 透明度（0.00~1.00，1=不透明）
     */
    private BigDecimal opacity;
}
```

**注意**：`part_colors` 表不含 `is_deleted`、`create_time` 等审计字段，因此直接实现 `Serializable`，不继承 `BaseEntity`。

- [ ] **Step 2：创建 PartColorMapper**

```java
package com.yigongbao.module.imaging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部位颜色透明度 Mapper
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Mapper
public interface PartColorMapper extends BaseMapper<PartColorEntity> {
}
```

- [ ] **Step 3：验证编译通过**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests -pl yigongbao-module-imaging -am
```

预期：BUILD SUCCESS

- [ ] **Step 4：提交**

```bash
git add yigongbao-parent/yigongbao-module-imaging/src/
git commit -m "feat(imaging): 新增 PartColorEntity 和 PartColorMapper"
```

---

## Task 4：VO 类

**Files:**
- Create: `vo/DcmPackageVO.java`
- Create: `vo/PackageModelFileVO.java`
- Create: `vo/PackageModelGroupVO.java`
- Create: `vo/ModelVO.java`

以下所有文件均在 `yigongbao-module-imaging/src/main/java/com/yigongbao/module/imaging/vo/` 下。

- [ ] **Step 1：创建 DcmPackageVO**

```java
package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DCM影像数据包 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "DCM影像数据包")
public class DcmPackageVO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "数据包编号")
    private String packageNo;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
}
```

- [ ] **Step 2：创建 PackageModelFileVO**

```java
package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据包内模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "数据包内模型文件")
public class PackageModelFileVO {

    @Schema(description = "包文件ID（design_package_file.id）")
    private Long packageFileId;

    @Schema(description = "文件名（含扩展名）")
    private String fileName;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "包内相对路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "颜色RGB值（如：170,255,0），匹配不到时为 null")
    private String colorCode;

    @Schema(description = "透明度（0.00~1.00），匹配不到时为 null")
    private BigDecimal opacity;
}
```

- [ ] **Step 3：创建 PackageModelGroupVO**

```java
package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单维度——按数据包分组的模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "按数据包分组的模型文件列表")
public class PackageModelGroupVO {

    @Schema(description = "数据包ID")
    private Long packageId;

    @Schema(description = "数据包编号（如：ORD20260410001-1）")
    private String packageCode;

    @Schema(description = "包内模型文件列表")
    private List<PackageModelFileVO> files;
}
```

- [ ] **Step 4：创建 ModelVO**

```java
package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 可视化模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "可视化模型文件")
public class ModelVO {

    @Schema(description = "模型ID（design_model.id）")
    private Long modelId;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名（含扩展名）")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "颜色RGB值（如：255,0,0），匹配不到时为 null")
    private String colorCode;

    @Schema(description = "透明度（0.00~1.00），匹配不到时为 null")
    private BigDecimal opacity;
}
```

- [ ] **Step 5：编译验证**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests -pl yigongbao-module-imaging -am
```

预期：BUILD SUCCESS

- [ ] **Step 6：提交**

```bash
git add yigongbao-parent/yigongbao-module-imaging/src/
git commit -m "feat(imaging): 新增4个 VO 类"
```

---

## Task 5：ImagingService 接口 + ImagingServiceImpl 实现

**Files:**
- Create: `service/ImagingService.java`
- Create: `service/impl/ImagingServiceImpl.java`

- [ ] **Step 1：先写单元测试（TDD）**

创建 `yigongbao-module-imaging/src/test/java/com/yigongbao/module/imaging/service/impl/ImagingServiceImplTest.java`：

```java
package com.yigongbao.module.imaging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import com.yigongbao.module.imaging.mapper.PartColorMapper;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ImagingServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImagingServiceImplTest {

    @Mock
    private OrderFileMapper orderFileMapper;
    @Mock
    private DesignPackageMapper designPackageMapper;
    @Mock
    private DesignPackageFileMapper designPackageFileMapper;
    @Mock
    private DesignModelMapper designModelMapper;
    @Mock
    private PartColorMapper partColorMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private ImagingServiceImpl imagingService;

    @BeforeEach
    void setUp() throws Exception {
        // 反射注入 baseMapper（ServiceImpl 要求）
        // ImagingServiceImpl 不继承 ServiceImpl，无需此步骤
    }

    // ==================== getDcmPackages ====================

    @Nested
    @DisplayName("getDcmPackages - 获取DCM影像包列表")
    class GetDcmPackagesTest {

        @Test
        @DisplayName("返回订单的影像数据包列表")
        void shouldReturnDcmPackageList() {
            // given
            Long orderId = 1L;
            OrderFileEntity orderFile = new OrderFileEntity();
            orderFile.setOrderId(orderId);
            orderFile.setFileId("file001");
            orderFile.setPackageNo("PKG001");
            orderFile.setCreateTime(LocalDateTime.now());

            FileVO fileVO = new FileVO();
            fileVO.setId("file001");
            fileVO.setFileName("影像包.zip");
            fileVO.setFileUrl("https://example.com/file001");
            fileVO.setFileSize(1024L);

            when(orderFileMapper.selectList(any())).thenReturn(List.of(orderFile));
            when(fileService.listByIds(List.of("file001"))).thenReturn(List.of(fileVO));

            // when
            List<DcmPackageVO> result = imagingService.getDcmPackages(orderId);

            // then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("file001", result.get(0).getFileId());
            assertEquals("影像包.zip", result.get(0).getFileName());
            assertEquals("PKG001", result.get(0).getPackageNo());
        }

        @Test
        @DisplayName("订单无影像文件时返回空列表")
        void shouldReturnEmptyListWhenNoFiles() {
            // given
            when(orderFileMapper.selectList(any())).thenReturn(List.of());

            // when
            List<DcmPackageVO> result = imagingService.getDcmPackages(1L);

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(fileService, never()).listByIds(any());
        }
    }

    // ==================== getPackageModelFiles（数据包维度）====================

    @Nested
    @DisplayName("getPackageModelFiles - 数据包维度模型文件")
    class GetPackageModelFilesTest {

        @Test
        @DisplayName("返回数据包内文件列表并附带颜色透明度")
        void shouldReturnFilesWithColorAndOpacity() {
            // given
            Long packageId = 10L;
            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(101L);
            file.setPackageId(packageId);
            file.setFileName("右肺上叶.stl");
            file.setFileExt("stl");
            file.setFilePath("models/右肺上叶.stl");
            file.setFileSize(2048L);

            PartColorEntity color = new PartColorEntity();
            color.setPartDetail("右肺上叶");
            color.setColorCode("170,255,0");
            color.setOpacity(new BigDecimal("0.80"));

            when(designPackageFileMapper.selectList(any())).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of(color));

            // when
            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(packageId);

            // then
            assertEquals(1, result.size());
            PackageModelFileVO vo = result.get(0);
            assertEquals(101L, vo.getPackageFileId());
            assertEquals("右肺上叶.stl", vo.getFileName());
            assertEquals("170,255,0", vo.getColorCode());
            assertEquals(new BigDecimal("0.80"), vo.getOpacity());
        }

        @Test
        @DisplayName("文件名无法匹配颜色时 colorCode 和 opacity 为 null")
        void shouldReturnNullColorWhenNoMatch() {
            // given
            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(102L);
            file.setFileName("未知部位.stl");
            file.setFileExt("stl");

            when(designPackageFileMapper.selectList(any())).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of());

            // when
            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(1L);

            // then
            assertEquals(1, result.size());
            assertNull(result.get(0).getColorCode());
            assertNull(result.get(0).getOpacity());
        }

        @Test
        @DisplayName("数据包无文件时返回空列表")
        void shouldReturnEmptyListWhenNoFiles() {
            when(designPackageFileMapper.selectList(any())).thenReturn(List.of());

            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(1L);

            assertTrue(result.isEmpty());
            verify(partColorMapper, never()).selectList(any());
        }
    }

    // ==================== getPackageModelFilesByOrder（订单维度）====================

    @Nested
    @DisplayName("getPackageModelFilesByOrder - 订单维度分组模型文件")
    class GetPackageModelFilesByOrderTest {

        @Test
        @DisplayName("返回按包分组的文件列表")
        void shouldReturnGroupedByPackage() {
            // given
            Long orderId = 1L;
            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(10L);
            pkg.setOrderId(orderId);
            pkg.setPackageCode("ORD001-1");

            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(101L);
            file.setPackageId(10L);
            file.setFileName("右肺上叶.stl");
            file.setFileExt("stl");
            file.setFilePath("models/右肺上叶.stl");
            file.setFileSize(2048L);

            when(designPackageMapper.selectList(any())).thenReturn(List.of(pkg));
            when(designPackageFileMapper.selectList(any())).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of());

            // when
            List<PackageModelGroupVO> result = imagingService.getPackageModelFilesByOrder(orderId);

            // then
            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getPackageId());
            assertEquals("ORD001-1", result.get(0).getPackageCode());
            assertEquals(1, result.get(0).getFiles().size());
        }

        @Test
        @DisplayName("订单无数据包时返回空列表")
        void shouldReturnEmptyWhenNoPackages() {
            when(designPackageMapper.selectList(any())).thenReturn(List.of());

            List<PackageModelGroupVO> result = imagingService.getPackageModelFilesByOrder(1L);

            assertTrue(result.isEmpty());
        }
    }

    // ==================== getModels ====================

    @Nested
    @DisplayName("getModels - 可视化模型列表")
    class GetModelsTest {

        @Test
        @DisplayName("返回可视化模型列表并附带颜色透明度")
        void shouldReturnModelsWithColor() {
            // given
            Long orderId = 1L;
            DesignModelEntity model = new DesignModelEntity();
            model.setId(1L);
            model.setOrderId(orderId);
            model.setFileId("fileABC");

            FileVO fileVO = new FileVO();
            fileVO.setId("fileABC");
            fileVO.setFileName("整体模型.stl");
            fileVO.setFileUrl("https://example.com/fileABC");
            fileVO.setFileSize(4096L);

            PartColorEntity color = new PartColorEntity();
            color.setPartDetail("整体模型");
            color.setColorCode("255,0,0");
            color.setOpacity(new BigDecimal("0.90"));

            when(designModelMapper.selectList(any())).thenReturn(List.of(model));
            when(fileService.listByIds(List.of("fileABC"))).thenReturn(List.of(fileVO));
            when(partColorMapper.selectList(any())).thenReturn(List.of(color));

            // when
            List<ModelVO> result = imagingService.getModels(orderId);

            // then
            assertEquals(1, result.size());
            ModelVO vo = result.get(0);
            assertEquals(1L, vo.getModelId());
            assertEquals("fileABC", vo.getFileId());
            assertEquals("整体模型.stl", vo.getFileName());
            assertEquals("255,0,0", vo.getColorCode());
            assertEquals(new BigDecimal("0.90"), vo.getOpacity());
        }

        @Test
        @DisplayName("订单无模型时返回空列表")
        void shouldReturnEmptyWhenNoModels() {
            when(designModelMapper.selectList(any())).thenReturn(List.of());

            List<ModelVO> result = imagingService.getModels(1L);

            assertTrue(result.isEmpty());
        }
    }
}
```

- [ ] **Step 2：运行测试，确认编译失败（类不存在）**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-imaging -Dtest=ImagingServiceImplTest 2>&1 | head -30
```

预期：编译失败，`ImagingServiceImpl` 不存在

- [ ] **Step 3：创建 ImagingService 接口**

```java
package com.yigongbao.module.imaging.service;

import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;

import java.util.List;

/**
 * 影像阅览服务接口
 *
 * @author hanjor
 * @date 2026-04-20
 */
public interface ImagingService {

    /**
     * 获取订单的DCM影像数据包列表
     *
     * @param orderId 订单ID
     * @return DCM影像包列表
     */
    List<DcmPackageVO> getDcmPackages(Long orderId);

    /**
     * 获取指定数据包内的模型文件列表（含颜色透明度）
     *
     * @param packageId 数据包ID
     * @return 模型文件列表
     */
    List<PackageModelFileVO> getPackageModelFiles(Long packageId);

    /**
     * 获取订单所有数据包内的模型文件，按包分组（含颜色透明度）
     *
     * @param orderId 订单ID
     * @return 按包分组的模型文件列表
     */
    List<PackageModelGroupVO> getPackageModelFilesByOrder(Long orderId);

    /**
     * 获取订单的可视化模型列表（含颜色透明度）
     *
     * @param orderId 订单ID
     * @return 可视化模型列表
     */
    List<ModelVO> getModels(Long orderId);
}
```

- [ ] **Step 4：创建 ImagingServiceImpl**

```java
package com.yigongbao.module.imaging.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 影像阅览服务实现
 * 只读操作：聚合订单影像文件和设计模型文件，为前端阅览提供数据
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingServiceImpl implements ImagingService {

    private final OrderFileMapper orderFileMapper;
    private final DesignPackageMapper designPackageMapper;
    private final DesignPackageFileMapper designPackageFileMapper;
    private final DesignModelMapper designModelMapper;
    private final PartColorMapper partColorMapper;
    private final FileService fileService;

    /**
     * DCM影像文件的字典编码
     */
    private static final String FILE_CATEGORY_DCM = "10.1";

    @Override
    public List<DcmPackageVO> getDcmPackages(Long orderId) {
        log.info("查询DCM影像包列表, orderId={}", orderId);

        // 1. 查询订单下的影像文件记录
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
                        .eq(OrderFileEntity::getFileCategory, FILE_CATEGORY_DCM)
                        .orderByAsc(OrderFileEntity::getId)
        );

        if (CollUtil.isEmpty(orderFiles)) {
            return new ArrayList<>();
        }

        // 2. 批量查询文件详情，避免 N+1
        List<String> fileIds = orderFiles.stream()
                .map(OrderFileEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 组装 VO
        return orderFiles.stream().map(of -> {
            DcmPackageVO vo = new DcmPackageVO();
            FileVO fileVO = fileMap.get(of.getFileId());
            vo.setFileId(of.getFileId());
            vo.setPackageNo(of.getPackageNo());
            vo.setUploadTime(of.getCreateTime());
            if (fileVO != null) {
                vo.setFileName(fileVO.getFileName());
                vo.setFileUrl(fileVO.getFileUrl());
                vo.setFileSize(fileVO.getFileSize());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PackageModelFileVO> getPackageModelFiles(Long packageId) {
        log.info("查询数据包内模型文件, packageId={}", packageId);

        // 1. 查询包内文件列表
        List<DesignPackageFileEntity> files = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder)
        );

        if (CollUtil.isEmpty(files)) {
            return new ArrayList<>();
        }

        // 2. 批量查询颜色透明度
        Map<String, PartColorEntity> colorMap = batchQueryColors(files.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 3. 组装 VO
        return files.stream()
                .map(f -> toPackageModelFileVO(f, colorMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<PackageModelGroupVO> getPackageModelFilesByOrder(Long orderId) {
        log.info("查询订单所有数据包模型文件（按包分组）, orderId={}", orderId);

        // 1. 查询该订单的所有数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq)
        );

        if (CollUtil.isEmpty(packages)) {
            return new ArrayList<>();
        }

        // 2. 批量查询所有包内文件
        List<Long> packageIds = packages.stream()
                .map(DesignPackageEntity::getId)
                .collect(Collectors.toList());
        List<DesignPackageFileEntity> allFiles = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .in(DesignPackageFileEntity::getPackageId, packageIds)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder)
        );

        // 3. 批量查询颜色透明度（一次 IN 查询，避免 N+1）
        Map<String, PartColorEntity> colorMap = batchQueryColors(allFiles.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 4. 按包分组组装 VO
        Map<Long, List<DesignPackageFileEntity>> filesByPackage = allFiles.stream()
                .collect(Collectors.groupingBy(DesignPackageFileEntity::getPackageId));

        return packages.stream().map(pkg -> {
            PackageModelGroupVO group = new PackageModelGroupVO();
            group.setPackageId(pkg.getId());
            group.setPackageCode(pkg.getPackageCode());
            List<DesignPackageFileEntity> pkgFiles = filesByPackage.getOrDefault(pkg.getId(), List.of());
            group.setFiles(pkgFiles.stream()
                    .map(f -> toPackageModelFileVO(f, colorMap))
                    .collect(Collectors.toList()));
            return group;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ModelVO> getModels(Long orderId) {
        log.info("查询可视化模型列表, orderId={}", orderId);

        // 1. 查询设计模型记录
        List<DesignModelEntity> models = designModelMapper.selectList(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .orderByAsc(DesignModelEntity::getId)
        );

        if (CollUtil.isEmpty(models)) {
            return new ArrayList<>();
        }

        // 2. 批量查询文件详情
        List<String> fileIds = models.stream()
                .map(DesignModelEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 批量查询颜色透明度（按文件名匹配）
        List<String> fileNames = fileMap.values().stream()
                .map(FileVO::getFileName)
                .collect(Collectors.toList());
        Map<String, PartColorEntity> colorMap = batchQueryColors(fileNames);

        // 4. 组装 VO
        return models.stream().map(m -> {
            ModelVO vo = new ModelVO();
            vo.setModelId(m.getId());
            vo.setFileId(m.getFileId());
            FileVO fileVO = fileMap.get(m.getFileId());
            if (fileVO != null) {
                vo.setFileName(fileVO.getFileName());
                vo.setFileUrl(fileVO.getFileUrl());
                vo.setFileSize(fileVO.getFileSize());
                // 颜色透明度按文件名去扩展名精确匹配（Hutool FileUtil.mainName）
                String nameWithoutExt = FileUtil.mainName(fileVO.getFileName());
                PartColorEntity color = colorMap.get(nameWithoutExt);
                if (color != null) {
                    vo.setColorCode(color.getColorCode());
                    vo.setOpacity(color.getOpacity());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 批量查询颜色透明度（一次 IN 查询）
     * key = partDetail（文件名去扩展名，使用 Hutool FileUtil.mainName）
     */
    private Map<String, PartColorEntity> batchQueryColors(List<String> fileNames) {
        if (CollUtil.isEmpty(fileNames)) {
            return Map.of();
        }
        // 提取所有文件名（去扩展名）作为查询条件，使用 Hutool FileUtil.mainName()
        Set<String> partDetails = fileNames.stream()
                .map(FileUtil::mainName)
                .collect(Collectors.toSet());

        List<PartColorEntity> colors = partColorMapper.selectList(
                new LambdaQueryWrapper<PartColorEntity>()
                        .in(PartColorEntity::getPartDetail, partDetails)
        );
        return colors.stream()
                .collect(Collectors.toMap(PartColorEntity::getPartDetail, c -> c,
                        (existing, replacement) -> existing)); // 同名取第一条
    }

    /**
     * 将 DesignPackageFileEntity 转换为 PackageModelFileVO，填充颜色透明度
     */
    private PackageModelFileVO toPackageModelFileVO(DesignPackageFileEntity f,
                                                     Map<String, PartColorEntity> colorMap) {
        PackageModelFileVO vo = new PackageModelFileVO();
        vo.setPackageFileId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileExt(f.getFileExt());
        vo.setFilePath(f.getFilePath());
        vo.setFileSize(f.getFileSize());
        // 精确匹配颜色（使用 Hutool FileUtil.mainName() 去扩展名）
        String nameWithoutExt = FileUtil.mainName(f.getFileName());
        PartColorEntity color = colorMap.get(nameWithoutExt);
        if (color != null) {
            vo.setColorCode(color.getColorCode());
            vo.setOpacity(color.getOpacity());
        }
        return vo;
    }
}
```

- [ ] **Step 5：运行测试，确认通过**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-imaging -Dtest=ImagingServiceImplTest
```

预期：Tests run: 9, Failures: 0, Errors: 0

- [ ] **Step 6：提交**

```bash
git add yigongbao-parent/yigongbao-module-imaging/src/
git commit -m "feat(imaging): 实现 ImagingService 和 ImagingServiceImpl，含完整单元测试"
```

---

## Task 6：ImagingController

**Files:**
- Create: `controller/ImagingController.java`

- [ ] **Step 1：创建 ImagingController**

```java
package com.yigongbao.module.imaging.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.yigongbao.common.result.Result;
import com.yigongbao.module.imaging.service.ImagingService;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影像阅览接口
 * 为前端2D/3D阅览器提供影像文件和模型文件数据，只读
 *
 * @author hanjor
 * @date 2026-04-20
 */
@SaCheckLogin
@Tag(name = "影像阅览")
@RestController
@RequestMapping("/imaging")
@RequiredArgsConstructor
public class ImagingController {

    private final ImagingService imagingService;

    @Operation(summary = "获取订单DCM影像数据包列表")
    @GetMapping("/{orderId}/dcm-packages")
    public Result<List<DcmPackageVO>> getDcmPackages(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        return Result.success(imagingService.getDcmPackages(orderId));
    }

    @Operation(summary = "获取订单所有打印数据包内的模型文件（按包分组）")
    @GetMapping("/{orderId}/package-model-files")
    public Result<List<PackageModelGroupVO>> getPackageModelFilesByOrder(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        return Result.success(imagingService.getPackageModelFilesByOrder(orderId));
    }

    @Operation(summary = "获取指定打印数据包内的模型文件列表")
    @GetMapping("/design-package/{packageId}/model-files")
    public Result<List<PackageModelFileVO>> getPackageModelFiles(
            @Parameter(description = "数据包ID") @PathVariable Long packageId) {
        return Result.success(imagingService.getPackageModelFiles(packageId));
    }

    @Operation(summary = "获取订单可视化模型列表")
    @GetMapping("/{orderId}/models")
    public Result<List<ModelVO>> getModels(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        return Result.success(imagingService.getModels(orderId));
    }
}
```

- [ ] **Step 2：完整编译整个项目**

```bash
cd yigongbao-parent
mvn clean compile -DskipTests
```

预期：BUILD SUCCESS

- [ ] **Step 3：运行全部测试**

```bash
cd yigongbao-parent
mvn test -pl yigongbao-module-imaging
```

预期：Tests run: 9, Failures: 0, Errors: 0

- [ ] **Step 4：提交**

```bash
git add yigongbao-parent/yigongbao-module-imaging/src/
git commit -m "feat(imaging): 新增 ImagingController，完成模块所有接口"
```

---

## Task 7：集成验证

- [ ] **Step 1：启动应用**

```bash
cd yigongbao-parent
mvn -pl yigongbao-boot spring-boot:run
```

预期：应用正常启动，无报错

- [ ] **Step 2：打开 Swagger UI，确认4个接口已注册**

访问 `http://localhost:8080/api/swagger-ui.html`，找到 **影像阅览** 分组，确认以下接口存在：
- `GET /imaging/{orderId}/dcm-packages`
- `GET /imaging/{orderId}/package-model-files`
- `GET /imaging/design-package/{packageId}/model-files`
- `GET /imaging/{orderId}/models`

- [ ] **Step 3：用真实数据调用接口验证**

使用 Swagger UI 或 curl，对已有测试订单调用各接口，确认：
1. DCM接口返回影像文件地址
2. 模型文件接口返回文件列表（有匹配颜色数据的文件带有颜色和透明度）
3. 可视化模型接口正常返回

- [ ] **Step 4：最终提交**

```bash
git add .
git commit -m "feat(imaging): yigongbao-module-imaging 模块完成，集成验证通过"
```
