package com.yigongbao.module.production.helper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
