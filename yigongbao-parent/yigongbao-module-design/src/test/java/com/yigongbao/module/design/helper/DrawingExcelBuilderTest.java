package com.yigongbao.module.design.helper;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrawingExcelBuilderTest {

    @Test
    void build_withFewProducts_shouldFillSlotsOnSingleSheet() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(buildRows(3));

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

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(buildRows(13));

        byte[] result = builder.build(ctx);

        assertNotNull(result);
        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(result))) {
            // 13 条产品：第1页11条，第2页2条，共2页
            assertEquals(2, wb.getNumberOfSheets());
        }
    }

    @Test
    void build_withWideScreenshot_shouldPreserveAspectRatioInsideSlot() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
        row.setPackageFileName("模型.stl");
        row.setProductName("产品");
        row.setScreenshotBytes(createPng(1600, 900));

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(List.of(row));

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFPicture picture = sheet.getDrawingPatriarch().getShapes().stream()
                    .filter(XSSFPicture.class::isInstance)
                    .map(XSSFPicture.class::cast)
                    .findFirst()
                    .orElseThrow();
            XSSFClientAnchor anchor = picture.getPreferredSize();

            double left = xPositionInEmu(sheet, anchor.getCol1(), anchor.getDx1());
            double right = xPositionInEmu(sheet, anchor.getCol2(), anchor.getDx2());
            double top = yPositionInEmu(sheet, anchor.getRow1(), anchor.getDy1());
            double bottom = yPositionInEmu(sheet, anchor.getRow2(), anchor.getDy2());

            assertEquals(1600d / 900d, (right - left) / (bottom - top), 0.03d);
            assertTrue(left >= xPositionInEmu(sheet, 0, 0));
            assertTrue(right <= xPositionInEmu(sheet, 4, 0));
            assertTrue(top >= yPositionInEmu(sheet, 2, 0));
            assertTrue(bottom <= yPositionInEmu(sheet, 12, 0));
            assertEquals(
                    (xPositionInEmu(sheet, 0, 0) + xPositionInEmu(sheet, 4, 0)) / 2,
                    (left + right) / 2,
                    Units.EMU_PER_PIXEL * 2);
            assertEquals(
                    (yPositionInEmu(sheet, 2, 0) + yPositionInEmu(sheet, 12, 0)) / 2,
                    (top + bottom) / 2,
                    Units.EMU_PER_PIXEL * 2);
        }
    }

    @Test
    void build_withPortraitScreenshot_shouldPreserveAspectRatioInsideSlot() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
        row.setPackageFileName("模型.stl");
        row.setProductName("产品");
        row.setScreenshotBytes(createPng(600, 1200));

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(List.of(row));

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFPicture picture = sheet.getDrawingPatriarch().getShapes().stream()
                    .filter(XSSFPicture.class::isInstance)
                    .map(XSSFPicture.class::cast)
                    .findFirst()
                    .orElseThrow();
            XSSFClientAnchor anchor = picture.getPreferredSize();

            double left = xPositionInEmu(sheet, anchor.getCol1(), anchor.getDx1());
            double right = xPositionInEmu(sheet, anchor.getCol2(), anchor.getDx2());
            double top = yPositionInEmu(sheet, anchor.getRow1(), anchor.getDy1());
            double bottom = yPositionInEmu(sheet, anchor.getRow2(), anchor.getDy2());

            assertEquals(600d / 1200d, (right - left) / (bottom - top), 0.03d);
            assertTrue(left >= xPositionInEmu(sheet, 0, 0));
            assertTrue(right <= xPositionInEmu(sheet, 4, 0));
            assertTrue(top >= yPositionInEmu(sheet, 2, 0));
            assertTrue(bottom <= yPositionInEmu(sheet, 12, 0));
            assertEquals(
                    (xPositionInEmu(sheet, 0, 0) + xPositionInEmu(sheet, 4, 0)) / 2,
                    (left + right) / 2,
                    Units.EMU_PER_PIXEL * 2);
            assertEquals(
                    (yPositionInEmu(sheet, 2, 0) + yPositionInEmu(sheet, 12, 0)) / 2,
                    (top + bottom) / 2,
                    Units.EMU_PER_PIXEL * 2);
        }
    }

    @Test
    void build_withNonSquareQr_shouldPreserveAspectRatioInsideQrRange() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(buildRows(1));
        ctx.setQrBytes(createPng(300, 900));

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFPicture picture = sheet.getDrawingPatriarch().getShapes().stream()
                    .filter(XSSFPicture.class::isInstance)
                    .map(XSSFPicture.class::cast)
                    .findFirst()
                    .orElseThrow();
            XSSFClientAnchor anchor = picture.getPreferredSize();

            double left = xPositionInEmu(sheet, anchor.getCol1(), anchor.getDx1());
            double right = xPositionInEmu(sheet, anchor.getCol2(), anchor.getDx2());
            double top = yPositionInEmu(sheet, anchor.getRow1(), anchor.getDy1());
            double bottom = yPositionInEmu(sheet, anchor.getRow2(), anchor.getDy2());

            assertEquals(300d / 900d, (right - left) / (bottom - top), 0.03d);
            assertTrue(left >= xPositionInEmu(sheet, 12, 0));
            assertTrue(right <= xPositionInEmu(sheet, 16, 0));
            assertTrue(top >= yPositionInEmu(sheet, 0, 0));
            assertTrue(bottom <= yPositionInEmu(sheet, 10, 0));
        }
    }

    @Test
    void build_withUnreadableQr_shouldStillInsertPictureUsingFallbackRange() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(buildRows(1));
        ctx.setQrBytes(new byte[]{0x01, 0x02, 0x03, 0x04});

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertTrue(wb.getSheetAt(0).getDrawingPatriarch().getShapes().stream()
                    .anyMatch(XSSFPicture.class::isInstance));
        }
    }

    @Test
    void build_withEmptyQr_shouldInsertMinimalFallbackPicture() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();
        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(buildRows(1));
        ctx.setQrBytes(new byte[0]);

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertTrue(wb.getSheetAt(0).getDrawingPatriarch().getShapes().stream()
                    .anyMatch(XSSFPicture.class::isInstance));
        }
    }

    @Test
    void build_withUnreadableScreenshot_shouldStillInsertPicture() throws Exception {
        DrawingExcelBuilder builder = new DrawingExcelBuilder();

        DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
        row.setPackageFileName("模型.stl");
        row.setProductName("产品");
        row.setScreenshotBytes(new byte[]{0x01, 0x02, 0x03, 0x04});

        DrawingExcelBuilder.BuildContext ctx = new DrawingExcelBuilder.BuildContext();
        ctx.setOrderCode("ORD-001");
        ctx.setPackageCode("PKG-001");
        ctx.setRows(List.of(row));

        byte[] result = builder.build(ctx);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet.getDrawingPatriarch());
            assertTrue(sheet.getDrawingPatriarch().getShapes().stream()
                    .anyMatch(XSSFPicture.class::isInstance));
        }
    }

    private byte[] createPng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private double xPositionInEmu(XSSFSheet sheet, int col, int dx) {
        double position = dx;
        for (int i = 0; i < col; i++) {
            position += Units.columnWidthToEMU(sheet.getColumnWidth(i));
        }
        return position;
    }

    private double yPositionInEmu(XSSFSheet sheet, int rowIndex, int dy) {
        double position = dy;
        for (int i = 0; i < rowIndex; i++) {
            Row row = sheet.getRow(i);
            double points = row == null || row.getHeightInPoints() < 0
                    ? sheet.getDefaultRowHeightInPoints()
                    : row.getHeightInPoints();
            position += points * Units.EMU_PER_POINT;
        }
        return position;
    }

    private List<DrawingExcelBuilder.ProductRow> buildRows(int count) {
        List<DrawingExcelBuilder.ProductRow> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            DrawingExcelBuilder.ProductRow row = new DrawingExcelBuilder.ProductRow();
            row.setPackageFileName("文件" + i + ".stl");
            row.setProductName("产品" + i);
            list.add(row);
        }
        return list;
    }
}
