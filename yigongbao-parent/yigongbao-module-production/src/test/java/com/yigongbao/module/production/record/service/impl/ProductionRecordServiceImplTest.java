package com.yigongbao.module.production.record.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 生产流转卡服务单元测试
 *
 * @author hanjor
 * @date 2026-05-27
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductionRecordServiceImplTest {

    @Mock
    private ProductionRecordMapper recordMapper;
    @Mock
    private CodeGeneratorService codeGeneratorService;
    @Mock
    private DesignPackageMapper designPackageMapper;
    @Mock
    private DesignPackageFileMapper designPackageFileMapper;
    @Mock
    private OrderMainMapper orderMainMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductionProductMapper productMapper;
    @Mock
    private ProductionProcessMapper processMapper;
    @Mock
    private FlowFacade flowFacade;

    @InjectMocks
    private ProductionRecordServiceImpl recordService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(recordService, recordMapper);
    }

    @Test
    void testServiceNotNull() {
        assertNotNull(recordService);
    }
}
