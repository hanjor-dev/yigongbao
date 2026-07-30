package com.yigongbao.module.production.helper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductLedgerExcelBuilderTest {

    @Test
    void buildOmitsVersionColumn() throws Exception {
        byte[] workbookBytes = new ProductLedgerExcelBuilder().build(List.of(Map.of(
                "product_no", "P-001",
                "current_process_type", "print",
                "processing_center_name", "加工中心A"
        )), 1);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            var headerRow = workbook.getSheet("生产产品台账").getRow(0);
            var dataRow = workbook.getSheet("生产产品台账").getRow(1);
            assertEquals(41, headerRow.getLastCellNum(), "移除版本号后应保留41列");

            List<String> headers = java.util.stream.IntStream.range(0, headerRow.getLastCellNum())
                    .mapToObj(index -> headerRow.getCell(index).getStringCellValue())
                    .toList();
            assertFalse(headers.contains("版本号"));
            assertEquals("加工中心", headers.get(22));
            assertEquals("3D打印成型", dataRow.getCell(9).getStringCellValue());
            assertEquals("加工中心A", dataRow.getCell(22).getStringCellValue());
        }
    }
}
