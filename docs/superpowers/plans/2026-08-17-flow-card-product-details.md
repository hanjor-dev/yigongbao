# 流转卡产品明细导出修正 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 流转卡 Excel 的产品编号继承模板 A 列居中格式，且“描述”列显示无后缀的打印文件名。

**Architecture:** 服务层将 `ProductionProductEntity.fileName` 映射到构建器上下文；构建器负责去除文件名的路径和最后一个扩展名并写入描述列。产品编号仅调用既有写值方法，动态行的格式继续由模板行样式复制逻辑提供。

**Tech Stack:** Java 17、Spring Boot、Apache POI、JUnit 5、Maven。

---

## File structure

- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java` — 将产品实体的打印文件名传入流转卡构建上下文。
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/FlowCardExcelBuilder.java` — 使用打印文件名生成“描述”，移除 A 列的显式样式覆盖。
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java` — 验证多行产品编号的模板样式继承与描述文本。
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java` — 验证实体的打印文件名传入构建器。

### Task 1: 保护构建器输出行为

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java`

- [ ] **Step 1: 写入失败测试，覆盖多产品行和描述文件名**

先从类路径模板读取 A17（零基 `16, 0`）的样式索引；再构建两条产品：第一条 `fileName` 为 `upper.stl`，第二条为 `folder\\lower.part.stl`。读取生成工作簿并断言：

```java
assertEquals("upper", readCell(excelBytes, 16, 5));
assertEquals("lower.part", readCell(excelBytes, 17, 5));
assertEquals(templateProductNoStyleIndex,
    workbook.getSheetAt(0).getRow(16).getCell(0).getCellStyle().getIndex());
assertEquals(templateProductNoStyleIndex,
    workbook.getSheetAt(0).getRow(17).getCell(0).getCellStyle().getIndex());
```

另加参数化或独立断言，覆盖 `fileName` 为 `"  "`、`"folder/"` 时描述为 `"-"`，以及 `"README"`、`".env"` 时描述保持原值。

- [ ] **Step 2: 运行单测并确认其因 `ProductInfo` 缺少 `fileName` 失败**

Run: `mvn -pl yigongbao-module-production -Dtest=FlowCardExcelBuilderTest#buildProductsUsesFileNameWithoutExtensionAndRetainsTemplateCentering test`

Expected: 编译失败，提示 `setFileName` 不存在。

### Task 2: 最小化实现打印文件名和模板样式继承

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/FlowCardExcelBuilder.java`
- Test: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/FlowCardExcelBuilderTest.java`

- [ ] **Step 1: 在 `ProductInfo` 中增加 `fileName`**

新增 `private String fileName;`，由 Lombok 生成访问器。

- [ ] **Step 2: 改写产品“描述”列赋值**

用局部 `formatFileName(String fileName)` 取最后一个 `/` 或 `\\` 之后的片段，再去除最后一个有效扩展名；空白值返回 `-`。在第 5 列写入该结果，删除材质/颜色拼接。

- [ ] **Step 3: 移除 A 列的显式居中覆盖**

保留 `setCellValue(sheet, rowIndex, 0, product.getProductNo());`；删除 `centerCellHorizontally` 调用及其仅为该调用存在的方法。动态行继续由 `copyRowStyle(templateRow, row)` 复制模板全部六列样式；不得创建新的 A 列 `CellStyle`。

- [ ] **Step 4: 运行新增单测并确认通过**

Run: `mvn -pl yigongbao-module-production -Dtest=FlowCardExcelBuilderTest#buildProductsUsesFileNameWithoutExtensionAndRetainsTemplateCentering test`

Expected: `BUILD SUCCESS`。

### Task 3: 将实体字段接入构建上下文

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImpl.java`
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/record/service/impl/ProductionRecordServiceImplTest.java`

- [ ] **Step 1: 写入服务层映射失败测试**

在 `ProductionRecordServiceImplTest` 中准备一条带 `fileName` 的 `ProductionProductEntity`，mock 记录、产品、工序查询及后续上传依赖。使用 `ArgumentCaptor<FlowCardExcelBuilder.BuildContext>` 捕获 `flowCardExcelBuilder.build(...)` 入参，并断言：

```java
assertEquals("print-file.stl",
    contextCaptor.getValue().getProducts().get(0).getFileName());
```

- [ ] **Step 2: 运行服务映射测试并确认先失败**

Run: `mvn -pl yigongbao-module-production -Dtest=ProductionRecordServiceImplTest#generateFlowCardExcelPassesProductFileNameToBuilder test`

Expected: 断言失败，捕获的 `fileName` 为 `null`。

- [ ] **Step 3: 在产品映射中传递 `fileName`**

在 `generateFlowCardExcel` 的 `products.stream().map(...)` 中增加：

```java
info.setFileName(p.getFileName());
```

- [ ] **Step 4: 运行服务映射测试并确认通过**

Run: `mvn -pl yigongbao-module-production -Dtest=ProductionRecordServiceImplTest#generateFlowCardExcelPassesProductFileNameToBuilder test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 运行构建器测试集**

Run: `mvn -pl yigongbao-module-production -Dtest=FlowCardExcelBuilderTest test`

Expected: `BUILD SUCCESS`，所有流转卡构建器测试通过。

### Task 4: 最终验证与提交

**Files:**
- Modify: `FlowCardExcelBuilder.java`、`ProductionRecordServiceImpl.java`、`FlowCardExcelBuilderTest.java`、`ProductionRecordServiceImplTest.java`。

- [ ] **Step 1: 运行生产模块测试**

Run: `mvn -pl yigongbao-module-production test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 检查范围**

Run: `git status --short` 和 `git diff --check`。

Expected: 不包含用户已修改的 `流转卡模板.xlsx` 或其他无关文件。

- [ ] **Step 3: 创建单一提交**

仅暂存以下四个源代码/测试文件，提交信息：

- `FlowCardExcelBuilder.java`
- `ProductionRecordServiceImpl.java`
- `FlowCardExcelBuilderTest.java`
- `ProductionRecordServiceImplTest.java`

```text
fix: 修正流转卡产品明细导出
```
