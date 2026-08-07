package com.yigongbao.module.production.helper;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FlowCardExcelBuilderTest {

    @Autowired
    private FlowCardExcelBuilder builder;

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
}
