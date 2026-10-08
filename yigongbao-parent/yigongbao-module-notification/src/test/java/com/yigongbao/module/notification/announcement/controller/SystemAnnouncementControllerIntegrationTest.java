package com.yigongbao.module.notification.announcement.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;
import com.yigongbao.framework.aspect.PermissionAspect;
import com.yigongbao.framework.handler.GlobalExceptionHandler;
import com.yigongbao.module.notification.announcement.service.ISystemAnnouncementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统公告 Controller 集成测试。
 *
 * <p>测试边界为 HTTP 请求、Controller、权限切面和全局异常处理器；数据库及推送基础设施由
 * Service Mock 隔离，公告状态流转由 {@code SystemAnnouncementServiceImplTest} 覆盖。</p>
 */
@WebMvcTest(SystemAnnouncementController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import({SystemAnnouncementController.class, PermissionAspect.class, GlobalExceptionHandler.class})
@ContextConfiguration(classes = SystemAnnouncementControllerIntegrationTest.TestApplication.class)
@DisplayName("系统公告 Controller 集成测试")
class SystemAnnouncementControllerIntegrationTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISystemAnnouncementService announcementService;

    /**
     * 提供 WebMvcTest 所需的最小 Spring Boot 配置，避免加载完整生产应用上下文。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }

    @Test
    @DisplayName("发布公告：拥有发布权限时应调用发布服务并返回成功")
    void publish_withPermission_shouldInvokeService() throws Exception {
        try (var ignored = mockLoginWithPermissions("notification:announcement:publish")) {
            mockMvc.perform(post("/notification/announcements/42/publish"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)));
        }

        verify(announcementService).publish(eq(42L), eq(USER_ID), nullable(String.class));
    }

    @Test
    @DisplayName("撤回公告：拥有撤回权限时应调用撤回服务并返回成功")
    void revoke_withPermission_shouldInvokeService() throws Exception {
        try (var ignored = mockLoginWithPermissions("notification:announcement:revoke")) {
            mockMvc.perform(post("/notification/announcements/42/revoke"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)));
        }

        verify(announcementService).revoke(eq(42L), eq(USER_ID), nullable(String.class));
    }

    @Test
    @DisplayName("确认公告：普通登录用户无需后台权限即可确认自己的公告")
    void acknowledge_shouldInvokeServiceWithoutManagementPermission() throws Exception {
        try (var ignored = mockLoginWithPermissions()) {
            mockMvc.perform(put("/notification/announcements/42/acknowledge"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(200)));
        }

        verify(announcementService).acknowledge(42L, USER_ID);
    }

    @Test
    @DisplayName("权限校验：缺少发布权限时应拒绝请求且不调用服务")
    void publish_withoutPermission_shouldReturnForbidden() throws Exception {
        try (var ignored = mockLoginWithPermissions("notification:announcement:view")) {
            mockMvc.perform(post("/notification/announcements/42/publish"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(403)));
        }

        verify(announcementService, never()).publish(eq(42L), eq(USER_ID), nullable(String.class));
    }

    @Test
    @DisplayName("权限校验：缺少撤回权限时应拒绝请求且不调用服务")
    void revoke_withoutPermission_shouldReturnForbidden() throws Exception {
        try (var ignored = mockLoginWithPermissions("notification:announcement:view")) {
            mockMvc.perform(post("/notification/announcements/42/revoke"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is(403)));
        }

        verify(announcementService, never()).revoke(eq(42L), eq(USER_ID), nullable(String.class));
    }

    /**
     * 在请求进入 Controller 前建立真实 Sa-Token 登录会话，模拟登录接口向会话写入权限列表。
     */
    private org.mockito.MockedStatic<StpUtil> mockLoginWithPermissions(String... permissions) {
        org.mockito.MockedStatic<StpUtil> stpUtil = org.mockito.Mockito.mockStatic(StpUtil.class);
        SaSession session = mock(SaSession.class);
        when(session.get("permissions")).thenReturn(Arrays.asList(permissions));
        stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
        stpUtil.when(StpUtil::getSession).thenReturn(session);
        return stpUtil;
    }
}
