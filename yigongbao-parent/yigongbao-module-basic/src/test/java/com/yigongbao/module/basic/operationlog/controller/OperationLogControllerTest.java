package com.yigongbao.module.basic.operationlog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.module.basic.operationlog.OperationLogTestApplication;
import com.yigongbao.module.basic.operationlog.service.OperationLogService;
import com.yigongbao.module.basic.operationlog.vo.OperationLogVO;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OperationLogTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("OperationLogController 接口测试")
class OperationLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationLogService operationLogService;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("page: 分页查询成功")
    void page_shouldSuccess() throws Exception {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(1L);
        vo.setModule("basic");
        vo.setOperation("CREATE");
        IPage<OperationLogVO> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(vo));
        when(operationLogService.pageLogs(any())).thenReturn(page);

        mockMvc.perform(get("/api/basic/operation-log/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].module").value("basic"));
    }

    @Test
    @DisplayName("export: 导出成功")
    void export_shouldSuccess() throws Exception {
        doNothing().when(operationLogService).exportLogs(any(), any());

        mockMvc.perform(get("/api/basic/operation-log/export"))
                .andExpect(status().isOk());
    }
}
