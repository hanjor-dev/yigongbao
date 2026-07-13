package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProductNumberServiceImpl 单元测试
 * 测试产品编号生成服务的核心功能
 *
 * @author hanjor
 * @date 2026-07-13
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductNumberServiceImplTest {

    @Mock
    private ProductionRecordMapper recordMapper;

    @Mock
    private ProductionProductMapper productMapper;

    @Mock
    private DesignProductMapper designProductMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @InjectMocks
    private ProductNumberServiceImpl productNumberService;

    /**
     * 初始化测试环境
     * 由于ProductNumberServiceImpl继承了ServiceImpl，需要通过反射注入baseMapper
     */
    @BeforeEach
    void setUp() throws Exception {
        // 反射注入baseMapper（ServiceImpl要求）
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(productNumberService, productMapper);
    }

    // ========== getProductTypeCode 测试 ==========

    /**
     * 测试产品类型代码获取 - 精准匹配所有产品类型
     * 验证A/B/C/D/X所有类型代码的正确映射
     */
    @Test
    void testGetProductTypeCode_精准匹配() {
        // 测试类型A：医用个性化手术导板
        assertEquals("A", productNumberService.getProductTypeCode("医用个性化手术导板"));

        // 测试类型B：定制式3D打印骨模型
        assertEquals("B", productNumberService.getProductTypeCode("定制式3D打印骨模型"));

        // 测试类型C：定制式神经外科手术导板
        assertEquals("C", productNumberService.getProductTypeCode("定制式神经外科手术导板"));

        // 测试类型D：定制式放射粒子手术导板
        assertEquals("D", productNumberService.getProductTypeCode("定制式放射粒子手术导板"));

        // 测试类型X：未知产品
        assertEquals("X", productNumberService.getProductTypeCode("其他产品"));
        assertEquals("X", productNumberService.getProductTypeCode("未知产品名称"));
    }

    /**
     * 测试产品类型代码获取 - null值处理
     * 验证null输入时返回默认代码X
     */
    @Test
    void testGetProductTypeCode_null值() {
        assertEquals("X", productNumberService.getProductTypeCode(null));
    }

    // ========== generateSingleNumber 测试 ==========

    /**
     * 测试单个产品编号生成 - 格式正确性
     * 验证生成的编号格式为15位数字
     */
    @Test
    void testGenerateSingleNumber_格式正确() {
        String result = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 2, 1);

        // 验证编号格式：260630B03700201
        assertEquals("260630B03700201", result);
        assertEquals(15, result.length());

        // 验证编号组成部分
        assertEquals("260630", result.substring(0, 6));  // 批号
        assertEquals("B", result.substring(6, 7));       // 产品代码
        assertEquals("037", result.substring(7, 10));    // 设备编号
        assertEquals("002", result.substring(10, 13));   // 上机次数
        assertEquals("01", result.substring(13, 15));    // 流水号
    }

    /**
     * 测试单个产品编号生成 - 设备编号边界值
     * 验证设备编号在1-999范围内的正确补零
     */
    @Test
    void testGenerateSingleNumber_设备编号边界值() {
        // 设备编号最小值：1 → 001
        String result1 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "001", 1, 1);
        assertEquals("260630B00100101", result1);
        assertEquals("001", result1.substring(7, 10));

        // 设备编号最大值：999 → 999
        String result999 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "999", 1, 1);
        assertEquals("260630B99900101", result999);
        assertEquals("999", result999.substring(7, 10));
    }

    /**
     * 测试单个产品编号生成 - 上机次数边界值
     * 验证上机次数在0-999范围内的正确补零
     */
    @Test
    void testGenerateSingleNumber_上机次数边界值() {
        // 上机次数最小值：0 → 000
        String result0 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 0, 1);
        assertEquals("260630B03700001", result0);
        assertEquals("000", result0.substring(10, 13));

        // 上机次数最大值：999 → 999
        String result999 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 999, 1);
        assertEquals("260630B03799901", result999);
        assertEquals("999", result999.substring(10, 13));
    }

    /**
     * 测试单个产品编号生成 - 产品流水号边界值
     * 验证流水号在1-99范围内的正确补零
     */
    @Test
    void testGenerateSingleNumber_产品流水号边界值() {
        // 流水号最小值：1 → 01
        String result1 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 2, 1);
        assertEquals("260630B03700201", result1);
        assertEquals("01", result1.substring(13, 15));

        // 流水号最大值：99 → 99
        String result99 = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 2, 99);
        assertEquals("260630B03700299", result99);
        assertEquals("99", result99.substring(13, 15));
    }

    /**
     * 测试单个产品编号生成 - 不同产品类型
     * 验证不同产品类型代码的正确应用
     */
    @Test
    void testGenerateSingleNumber_不同产品类型() {
        // 类型A
        String resultA = productNumberService.generateSingleNumber(
                "260630", "医用个性化手术导板", "037", 2, 1);
        assertEquals("260630A03700201", resultA);

        // 类型B
        String resultB = productNumberService.generateSingleNumber(
                "260630", "定制式3D打印骨模型", "037", 2, 1);
        assertEquals("260630B03700201", resultB);

        // 类型C
        String resultC = productNumberService.generateSingleNumber(
                "260630", "定制式神经外科手术导板", "037", 2, 1);
        assertEquals("260630C03700201", resultC);

        // 类型D
        String resultD = productNumberService.generateSingleNumber(
                "260630", "定制式放射粒子手术导板", "037", 2, 1);
        assertEquals("260630D03700201", resultD);

        // 类型X
        String resultX = productNumberService.generateSingleNumber(
                "260630", "未知产品", "037", 2, 1);
        assertEquals("260630X03700201", resultX);
    }

    // ========== checkUniqueness 测试 ==========

    /**
     * 测试编号唯一性校验 - 编号唯一
     * 验证当编号不存在时返回true
     */
    @Test
    void testCheckUniqueness_编号唯一() {
        // Mock: 数据库中不存在该编号（count = 0）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // 执行测试
        boolean result = productNumberService.checkUniqueness("260630B03700201");

        // 验证结果
        assertTrue(result);
        verify(productMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    /**
     * 测试编号唯一性校验 - 编号重复
     * 验证当编号已存在时返回false
     */
    @Test
    void testCheckUniqueness_编号重复() {
        // Mock: 数据库中存在该编号（count = 1）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行测试
        boolean result = productNumberService.checkUniqueness("260630B03700201");

        // 验证结果
        assertFalse(result);
        verify(productMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
    }

    /**
     * 测试编号唯一性校验 - 多个重复
     * 验证当存在多个相同编号时也返回false
     */
    @Test
    void testCheckUniqueness_多个重复() {
        // Mock: 数据库中存在多个该编号（count = 3）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

        // 执行测试
        boolean result = productNumberService.checkUniqueness("260630B03700201");

        // 验证结果
        assertFalse(result);
    }

    // ========== generateFormalNumbers 测试 ==========

    /**
     * 测试批量生成正式编号 - 成功生成
     * 验证完整的编号生成流程：查询流转卡、设备、产品列表，生成并更新编号
     */
    @Test
    void testGenerateFormalNumbers_成功生成() {
        // 准备测试数据
        Long recordId = 1L;
        Long deviceId = 37L;
        Integer usageCount = 2;

        // Mock 流转卡数据
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(recordId);
        record.setProductionBatchNo("260630");
        record.setDesignPackageId(100L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        // Mock 设备数据
        DeviceEntity device = new DeviceEntity();
        device.setId(deviceId);
        device.setDeviceId("37");  // 注意：这是VARCHAR类型的业务编号
        device.setDeviceName("3D打印机-37");
        when(deviceMapper.selectById(deviceId)).thenReturn(device);

        // Mock 产品列表（3个产品）
        List<ProductionProductEntity> products = new ArrayList<>();
        
        ProductionProductEntity product1 = new ProductionProductEntity();
        product1.setId(101L);
        product1.setProductionRecordId(recordId);
        product1.setProductName("定制式3D打印骨模型");
        product1.setCreateTime(LocalDateTime.now());
        products.add(product1);

        ProductionProductEntity product2 = new ProductionProductEntity();
        product2.setId(102L);
        product2.setProductionRecordId(recordId);
        product2.setProductName("定制式3D打印骨模型");
        product2.setCreateTime(LocalDateTime.now().plusSeconds(1));
        products.add(product2);

        ProductionProductEntity product3 = new ProductionProductEntity();
        product3.setId(103L);
        product3.setProductionRecordId(recordId);
        product3.setProductName("定制式3D打印骨模型");
        product3.setCreateTime(LocalDateTime.now().plusSeconds(2));
        products.add(product3);

        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(products);

        // Mock 唯一性校验（所有编号都唯一）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // Mock 更新操作
        when(productMapper.updateById(ArgumentMatchers.<ProductionProductEntity>any())).thenReturn(1);

        // 执行测试
        productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);

        // 验证流转卡查询
        verify(recordMapper, times(1)).selectById(recordId);

        // 验证设备查询
        verify(deviceMapper, times(1)).selectById(deviceId);

        // 验证产品列表查询
        verify(productMapper, times(1)).selectList(any(LambdaQueryWrapper.class));

        // 验证唯一性校验（3次，每个产品1次）
        verify(productMapper, times(3)).selectCount(any(LambdaQueryWrapper.class));

        // 验证产品编号更新（3次，每个产品1次）
        verify(productMapper, times(3)).updateById(ArgumentMatchers.<ProductionProductEntity>any());

        // 验证生成的编号格式
        assertEquals("260630B03700201", product1.getProductNo());
        assertEquals("260630B03700202", product2.getProductNo());
        assertEquals("260630B03700203", product3.getProductNo());
    }

    /**
     * 测试批量生成正式编号 - 流转卡不存在
     * 验证当流转卡ID无效时抛出异常
     */
    @Test
    void testGenerateFormalNumbers_流转卡不存在() {
        // Mock: 流转卡不存在
        when(recordMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> productNumberService.generateFormalNumbers(999L, 37L, 2));

        // 验证异常信息
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());

        // 验证只调用了流转卡查询，后续操作未执行
        verify(recordMapper, times(1)).selectById(999L);
        verify(deviceMapper, never()).selectById(anyLong());
        verify(productMapper, never()).selectList(any());
    }

    /**
     * 测试批量生成正式编号 - 设备不存在
     * 验证当设备ID无效时抛出异常
     */
    @Test
    void testGenerateFormalNumbers_设备不存在() {
        // Mock 流转卡数据
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(1L);
        record.setProductionBatchNo("260630");
        record.setDesignPackageId(100L);
        when(recordMapper.selectById(1L)).thenReturn(record);

        // Mock: 设备不存在
        when(deviceMapper.selectById(anyLong())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
            () -> productNumberService.generateFormalNumbers(1L, 999L, 2));

        // 验证异常信息
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());

        // 验证执行流程
        verify(recordMapper, times(1)).selectById(1L);
        verify(deviceMapper, times(1)).selectById(999L);
        verify(productMapper, never()).selectList(any());
    }

    /**
     * 测试批量生成正式编号 - 流转卡无产品
     * 验证当流转卡下没有产品时抛出异常
     */
    @Test
    void testGenerateFormalNumbers_无产品() {
        // Mock 流转卡数据
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(1L);
        record.setProductionBatchNo("260630");
        record.setDesignPackageId(100L);
        when(recordMapper.selectById(1L)).thenReturn(record);

        // Mock 设备数据
        DeviceEntity device = new DeviceEntity();
        device.setId(37L);
        device.setDeviceId("37");
        when(deviceMapper.selectById(37L)).thenReturn(device);

        // Mock: 产品列表为空
        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
            () -> productNumberService.generateFormalNumbers(1L, 37L, 2));

        // 验证异常信息
        assertEquals(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), exception.getCode());

        // 验证执行流程
        verify(recordMapper, times(1)).selectById(1L);
        verify(deviceMapper, times(1)).selectById(37L);
        verify(productMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(productMapper, never()).updateById(ArgumentMatchers.<ProductionProductEntity>any());
    }

    /**
     * 测试批量生成正式编号 - 编号重复
     * 验证当生成的编号已存在时抛出异常
     */
    @Test
    void testGenerateFormalNumbers_编号重复() {
        // 准备测试数据
        Long recordId = 1L;
        Long deviceId = 37L;
        Integer usageCount = 2;

        // Mock 流转卡数据
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(recordId);
        record.setProductionBatchNo("260630");
        record.setDesignPackageId(100L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        // Mock 设备数据
        DeviceEntity device = new DeviceEntity();
        device.setId(deviceId);
        device.setDeviceId("37");
        when(deviceMapper.selectById(deviceId)).thenReturn(device);

        // Mock 产品列表（1个产品）
        List<ProductionProductEntity> products = new ArrayList<>();
        ProductionProductEntity product1 = new ProductionProductEntity();
        product1.setId(101L);
        product1.setProductionRecordId(recordId);
        product1.setProductName("定制式3D打印骨模型");
        product1.setCreateTime(LocalDateTime.now());
        products.add(product1);

        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(products);

        // Mock 唯一性校验：编号已存在（count = 1）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class,
            () -> productNumberService.generateFormalNumbers(recordId, deviceId, usageCount));

        // 验证异常信息
        assertEquals(ErrorCodeEnum.PRODUCT_NUMBER_DUPLICATE.getCode(), exception.getCode());
        System.out.println("实际异常消息: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("260630B03700201"), "异常消息应包含产品编号，实际消息: " + exception.getMessage());

        // 验证执行流程
        verify(recordMapper, times(1)).selectById(recordId);
        verify(deviceMapper, times(1)).selectById(deviceId);
        verify(productMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(productMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        verify(productMapper, never()).updateById(ArgumentMatchers.<ProductionProductEntity>any());  // 编号重复，不应执行更新
    }

    /**
     * 测试批量生成正式编号 - 多个产品不同类型
     * 验证混合产品类型时编号生成的正确性
     */
    @Test
    void testGenerateFormalNumbers_混合产品类型() {
        // 准备测试数据
        Long recordId = 1L;
        Long deviceId = 37L;
        Integer usageCount = 2;

        // Mock 流转卡数据
        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setId(recordId);
        record.setProductionBatchNo("260630");
        record.setDesignPackageId(100L);
        when(recordMapper.selectById(recordId)).thenReturn(record);

        // Mock 设备数据
        DeviceEntity device = new DeviceEntity();
        device.setId(deviceId);
        device.setDeviceId("37");
        when(deviceMapper.selectById(deviceId)).thenReturn(device);

        // Mock 产品列表（混合类型）
        List<ProductionProductEntity> products = new ArrayList<>();
        
        ProductionProductEntity productA = new ProductionProductEntity();
        productA.setId(101L);
        productA.setProductionRecordId(recordId);
        productA.setProductName("医用个性化手术导板");
        productA.setCreateTime(LocalDateTime.now());
        products.add(productA);

        ProductionProductEntity productB = new ProductionProductEntity();
        productB.setId(102L);
        productB.setProductionRecordId(recordId);
        productB.setProductName("定制式3D打印骨模型");
        productB.setCreateTime(LocalDateTime.now().plusSeconds(1));
        products.add(productB);

        when(productMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(products);

        // Mock 唯一性校验（所有编号都唯一）
        when(productMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // Mock 更新操作
        when(productMapper.updateById(ArgumentMatchers.<ProductionProductEntity>any())).thenReturn(1);

        // 执行测试
        productNumberService.generateFormalNumbers(recordId, deviceId, usageCount);

        // 验证生成的编号（不同产品类型代码）
        assertEquals("260630A03700201", productA.getProductNo());  // 类型A
        assertEquals("260630B03700202", productB.getProductNo());  // 类型B

        // 验证更新次数
        verify(productMapper, times(2)).updateById(ArgumentMatchers.<ProductionProductEntity>any());
    }
}
