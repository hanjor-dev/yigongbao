package com.yigongbao.module.production.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.product.entity.ProductEntity;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock private ProductMapper baseProductMapper;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DesignCompletedListener listener;

    @Test
    void testGroupDesignProductsByCategory() throws Exception {
        // Arrange: 准备测试数据
        Long packageId = 1L;

        // 模拟3个设计产品：2个模型类 + 1个导板类
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

        // 模拟产品大类查询
        ProductEntity modelProduct = new ProductEntity();
        modelProduct.setId(101L);
        modelProduct.setCategory("17.1");

        ProductEntity guideProduct = new ProductEntity();
        guideProduct.setId(102L);
        guideProduct.setCategory("17.2");

        when(baseProductMapper.selectBatchIds(any())).thenReturn(Arrays.asList(modelProduct, guideProduct));

        // Act: 使用反射调用私有方法
        Method method = DesignCompletedListener.class.getDeclaredMethod("groupByProductCategory", Long.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, List<DesignProductEntity>> grouped = (Map<String, List<DesignProductEntity>>) method.invoke(listener, packageId);

        // Assert: 验证分组结果
        assertNotNull(grouped);
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey("17.1"));
        assertTrue(grouped.containsKey("17.2"));
        assertEquals(2, grouped.get("17.1").size());
        assertEquals(1, grouped.get("17.2").size());
    }

    @Test
    void testOnDesignCompleted_SplitByCategory() {
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

        // 模拟2个模型 + 1个导板
        DesignProductEntity modelProduct1 = new DesignProductEntity();
        modelProduct1.setId(1L);
        modelProduct1.setProductId(101L);
        modelProduct1.setQuantity(1);

        DesignProductEntity modelProduct2 = new DesignProductEntity();
        modelProduct2.setId(2L);
        modelProduct2.setProductId(101L);
        modelProduct2.setQuantity(1);

        DesignProductEntity guideProduct = new DesignProductEntity();
        guideProduct.setId(3L);
        guideProduct.setProductId(102L);
        guideProduct.setQuantity(1);

        when(designProductMapper.selectList(any())).thenReturn(
            Arrays.asList(modelProduct1, modelProduct2, guideProduct));

        // 模拟产品大类查询
        ProductEntity productModel = new ProductEntity();
        productModel.setId(101L);
        productModel.setCategory("17.1");

        ProductEntity productGuide = new ProductEntity();
        productGuide.setId(102L);
        productGuide.setCategory("17.2");

        when(baseProductMapper.selectBatchIds(any())).thenReturn(
            Arrays.asList(productModel, productGuide));

        // Mock幂等性检查返回null（不存在）
        when(recordMapper.selectOne(any())).thenReturn(null);

        // Mock编码生成器
        when(codeGeneratorService.generate(anyString())).thenReturn("MOCK_CODE");

        // Act
        listener.onDesignCompleted(new DesignCompletedEvent(this, orderId));

        // Assert: 应该创建2张流转卡（模型1张，导板1张）
        verify(recordMapper, times(2)).insert(any(ProductionRecordEntity.class));
    }
}
