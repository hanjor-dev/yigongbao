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
