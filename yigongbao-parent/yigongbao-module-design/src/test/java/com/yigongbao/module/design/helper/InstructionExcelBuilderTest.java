package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
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
