package com.yigongbao.module.design.helper;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstructionExcelBuilderTest {

    @Test
    void build_withSingleProduct_shouldFillBasicInfoAndProductRow() throws Exception {
        InstructionExcelBuilder builder = new InstructionExcelBuilder();

        List<InstructionExcelBuilder.ProductRow> rows = new ArrayList<>();
        InstructionExcelBuilder.ProductRow row = new InstructionExcelBuilder.ProductRow();
        row.setCertNo("CERT-001");
        row.setProductName("PEEK骨模型");
        row.setPackageFileName("左髋骨.stl");
        row.setSpecName("47mm");
        row.setMaterialName("树脂");
        row.setQuantity(1);
        row.setIsUrgent(0);
        row.setColorName("白色");
        rows.add(row);

        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPatientName("张三");
        ctx.setHospitalName("北京协和医院");
        ctx.setContactName("李医生");
        ctx.setPackageCode("PKG-001");
        ctx.setExpectedDeliveryDate("2026-04-20");
        ctx.setVersion("A/1");
        ctx.setRows(rows);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        assertTrue(result.length > 0);

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

        List<InstructionExcelBuilder.ProductRow> rows = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            InstructionExcelBuilder.ProductRow row = new InstructionExcelBuilder.ProductRow();
            row.setCertNo("CERT-" + i);
            row.setProductName("产品" + i);
            row.setPackageFileName("文件" + i + ".stl");
            row.setSpecName("规格" + i);
            row.setMaterialName("树脂");
            row.setQuantity(1);
            row.setIsUrgent(0);
            rows.add(row);
        }

        InstructionExcelBuilder.BuildContext ctx = new InstructionExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setVersion("A/1");
        ctx.setRows(rows);

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            // 产品区从 row=7 开始，20条产品，最后一条在 row=26（7+19）
            assertEquals("CERT-20", getCellValue(sheet, 26, 1));
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
