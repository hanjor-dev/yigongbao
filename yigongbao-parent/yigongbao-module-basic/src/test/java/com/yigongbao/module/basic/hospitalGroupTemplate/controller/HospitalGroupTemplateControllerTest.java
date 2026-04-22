package com.yigongbao.module.basic.hospitalGroupTemplate.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.UpdateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateDetailVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医院组合模板 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-20
 */
@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HospitalGroupTemplateController 接口测试")
class HospitalGroupTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HospitalGroupTemplateService templateService;

    private HospitalGroupTemplateVO buildTemplateVO(Long id, String name) {
        HospitalGroupTemplateVO vo = new HospitalGroupTemplateVO();
        vo.setId(id);
        vo.setTemplateName(name);
        vo.setTemplateCode("TPL-HOS-" + String.format("%03d", id));
        vo.setTemplateDesc("测试模板描述");
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setRemark("测试备注");
        vo.setHospitalCount(3);
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    private HospitalGroupTemplateSimpleVO buildSimpleVO(Long id, String name) {
        HospitalGroupTemplateSimpleVO vo = new HospitalGroupTemplateSimpleVO();
        vo.setId(id);
        vo.setTemplateName(name);
        vo.setTemplateCode("TPL-HOS-" + String.format("%03d", id));
        vo.setHospitalCount(2);
        return vo;
    }

    // ==================== list 测试 ====================

    /**
     * 测试用例：分页查询模板列表 - 成功场景
     */
    @Test
    @DisplayName("list: 分页查询成功返回200")
    void list_shouldReturnPageData() throws Exception {
        IPage<HospitalGroupTemplateVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildTemplateVO(1L, "北京市医院联盟"), buildTemplateVO(2L, "华东地区医院群")));
        page.setTotal(2);
        when(templateService.listTemplate(any())).thenReturn(page);

        mockMvc.perform(get("/basic/hospital-group-template/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].templateName").value("北京市医院联盟"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    /**
     * 测试用例：分页查询模板列表 - 带名称筛选
     */
    @Test
    @DisplayName("list: 带名称筛选返回过滤结果")
    void list_withTemplateName_shouldReturnFilteredData() throws Exception {
        IPage<HospitalGroupTemplateVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildTemplateVO(1L, "北京市医院联盟")));
        page.setTotal(1);
        when(templateService.listTemplate(any())).thenReturn(page);

        mockMvc.perform(get("/basic/hospital-group-template/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("templateName", "北京"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].templateName").value("北京市医院联盟"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ==================== getById 测试 ====================

    /**
     * 测试用例：根据ID查询模板详情 - 成功场景
     */
    @Test
    @DisplayName("getById: 存在数据时返回模板详情")
    void getById_whenExists_shouldReturnData() throws Exception {
        HospitalGroupTemplateVO vo = buildTemplateVO(1L, "北京市医院联盟");
        vo.setDetails(new ArrayList<>());
        when(templateService.getTemplateById(1L)).thenReturn(vo);

        mockMvc.perform(get("/basic/hospital-group-template/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.templateName").value("北京市医院联盟"))
                .andExpect(jsonPath("$.data.templateCode").value("TPL-HOS-001"));
    }

    /**
     * 测试用例：根据ID查询模板详情 - 数据不存在
     */
    @Test
    @DisplayName("getById: 数据不存在时返回错误码")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        when(templateService.getTemplateById(999L))
                .thenThrow(new BusinessException(ErrorCodeEnum.TEMPLATE_NOT_FOUND));

        mockMvc.perform(get("/basic/hospital-group-template/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(656))
                .andExpect(jsonPath("$.message").value("医院组合模板不存在"));
    }

    // ==================== create 测试 ====================

    /**
     * 测试用例：创建模板 - 成功场景
     */
    @Test
    @DisplayName("create: 创建成功返回200")
    void create_shouldSuccess() throws Exception {
        mockMvc.perform(post("/basic/hospital-group-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateName", "新模板",
                                "templateDesc", "新模板描述",
                                "hospitalIds", List.of(1L, 2L),
                                "remark", "新模板备注"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：创建模板 - 参数校验失败（模板名称为空）
     */
    @Test
    @DisplayName("create: 模板名称为空时返回400")
    void create_whenNameBlank_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/basic/hospital-group-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hospitalIds", List.of(1L, 2L)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用例：创建模板 - 参数校验失败（医院列表为空）
     */
    @Test
    @DisplayName("create: 医院列表为空时返回400")
    void create_whenHospitalIdsEmpty_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/basic/hospital-group-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateName", "新模板"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== update 测试 ====================

    /**
     * 测试用例：更新模板 - 成功场景
     */
    @Test
    @DisplayName("update: 更新成功返回200")
    void update_shouldSuccess() throws Exception {
        mockMvc.perform(put("/basic/hospital-group-template/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "templateName", "更新后的模板名称",
                                "hospitalIds", List.of(1L)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    // ==================== remove 测试 ====================

    /**
     * 测试用例：删除模板 - 成功场景
     */
    @Test
    @DisplayName("remove: 删除成功返回200")
    void remove_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/basic/hospital-group-template/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    // ==================== updateStatus 测试 ====================

    /**
     * 测试用例：修改模板状态 - 成功场景
     */
    @Test
    @DisplayName("updateStatus: 修改状态成功返回200")
    void updateStatus_shouldSuccess() throws Exception {
        mockMvc.perform(put("/basic/hospital-group-template/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：修改模板状态 - 状态值超范围时返回400
     */
    @Test
    @DisplayName("updateStatus: 状态值超范围时返回400")
    void updateStatus_whenInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/basic/hospital-group-template/1/status")
                        .param("status", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== options 测试 ====================

    /**
     * 测试用例：获取模板下拉选项 - 成功场景
     */
    @Test
    @DisplayName("options: 获取下拉选项成功返回200")
    void options_shouldReturnOptions() throws Exception {
        when(templateService.listOptions(any()))
                .thenReturn(List.of(
                        buildSimpleVO(1L, "北京市医院联盟"),
                        buildSimpleVO(2L, "华东地区医院群")
                ));

        mockMvc.perform(get("/basic/hospital-group-template/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].templateName").value("北京市医院联盟"));
    }
}
