package com.yigongbao.module.order.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import org.mybatis.spring.annotation.MapperScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.SpringBootConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 订单机构数据权限集成测试。
 * 使用 H2 + MyBatis-Plus Mapper 验证 ORG 条件实际作用于订单查询。
 */
@SpringBootTest(classes = OrderDataScopeIntegrationTest.TestApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql",
        "spring.datasource.url=jdbc:h2:mem:order_scope_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml",
        "spring.main.web-application-type=none"
})
class OrderDataScopeIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.yigongbao.module.order.mapper")
    static class TestApplication {
    }

    @Autowired
    private OrderMainMapper orderMainMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private UserHospitalService userHospitalService;
    @MockBean
    private ConfigService configService;
    @MockBean
    private DictService dictService;
    @MockBean
    private OrderItemMapper orderItemMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderQueryHelper orderQueryHelper;
    private Long sameOrgOrderId;
    private Long otherOrgOrderId;

    @BeforeEach
    void setUp() {
        orderMainMapper.delete(new LambdaQueryWrapper<OrderMainEntity>()
                .isNotNull(OrderMainEntity::getId));
        long baseId = Math.abs(System.nanoTime());
        sameOrgOrderId = baseId;
        otherOrgOrderId = baseId + 1;
        orderQueryHelper = new OrderQueryHelper(
                userService, userHospitalService, configService, dictService, objectMapper, orderItemMapper);
    }

    @Test
    void orgScope_returnsOrderFromSameOrganization() {
        insertOrder(sameOrgOrderId, 100L);
        insertOrder(otherOrgOrderId, 200L);

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setOrgId(100L);
        when(userService.getById(10L)).thenReturn(user);

        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(10L);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 10L, DataScopeTypeEnum.ORG);

            List<OrderMainEntity> visibleOrders = orderMainMapper.selectList(wrapper);

            assertThat(visibleOrders).extracting(OrderMainEntity::getId).containsExactly(sameOrgOrderId);
        }
    }

    @Test
    void orgScope_hidesOrderFromAnotherOrganization() {
        insertOrder(sameOrgOrderId, 100L);
        insertOrder(otherOrgOrderId, 200L);

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setOrgId(100L);
        when(userService.getById(10L)).thenReturn(user);

        try (MockedStatic<StpUtil> stpMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(10L);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            orderQueryHelper.buildDataScopeCondition(wrapper, 10L, DataScopeTypeEnum.ORG);

            List<OrderMainEntity> visibleOrders = orderMainMapper.selectList(wrapper);

            assertThat(visibleOrders).extracting(OrderMainEntity::getId).doesNotContain(otherOrgOrderId);
        }
    }

    private void insertOrder(Long id, Long orgId) {
        OrderMainEntity order = new OrderMainEntity();
        order.setId(id);
        order.setOrderCode("ORD-" + id);
        order.setOrgId(orgId);
        order.setOrgName("机构-" + orgId);
        order.setOrderType(1);
        order.setNeedsPhysicalDelivery(0);
        order.setPhase(10);
        order.setStatus(1010);
        order.setVersion(0);
        order.setIsDeleted(0);
        orderMainMapper.insert(order);
    }
}
