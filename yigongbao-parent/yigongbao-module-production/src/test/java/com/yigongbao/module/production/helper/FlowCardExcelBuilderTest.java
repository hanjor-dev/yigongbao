package com.yigongbao.module.production.helper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlowCardExcelBuilderTest {

    private final FlowCardExcelBuilder builder = new FlowCardExcelBuilder();

    @Test
    void testBuild() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setRecordNo("LC202605290001");
        context.setVersionNo("A/0");
        context.setDesignPackageCode("PKG001");
        context.setTotalProductCount(2);
        context.setProductionBatchNo("BATCH001");
        context.setMaterial("树脂");
        context.setMaterialBatchNo("MAT001");
        context.setPrintStartTime(LocalDateTime.now());
        context.setPrintFinishTime(LocalDateTime.now().plusHours(2));
        context.setDesignerAssetNo("PC001");

        List<FlowCardExcelBuilder.ProcessInfo> processes = new ArrayList<>();
        FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
        process.setProcessType("print");
        process.setDeviceNo("PRINTER001");
        process.setProcessParams("{\"layerThickness\":0.05,\"exposureTime\":8}");
        processes.add(process);
        context.setProcesses(processes);

        List<FlowCardExcelBuilder.ProductInfo> products = new ArrayList<>();
        FlowCardExcelBuilder.ProductInfo product = new FlowCardExcelBuilder.ProductInfo();
        product.setProductNo("PROD001");
        product.setProductName("测试产品");
        product.setSpecName("标准型");
        product.setMaterialName("树脂");
        product.setColorName("白色");
        products.add(product);
        context.setProducts(products);

        byte[] result = builder.build(context);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void buildHeaderUsesPrintStartDateAsProductionBatchNo() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setProductionBatchNo("OLD-BATCH");
        context.setPrintStartTime(LocalDateTime.of(2026, 8, 13, 14, 15, 24));

        assertEquals("20260813", readCell(builder.build(context), 3, 2));
    }

    @Test
    void buildHeaderFallsBackToStoredBatchNoWithoutPrintStartTime() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setProductionBatchNo("STORED-BATCH");

        assertEquals("STORED-BATCH", readCell(builder.build(context), 3, 2));
    }

    @Test
    void buildHeaderShowsDashWhenPrintStartAndStoredBatchAreBlank() throws Exception {
        FlowCardExcelBuilder.BuildContext nullBatchContext = new FlowCardExcelBuilder.BuildContext();
        assertEquals("-", readCell(builder.build(nullBatchContext), 3, 2));

        FlowCardExcelBuilder.BuildContext blankBatchContext = new FlowCardExcelBuilder.BuildContext();
        blankBatchContext.setProductionBatchNo("   ");
        assertEquals("-", readCell(builder.build(blankBatchContext), 3, 2));
    }

    @Test
    void buildHeaderShowsCompleteTimeLabelsAndKeepsSeconds() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setPrintStartTime(LocalDateTime.of(2026, 8, 13, 14, 15, 24));
        context.setPrintFinishTime(LocalDateTime.of(2026, 8, 13, 16, 15, 24));

        assertEquals("开始时间: 2026-08-13 14:15:24", readCell(builder.build(context), 4, 2));
        assertEquals("结束时间: 2026-08-13 16:15:24", readCell(builder.build(context), 5, 2));
    }

    @Test
    void buildHeaderKeepsCompleteStructureWhenTimesAreBlank() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();

        byte[] excelBytes = builder.build(context);

        assertEquals("开始时间: -", readCell(excelBytes, 4, 2));
        assertEquals("结束时间: -", readCell(excelBytes, 5, 2));
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void buildPostProcessingTimesWithoutSeconds() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        LocalDateTime startTime = LocalDateTime.of(2026, 8, 13, 15, 1, 34);
        LocalDateTime endTime = LocalDateTime.of(2026, 8, 13, 15, 11, 34);
        List<FlowCardExcelBuilder.ProcessInfo> processes = new ArrayList<>();
        for (String processType : List.of("wash", "cure", "clean_dry")) {
            FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
            process.setProcessType(processType);
            process.setStartTime(startTime);
            process.setEndTime(endTime);
            processes.add(process);
        }
        context.setProcesses(processes);

        byte[] excelBytes = builder.build(context);
        String expected = "开始：2026-08-13 15:01\n结束：2026-08-13 15:11";
        assertEquals(expected, readCell(excelBytes, 9, 4));
        assertEquals(expected, readCell(excelBytes, 10, 4));
        assertEquals(expected, readCell(excelBytes, 11, 4));
    }

    @Test
    void buildPostProcessingTimesShowsDashWhenTimesAreBlank() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        List<FlowCardExcelBuilder.ProcessInfo> processes = new ArrayList<>();
        for (String processType : List.of("wash", "cure", "clean_dry")) {
            FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
            process.setProcessType(processType);
            processes.add(process);
        }
        context.setProcesses(processes);

        byte[] excelBytes = builder.build(context);
        String expected = "开始：-\n结束：-";

        assertEquals(expected, readCell(excelBytes, 9, 4));
        assertEquals(expected, readCell(excelBytes, 10, 4));
        assertEquals(expected, readCell(excelBytes, 11, 4));
    }

    @Test
    void buildCleanDryShowsAirCompressorDeviceNo() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
        process.setProcessType("clean_dry");
        process.setDeviceNo("CLEANER-001");
        process.setSecondaryDeviceNo("AIR-001");
        context.setProcesses(List.of(process));

        assertEquals("AIR-001", readCell(builder.build(context), 12, 3));
    }

    @Test
    void buildProcessShowsDashForMissingDeviceNumbers() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
        process.setProcessType("clean_dry");
        context.setProcesses(List.of(process));

        byte[] excelBytes = builder.build(context);

        assertEquals("-", readCell(excelBytes, 11, 3));
        assertEquals("-", readCell(excelBytes, 12, 3));
    }

    @Test
    void buildPrintAndPackDoNotAppendProcessTimes() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProcessInfo print = new FlowCardExcelBuilder.ProcessInfo();
        print.setProcessType("print");
        print.setProcessParams("{\"layerThickness\":0.05}");
        print.setStartTime(LocalDateTime.of(2026, 8, 13, 15, 1, 34));
        print.setEndTime(LocalDateTime.of(2026, 8, 13, 15, 11, 34));
        FlowCardExcelBuilder.ProcessInfo pack = new FlowCardExcelBuilder.ProcessInfo();
        pack.setProcessType("pack");
        pack.setProcessParams("{\"sealTime\":30}");
        pack.setStartTime(print.getStartTime());
        pack.setEndTime(print.getEndTime());
        context.setProcesses(List.of(print, pack));

        byte[] excelBytes = builder.build(context);

        assertEquals("层厚：0.05 mm\n激光器功率：- mW", readCell(excelBytes, 8, 4));
        assertEquals("热封时间：30秒", readCell(excelBytes, 13, 4));
    }

    @Test
    void buildPackParamsForModel() throws Exception {
        FlowCardExcelBuilder.BuildContext context = buildPackContext(
            "{\"zipBagSealTime\":3,\"zipBagSealTemperature\":130}"
        );

        String params = readPackParams(builder.build(context));

        assertEquals("PE复合食品包装袋热封温度：130℃\n热封时间：3秒", params);
    }

    @Test
    void buildPackParamsForGuide() throws Exception {
        FlowCardExcelBuilder.BuildContext context = buildPackContext(
            "{\"zipBagSealTime\":31,\"sealTemperature\":123,\"zipBagSealTemperature\":1301}"
        );

        String params = readPackParams(builder.build(context));

        assertEquals(
            "纸塑袋热封温度：123℃\nPE复合食品包装袋热封温度：1301℃\n热封时间：31秒",
            params
        );
    }

    @Test
    void buildPackParamsSkipsBlankTemperatures() throws Exception {
        FlowCardExcelBuilder.BuildContext context = buildPackContext(
            "{\"sealTemperature\":null,\"zipBagSealTemperature\":\"\",\"zipBagSealTime\":3}"
        );

        String params = readPackParams(builder.build(context));

        assertEquals("热封时间：3秒", params);
    }

    @Test
    void buildPackParamsSupportsLegacySealTime() throws Exception {
        FlowCardExcelBuilder.BuildContext context = buildPackContext(
            "{\"sealTemperature\":180,\"sealTime\":30}"
        );

        String params = readPackParams(builder.build(context));

        assertEquals("纸塑袋热封温度：180℃\n热封时间：30秒", params);
    }

    @Test
    void buildPrintParamsShowsChineseLabels() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
        process.setProcessType("print");
        process.setProcessParams("{\"laserPower\":12,\"layerThickness\":12}");
        context.setProcesses(List.of(process));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(builder.build(context)))) {
            String params = workbook.getSheetAt(0).getRow(8).getCell(4).getStringCellValue();
            assertEquals("层厚：12 mm\n激光器功率：12 mW", params);
        }
    }

    @Test
    void buildProductsCentersProductNumbersOnEveryRow() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProductInfo first = new FlowCardExcelBuilder.ProductInfo();
        first.setProductNo("PROD001");
        FlowCardExcelBuilder.ProductInfo second = new FlowCardExcelBuilder.ProductInfo();
        second.setProductNo("PROD002");
        context.setProducts(List.of(first, second));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(builder.build(context)))) {
            assertEquals(HorizontalAlignment.CENTER,
                    workbook.getSheetAt(0).getRow(16).getCell(0).getCellStyle().getAlignment());
            assertEquals(HorizontalAlignment.CENTER,
                    workbook.getSheetAt(0).getRow(17).getCell(0).getCellStyle().getAlignment());
        }
    }

    @Test
    void buildProductsUsesFileNameWithoutPathOrFinalExtensionAndPreservesTemplateNumberStyle() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProductInfo first = new FlowCardExcelBuilder.ProductInfo();
        first.setProductNo("PROD001");
        first.setFileName("upper.stl");
        FlowCardExcelBuilder.ProductInfo second = new FlowCardExcelBuilder.ProductInfo();
        second.setProductNo("PROD002");
        second.setFileName("folder\\lower.part.stl");
        context.setProducts(List.of(first, second));

        short templateStyleIndex;
        try (InputStream inputStream = new ClassPathResource("template/流转卡模板.xlsx").getInputStream();
             XSSFWorkbook template = new XSSFWorkbook(inputStream)) {
            templateStyleIndex = template.getSheetAt(0).getRow(16).getCell(0).getCellStyle().getIndex();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(builder.build(context)))) {
            assertEquals("upper", workbook.getSheetAt(0).getRow(16).getCell(5).getStringCellValue());
            assertEquals("lower.part", workbook.getSheetAt(0).getRow(17).getCell(5).getStringCellValue());
            assertEquals(templateStyleIndex, workbook.getSheetAt(0).getRow(16).getCell(0).getCellStyle().getIndex());
            assertEquals(templateStyleIndex, workbook.getSheetAt(0).getRow(17).getCell(0).getCellStyle().getIndex());
        }
    }

    @Test
    void buildProductsHandlesBlankPathOnlyExtensionlessAndDotFileNames() throws Exception {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setProducts(List.of(
            productWithFileName(""),
            productWithFileName("folder/"),
            productWithFileName("README"),
            productWithFileName(".env")
        ));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(builder.build(context)))) {
            assertEquals("-", workbook.getSheetAt(0).getRow(16).getCell(5).getStringCellValue());
            assertEquals("-", workbook.getSheetAt(0).getRow(17).getCell(5).getStringCellValue());
            assertEquals("README", workbook.getSheetAt(0).getRow(18).getCell(5).getStringCellValue());
            assertEquals(".env", workbook.getSheetAt(0).getRow(19).getCell(5).getStringCellValue());
        }
    }

    private FlowCardExcelBuilder.ProductInfo productWithFileName(String fileName) {
        FlowCardExcelBuilder.ProductInfo product = new FlowCardExcelBuilder.ProductInfo();
        product.setFileName(fileName);
        return product;
    }

    private FlowCardExcelBuilder.BuildContext buildPackContext(String processParams) {
        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        FlowCardExcelBuilder.ProcessInfo process = new FlowCardExcelBuilder.ProcessInfo();
        process.setProcessType("pack");
        process.setProcessParams(processParams);
        context.setProcesses(List.of(process));
        return context;
    }

    private String readPackParams(byte[] excelBytes) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            return workbook.getSheetAt(0).getRow(13).getCell(4).getStringCellValue();
        }
    }

    private String readCell(byte[] excelBytes, int rowIndex, int columnIndex) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            return workbook.getSheetAt(0).getRow(rowIndex).getCell(columnIndex).getStringCellValue();
        }
    }
}
