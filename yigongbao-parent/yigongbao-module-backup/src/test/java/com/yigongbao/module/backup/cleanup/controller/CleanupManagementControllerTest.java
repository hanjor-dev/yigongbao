package com.yigongbao.module.backup.cleanup.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.framework.handler.GlobalExceptionHandler;
import com.yigongbao.module.backup.cleanup.CleanupTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CleanupManagementControllerTest {

    private CleanupTaskService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = new CleanupTaskService();
        mockMvc = MockMvcBuilders.standaloneSetup(new CleanupManagementController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void adminCanAccessFrontendEndpointWithStandardResultEnvelope() throws Exception {
        try (MockedStatic<StpUtil> ignored = role("admin")) {
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Test
    void companyAdminCanAccessFrontendEndpoint() throws Exception {
        try (MockedStatic<StpUtil> ignored = role("company-admin")) {
            mockMvc.perform(post("/api/backup/cleanup/v1/dry-runs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"olderThanDays\":180}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.status").value("DRY_RUN"));
        }
    }

    @Test
    void nonManagementRoleIsRejectedWithForbiddenResponse() throws Exception {
        try (MockedStatic<StpUtil> ignored = role("designer")) {
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Test
    void frontendApiPathsRemainMappedToManagementController() throws Exception {
        CleanupTaskService.Task task = service.create(30, List.of("cos/object-a"));

        try (MockedStatic<StpUtil> ignored = role("admin")) {
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/dry-runs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"olderThanDays\":30}"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks/{id}", task.id()))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/tasks/{id}/pause", task.id()))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/tasks/{id}/resume", task.id()))
                    .andExpect(status().isOk());
        }
    }

    private MockedStatic<StpUtil> role(String roleCode) {
        SaSession session = mock(SaSession.class);
        when(session.get("roleCode")).thenReturn(roleCode);
        MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
        stp.when(StpUtil::getSession).thenReturn(session);
        return stp;
    }
}
