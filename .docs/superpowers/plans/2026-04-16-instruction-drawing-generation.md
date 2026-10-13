# 指令单与图纸生成管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现生产指令单和图纸的 Excel 模板填充生成、多版本历史管理、模板版下载、修订版上传功能。

**Architecture:** DesignDocController 接收请求 → DesignDocServiceImpl 负责业务逻辑（校验、版本管理、存储） → InstructionExcelBuilder / DrawingExcelBuilder 专职 POI Excel 填充，两个 Builder 均在 `helper` 包中，与 Service 分离。文件存储通过 `FileService` + Spring `MockMultipartFile` 包装 byte[] 完成上传（复用现有 FileService，无需引入新依赖）。

**Tech Stack:** Java 21, Spring Boot 3, MyBatis Plus 3.5.8, Apache POI 5.2.5, SaToken 1.37.0, x-file-storage 2.3.0（通过 FileService 封装）

---

## 文件清单

### 新增文件
| 文件 | 说明 |
|------|------|
| `helper/InstructionExcelBuilder.java` | 生产指令单 Excel 填充（POI 动态行展开） |
| `helper/DrawingExcelBuilder.java` | 图纸 Excel 填充（槽位填充 + 分页） |
| `service/DesignDocService.java` | 指令单/图纸业务服务接口 |
| `service/impl/DesignDocServiceImpl.java` | 指令单/图纸业务服务实现 |
| `controller/DesignDocController.java` | 指令单/图纸 REST 接口 |
| `vo/GenerateDocsResultVO.java` | 生成结果 VO |
| `vo/DocItemVO.java` | 单个文档（指令单或图纸）结果 VO |
| `vo/DesignDocVersionVO.java` | 版本列表 VO（指令单和图纸通用） |
| `test/.../helper/InstructionExcelBuilderTest.java` | Builder 单元测试 |
| `test/.../helper/DrawingExcelBuilderTest.java` | Builder 单元测试 |
| `test/.../service/impl/DesignDocServiceImplTest.java` | Service 单元测试 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `common/.../enums/ErrorCodeEnum.java` | 新增 PRINT_INFO_REQUIRED(750)、DOC_VERSION_NOT_FOUND(751) |
| `service/DesignInstructionService.java` | 新增业务方法声明 |
| `service/DesignDrawingService.java` | 新增业务方法声明 |
| `service/impl/DesignInstructionServiceImpl.java` | 实现业务方法 |
| `service/impl/DesignDrawingServiceImpl.java` | 实现业务方法 |

---

## Task 1: 新增错误码 + VO 类

**Files:**
- Modify: `yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java`
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/GenerateDocsResultVO.java`
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DocItemVO.java`
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignDocVersionVO.java`

- [ ] **Step 1: 在 ErrorCodeEnum 末尾（PRODUCT_SPEC_IN_USE 之后）新增两个错误码**

在 `PRODUCT_SPEC_IN_USE(744, ...)` 后面追加：

```java
// ==================== 设计文档生成（750-751）====================
PRINT_INFO_REQUIRED(750, "请先填写数据包的打印信息，再生成指令单和图纸"),
DOC_VERSION_NOT_FOUND(751, "指定版本的文档不存在");
```

- [ ] **Step 2: 创建 DocItemVO.java**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单个文档（指令单/图纸）生成结果 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DocItemVO {
    /** 记录ID */
    private Long id;
    /** 版本号，如 A/1 */
    private String version;
    /** 模板文件访问 URL */
    private String templateFileUrl;
    /** 生成时间 */
    private LocalDateTime generateTime;
}
```

- [ ] **Step 3: 创建 GenerateDocsResultVO.java**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 生成指令单和图纸的结果 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class GenerateDocsResultVO {
    /** 指令单 */
    private DocItemVO instruction;
    /** 图纸 */
    private DocItemVO drawing;
}
```

- [ ] **Step 4: 创建 DesignDocVersionVO.java**

```java
package com.yigongbao.module.design.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 指令单/图纸版本列表 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignDocVersionVO {
    /** 记录ID */
    private Long id;
    /** 版本号，如 A/1 */
    private String version;
    /** 版本序号 */
    private Integer versionSeq;
    /** 系统生成的模板文件 URL */
    private String templateFileUrl;
    /** 设计师上传的修订版文件 URL（可为 null） */
    private String revisedFileUrl;
    /** 生成时间 */
    private LocalDateTime generateTime;
    /** 修订版上传时间（可为 null） */
    private LocalDateTime revisedUploadTime;
}
```

- [ ] **Step 5: 编译验证**

在 `yigongbao-parent/` 下执行：
```bash
mvn compile -pl yigongbao-common,yigongbao-module-design -am -q
```
预期：BUILD SUCCESS，无编译错误。

- [ ] **Step 6: Commit**

```bash
git add yigongbao-common/src/main/java/com/yigongbao/common/enums/ErrorCodeEnum.java \
        yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DocItemVO.java \
        yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/GenerateDocsResultVO.java \
        yigongbao-module-design/src/main/java/com/yigongbao/module/design/vo/DesignDocVersionVO.java
git commit -m "feat(design): add error codes and VO classes for doc generation"
```

---

## Task 2: InstructionExcelBuilder — 生产指令单填充

**Files:**
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/InstructionExcelBuilder.java`
- Create: `yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/InstructionExcelBuilderTest.java`

**模板关键坐标（0-indexed，row/col 从 0 开始）：**
- 标题行：row=0，A1:I1 合并
- 版本号：row=1，G2:I2 合并 → G1（0-indexed: row=1, col=6）
- 基本信息区：
  - 订单编号值：row=3, col=1（B4 → 0-indexed）
  - 客户名称值：row=3, col=4（E4 → col=4）
  - 联系人值：row=3, col=6（I4 → col=8，但 G4:H4 合并，写 col=6）
  - 数据包编号值：row=4, col=1（B5）
  - 医院值：row=4, col=3（D5，但合并到 E5，写 D5 col=3）
  - 预交货时间值：row=4, col=6（G5，合并 G5:H5）
- 表头行：row=6（序号、注册证号...）
- 序号"1"行：row=7（原A8）
- 产品数据区：row=8 到 row=24（A9:I25，0-indexed: 8-24，共 17 行，全列合并）
- 底部固定区（原行26-30）：产品标识=row=25,col=1；包装数量=row=25,col=4；患者姓名=row=26,col=1；是否邮寄=row=26,col=4；开始时间=row=25,col=7；结束时间=row=26,col=7；邮寄地址=row=27,col=1；备注=row=28,col=1；签名行=row=29

