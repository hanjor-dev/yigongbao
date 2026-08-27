package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.production.ProductionTestConfiguration;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.production.device.entity.DeviceUsageCounterEntity;
import com.yigongbao.module.production.device.mapper.DeviceUsageCounterMapper;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProductionTestConfiguration.class)
@ActiveProfiles("test")
    @Transactional
class ProductNumberIntegrationTest {

    @Autowired
    private IProductionRecordService productionRecordService;
    @Autowired
    private IProductNumberService productNumberService;
    @Autowired
    private IDeviceUsageCounterService deviceUsageCounterService;
    @Autowired
    private ProductionRecordMapper recordMapper;
    @Autowired
    private ProductionProductMapper productMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private DeviceUsageCounterMapper counterMapper;

    private static final DateTimeFormatter BATCH_NO_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    @BeforeEach
    void setUp() {
        counterMapper.delete(null);
        productMapper.delete(null);
        recordMapper.delete(null);
        deviceMapper.delete(null);
    }

    private Long createTestDevice(String deviceNo) {
        DeviceEntity device = new DeviceEntity();
        device.setDeviceId(deviceNo);
        device.setDeviceName("测试设备" + deviceNo);
        device.setDeviceType("3D打印机");
        device.setCenterId(1L);
        device.setCenterName("测试加工中心");
        device.setState(0);
        device.setConnectionStatus(1);
        device.setLastHeartbeat(LocalDateTime.now());
        deviceMapper.insert(device);
        return device.getId();
    }

    private Long createTestFlowCard(String batchNo, List<String> productNames) {
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setRecordNo("LC" + System.currentTimeMillis());
        record.setOrderId(1L);
        record.setOrderCode("ORD001");
        record.setDesignPackageId(1L);
        record.setDesignPackageCode("PKG001");
        record.setProductId(1L);
        record.setProductName("测试产品");
        record.setProductionBatchNo(batchNo);
        record.setVersionNo("A/0");
        record.setTotalProductCount(productNames.size());
        record.setStatus(10);
        recordMapper.insert(record);

        for (int i = 0; i < productNames.size(); i++) {
            ProductionProductEntity product = new ProductionProductEntity();
            product.setProductionRecordId(record.getId());
            product.setPrintFileId((long) (i + 1));
            product.setProductNo("PD-" + String.format("%06d", record.getId() * 100 + i));
            product.setProductName(productNames.get(i));
            product.setSpecName("标准型");
            product.setCertNo("TEST-CERT-001");
            product.setMaterialName("树脂");
            product.setColorName("白色");
            product.setFileName("test_file_" + i + ".stl");
            product.setStatus("in_process");
            productMapper.insert(product);
        }
        return record.getId();
    }

    private void assertProductNumberFormat(String productNo, String expectedBatchNo,
                                          String expectedProductCode, String expectedDeviceNo,
                                          int expectedUsageCount, int expectedSequence) {
        assertNotNull(productNo);
        assertEquals(14, productNo.length(), "产品编号应为14位");
        assertEquals(expectedBatchNo, productNo.substring(0, 6), "生产批号不匹配");
        assertEquals(expectedProductCode, productNo.substring(6, 7), "产品代码不匹配");
        assertEquals(expectedDeviceNo, productNo.substring(7, 10), "设备编号不匹配");
        assertEquals(String.format("%02d", expectedUsageCount), productNo.substring(10, 12), "上机次数不匹配");
        assertEquals(String.format("%02d", expectedSequence), productNo.substring(12, 14), "产品流水号不匹配");
    }

