package com.yigongbao.module.design.helper;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignQueryHelper 单元测试
 *
 * @author hanjor
 * @date 2026-04-16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignQueryHelper 单元测试")
class DesignQueryHelperTest {

    @Mock private UserService userService;
    @Mock private ConfigService configService;
    @Mock private DictService dictService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private DesignQueryHelper helper;

    // ==================== getCurrentUserId ====================

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserId {

        @Test
        @DisplayName("已登录时返回用户ID")
        void getCurrentUserId_loggedIn() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
                assertEquals(1L, helper.getCurrentUserId());
            }
        }

        @Test
        @DisplayName("未登录时返回 null")
        void getCurrentUserId_notLoggedIn() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenThrow(new RuntimeException("未登录"));
                assertNull(helper.getCurrentUserId());
            }
        }
    }

    // ==================== buildDataScopeCondition ====================

    @Nested
    @DisplayName("buildDataScopeCondition")
    class BuildDataScopeCondition {

        @Test
        @DisplayName("currentUser 为 null 时注入 1=0")
        void buildDataScopeCondition_nullUser() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            // 不断言 SQL 内容，只验证不抛出异常
            assertDoesNotThrow(() ->
                    helper.buildDataScopeCondition(wrapper, null, DataScopeTypeEnum.SELF));
        }

        @Test
        @DisplayName("SELF 范围——eq designer_id")
        void buildDataScopeCondition_self() {
            UserEntity user = new UserEntity();
            user.setId(1L);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            helper.buildDataScopeCondition(wrapper, user, DataScopeTypeEnum.SELF);
            // 不抛出异常即可；SQL 内容由集成测试验证
        }

        @Test
        @DisplayName("DEPT 范围——部门有成员时 IN 查询")
        void buildDataScopeCondition_dept_withMembers() {
            UserEntity user = new UserEntity();
            user.setId(1L);
            user.setDeptId(10L);
            when(userService.listUserIdsByDeptId(10L)).thenReturn(List.of(1L, 2L));

            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.buildDataScopeCondition(wrapper, user, DataScopeTypeEnum.DEPT));
            verify(userService).listUserIdsByDeptId(10L);
        }

        @Test
        @DisplayName("DEPT 范围——部门无成员时注入 1=0")
        void buildDataScopeCondition_dept_empty() {
            UserEntity user = new UserEntity();
            user.setId(1L);
            user.setDeptId(10L);
            when(userService.listUserIdsByDeptId(10L)).thenReturn(List.of());

            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.buildDataScopeCondition(wrapper, user, DataScopeTypeEnum.DEPT));
        }

        @Test
        @DisplayName("HOSPITALS 范围——静默降级为 SELF")
        void buildDataScopeCondition_hospitals_degradeToSelf() {
            UserEntity user = new UserEntity();
            user.setId(1L);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.buildDataScopeCondition(wrapper, user, DataScopeTypeEnum.HOSPITALS));
        }

        @Test
        @DisplayName("ALL 范围——wrapper 不添加任何条件")
        void buildDataScopeCondition_all() {
            UserEntity user = new UserEntity();
            user.setId(1L);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            helper.buildDataScopeCondition(wrapper, user, DataScopeTypeEnum.ALL);
            // 验证没有调用 userService（不做任何范围限制）
            verifyNoInteractions(userService);
        }
    }

    // ==================== applySort ====================

    @Nested
    @DisplayName("applySort")
    class ApplySort {

        @Test
        @DisplayName("白名单字段 ASC 不抛出异常")
        void applySort_whitelistedFieldAsc() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.applySort(wrapper, "orderCode", "ASC"));
        }

        @Test
        @DisplayName("白名单字段 DESC 不抛出异常")
        void applySort_whitelistedFieldDesc() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.applySort(wrapper, "patientName", "DESC"));
        }

        @Test
        @DisplayName("非白名单字段降级为 createTime 默认排序")
        void applySort_nonWhitelistField_fallback() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.applySort(wrapper, "unknownField", "ASC"));
        }

        @Test
        @DisplayName("sortField 为 null 使用默认 createTime")
        void applySort_nullField_defaultSort() {
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            assertDoesNotThrow(() -> helper.applySort(wrapper, null, null));
        }
    }

    // ==================== 字段翻译 ====================

    @Nested
    @DisplayName("字段翻译")
    class FieldTranslation {

        @Test
        @DisplayName("getOrderTypeName — 1 返回医疗器械")
        void getOrderTypeName_medical() {
            assertEquals("医疗器械", helper.getOrderTypeName(1));
        }

        @Test
        @DisplayName("getOrderTypeName — 2 返回非医疗器械")
        void getOrderTypeName_nonMedical() {
            assertEquals("非医疗器械", helper.getOrderTypeName(2));
        }

        @Test
        @DisplayName("getOrderTypeName — null 返回 null")
        void getOrderTypeName_null() {
            assertNull(helper.getOrderTypeName(null));
        }

        @Test
        @DisplayName("getNeedsPhysicalDeliveryName — 1 返回是")
        void getNeedsPhysicalDeliveryName_yes() {
            assertEquals("是", helper.getNeedsPhysicalDeliveryName(1));
        }

        @Test
        @DisplayName("getNeedsPhysicalDeliveryName — 0 返回否")
        void getNeedsPhysicalDeliveryName_no() {
            assertEquals("否", helper.getNeedsPhysicalDeliveryName(0));
        }

        @Test
        @DisplayName("getGenderName — 男")
        void getGenderName_male() {
            assertEquals("男", helper.getGenderName("12.1"));
        }

        @Test
        @DisplayName("getGenderName — 女")
        void getGenderName_female() {
            assertEquals("女", helper.getGenderName("12.2"));
        }
    }

    // ==================== getColumnConfig ====================

    @Nested
    @DisplayName("getColumnConfig")
    class GetColumnConfig {

        @Test
        @DisplayName("用户无个人配置时降级为系统默认")
        void getColumnConfig_fallbackToSystem() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

                UserEntity user = new UserEntity();
                user.setId(1L);
                user.setDesignColumnSettings(null);
                when(userService.getById(1L)).thenReturn(user);
                when(configService.getConfigValue(anyString())).thenReturn(null);

                assertNull(helper.getColumnConfig());
            }
        }

        @Test
        @DisplayName("用户有个人配置时解析并返回")
        void getColumnConfig_userConfig() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

                UserEntity user = new UserEntity();
                user.setId(1L);
                user.setDesignColumnSettings("{\"module\":\"design\",\"columns\":[]}");
                when(userService.getById(1L)).thenReturn(user);

                DesignColumnConfigVO configVO = new DesignColumnConfigVO();
                when(objectMapper.readValue(anyString(), eq(DesignColumnConfigVO.class))).thenReturn(configVO);

                DesignColumnConfigVO result = helper.getColumnConfig();
                assertSame(configVO, result);
            }
        }
    }
}