**注意**：`sheet.shiftRows(fromRow, toRow, shift)` 会下移行，但不会自动复制行样式。需要手动将原样式行（行7，即 0-indexed=7）的 CellStyle 复制到新插入行。

- [ ] **Step 1: 创建 InstructionExcelBuilderTest.java（先写测试）**

```java
package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionExcelBuilderTest {

    @Test
    void build_withSingleProduct_shouldFillBasicInfoAndProductRow() throws Exception {
        InstructionExcelBuilder builder = new InstructionExcelBuilder();

        List<DesignProductEntity> products = new ArrayList<>();
        DesignProductEntity p = new DesignProductEntity();
        p.setCertNo("CERT-001");
        p.setProductName("PEEK骨模型");
        p.setPackageFileName("左髋骨.stl");
        p.setSpecName("47mm");
        p.setMaterialName("树脂");
        p.setQuantity(1);
        p.setTimeliness("7天");
        p.setColorName("白色");
        products.add(p);

        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPatientName("张三");
        ctx.setHospitalName("北京协和医院");
        ctx.setContactName("李医生");
        ctx.setPackageCode("PKG-001");
        ctx.setExpectedDeliveryDate("2026-04-20");
        ctx.setVersion("A/1");
        ctx.setProducts(products);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        assertTrue(result.length > 0);

        // 验证填充结果
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            // 订单编号
            assertEquals("ORD-001", getCellValue(sheet, 3, 1));
            // 版本号
            assertTrue(getCellValue(sheet, 1, 6).contains("A/1"));
        }
    }

    @Test
    void build_withMoreThan17Products_shouldShiftRowsAndFillAll() throws Exception {
        InstructionExcelBuilder builder = new InstructionExcelBuilder();

        List<DesignProductEntity> products = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            DesignProductEntity p = new DesignProductEntity();
            p.setCertNo("CERT-" + i);
            p.setProductName("产品" + i);
            p.setPackageFileName("文件" + i + ".stl");
            p.setSpecName("规格" + i);
            p.setMaterialName("树脂");
            p.setQuantity(1);
            products.add(p);
        }

        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setVersion("A/1");
        ctx.setProducts(products);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            // 产品区从 row=8 开始，20条产品，最后一条在 row=27（8+19）
            assertEquals("CERT-20", getCellValue(sheet, 27, 1));
        }
    }

    private String getCellValue(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=InstructionExcelBuilderTest -q 2>&1 | tail -20
```
预期：FAIL，`InstructionExcelBuilder` 类不存在。

- [ ] **Step 3: 创建 InstructionExcelBuilder.java**

```java
package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 生产指令单 Excel 填充器
 * <p>
 * 模板路径：classpath:template/生产指令单.xlsx
 * 核心逻辑：将行9-25的纵向合并大格子拆开，按产品数量动态展开多行。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Component
public class InstructionExcelBuilder {

    private static final String TEMPLATE_PATH = "template/生产指令单.xlsx";
    /** 产品数据区起始行（0-indexed，对应模板行9） */
    private static final int DATA_ROW_START = 8;
    /** 产品数据区原始行数（模板行9-25，共17行） */
    private static final int DATA_ROW_ORIGINAL_COUNT = 17;
    /** 产品数据区结束行（0-indexed，对应模板行25） */
    private static final int DATA_ROW_END = DATA_ROW_START + DATA_ROW_ORIGINAL_COUNT - 1;

    /**
     * 填充上下文数据（Builder Pattern）
     */
    @Data
    public static class BuildContext {
        private String orderCode;
        private String patientName;
        private String hospitalName;
        private String contactName;
        private String packageCode;
        private String expectedDeliveryDate;
        private String postalAddress;
        private String remark;
        private String version;
        private List<DesignProductEntity> products;
    }

    /**
     * 根据上下文填充指令单模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        log.info("开始生成生产指令单，orderCode={}, version={}, productCount={}",
                ctx.getOrderCode(), ctx.getVersion(), ctx.getProducts() == null ? 0 : ctx.getProducts().size());

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            List<DesignProductEntity> products = ctx.getProducts() == null ? List.of() : ctx.getProducts();
            int n = products.size();

            // 1. 覆盖版本号（row=1, col=6，即 G2）
            setCell(sheet, 1, 6, "版本号：" + strOrEmpty(ctx.getVersion()));

            // 2. 填充基本信息区
            setCell(sheet, 3, 1, strOrEmpty(ctx.getOrderCode()));       // B4：订单编号
            setCell(sheet, 3, 4, strOrEmpty(ctx.getPatientName()));     // E4：客户名称（患者姓名）
            setCell(sheet, 3, 6, strOrEmpty(ctx.getContactName()));     // G4：联系人
            setCell(sheet, 4, 1, strOrEmpty(ctx.getPackageCode()));     // B5：数据包编号
            setCell(sheet, 4, 3, strOrEmpty(ctx.getHospitalName()));    // D5：医院
            setCell(sheet, 4, 6, strOrEmpty(ctx.getExpectedDeliveryDate())); // G5：预交货时间

            // 3. 处理产品数据区（row 8-24，共17行，列A-I全部纵向合并）
            //    先移除这17行中所有合并区域
            removeMergedRegionsInRows(sheet, DATA_ROW_START, DATA_ROW_END);

            // 4. 若产品数 > 17，需要下移后续行腾出空间
            int lastRow = sheet.getLastRowNum();
            if (n > DATA_ROW_ORIGINAL_COUNT) {
                int extra = n - DATA_ROW_ORIGINAL_COUNT;
                sheet.shiftRows(DATA_ROW_END + 1, lastRow, extra);
                // 插入新行并复制行7（表头下方第一数据行）的样式
                CellStyle templateStyle = getRowStyle(sheet, DATA_ROW_START);
                for (int i = DATA_ROW_ORIGINAL_COUNT; i < n; i++) {
                    Row newRow = sheet.createRow(DATA_ROW_START + i);
                    copyRowStyle(newRow, templateStyle, 9); // 9列
                }
            }

            // 5. 逐行写入产品数据
            for (int i = 0; i < n; i++) {
                DesignProductEntity p = products.get(i);
                int rowIdx = DATA_ROW_START + i;
                setCell(sheet, rowIdx, 0, String.valueOf(i + 1));                // 序号
                setCell(sheet, rowIdx, 1, strOrEmpty(p.getCertNo()));            // 注册证号
                setCell(sheet, rowIdx, 2, strOrEmpty(p.getProductName()));       // 产品名称
                setCell(sheet, rowIdx, 3, strOrEmpty(p.getPackageFileName()));   // 数据文件名称
                setCell(sheet, rowIdx, 4, strOrEmpty(p.getSpecName()));          // 型号/规格
                setCell(sheet, rowIdx, 5, strOrEmpty(p.getMaterialName()));      // 材质
                setCell(sheet, rowIdx, 6, p.getQuantity() != null ? String.valueOf(p.getQuantity()) : ""); // 数量
                setCell(sheet, rowIdx, 7, strOrEmpty(p.getTimeliness()));        // 时效
                setCell(sheet, rowIdx, 8, strOrEmpty(p.getColorName()));         // 颜色
            }

            // 6. 填充底部区域（行26起，经过 shift 后实际行索引 = DATA_ROW_START + n）
            //    原模板行26=DATA_ROW_START+17=25（0-indexed），shift 后位移量=max(0, n-17)
            int bottomOffset = Math.max(0, n - DATA_ROW_ORIGINAL_COUNT);
            int row26 = 25 + bottomOffset; // 原行26（0-indexed=25）
            int row27 = 26 + bottomOffset;
            int row28 = 27 + bottomOffset;
            int row29 = 28 + bottomOffset;
            setCell(sheet, row26, 1, "");   // 产品标识（留空由线下填写）
            setCell(sheet, row26, 4, "");   // 包装数量
            setCell(sheet, row27, 1, strOrEmpty(ctx.getPatientName())); // 患者姓名
            setCell(sheet, row28, 1, strOrEmpty(ctx.getPostalAddress())); // 邮寄地址
            setCell(sheet, row29, 1, strOrEmpty(ctx.getRemark()));        // 备注

            // 7. 写出为 byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("生产指令单生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== 私有工具方法 ====================

    /** 移除指定行范围内的所有合并区域 */
    private void removeMergedRegionsInRows(Sheet sheet, int startRow, int endRow) {
        // 倒序遍历避免 remove 后下标错位
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() >= startRow && region.getLastRow() <= endRow) {
                sheet.removeMergedRegion(i);
            }
        }
    }

    /** 获取指定行的第一个单元格样式（用于后续复制） */
    private CellStyle getRowStyle(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null || row.getCell(0) == null) {
            return sheet.getWorkbook().createCellStyle();
        }
        return row.getCell(0).getCellStyle();
    }

    /** 为新插入行的每列设置样式 */
    private void copyRowStyle(Row row, CellStyle style, int colCount) {
        for (int c = 0; c < colCount; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(style);
        }
    }

    /** 向指定行列写入字符串值 */
    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        cell.setCellValue(value != null ? value : "");
    }

    private String strOrEmpty(String s) {
        return s != null ? s : "";
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=InstructionExcelBuilderTest -q 2>&1 | tail -20
```
预期：Tests run: 2, Failures: 0, Errors: 0。