    @Test
    void testCompleteFlow_FromTemporaryToFormalNumber() {
        Long deviceId = createTestDevice("037");
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        List<String> productNames = List.of("医用个性化手术导板", "定制式3D打印骨模型", "医用个性化手术导板");
        Long recordId = createTestFlowCard(batchNo, productNames);

        List<ProductionProductEntity> tempProducts = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getCreateTime));
        assertEquals(3, tempProducts.size());
        for (ProductionProductEntity product : tempProducts) {
            assertTrue(product.getProductNo().matches("^PD-\\d+$"));
        }

        Integer usageCount = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);

        List<ProductionProductEntity> formalProducts = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getCreateTime));
        assertEquals(3, formalProducts.size());
        assertProductNumberFormat(formalProducts.get(0).getProductNo(), batchNo, "A", "037", usageCount, 1);
        assertProductNumberFormat(formalProducts.get(1).getProductNo(), batchNo, "B", "037", usageCount, 2);
        assertProductNumberFormat(formalProducts.get(2).getProductNo(), batchNo, "A", "037", usageCount, 3);

        assertEquals(1, usageCount);
        DeviceUsageCounterEntity counter = counterMapper.selectOne(
            new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                .eq(DeviceUsageCounterEntity::getUsageDate, LocalDate.now()));
        assertNotNull(counter);
        assertEquals(1, counter.getUsageCount());
    }

    @Test
    void testProductTypeCodeMapping() {
        assertEquals("A", productNumberService.getProductTypeCode("医用个性化手术导板"));
        assertEquals("B", productNumberService.getProductTypeCode("定制式3D打印骨模型"));
        assertEquals("C", productNumberService.getProductTypeCode("定制式神经外科手术导板"));
        assertEquals("D", productNumberService.getProductTypeCode("定制式放射粒子手术导板"));
        assertEquals("X", productNumberService.getProductTypeCode("未知产品"));
        assertEquals("X", productNumberService.getProductTypeCode(null));
        assertEquals("X", productNumberService.getProductTypeCode(""));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testConcurrentScenario_MultipleFlowCardsAssigningSameDevice() {
        Long deviceId = createTestDevice("050");
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);

        int flowCardCount = 5;
        List<Long> recordIds = new ArrayList<>();
        for (int i = 0; i < flowCardCount; i++) {
            recordIds.add(createTestFlowCard(batchNo, List.of("医用个性化手术导板", "定制式3D打印骨模型")));
        }

        for (Long recordId : recordIds) {
            Integer usageCount = deviceUsageCounterService.incrementAndGet(deviceId);
            productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);
        }

        List<ProductionProductEntity> allProducts = productMapper.selectList(null);
        Set<String> productNos = allProducts.stream().map(ProductionProductEntity::getProductNo).collect(Collectors.toSet());
        assertEquals(flowCardCount * 2, productNos.size());

        DeviceUsageCounterEntity counter = counterMapper.selectOne(
            new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                .eq(DeviceUsageCounterEntity::getUsageDate, LocalDate.now()));
        assertNotNull(counter);
        assertEquals(flowCardCount, counter.getUsageCount());

        Set<Integer> usageCounts = allProducts.stream()
            .map(p -> Integer.parseInt(p.getProductNo().substring(10, 12)))
            .collect(Collectors.toSet());
        assertEquals(flowCardCount, usageCounts.size());
        for (int i = 1; i <= flowCardCount; i++) {
            assertTrue(usageCounts.contains(i));
        }
    }

    @Test
    void testBoundary_DeviceNumber() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);

        Long deviceId001 = createTestDevice("001");
        Long recordId001 = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        Integer usageCount001 = deviceUsageCounterService.incrementAndGet(deviceId001);
        productNumberService.generateFormalNumbers(recordId001, deviceId001, usageCount001);
        ProductionProductEntity product001 = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId001));
        assertProductNumberFormat(product001.getProductNo(), batchNo, "A", "001", 1, 1);

        Long deviceId999 = createTestDevice("999");
        Long recordId999 = createTestFlowCard(batchNo, List.of("定制式3D打印骨模型"));
        Integer usageCount999 = deviceUsageCounterService.incrementAndGet(deviceId999);
        productNumberService.generateFormalNumbers(recordId999, deviceId999, usageCount999);
        ProductionProductEntity product999 = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId999));
        assertProductNumberFormat(product999.getProductNo(), batchNo, "B", "999", 1, 1);
    }

    @Test
    void testBoundary_UsageCount() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long deviceId = createTestDevice("100");

        Long recordId1 = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        Integer usageCount1 = deviceUsageCounterService.incrementAndGet(deviceId);
        assertEquals(1, usageCount1);
        productNumberService.generateFormalNumbers(recordId1, deviceId, usageCount1);
        ProductionProductEntity product1 = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId1));
        assertProductNumberFormat(product1.getProductNo(), batchNo, "A", "100", 1, 1);

        DeviceUsageCounterEntity counter = counterMapper.selectOne(
            new LambdaQueryWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getDeviceId, deviceId)
                .eq(DeviceUsageCounterEntity::getUsageDate, LocalDate.now()));
        counter.setUsageCount(998);
        counterMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeviceUsageCounterEntity>()
                .eq(DeviceUsageCounterEntity::getId, counter.getId())
                .set(DeviceUsageCounterEntity::getUsageCount, 998)
                .set(DeviceUsageCounterEntity::getVersion, counter.getVersion() + 1));

        Long recordId999 = createTestFlowCard(batchNo, List.of("定制式3D打印骨模型"));
        Integer usageCount999 = deviceUsageCounterService.incrementAndGet(deviceId);
        assertEquals(999, usageCount999);
        assertThrows(BusinessException.class,
            () -> productNumberService.generateFormalNumbers(recordId999, deviceId, usageCount999));
    }

    @Test
    void testBoundary_ProductSequence() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long deviceId = createTestDevice("200");

        Long recordId1 = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        Integer usageCount1 = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId1, deviceId, usageCount1);
        ProductionProductEntity product01 = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId1));
        assertProductNumberFormat(product01.getProductNo(), batchNo, "A", "200", 1, 1);

        List<String> productNames99 = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            productNames99.add("医用个性化手术导板");
        }
        Long recordId99 = createTestFlowCard(batchNo, productNames99);
        Integer usageCount2 = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId99, deviceId, usageCount2);

        List<ProductionProductEntity> products99 = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId99)
                .orderByAsc(ProductionProductEntity::getCreateTime));
        assertEquals(99, products99.size());
        assertProductNumberFormat(products99.get(0).getProductNo(), batchNo, "A", "200", 2, 1);
        assertProductNumberFormat(products99.get(98).getProductNo(), batchNo, "A", "200", 2, 99);
    }

    @Test
    void testBoundary_ProductNameNullOrUnknown() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long deviceId = createTestDevice("300");

        Long recordId = createTestFlowCard(batchNo, List.of("null"));
        ProductionProductEntity product = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId));
        product.setProductName(null);
        productMapper.updateById(product);

        Integer usageCount = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);
        product = productMapper.selectById(product.getId());
        assertProductNumberFormat(product.getProductNo(), batchNo, "X", "300", 1, 1);

        Long recordId2 = createTestFlowCard(batchNo, List.of("未知产品"));
        Integer usageCount2 = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId2, deviceId, usageCount2);
        ProductionProductEntity product2 = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId2));
        assertProductNumberFormat(product2.getProductNo(), batchNo, "X", "300", 2, 1);
    }

    @Test
    void testError_FlowCardNotExists() {
        Long deviceId = createTestDevice("400");
        Integer usageCount = deviceUsageCounterService.incrementAndGet(deviceId);
        assertThrows(BusinessException.class, () -> productNumberService.generateFormalNumbers(99999L, deviceId, usageCount));
    }

    @Test
    void testError_DeviceNotExists() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long recordId = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        assertThrows(BusinessException.class, () -> productNumberService.generateFormalNumbers(recordId, 99999L, 1));
    }

    @Test
    void testError_DeviceIdInvalidFormat() {
        DeviceEntity device = new DeviceEntity();
        device.setDeviceId("ABC");
        device.setDeviceName("测试设备ABC");
        device.setDeviceType("3D打印机");
        device.setCenterId(1L);
        device.setCenterName("测试加工中心");
        device.setState(0);
        device.setConnectionStatus(1);
        device.setLastHeartbeat(LocalDateTime.now());
        deviceMapper.insert(device);

        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long recordId = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        Integer usageCount = deviceUsageCounterService.incrementAndGet(device.getId());
        assertThrows(BusinessException.class, () -> productNumberService.generateFormalNumbers(recordId, device.getId(), usageCount));
    }

    @Test
    void testError_ProductNumberDuplicate() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long deviceId = createTestDevice("500");

        Long recordId1 = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        Integer usageCount1 = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId1, deviceId, usageCount1);

        Long recordId2 = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));
        BusinessException exception = assertThrows(BusinessException.class,
            () -> productNumberService.generateFormalNumbers(recordId2, deviceId, usageCount1));
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("重复") || exception.getMessage().contains("duplicate"));
    }

    @Test
    void testCheckUniqueness() {
        String batchNo = LocalDate.now().format(BATCH_NO_FORMATTER);
        Long deviceId = createTestDevice("600");
        Long recordId = createTestFlowCard(batchNo, List.of("医用个性化手术导板"));

        Integer usageCount = deviceUsageCounterService.incrementAndGet(deviceId);
        productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);

        ProductionProductEntity product = productMapper.selectOne(
            new LambdaQueryWrapper<ProductionProductEntity>().eq(ProductionProductEntity::getProductionRecordId, recordId));
        assertFalse(productNumberService.checkUniqueness(product.getProductNo()));
        assertTrue(productNumberService.checkUniqueness("260630A00100101"));
    }
}
