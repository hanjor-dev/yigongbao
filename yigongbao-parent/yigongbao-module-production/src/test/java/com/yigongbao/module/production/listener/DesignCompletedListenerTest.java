package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.product.mapper.ProductMapper;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yigongbao.common.event.DesignCompletedEvent;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;

/**
 * 设计完成监听器单元测试
 *
 * @author hanjor
 * @date 2026-07-10
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DesignCompletedListenerTest {

    @Mock private OrderMainMapper orderMainMapper;
    @Mock private DesignPackageMapper designPackageMapper;
    @Mock private DesignProductMapper designProductMapper;
    @Mock private DesignProductFileMapper designProductFileMapper;
    @Mock private ProductionRecordMapper recordMapper;
    @Mock private ProductionProductMapper productMapper;
    @Mock private ProductionProcessMapper processMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private ProductMapper basicProductMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DesignCompletedListener listener;

    @Test
    void testGroupDesignProductsByProductId() throws Exception {
        // Arrange: 准备测试数据
        Long packageId = 1L;

        // 模拟3个设计产品：2个产品101 + 1个产品102
        DesignProductEntity product1 = new DesignProductEntity();
        product1.setId(1L);
        product1.setProductId(101L);
        product1.setProductName("模型A");

        DesignProductEntity product2 = new DesignProductEntity();
        product2.setId(2L);
        product2.setProductId(101L);
        product2.setProductName("模型B");

        DesignProductEntity product3 = new DesignProductEntity();
        product3.setId(3L);
        product3.setProductId(102L);
        product3.setProductName("导板A");

        when(designProductMapper.selectList(any())).thenReturn(Arrays.asList(product1, product2, product3));

        // Act: 使用反射调用私有方法
        Method method = DesignCompletedListener.class.getDeclaredMethod("groupByProductId", Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, List<DesignProductEntity>> grouped = (Map<Long, List<DesignProductEntity>>) method.invoke(listener, packageId);

        // Assert: 验证分组结果
        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey(101L));
        assertTrue(grouped.containsKey(102L));
        assertEquals(2, grouped.get(101L).size());
        assertEquals(1, grouped.get(102L).size());
    }

    @Test
    void testOnDesignCompleted_SplitByProductId() {
        // Arrange
        Long orderId = 1L;
        Long packageId = 1L;

        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setOrderCode("ORD001");
        order.setOrderType(1);

        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(packageId);
        pkg.setPackageCode("PKG001");

        when(orderMainMapper.selectById(orderId)).thenReturn(order);
        when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));

        // 模拟2个产品101 + 1个产品102
        DesignProductEntity product1 = new DesignProductEntity();
        product1.setId(1L);
        product1.setProductId(101L);
        product1.setProductName("产品A");
        product1.setQuantity(1);

        DesignProductEntity product2 = new DesignProductEntity();
        product2.setId(2L);
        product2.setProductId(101L);
        product2.setProductName("产品A");
        product2.setQuantity(1);

        DesignProductEntity product3 = new DesignProductEntity();
        product3.setId(3L);
        product3.setProductId(102L);
        product3.setProductName("产品B");
        product3.setQuantity(1);

        when(designProductMapper.selectList(any())).thenReturn(
            Arrays.asList(product1, product2, product3));

        // Mock幂等性检查返回null（不存在）
        when(recordMapper.selectOne(any())).thenReturn(null);

        // Mock编码生成器
        when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");

        // Act
        listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));

        // Assert: 应该创建2张流转卡（产品101一张，产品102一张）
        verify(recordMapper, times(2)).insert(any(ProductionRecordEntity.class));
    }

    @Test
    void testOnDesignCompleted_SingleProductId() {
        // Arrange: 只有一种产品
        Long orderId = 1L;
        Long packageId = 1L;

        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setOrderCode("ORD001");
        order.setOrderType(1);

        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(packageId);
        pkg.setPackageCode("PKG001");

        when(orderMainMapper.selectById(orderId)).thenReturn(order);
        when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));

        // 模拟3个相同产品101的设计产品
        DesignProductEntity product1 = new DesignProductEntity();
        product1.setId(1L);
        product1.setProductId(101L);
        product1.setProductName("产品A");
        product1.setQuantity(1);

        DesignProductEntity product2 = new DesignProductEntity();
        product2.setId(2L);
        product2.setProductId(101L);
        product2.setProductName("产品A");
        product2.setQuantity(1);

        DesignProductEntity product3 = new DesignProductEntity();
        product3.setId(3L);
        product3.setProductId(101L);
        product3.setProductName("产品A");
        product3.setQuantity(1);

        when(designProductMapper.selectList(any())).thenReturn(
            Arrays.asList(product1, product2, product3));

        when(recordMapper.selectOne(any())).thenReturn(null);
        when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");

        // Act
        listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));

        // Assert: 应该只创建1张流转卡（产品101）
        verify(recordMapper, times(1)).insert(any(ProductionRecordEntity.class));
    }

    @Test
    void testOnDesignCompleted_EmptyPackage() {
        // Arrange: 数据包无设计产品
        Long orderId = 1L;
        Long packageId = 1L;

        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setOrderCode("ORD001");
        order.setOrderType(1);

        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(packageId);
        pkg.setPackageCode("PKG001");

        when(orderMainMapper.selectById(orderId)).thenReturn(order);
        when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));

        // 模拟空的设计产品列表
        when(designProductMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));

        // Assert: 应该不创建流转卡
        verify(recordMapper, never()).insert(any(ProductionRecordEntity.class));
    }

    @Test
    void testOnDesignCompleted_IdempotencyCheck() {
        // Arrange: 产品101流转卡已存在，只应创建产品102流转卡
        Long orderId = 1L;
        Long packageId = 1L;

        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setOrderCode("ORD001");
        order.setOrderType(1);

        DesignPackageEntity pkg = new DesignPackageEntity();
        pkg.setId(packageId);
        pkg.setPackageCode("PKG001");

        when(orderMainMapper.selectById(orderId)).thenReturn(order);
        when(designPackageMapper.selectList(any())).thenReturn(Arrays.asList(pkg));

        // 模拟2个产品101 + 1个产品102
        DesignProductEntity product1 = new DesignProductEntity();
        product1.setId(1L);
        product1.setProductId(101L);
        product1.setProductName("产品A");
        product1.setQuantity(1);

        DesignProductEntity product2 = new DesignProductEntity();
        product2.setId(2L);
        product2.setProductId(101L);
        product2.setProductName("产品A");
        product2.setQuantity(1);

        DesignProductEntity product3 = new DesignProductEntity();
        product3.setId(3L);
        product3.setProductId(102L);
        product3.setProductName("产品B");
        product3.setQuantity(1);

        when(designProductMapper.selectList(any())).thenReturn(
            Arrays.asList(product1, product2, product3));

        // 模拟产品101流转卡已存在
        ProductionRecordEntity existingRecord = new ProductionRecordEntity();
        existingRecord.setId(100L);
        existingRecord.setRecordNo("REC001");
        existingRecord.setProductId(101L);
        existingRecord.setProductName("产品A");

        // 批量查询返回已存在的产品101流转卡（新实现使用selectList批量查询）
        when(recordMapper.selectList(any())).thenReturn(Arrays.asList(existingRecord));
        when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");

        // Act
        listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));

        // Assert: 应该只创建1张流转卡（跳过产品101，只创建产品102）
        verify(recordMapper, times(1)).insert(any(ProductionRecordEntity.class));
    }
}