- [ ] **Step 5: Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/InstructionExcelBuilder.java \
        yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/InstructionExcelBuilderTest.java
git commit -m "feat(design): implement InstructionExcelBuilder with dynamic row expansion"
```

---

## Task 3: DrawingExcelBuilder — 图纸填充

**Files:**
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DrawingExcelBuilder.java`
- Create: `yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/DrawingExcelBuilderTest.java`

**图纸模板槽位坐标（0-indexed）：**

第1页（row 0-41）共11个内容槽位（右上角 M-P 区域为二维码，不填产品）：

| 槽位序号 | 行组 | 列组 | 文件名行 | 产品名行 |
|---------|------|------|---------|---------|
| 1 | rows 0-11 | cols 0-3 (A-D) | row=0, col=2 | row=1, col=0 |
| 2 | rows 0-11 | cols 4-7 (E-H) | row=0, col=6 | row=1, col=4 |
| 3 | rows 0-11 | cols 8-11 (I-L) | row=0, col=10 | row=1, col=8 |
| 4 | rows 12-23 | cols 0-3 | row=12, col=2 | row=13, col=0 |
| 5 | rows 12-23 | cols 4-7 | row=12, col=6 | row=13, col=4 |
| 6 | rows 12-23 | cols 8-11 | row=12, col=10 | row=13, col=8 |
| 7 | rows 12-23 | cols 12-15 | row=12, col=14 | row=13, col=12 |
| 8 | rows 24-35 | cols 0-3 | row=24, col=2 | row=25, col=0 |
| 9 | rows 24-35 | cols 4-7 | row=24, col=6 | row=25, col=4 |
| 10 | rows 24-35 | cols 8-11 | row=24, col=10 | row=25, col=8 |
| 11 | rows 24-35 | cols 12-15 | row=24, col=14 | row=25, col=12 |

Footer（rows 36-41）：
- 数据包编号值：row=36, col=9（J37 对应 0-indexed col=9）
- 订单编号值：row=36, col=13（N37）
- 页码文本："第X页/共Y页"：row=38, col=12（M39）

- [ ] **Step 1: 创建 DrawingExcelBuilderTest.java（先写测试）**

```java
package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingExcelBuilderTest {

    @Test
    void build_withFewProducts_shouldFillSlotsOnSingleSheet() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        List<DesignProductEntity> products = buildProducts(3);

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setProducts(products);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            // 3 条产品只有 1 页
            assertEquals(1, wb.getNumberOfSheets());
        }
    }

    @Test
    void build_withMoreThan11Products_shouldCreateMultipleSheets() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        List<DesignProductEntity> products = buildProducts(13);

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setProducts(products);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            // 13 条产品：第1页11条，第2页2条，共2页
            assertEquals(2, wb.getNumberOfSheets());
        }
    }

    private List<DesignProductEntity> buildProducts(int count) {
        List<DesignProductEntity> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            DesignProductEntity p = new DesignProductEntity();
            p.setPackageFileName("文件" + i + ".stl");
            p.setProductName("产品" + i);
            list.add(p);
        }
        return list;
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=DrawingExcelBuilderTest -q 2>&1 | tail -10
```
预期：FAIL，类不存在。

- [ ] **Step 3: 创建 DrawingExcelBuilder.java**

