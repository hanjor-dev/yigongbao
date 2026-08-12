package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.module.production.helper.ProductLedgerExcelBuilder;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProductionRecordClaimTransactionTest.TestConfig.class)
class ProductionRecordClaimTransactionTest {

    @Autowired private IProductionRecordService recordService;
    @Autowired private DesignPackageMapper designPackageMapper;
    @Autowired private OrderMainMapper orderMainMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() throws Exception {
        reset(designPackageMapper, orderMainMapper, userMapper);
        if (TableInfoHelper.getTableInfo(OrderMainEntity.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                    new org.apache.ibatis.session.Configuration(), "");
            TableInfoHelper.initTableInfo(assistant, OrderMainEntity.class);
        }
        jdbcTemplate.execute("DROP TABLE IF EXISTS production_record");
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
        }
        jdbcTemplate.update("""
                INSERT INTO production_record
                    (id, design_package_id, status, is_deleted)
                VALUES (?, ?, ?, 0)
                """, 1L, 11L, FlowStatusEnum.DESIGN_COMPLETED.getValue());
    }

    @Test
    void downloadDataPackage_orderUpdateFails_rollsBackClaimFields() {
        assertTrue(AopUtils.isAopProxy(recordService));

        DesignPackageEntity designPackage = new DesignPackageEntity();
        designPackage.setId(11L);
        designPackage.setOrderId(21L);
        designPackage.setFileUrl("/package.zip");
        when(designPackageMapper.selectById(11L)).thenReturn(designPackage);

        OrderMainEntity order = new OrderMainEntity();
        order.setId(21L);
        order.setStatus(FlowStatusEnum.DESIGN_COMPLETED.getValue());
        when(orderMainMapper.selectById(21L)).thenReturn(order);

        UserEntity user = new UserEntity();
        user.setId(31L);
        user.setRealName("生产员A");
        user.setCenterId(41L);
        user.setCenterName("生产中心A");
        when(userMapper.selectById(31L)).thenReturn(user);
        when(orderMainMapper.update(isNull(), any())).thenThrow(new IllegalStateException("order update failed"));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(31L);
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> recordService.downloadDataPackage(1L));
            assertEquals("order update failed", exception.getMessage());
        }

        TransactionTemplate verifyTransaction = new TransactionTemplate(transactionManager);
        verifyTransaction.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        Map<String, Object> row = verifyTransaction.execute(status -> jdbcTemplate.queryForMap("""
                SELECT status, producer_id, producer_name,
                       processing_center_id, processing_center_name
                FROM production_record WHERE id = 1
                """));

        assertNotNull(row);
        assertEquals(FlowStatusEnum.DESIGN_COMPLETED.getValue(), ((Number) row.get("STATUS")).intValue());
        assertNull(row.get("PRODUCER_ID"));
        assertNull(row.get("PRODUCER_NAME"));
        assertNull(row.get("PROCESSING_CENTER_ID"));
        assertNull(row.get("PROCESSING_CENTER_NAME"));
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = ProductionRecordMapper.class)
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:claim-center;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            return factory.getObject();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean DesignPackageMapper designPackageMapper() { return mock(DesignPackageMapper.class); }
        @Bean OrderMainMapper orderMainMapper() { return mock(OrderMainMapper.class); }
        @Bean UserMapper userMapper() { return mock(UserMapper.class); }

        @Bean
        ProductionRecordServiceImpl productionRecordService(
                DesignPackageMapper designPackageMapper,
                OrderMainMapper orderMainMapper,
                UserMapper userMapper,
                ApplicationEventPublisher eventPublisher) {
            return new ProductionRecordServiceImpl(
                    mock(CodeGeneratorService.class),
                    designPackageMapper,
                    mock(DesignInstructionMapper.class),
                    mock(DesignDrawingMapper.class),
                    orderMainMapper,
                    mock(DeviceMapper.class),
                    userMapper,
                    mock(ProductionProductMapper.class),
                    mock(ProductionProcessMapper.class),
                    mock(FlowFacade.class),
                    mock(FlowCardExcelBuilder.class),
                    mock(ProductLedgerExcelBuilder.class),
                    mock(com.yigongbao.module.basic.file.service.FileService.class),
                    mock(ConfigService.class),
                    mock(UserService.class),
                    mock(UserHospitalService.class),
                    new ObjectMapper(),
                    eventPublisher,
                    mock(IDeviceUsageCounterService.class),
                    mock(IProductNumberService.class));
        }
    }
}
