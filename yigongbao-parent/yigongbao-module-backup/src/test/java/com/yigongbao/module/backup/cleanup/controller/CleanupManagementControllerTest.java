package com.yigongbao.module.backup.cleanup.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.framework.handler.GlobalExceptionHandler;
import com.yigongbao.module.backup.cleanup.CleanupTaskService;
import com.yigongbao.module.backup.cleanup.mapper.CleanupItemMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupPermitMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupPolicyMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupSnapshotMapper;
import com.yigongbao.module.backup.cleanup.mapper.CleanupTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CleanupManagementControllerTest {

    private CleanupTaskService service;
    private MockMvc mockMvc;
    private CleanupTaskMapper taskMapper;
    private CleanupItemMapper itemMapper;

    @BeforeEach
    void setUp() {
        taskMapper = mock(CleanupTaskMapper.class);
        itemMapper = mock(CleanupItemMapper.class);
        service = new CleanupTaskService(taskMapper, itemMapper,
                mock(CleanupPolicyMapper.class), mock(CleanupSnapshotMapper.class), mock(CleanupPermitMapper.class));
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
        var task = new com.yigongbao.module.backup.cleanup.entity.CleanupTaskEntity();
        task.setId(1L);
        task.setTaskNo("task-1");
        task.setTaskStatus("RUNNING");
        task.setVersion(0);
        when(taskMapper.selectByTaskNo("task-1")).thenReturn(task);
        when(itemMapper.selectByTaskId(1L)).thenReturn(List.of());
        when(taskMapper.updateStatus("task-1", "RUNNING", "PAUSED", 0)).thenReturn(1);
        when(taskMapper.updateStatus("task-1", "PAUSED", "RUNNING", 1)).thenReturn(1);

        try (MockedStatic<StpUtil> ignored = role("admin")) {
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/dry-runs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"olderThanDays\":30}"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/api/backup/cleanup/v1/tasks/{id}", "task-1"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/tasks/{id}/pause", "task-1"))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/api/backup/cleanup/v1/tasks/{id}/resume", "task-1"))
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