```java
package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 图纸 Excel 填充器
 * <p>
 * 模板路径：classpath:template/图纸.xlsx
 * 核心逻辑：11个槽位/页，超出时复制 Sheet 分页，更新页码。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Component
public class DrawingExcelBuilder {

    private static final String TEMPLATE_PATH = "template/图纸.xlsx";
    /** 每页最多容纳的产品槽位数（右上角为二维码，共11个内容位） */
    private static final int SLOTS_PER_PAGE = 11;

    /**
     * 填充上下文
     */
    @Data
    public static class BuildContext {
        private String orderCode;
        private String packageCode;
        private String remark;
        private List<DesignProductEntity> products;
    }

    /**
     * 槽位坐标定义：[文件名行, 文件名列, 产品名行, 产品名列]
     * 基于模板分析（0-indexed），共11个内容槽位
     */
    private static final int[][] SLOT_COORDS = {
        // {fileNameRow, fileNameCol, productNameRow, productNameCol}
        {0,  2,  1,  0},   // 槽1：A-D区
        {0,  6,  1,  4},   // 槽2：E-H区
        {0,  10, 1,  8},   // 槽3：I-L区
        {12, 2,  13, 0},   // 槽4：A-D区 第2行组
        {12, 6,  13, 4},   // 槽5：E-H区
        {12, 10, 13, 8},   // 槽6：I-L区
        {12, 14, 13, 12},  // 槽7：M-P区
        {24, 2,  25, 0},   // 槽8：A-D区 第3行组
        {24, 6,  25, 4},   // 槽9：E-H区
        {24, 10, 25, 8},   // 槽10：I-L区
        {24, 14, 25, 12},  // 槽11：M-P区
    };

    /** footer 行（0-indexed）：原模板行37=row36 */
    private static final int FOOTER_ROW = 36;
    /** 数据包编号值列：J37=col9 */
    private static final int PKG_CODE_COL = 9;
    /** 订单编号值列：N37=col13 */
    private static final int ORDER_CODE_COL = 13;
    /** 页码文本行列：M39=row38,col12 */
    private static final int PAGE_TEXT_ROW = 38;
    private static final int PAGE_TEXT_COL = 12;

    /**
     * 根据上下文填充图纸模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        List<DesignProductEntity> products = ctx.getProducts() == null ? List.of() : ctx.getProducts();
        int n = products.size();
        // 计算总页数：至少1页
        int totalPages = Math.max(1, (int) Math.ceil((double) n / SLOTS_PER_PAGE));

        log.info("开始生成图纸，orderCode={}, productCount={}, totalPages={}",
                ctx.getOrderCode(), n, totalPages);

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet templateSheet = wb.getSheetAt(0);

            // 对每一页处理
            for (int page = 0; page < totalPages; page++) {
                Sheet sheet;
                if (page == 0) {
                    sheet = templateSheet;
                } else {
                    // 复制模板 Sheet 作为新页（先清空再复制）
                    sheet = wb.cloneSheet(0);
                    wb.setSheetName(wb.getSheetIndex(sheet), "图纸-" + (page + 1));
                }

                // 计算本页产品范围
                int from = page * SLOTS_PER_PAGE;
                int to = Math.min(from + SLOTS_PER_PAGE, n);

                // 填充槽位
                for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
                    int productIdx = from + slot;
                    int[] coord = SLOT_COORDS[slot];
                    if (productIdx < to) {
                        DesignProductEntity p = products.get(productIdx);
                        setCell(sheet, coord[0], coord[1], strOrEmpty(p.getPackageFileName())); // 文件名
                        setCell(sheet, coord[2], coord[3], strOrEmpty(p.getProductName()));     // 产品名
                    } else {
                        // 清空多余槽位
                        setCell(sheet, coord[0], coord[1], "");
                        setCell(sheet, coord[2], coord[3], "");
                    }
                }

                // 填充 footer
                setCell(sheet, FOOTER_ROW, PKG_CODE_COL, strOrEmpty(ctx.getPackageCode()));
                setCell(sheet, FOOTER_ROW, ORDER_CODE_COL, strOrEmpty(ctx.getOrderCode()));
                setCell(sheet, PAGE_TEXT_ROW, PAGE_TEXT_COL,
                        "第" + (page + 1) + "页/共" + totalPages + "页");
            }

            // 写出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("图纸生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value != null ? value : "");
    }

    private String strOrEmpty(String s) {
        return s != null ? s : "";
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=DrawingExcelBuilderTest -q 2>&1 | tail -10
```
预期：Tests run: 2, Failures: 0, Errors: 0。

- [ ] **Step 5: Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/helper/DrawingExcelBuilder.java \
        yigongbao-module-design/src/test/java/com/yigongbao/module/design/helper/DrawingExcelBuilderTest.java
git commit -m "feat(design): implement DrawingExcelBuilder with multi-page support"
```

---

## Task 4: DesignDocService — 接口与持久化层扩展

**Files:**
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignDocService.java`
- Modify: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignInstructionService.java`
- Modify: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/DesignDrawingService.java`
- Modify: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignInstructionServiceImpl.java`
- Modify: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDrawingServiceImpl.java`

- [ ] **Step 1: 在 DesignInstructionService 中新增方法**

```java
// 在 DesignInstructionService extends IService<DesignInstructionEntity> 接口中新增：

/**
 * 查询数据包的最大版本序号（无记录时返回 0）
 */
int getMaxVersionSeq(Long packageId);

/**
 * 查询数据包的版本历史列表（按 version_seq 倒序）
 */
List<DesignInstructionEntity> listVersions(Long packageId);
```

- [ ] **Step 2: 在 DesignInstructionServiceImpl 中实现方法**

```java
@Override
public int getMaxVersionSeq(Long packageId) {
    return lambdaQuery()
            .eq(DesignInstructionEntity::getPackageId, packageId)
            .orderByDesc(DesignInstructionEntity::getVersionSeq)
            .last("LIMIT 1")
            .oneOpt()
            .map(DesignInstructionEntity::getVersionSeq)
            .orElse(0);
}

@Override
public List<DesignInstructionEntity> listVersions(Long packageId) {
    return lambdaQuery()
            .eq(DesignInstructionEntity::getPackageId, packageId)
            .orderByDesc(DesignInstructionEntity::getVersionSeq)
            .list();
}
```

- [ ] **Step 3: 在 DesignDrawingService 中新增方法**

```java
/**
 * 查询数据包的最大版本序号（无记录时返回 0）
 */
int getMaxVersionSeq(Long packageId);

/**
 * 查询数据包的版本历史列表（按 version_seq 倒序）
 */
List<DesignDrawingEntity> listVersions(Long packageId);
```

- [ ] **Step 4: 在 DesignDrawingServiceImpl 中实现方法**

