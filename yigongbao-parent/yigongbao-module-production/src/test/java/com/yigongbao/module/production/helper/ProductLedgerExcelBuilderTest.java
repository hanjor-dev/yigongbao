package com.yigongbao.module.production.helper;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductLedgerExcelBuilderTest {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "序号", "订单流水号", "时间", "产品编号", "数据文件名称", "产品名称", "型号/规格", "材质",
            "打印时长", "总重量（g）", "处理时长", "数量", "医院", "患者", "性别", "年龄", "操作人员", "医生",
            "科室", "业务员", "出库情况", "备注");

    @Test
    void buildWritesExactTwentyTwoColumnLedgerContract() throws Exception {
        Map<String, Object> data = emptyLedgerData();
        data.put("order_code", "ORD-20260807-001");
        data.put("order_create_time", LocalDateTime.of(2026, 8, 7, 14, 35, 20));
        data.put("product_no", "P-001");
        data.put("file_name", "patient.v2.stl");
        data.put("product_name", "导板");
        data.put("spec_name", "L");
        data.put("color_name", "  白色 ");
        data.put("material_name", " 树脂  ");
        data.put("print_duration_seconds", 90061L);
        data.put("weight", new BigDecimal("12.345"));
        data.put("processing_duration_seconds", 3660L);
        data.put("hospital_name", "人民医院");
        data.put("patient_name", "张三");
        data.put("patient_gender", "12.1");
        data.put("patient_age", 35);
        data.put("producer_name", "操作员A");
        data.put("doctor_name", "李医生");
        data.put("hospital_dept_name", "骨科");
        data.put("business_operator", "业务员B");
        data.put("warehouse_out_time", LocalDateTime.of(2026, 8, 8, 9, 0));

        byte[] workbookBytes = new ProductLedgerExcelBuilder().build(List.of(data), 1);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            var sheet = workbook.getSheet("生产产品台账");
            var headerRow = sheet.getRow(0);
            var dataRow = sheet.getRow(1);

            assertHeaders(headerRow);
            assertEquals(2, sheet.getLastRowNum() + 1, "一万条以内不应添加额外标题行");
            assertEquals(CellType.NUMERIC, dataRow.getCell(0).getCellType());
            assertEquals(1d, dataRow.getCell(0).getNumericCellValue());
            assertEquals("ORD-20260807-001", dataRow.getCell(1).getStringCellValue());
            assertEquals("2026年8月7日", dataRow.getCell(2).getStringCellValue());
            assertEquals("P-001", dataRow.getCell(3).getStringCellValue());
            assertEquals("patient.v2", dataRow.getCell(4).getStringCellValue());
            assertEquals("导板", dataRow.getCell(5).getStringCellValue());
            assertEquals("L", dataRow.getCell(6).getStringCellValue());
            assertEquals("白色树脂", dataRow.getCell(7).getStringCellValue());
            assertEquals("25:01:01", dataRow.getCell(8).getStringCellValue());
            assertEquals(CellType.NUMERIC, dataRow.getCell(9).getCellType());
            assertEquals(12.345d, dataRow.getCell(9).getNumericCellValue());
            assertEquals("0.00", dataRow.getCell(9).getCellStyle().getDataFormatString());
            assertEquals("1小时1分钟", dataRow.getCell(10).getStringCellValue());
            assertEquals(CellType.NUMERIC, dataRow.getCell(11).getCellType());
            assertEquals(1d, dataRow.getCell(11).getNumericCellValue());
            assertEquals("人民医院", dataRow.getCell(12).getStringCellValue());
            assertEquals("张三", dataRow.getCell(13).getStringCellValue());
            assertEquals("男", dataRow.getCell(14).getStringCellValue());
            assertEquals(CellType.NUMERIC, dataRow.getCell(15).getCellType());
            assertEquals(35d, dataRow.getCell(15).getNumericCellValue());
            assertEquals("操作员A", dataRow.getCell(16).getStringCellValue());
            assertEquals("李医生", dataRow.getCell(17).getStringCellValue());
            assertEquals("骨科", dataRow.getCell(18).getStringCellValue());
            assertEquals("业务员B", dataRow.getCell(19).getStringCellValue());
            assertEquals("已出库", dataRow.getCell(20).getStringCellValue());
            assertEquals("", dataRow.getCell(21).getStringCellValue());
        }
    }

    @Test
    void buildFormatsEdgeValuesAndKeepsNullableCellsBlank() throws Exception {
        Map<String, Object> zeroMinutes = emptyLedgerData();
        zeroMinutes.put("order_create_time", "2026-08-07 09:30:00");
        zeroMinutes.put("file_name", "model");
        zeroMinutes.put("processing_duration_seconds", 0L);
        zeroMinutes.put("patient_gender", "其他");
        zeroMinutes.put("color_name", "   ");
        zeroMinutes.put("weight", Double.NaN);
        zeroMinutes.put("patient_age", Double.POSITIVE_INFINITY);
        zeroMinutes.put("product_name", "x".repeat(32768));

        Map<String, Object> oneHour = emptyLedgerData();
        oneHour.put("order_create_time", LocalDate.of(2026, 8, 8));
        oneHour.put("file_name", ".gitignore");
        oneHour.put("print_duration_seconds", -1L);
        oneHour.put("processing_duration_seconds", 3600L);
        oneHour.put("patient_gender", "12.2");
        oneHour.put("color_name", " 红色 ");

        Map<String, Object> oneHourOneMinute = emptyLedgerData();
        oneHourOneMinute.put("file_name", "guide.stl");
        oneHourOneMinute.put("processing_duration_seconds", 3660L);
        oneHourOneMinute.put("patient_gender", "女");
        oneHourOneMinute.put("material_name", " 钛合金 ");

        Map<String, Object> flooredMinutes = emptyLedgerData();
        flooredMinutes.put("order_create_time", Timestamp.valueOf("2026-08-09 10:11:12.123456789"));
        flooredMinutes.put("file_name", ".patient.v2.stl");
        flooredMinutes.put("processing_duration_seconds", 2428L);
        flooredMinutes.put("color_name", " ");
        flooredMinutes.put("material_name", "  ");

        Map<String, Object> negativeMinutes = emptyLedgerData();
        negativeMinutes.put("print_duration_seconds", -60L);
        negativeMinutes.put("processing_duration_seconds", -60L);

        byte[] workbookBytes = new ProductLedgerExcelBuilder().build(
                List.of(zeroMinutes, oneHour, oneHourOneMinute, flooredMinutes, negativeMinutes), 5);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            var sheet = workbook.getSheet("生产产品台账");
            var row1 = sheet.getRow(1);
            assertEquals("2026年8月7日", row1.getCell(2).getStringCellValue());
            assertEquals("model", row1.getCell(4).getStringCellValue());
            assertEquals("", row1.getCell(8).getStringCellValue());
            assertEquals(CellType.BLANK, row1.getCell(9).getCellType());
            assertEquals("0分钟", row1.getCell(10).getStringCellValue());
            assertEquals("其他", row1.getCell(14).getStringCellValue());
            assertEquals(CellType.BLANK, row1.getCell(15).getCellType());
            assertEquals("未出库", row1.getCell(20).getStringCellValue());
            assertEquals(32767, row1.getCell(5).getStringCellValue().length());

            var row2 = sheet.getRow(2);
            assertEquals("2026年8月8日", row2.getCell(2).getStringCellValue());
            assertEquals(".gitignore", row2.getCell(4).getStringCellValue());
            assertEquals("", row2.getCell(8).getStringCellValue());
            assertEquals("红色", row2.getCell(7).getStringCellValue());
            assertEquals("1小时", row2.getCell(10).getStringCellValue());
            assertEquals("女", row2.getCell(14).getStringCellValue());

            var row3 = sheet.getRow(3);
            assertEquals("guide", row3.getCell(4).getStringCellValue());
            assertEquals("钛合金", row3.getCell(7).getStringCellValue());
            assertEquals("1小时1分钟", row3.getCell(10).getStringCellValue());

            var row4 = sheet.getRow(4);
            assertEquals("2026年8月9日", row4.getCell(2).getStringCellValue());
            assertEquals(".patient.v2", row4.getCell(4).getStringCellValue());
            assertEquals("", row4.getCell(7).getStringCellValue());
            assertEquals("40分钟", row4.getCell(10).getStringCellValue());

            var row5 = sheet.getRow(5);
            assertEquals("", row5.getCell(8).getStringCellValue());
            assertEquals("", row5.getCell(10).getStringCellValue());
        }
    }

    @Test
    void buildPlacesWarningAboveHeaderAndMergesAcrossTwentyTwoColumns() throws Exception {
        byte[] workbookBytes = new ProductLedgerExcelBuilder().build(List.of(), 10001);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            var sheet = workbook.getSheet("生产产品台账");
            assertEquals(1, sheet.getNumMergedRegions());
            var warningRegion = sheet.getMergedRegion(0);
            assertEquals(0, warningRegion.getFirstRow());
            assertEquals(0, warningRegion.getLastRow());
            assertEquals(0, warningRegion.getFirstColumn());
            assertEquals(21, warningRegion.getLastColumn());
            assertNull(sheet.getRow(1), "警告与表头之间应保留空白行");
            assertHeaders(sheet.getRow(2));
        }
    }

    private static Map<String, Object> emptyLedgerData() {
        Map<String, Object> data = new HashMap<>();
        List.of(
                "order_code", "order_create_time", "product_no", "file_name", "product_name", "spec_name",
                "color_name", "material_name", "print_duration_seconds", "weight",
                "processing_duration_seconds", "hospital_name", "patient_name", "patient_gender", "patient_age",
                "producer_name", "doctor_name", "hospital_dept_name", "business_operator", "warehouse_out_time"
        ).forEach(key -> data.put(key, null));
        return data;
    }

    private static void assertHeaders(org.apache.poi.ss.usermodel.Row headerRow) {
        assertEquals(EXPECTED_HEADERS.size(), headerRow.getLastCellNum());
        List<String> actualHeaders = IntStream.range(0, EXPECTED_HEADERS.size())
                .mapToObj(index -> headerRow.getCell(index).getStringCellValue())
                .toList();
        assertEquals(EXPECTED_HEADERS, actualHeaders);
    }
}
