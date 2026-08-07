# Production Ledger Excel Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Center all 22 production-ledger header/data cells and widen only the product-name column from 4500 to 6000 while preserving the left-aligned warning row.

**Architecture:** Keep the existing workbook structure and formatting helpers. Extend the workbook contract test first, then make two focused presentation changes in `ProductLedgerExcelBuilder`: center the shared data style and choose the column width by header index.

**Tech Stack:** Java 21, Apache POI, JUnit 5, Maven

---

### Task 1: Lock the workbook alignment and width contract

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilderTest.java`

- [ ] **Step 1: Record the implementation base commit**

Before changing either Java file, run and record the returned SHA for the final committed-range inspection:

```powershell
git rev-parse HEAD
```

- [ ] **Step 2: Write failing style assertions**

Import `HorizontalAlignment`. In `buildWritesExactTwentyTwoColumnLedgerContract`, assert every header and data cell from index `0` through `21` is centered:

```java
IntStream.range(0, EXPECTED_HEADERS.size()).forEach(index -> {
    assertEquals(HorizontalAlignment.CENTER, headerRow.getCell(index).getCellStyle().getAlignment());
    assertEquals(HorizontalAlignment.CENTER, dataRow.getCell(index).getCellStyle().getAlignment());
});
```

Add a separate test so the width defect is independently observed during RED:

```java
@Test
void buildWidensOnlyProductNameColumnByOneThird() throws Exception {
    byte[] workbookBytes = new ProductLedgerExcelBuilder().build(List.of(), 0);
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
        var sheet = workbook.getSheet("生产产品台账");
        IntStream.range(0, EXPECTED_HEADERS.size()).forEach(index ->
                assertEquals(index == 5 ? 6000 : 4500, sheet.getColumnWidth(index)));
    }
}
```

In `buildPlacesWarningAboveHeaderAndMergesAcrossTwentyTwoColumns`, explicitly preserve the warning exception:

```java
assertEquals(HorizontalAlignment.LEFT,
        sheet.getRow(0).getCell(0).getCellStyle().getAlignment());
```

- [ ] **Step 3: Run the test and verify RED**

Run from `yigongbao-parent`:

```powershell
mvn -pl yigongbao-module-production -am '-Dtest=ProductLedgerExcelBuilderTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: two failures are reported independently: `buildWritesExactTwentyTwoColumnLedgerContract` fails because data cells are `LEFT`, and `buildWidensOnlyProductNameColumnByOneThird` fails because product-name width is still `4500`. The warning alignment assertion already passes.

- [ ] **Step 4: Commit the failing test**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilderTest.java
git commit -m "test(production): lock ledger Excel alignment"
```

### Task 2: Implement the presentation changes

**Files:**
- Modify: `yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilder.java`

- [ ] **Step 1: Add named width constants**

Add constants next to the existing sheet constants:

```java
private static final int DEFAULT_COLUMN_WIDTH = 4500;
private static final int PRODUCT_NAME_COLUMN_INDEX = 5;
private static final int PRODUCT_NAME_COLUMN_WIDTH = 6000;
```

- [ ] **Step 2: Set only the product-name column to 6000**

Replace the uniform width assignment in the header loop:

```java
sheet.setColumnWidth(i,
        i == PRODUCT_NAME_COLUMN_INDEX ? PRODUCT_NAME_COLUMN_WIDTH : DEFAULT_COLUMN_WIDTH);
```

- [ ] **Step 3: Center the shared data style**

Change only the base data alignment:

```java
style.setAlignment(HorizontalAlignment.CENTER);
```

Do not change `createWarningStyle`; it must remain `LEFT`. Weight continues cloning the centered base data style.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 command. Expected: 4 tests pass, 0 failures/errors.

- [ ] **Step 5: Commit the implementation**

```powershell
git add -- yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilder.java
git commit -m "style(production): center ledger Excel content"
```

### Task 3: Regression verification

**Files:**
- Verify the two modified Java files only

- [ ] **Step 1: Run the ledger query and workbook tests together**

```powershell
mvn -pl yigongbao-module-production -am '-Dtest=ProductLedgerExcelBuilderTest,ProductionProductMapperSqlTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: 9 tests pass, 0 failures/errors.

- [ ] **Step 2: Run all production-module tests**

Build the comma-separated production test-class list and run it through the Reactor so upstream dependencies compile without running unrelated tests:

```powershell
$prodTests=(Get-ChildItem -Recurse 'yigongbao-module-production\src\test\java' -Filter '*Test.java' | ForEach-Object {$_.BaseName}) -join ','
mvn -pl yigongbao-module-production -am "-Dtest=$prodTests" '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Expected: production-module tests pass with zero failures/errors.

- [ ] **Step 3: Inspect the final diff**

Use the SHA recorded in Task 1 as `<base_sha>`. Verify the committed range and exact file scope:

```powershell
git diff --check <base_sha>..HEAD
git diff --name-only <base_sha>..HEAD
git diff <base_sha>..HEAD -- `
  yigongbao-parent/yigongbao-module-production/src/main/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilder.java `
  yigongbao-parent/yigongbao-module-production/src/test/java/com/yigongbao/module/production/helper/ProductLedgerExcelBuilderTest.java
```

The name-only output must contain exactly those two Java files. The production diff must contain only:

- exhaustive alignment/width assertions;
- three width constants;
- conditional width assignment;
- `LEFT` to `CENTER` for the base data style.