```java
@Override
public int getMaxVersionSeq(Long packageId) {
    return lambdaQuery()
            .eq(DesignDrawingEntity::getPackageId, packageId)
            .orderByDesc(DesignDrawingEntity::getVersionSeq)
            .last("LIMIT 1")
            .oneOpt()
            .map(DesignDrawingEntity::getVersionSeq)
            .orElse(0);
}

@Override
public List<DesignDrawingEntity> listVersions(Long packageId) {
    return lambdaQuery()
            .eq(DesignDrawingEntity::getPackageId, packageId)
            .orderByDesc(DesignDrawingEntity::getVersionSeq)
            .list();
}
```

- [ ] **Step 5: 创建 DesignDocService.java 接口**

```java
package com.yigongbao.module.design.service;

import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 指令单/图纸生成与管理服务接口
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface DesignDocService {

    /**
     * 同时生成指令单和图纸（填充 Excel 模板 → 上传 → 保存记录）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果（两个文档的 id、version、url）
     */
    GenerateDocsResultVO generateDocs(Long orderId, Long packageId);

    /**
     * 查询指令单版本历史列表
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 版本列表（按 versionSeq 倒序）
     */
    List<DesignDocVersionVO> listInstructionVersions(Long orderId, Long packageId);

    /**
     * 查询图纸版本历史列表
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 版本列表（按 versionSeq 倒序）
     */
    List<DesignDocVersionVO> listDrawingVersions(Long orderId, Long packageId);

    /**
     * 下载指定版本的指令单（模板版）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID
     * @param response  HTTP 响应
     */
    void downloadInstruction(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException;

    /**
     * 下载指定版本的图纸（模板版）
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID
     * @param response  HTTP 响应
     */
    void downloadDrawing(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException;

    /**
     * 上传修订版指令单
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        指令单记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file);

    /**
     * 上传修订版图纸
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @param id        图纸记录ID（要更新的版本）
     * @param file      修订版文件
     */
    void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file);
}
```

- [ ] **Step 6: 编译验证**

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-design -am -q 2>&1 | tail -5
```
预期：BUILD SUCCESS。

- [ ] **Step 7: Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/
git commit -m "feat(design): add DesignDocService interface and extend Instruction/Drawing services"
```

---

## Task 5: DesignDocServiceImpl — 核心业务实现

