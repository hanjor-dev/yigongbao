package com.yigongbao.module.system.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
import com.yigongbao.module.system.user.dto.AssignHospitalsDTO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户-医院关联 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-20
 */
@SpringBootTest(
    classes = com.yigongbao.module.system.SystemTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserHospitalController 接口测试")
class UserHospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserHospitalService userHospitalService;

    @MockBean
    private HospitalGroupTemplateService hospitalGroupTemplateService;

    private HospitalVO testHospital1;
    private HospitalVO testHospital2;

    @BeforeEach
    void setUp() {
        testHospital1 = new HospitalVO();
        testHospital1.setId(1L);
        testHospital1.setHospitalName("北京协和医院");
        testHospital1.setHospitalCode("HOS-001");

        testHospital2 = new HospitalVO();
        testHospital2.setId(2L);
        testHospital2.setHospitalName("上海市第一人民医院");
        testHospital2.setHospitalCode("HOS-002");
    }

    // ==================== getHospitals 测试 ====================

    /**
     * 测试用例：查询用户的医院列表 - 有数据时返回医院列表
     */
    @Test
    @DisplayName("getHospitals: 有数据时返回医院列表")
    void getHospitals_whenHasData_shouldReturnHospitals() throws Exception {
        when(userHospitalService.getHospitalsByUserId(1L))
                .thenReturn(List.of(testHospital1, testHospital2));

        mockMvc.perform(get("/system/user/1/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].hospitalName").value("北京协和医院"))
                .andExpect(jsonPath("$.data[1].hospitalName").value("上海市第一人民医院"));
    }

    /**
     * 测试用例：查询用户的医院列表 - 无数据时返回空数组
     */
    @Test
    @DisplayName("getHospitals: 无数据时返回空数组")
    void getHospitals_whenNoData_shouldReturnEmptyArray() throws Exception {
        when(userHospitalService.getHospitalsByUserId(1L))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/system/user/1/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== assignHospitals 测试 ====================

    /**
     * 测试用例：分配用户医院范围 - 成功场景
     */
    @Test
    @DisplayName("assignHospitals: 分配医院成功返回200")
    void assignHospitals_shouldSuccess() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("hospitalIds", List.of(1L, 2L));

        mockMvc.perform(put("/system/user/1/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：分配用户医院范围 - 空列表触发参数校验失败（@NotEmpty）
     */
    @Test
    @DisplayName("assignHospitals: 空列表触发参数校验返回400")
    void assignHospitals_whenEmptyList_shouldReturnBadRequest() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("hospitalIds", List.of());

        mockMvc.perform(put("/system/user/1/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest());
    }

    // ==================== getHospitalOptions 测试 ====================

    /**
     * 测试用例：获取可分配医院下拉选项 - 成功返回
     */
    @Test
    @DisplayName("getHospitalOptions: 返回可分配医院下拉列表")
    void getHospitalOptions_shouldReturnOptions() throws Exception {
        when(userHospitalService.getHospitalOptionsByUserId(1L))
                .thenReturn(List.of(testHospital1, testHospital2));

        mockMvc.perform(get("/system/user/1/hospitals/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].hospitalName").value("北京协和医院"));
    }

    // ==================== previewTemplate 测试 ====================

    /**
     * 测试用例：预览模板包含的医院 - 成功返回模板详情
     */
    @Test
    @DisplayName("previewTemplate: 预览模板成功返回200")
    void previewTemplate_shouldSuccess() throws Exception {
        HospitalGroupTemplateVO template = new HospitalGroupTemplateVO();
        template.setId(1L);
        template.setTemplateName("北京市医院联盟");
        template.setTemplateCode("TPL-HOS-001");
        template.setHospitalCount(2);

        when(hospitalGroupTemplateService.getTemplateById(1L))
                .thenReturn(template);

        mockMvc.perform(get("/system/user/1/hospitals/template/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.templateName").value("北京市医院联盟"))
                .andExpect(jsonPath("$.data.templateCode").value("TPL-HOS-001"))
                .andExpect(jsonPath("$.data.hospitalCount").value(2));
    }
}