**Files:**
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java`

**关键依赖说明：**
- `FileService.uploadFile(MultipartFile, bizType)` — 不支持 byte[]，需要用 `MockMultipartFile`（来自 `spring-test`，但在 main 代码中不应引入 test 依赖）。改用 x-file-storage 原生 API `FileStorageService.of(byte[])` 直接上传，绕过 FileService 封装。
- 指令单 bizType = `FileBizTypeEnum.INSTRUCTION_FILE.getDictCode()` = `"10.8"`
- 图纸 bizType = `FileBizTypeEnum.DRAWING_FILE.getDictCode()` = `"10.7"`
- 指令单编号规则：`CodeRuleConstants.INSTRUCTION_NO`（已有）

- [ ] **Step 1: 先写单元测试 DesignDocServiceImplTest.java（关键场景）**

```java
package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import com.yigongbao.module.order.service.OrderMainService;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignDocServiceImpl 单元测试")
class DesignDocServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private DesignPackageService packageService;
    @Mock private DesignProductService productService;
    @Mock private DesignInstructionService instructionService;
    @Mock private DesignDrawingService drawingService;
    @Mock private InstructionExcelBuilder instructionBuilder;
    @Mock private DrawingExcelBuilder drawingBuilder;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private FileStorageService fileStorageService;

    @Spy
    @InjectMocks
    private DesignDocServiceImpl docService;

    private static final Long ORDER_ID = 1L;
    private static final Long PACKAGE_ID = 10L;
    private static final Long USER_ID = 100L;

    private OrderMainEntity order;
    private DesignPackageEntity pkg;

    @BeforeEach
    void setUp() throws Exception {
        order = new OrderMainEntity();
        order.setId(ORDER_ID);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(USER_ID);
        order.setOrderCode("ORD-001");

        pkg = new DesignPackageEntity();
        pkg.setId(PACKAGE_ID);
        pkg.setOrderId(ORDER_ID);
        pkg.setPackageCode("PKG-001");
    }

    @Nested
    @DisplayName("generateDocs")
    class GenerateDocs {

        @Test
        @DisplayName("成功生成指令单和图纸")
        void generateDocs_success() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getMaxVersionSeq(PACKAGE_ID)).thenReturn(0);
                when(drawingService.getMaxVersionSeq(PACKAGE_ID)).thenReturn(0);
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0001");
                when(productService.list(any())).thenReturn(List.of(new DesignProductEntity()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});

                // uploadBytes 是 protected 方法，用 spy + doReturn 绕过 FileStorageService 链式调用
                FileInfo mockFileInfo = new FileInfo();
                mockFileInfo.setId("file-001");
                mockFileInfo.setUrl("http://storage/test.xlsx");
                doReturn(mockFileInfo).when(docService).uploadBytes(any(), any(), any(), any());

                when(instructionService.save(any())).thenReturn(true);
                when(drawingService.save(any())).thenReturn(true);

                GenerateDocsResultVO result = docService.generateDocs(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertNotNull(result.getInstruction());
                assertEquals("A/1", result.getInstruction().getVersion());
            }
        }

        @Test
        @DisplayName("打印信息未填写时抛出 PRINT_INFO_REQUIRED")
        void generateDocs_noPrintInfo_throwsException() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(0L);  // 无打印信息

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateDocs(ORDER_ID, PACKAGE_ID));
                assertEquals(750, ex.getCode());
            }
        }

        @Test
        @DisplayName("非设计师时抛出 DESIGN_OPERATOR_NOT_ALLOWED")
        void generateDocs_notDesigner_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L); // 非设计师

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateDocs(ORDER_ID, PACKAGE_ID));
                assertEquals(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED.getCode(), ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("uploadRevisedInstruction")
    class UploadRevisedInstruction {

        @Test
        @DisplayName("版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void uploadRevised_notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(instructionService.getById(999L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 999L,
                                mock(org.springframework.web.multipart.MultipartFile.class)));
                assertEquals(751, ex.getCode());
            }
        }
    }
}
```

**重要**：`FileStorageService` 链式调用难以 mock，测试中使用 `@Spy @InjectMocks` + `doReturn` 绕过 `uploadBytes` 方法。测试类字段声明改为：

```java
@Spy
@InjectMocks
private DesignDocServiceImpl docService;
```

在 `generateDocs_success` 中用以下方式 stub 上传：

```java
FileInfo mockFileInfo = new FileInfo();
mockFileInfo.setId("file-001");
mockFileInfo.setUrl("http://storage/test.xlsx");
doReturn(mockFileInfo).when(docService).uploadBytes(any(), any(), any(), any());
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=DesignDocServiceImplTest -q 2>&1 | tail -10
```
预期：FAIL，类不存在。

- [ ] **Step 3: 创建 DesignDocServiceImpl.java**

```java
package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import com.yigongbao.module.order.service.OrderMainService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 指令单/图纸生成与管理服务实现类
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignDocServiceImpl implements DesignDocService {

    private final OrderMainService orderMainService;
    private final DesignPackageService packageService;
    private final DesignProductService productService;
    private final DesignInstructionService instructionService;
    private final DesignDrawingService drawingService;
    private final InstructionExcelBuilder instructionBuilder;
    private final DrawingExcelBuilder drawingBuilder;
    private final CodeGeneratorService codeGeneratorService;
    private final FileStorageService fileStorageService;

    /**
     * 同时生成指令单和图纸
     *
     * @param orderId   订单ID
     * @param packageId 数据包ID
     * @return 生成结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateDocsResultVO generateDocs(Long orderId, Long packageId) {
        log.info("开始生成指令单和图纸，orderId={}, packageId={}", orderId, packageId);

        // 1. 权限校验
        OrderMainEntity order = checkOrderAndPermission(orderId);
        DesignPackageEntity pkg = validatePackage(orderId, packageId);

        // 2. 前置校验：打印信息已填写
        long productCount = productService.count(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount == 0) {
            throw new BusinessException(ErrorCodeEnum.PRINT_INFO_REQUIRED);
        }

        // 3. 计算新版本号
        int newSeq = Math.max(
                instructionService.getMaxVersionSeq(packageId),
                drawingService.getMaxVersionSeq(packageId)) + 1;
        String version = "A/" + newSeq;
        log.info("新版本号：{}，packageId={}", version, packageId);

        // 4. 查询打印产品列表
        List<DesignProductEntity> products = productService.list(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId)
                        .orderByAsc(DesignProductEntity::getSortOrder));

        // 5. 生成指令单 Excel
        String instructionCode = codeGeneratorService.generate(CodeRuleConstants.INSTRUCTION_NO);
        InstructionExcelBuilder.BuildContext instrCtx = buildInstructionContext(order, pkg, products, version);
        byte[] instrBytes;
        try {
            instrBytes = instructionBuilder.build(instrCtx);
        } catch (IOException e) {
            log.error("生成指令单 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 6. 生成图纸 Excel
        DrawingExcelBuilder.BuildContext drawCtx = buildDrawingContext(order, pkg, products);
        byte[] drawBytes;
        try {
            drawBytes = drawingBuilder.build(drawCtx);
        } catch (IOException e) {
            log.error("生成图纸 Excel 失败，packageId={}", packageId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR);
        }

        // 7. 上传文件
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo instrFile = uploadBytes(instrBytes,
                FileBizTypeEnum.INSTRUCTION_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.INSTRUCTION_FILE.getDictCode(),
                instructionCode + ".xlsx");
        FileInfo drawFile = uploadBytes(drawBytes,
                FileBizTypeEnum.DRAWING_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.DRAWING_FILE.getDictCode(),
                pkg.getPackageCode() + "-图纸-" + version + ".xlsx");

        // 8. 插入指令单记录
        DesignInstructionEntity instrEntity = new DesignInstructionEntity();
        instrEntity.setOrderId(orderId);
        instrEntity.setPackageId(packageId);
        instrEntity.setInstructionCode(instructionCode);
        instrEntity.setVersion(version);
        instrEntity.setVersionSeq(newSeq);
        instrEntity.setTemplateFileId(instrFile.getId());
        instrEntity.setTemplateFileUrl(instrFile.getUrl());
        instrEntity.setGenerateTime(LocalDateTime.now());
        instructionService.save(instrEntity);

        // 9. 插入图纸记录
        DesignDrawingEntity drawEntity = new DesignDrawingEntity();
        drawEntity.setOrderId(orderId);
        drawEntity.setPackageId(packageId);
        drawEntity.setVersion(version);
        drawEntity.setVersionSeq(newSeq);
        drawEntity.setTemplateFileId(drawFile.getId());
        drawEntity.setTemplateFileUrl(drawFile.getUrl());
        drawEntity.setGenerateTime(LocalDateTime.now());
        drawingService.save(drawEntity);

        // 10. 构造返回值
        GenerateDocsResultVO result = new GenerateDocsResultVO();
        DocItemVO instrVO = new DocItemVO();
        instrVO.setId(instrEntity.getId());
        instrVO.setVersion(version);
        instrVO.setTemplateFileUrl(instrFile.getUrl());
        instrVO.setGenerateTime(instrEntity.getGenerateTime());
        result.setInstruction(instrVO);

        DocItemVO drawVO = new DocItemVO();
        drawVO.setId(drawEntity.getId());
        drawVO.setVersion(version);
        drawVO.setTemplateFileUrl(drawFile.getUrl());
        drawVO.setGenerateTime(drawEntity.getGenerateTime());
        result.setDrawing(drawVO);

        log.info("生成指令单和图纸完成，orderId={}, packageId={}, version={}", orderId, packageId, version);
        return result;
    }

    /**
     * 查询指令单版本历史列表
     */
    @Override
    public List<DesignDocVersionVO> listInstructionVersions(Long orderId, Long packageId) {
        log.info("查询指令单版本列表，orderId={}, packageId={}", orderId, packageId);
        validatePackage(orderId, packageId);
        return instructionService.listVersions(packageId).stream()
                .map(this::toInstructionVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询图纸版本历史列表
     */
    @Override
    public List<DesignDocVersionVO> listDrawingVersions(Long orderId, Long packageId) {
        log.info("查询图纸版本列表，orderId={}, packageId={}", orderId, packageId);
        validatePackage(orderId, packageId);
        return drawingService.listVersions(packageId).stream()
                .map(this::toDrawingVersionVO)
                .collect(Collectors.toList());
    }

    /**
     * 下载指定版本的指令单（模板版）
     */
    @Override
    public void downloadInstruction(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException {
        log.info("下载指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        validatePackage(orderId, packageId);
        DesignInstructionEntity entity = instructionService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        downloadFile(entity.getTemplateFileUrl(), entity.getInstructionCode() + ".xlsx", response);
    }

    /**
     * 下载指定版本的图纸（模板版）
     */
    @Override
    public void downloadDrawing(Long orderId, Long packageId, Long id, HttpServletResponse response) throws IOException {
        log.info("下载图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        validatePackage(orderId, packageId);
        DesignDrawingEntity entity = drawingService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        String filename = "图纸-" + entity.getVersion() + ".xlsx";
        downloadFile(entity.getTemplateFileUrl(), filename, response);
    }

    /**
     * 上传修订版指令单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedInstruction(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版指令单，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignInstructionEntity entity = instructionService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo fileInfo = uploadMultipartFile(file,
                FileBizTypeEnum.INSTRUCTION_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());
        entity.setRevisedFileId(fileInfo.getId());
        entity.setRevisedFileUrl(fileInfo.getUrl());
        entity.setRevisedUploadTime(LocalDateTime.now());
        instructionService.updateById(entity);
        log.info("上传修订版指令单成功，id={}", id);
    }

    /**
     * 上传修订版图纸
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadRevisedDrawing(Long orderId, Long packageId, Long id, MultipartFile file) {
        log.info("上传修订版图纸，orderId={}, packageId={}, id={}", orderId, packageId, id);
        checkOrderAndPermission(orderId);
        validatePackage(orderId, packageId);
        DesignDrawingEntity entity = drawingService.getById(id);
        if (entity == null || !entity.getPackageId().equals(packageId)) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        FileInfo fileInfo = uploadMultipartFile(file,
                FileBizTypeEnum.DRAWING_FILE.getCode() + "/" + datePath + "/",
                FileBizTypeEnum.DRAWING_FILE.getDictCode());
        entity.setRevisedFileId(fileInfo.getId());
        entity.setRevisedFileUrl(fileInfo.getUrl());
        entity.setRevisedUploadTime(LocalDateTime.now());
        drawingService.updateById(entity);
        log.info("上传修订版图纸成功，id={}", id);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验订单状态和操作权限
     */
    private OrderMainEntity checkOrderAndPermission(Long orderId) {
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        FlowStatusEnum status = FlowStatusEnum.getByValue(order.getStatus());
        if (status == null || !status.belongsTo(FlowPhaseEnum.DESIGN)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        if (status != FlowStatusEnum.DESIGN_IN_PROGRESS
                && status != FlowStatusEnum.DESIGN_REVIEW_REJECTED) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED);
        }
        return order;
    }

    /**
     * 校验 packageId 属于 orderId
     */
    private DesignPackageEntity validatePackage(Long orderId, Long packageId) {
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        return pkg;
    }

    /**
     * 构建指令单填充上下文
     */
    private InstructionExcelBuilder.BuildContext buildInstructionContext(
            OrderMainEntity order, DesignPackageEntity pkg,
            List<DesignProductEntity> products, String version) {
        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPatientName(order.getPatientName());
        ctx.setHospitalName(order.getHospitalName());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setVersion(version);
        ctx.setProducts(products);
        // 联系人、预交货时间等从 order 中取
        if (order.getExpectedDeliveryDate() != null) {
            ctx.setExpectedDeliveryDate(order.getExpectedDeliveryDate().toString());
        }
        ctx.setPostalAddress(order.getPostalAddress());
        return ctx;
    }

    /**
     * 构建图纸填充上下文
     */
    private DrawingExcelBuilder.BuildContext buildDrawingContext(
            OrderMainEntity order, DesignPackageEntity pkg,
            List<DesignProductEntity> products) {
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode(order.getOrderCode());
        ctx.setPackageCode(pkg.getPackageCode());
        ctx.setProducts(products);
        return ctx;
    }

    /**
     * 将 byte[] 上传到 x-file-storage
     */
    protected FileInfo uploadBytes(byte[] bytes, String path, String objectType, String filename) {
        try {
            return fileStorageService.of(bytes)
                    .setPath(path)
                    .setObjectType(objectType)
                    .setOriginalFilename(filename)
                    .upload();
        } catch (Exception e) {
            log.error("上传文件失败，filename={}", filename, e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    /**
     * 将 MultipartFile 上传到 x-file-storage
     */
    protected FileInfo uploadMultipartFile(MultipartFile file, String path, String objectType) {
        try {
            return fileStorageService.of(file)
                    .setPath(path)
                    .setObjectType(objectType)
                    .upload();
        } catch (Exception e) {
            log.error("上传修订版文件失败", e);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_UPLOAD_FAILED);
        }
    }

    /**
     * 通过 URL 直接流式下载文件到响应
     */
    private void downloadFile(String fileUrl, String filename, HttpServletResponse response) throws IOException {
        if (fileUrl == null) {
            throw new BusinessException(ErrorCodeEnum.DOC_VERSION_NOT_FOUND);
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + java.net.URLEncoder.encode(filename, "UTF-8") + "\"");
        try (InputStream is = new URL(fileUrl).openStream()) {
            is.transferTo(response.getOutputStream());
        }
    }

    private DesignDocVersionVO toInstructionVersionVO(DesignInstructionEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        return vo;
    }

    private DesignDocVersionVO toDrawingVersionVO(DesignDrawingEntity entity) {
        DesignDocVersionVO vo = new DesignDocVersionVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setVersionSeq(entity.getVersionSeq());
        vo.setTemplateFileUrl(entity.getTemplateFileUrl());
        vo.setRevisedFileUrl(entity.getRevisedFileUrl());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setRevisedUploadTime(entity.getRevisedUploadTime());
        return vo;
    }
}
```

- [ ] **Step 4: 运行测试**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design -Dtest=DesignDocServiceImplTest -q 2>&1 | tail -20
```
预期：Tests run: ≥3，Failures: 0，Errors: 0。若 `FileStorageService` mock 因链式调用难以实现，将 `uploadBytes` 从 test 中用 `spy + doReturn` 绕过，参考 Task 5 说明。

- [ ] **Step 5: Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/service/impl/DesignDocServiceImpl.java \
        yigongbao-module-design/src/test/java/com/yigongbao/module/design/service/impl/DesignDocServiceImplTest.java
git commit -m "feat(design): implement DesignDocServiceImpl with generate/version/upload logic"
```

---

## Task 6: DesignDocController — REST 接口

**Files:**
- Create: `yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignDocController.java`

- [ ] **Step 1: 创建 DesignDocController.java**

```java
package com.yigongbao.module.design.controller;

import com.yigongbao.common.result.Result;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 指令单/图纸生成与管理 Controller
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Tag(name = "指令单/图纸管理", description = "设计阶段指令单和图纸的生成、版本查询、下载、修订版上传")
@RestController
@RequestMapping("/design/workorder")
@RequiredArgsConstructor
public class DesignDocController {

    private final DesignDocService docService;

    /**
     * 同时生成指令单和图纸
     */
    @Operation(summary = "生成指令单和图纸")
    @PostMapping("/{orderId}/package/{packageId}/generate-docs")
    public Result<GenerateDocsResultVO> generateDocs(@PathVariable Long orderId,
                                                      @PathVariable Long packageId) {
        return Result.success(docService.generateDocs(orderId, packageId));
    }

    /**
     * 查询指令单历史版本列表
     */
    @Operation(summary = "查询指令单版本列表")
    @GetMapping("/{orderId}/package/{packageId}/instruction/versions")
    public Result<List<DesignDocVersionVO>> listInstructionVersions(@PathVariable Long orderId,
                                                                     @PathVariable Long packageId) {
        return Result.success(docService.listInstructionVersions(orderId, packageId));
    }

    /**
     * 查询图纸历史版本列表
     */
    @Operation(summary = "查询图纸版本列表")
    @GetMapping("/{orderId}/package/{packageId}/drawing/versions")
    public Result<List<DesignDocVersionVO>> listDrawingVersions(@PathVariable Long orderId,
                                                                 @PathVariable Long packageId) {
        return Result.success(docService.listDrawingVersions(orderId, packageId));
    }

    /**
     * 下载指定版本的指令单（模板版）
     */
    @Operation(summary = "下载指令单（模板版）")
    @GetMapping("/{orderId}/package/{packageId}/instruction/download/{id}")
    public void downloadInstruction(@PathVariable Long orderId,
                                    @PathVariable Long packageId,
                                    @PathVariable Long id,
                                    HttpServletResponse response) throws IOException {
        docService.downloadInstruction(orderId, packageId, id, response);
    }

    /**
     * 下载指定版本的图纸（模板版）
     */
    @Operation(summary = "下载图纸（模板版）")
    @GetMapping("/{orderId}/package/{packageId}/drawing/download/{id}")
    public void downloadDrawing(@PathVariable Long orderId,
                                @PathVariable Long packageId,
                                @PathVariable Long id,
                                HttpServletResponse response) throws IOException {
        docService.downloadDrawing(orderId, packageId, id, response);
    }

    /**
     * 上传修订版指令单
     */
    @Operation(summary = "上传修订版指令单")
    @PostMapping("/{orderId}/package/{packageId}/instruction/upload-revised/{id}")
    public Result<Void> uploadRevisedInstruction(@PathVariable Long orderId,
                                                  @PathVariable Long packageId,
                                                  @PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        docService.uploadRevisedInstruction(orderId, packageId, id, file);
        return Result.success();
    }

    /**
     * 上传修订版图纸
     */
    @Operation(summary = "上传修订版图纸")
    @PostMapping("/{orderId}/package/{packageId}/drawing/upload-revised/{id}")
    public Result<Void> uploadRevisedDrawing(@PathVariable Long orderId,
                                              @PathVariable Long packageId,
                                              @PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        docService.uploadRevisedDrawing(orderId, packageId, id, file);
        return Result.success();
    }
}
```

- [ ] **Step 2: 编译并全量测试**

> **注意**：upload-revised 接口路径含 `/{id}`（指定要更新的版本记录），这是对原始设计文档的主动改进（原文档无版本ID），Task 8 的接口文档需以此版本为准。

```bash
cd yigongbao-parent && mvn compile -pl yigongbao-module-design -am -q 2>&1 | tail -5
cd yigongbao-parent && mvn test -pl yigongbao-module-design -q 2>&1 | tail -15
```
预期：编译 BUILD SUCCESS；所有测试 GREEN。

- [ ] **Step 3: Commit**

```bash
git add yigongbao-module-design/src/main/java/com/yigongbao/module/design/controller/DesignDocController.java
git commit -m "feat(design): add DesignDocController for instruction/drawing REST endpoints"
```

---

## Task 7: OrderMainEntity 字段校验 + 完整测试覆盖

**目的**：Service 实现中引用了 `order.getPatientName()`、`order.getHospitalName()`、`order.getPostalAddress()`、`order.getExpectedDeliveryDate()` 等字段。需确认 `OrderMainEntity` 中这些字段存在，如不存在则改为从关联表获取或留空。

- [ ] **Step 1: 确认 OrderMainEntity 字段**

```bash
grep -n "patientName\|hospitalName\|postalAddress\|expectedDeliveryDate" \
  yigongbao-common/src/main/java/com/yigongbao/common/entity/OrderMainEntity.java
```

如字段不存在，在 `DesignDocServiceImpl.buildInstructionContext` 中将缺失字段设为空字符串即可（指令单中该信息可线下补填）。

- [ ] **Step 2: 运行全量测试（design 模块）**

```bash
cd yigongbao-parent && mvn test -pl yigongbao-module-design 2>&1 | tail -20
```
预期：所有测试通过，无 FAIL / ERROR。

- [ ] **Step 3: 若测试失败则修复后重新运行**

- [ ] **Step 4: Commit**

```bash
git add -A -- yigongbao-module-design/
git commit -m "test(design): ensure full test coverage for doc generation module"
```

---

## Task 8: 接口文档更新

**Files:**
- Modify: `.docs/接口文档/20_设计模块接口文档.md`
- Modify: `.docs/技术实现/design/05_指令单图纸生成与管理.md`（更新完成状态勾选）

- [ ] **Step 1: 在接口文档 20_设计模块接口文档.md 中追加新接口章节**

在文档末尾（"附录"之前）添加"指令单/图纸接口"章节，包含以下接口的完整文档：
- 20.18 生成指令单和图纸
- 20.19 查询指令单版本列表
- 20.20 查询图纸版本列表
- 20.21 下载指令单（模板版）
- 20.22 下载图纸（模板版）
- 20.23 上传修订版指令单
- 20.24 上传修订版图纸

每个接口包含：接口地址、方法、说明、路径参数、响应参数、响应示例、错误码。

- [ ] **Step 2: 更新 05 文档的完成状态**

将 `05_指令单图纸生成与管理.md` 中"八、完成状态"下所有待完成项全部勾选 `[x]`。

- [ ] **Step 3: 最终全量编译 + 测试**

```bash
cd yigongbao-parent && mvn clean package -DskipTests -q 2>&1 | tail -5
cd yigongbao-parent && mvn test -pl yigongbao-module-design 2>&1 | tail -20
```
预期：BUILD SUCCESS，所有测试通过。

- [ ] **Step 4: Final Commit**

```bash
git add .docs/接口文档/20_设计模块接口文档.md \
        .docs/技术实现/design/05_指令单图纸生成与管理.md
git commit -m "docs(design): update interface docs and mark task 05 as completed"
```
