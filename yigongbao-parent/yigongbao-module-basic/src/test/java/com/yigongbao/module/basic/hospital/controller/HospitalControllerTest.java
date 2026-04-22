package com.yigongbao.module.basic.hospital.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医院管理 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-20
 */
@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HospitalController 接口测试")
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HospitalService hospitalService;

    private HospitalVO buildTestVO(Long id, String name) {
        HospitalVO vo = new HospitalVO();
        vo.setId(id);
        vo.setHospitalName(name);
        vo.setHospitalCode("HOS-" + String.format("%03d", id));
        vo.setAreaId(111L);
        vo.setAreaName("东城区");
        vo.setFullAreaName("中国,北京,北京市,东城区");
        vo.setHospitalLevel("3.1");
        vo.setHospitalLevelName("三甲");
        vo.setHospitalType("4.1");
        vo.setHospitalTypeName("综合");
        vo.setContact("张医生");
        vo.setPhone("13800138001");
        vo.setEmail("info@test.com");
        vo.setAddress("测试地址");
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== list 测试 ====================

    /**
     * 测试用例：分页查询医院列表 - 成功场景
     */
    @Test
    @DisplayName("list: 分页查询成功返回200")
    void list_shouldReturnPageData() throws Exception {
        IPage<HospitalVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildTestVO(1L, "北京协和医院"), buildTestVO(2L, "上海市第一人民医院")));
        page.setTotal(2);
        when(hospitalService.listHospital(any()))
                .thenReturn(page);

        mockMvc.perform(get("/basic/hospital/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].hospitalName").value("北京协和医院"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    /**
     * 测试用例：分页查询医院列表 - 带条件筛选
     */
    @Test
    @DisplayName("list: 带医院名称筛选返回过滤结果")
    void list_withHospitalName_shouldReturnFilteredData() throws Exception {
        IPage<HospitalVO> page = new Page<>(1, 10);
        page.setRecords(List.of(buildTestVO(1L, "北京协和医院")));
        page.setTotal(1);
        when(hospitalService.listHospital(any()))
                .thenReturn(page);

        mockMvc.perform(get("/basic/hospital/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("hospitalName", "协和"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].hospitalName").value("北京协和医院"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    // ==================== getById 测试 ====================

    /**
     * 测试用例：根据ID查询医院详情 - 成功场景
     */
    @Test
    @DisplayName("getById: 存在数据时返回医院详情")
    void getById_whenExists_shouldReturnData() throws Exception {
        when(hospitalService.getHospitalById(1L)).thenReturn(buildTestVO(1L, "北京协和医院"));

        mockMvc.perform(get("/basic/hospital/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.hospitalName").value("北京协和医院"))
                .andExpect(jsonPath("$.data.hospitalCode").value("HOS-001"));
    }

    /**
     * 测试用例：根据ID查询医院详情 - 数据不存在
     */
    @Test
    @DisplayName("getById: 数据不存在时返回错误码")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        when(hospitalService.getHospitalById(999L))
                .thenThrow(new com.yigongbao.common.exception.BusinessException(
                        com.yigongbao.common.enums.ErrorCodeEnum.HOSPITAL_NOT_FOUND));

        mockMvc.perform(get("/basic/hospital/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(650))
                .andExpect(jsonPath("$.message").value("医院不存在"));
    }

    // ==================== create 测试 ====================

    /**
     * 测试用例：创建医院 - 成功场景
     */
    @Test
    @DisplayName("create: 创建成功返回200")
    void create_shouldSuccess() throws Exception {
        when(hospitalService.listHospital(any()))
                .thenReturn(new Page<>(1, 10));

        mockMvc.perform(post("/basic/hospital")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hospitalName", "测试医院",
                                "areaId", 111L,
                                "contact", "李医生",
                                "phone", "13900139001"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：创建医院 - 参数校验失败（医院名称为空）
     */
    @Test
    @DisplayName("create: 医院名称为空时返回400")
    void create_whenNameBlank_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/basic/hospital")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "areaId", 111L,
                                "contact", "李医生",
                                "phone", "13900139001"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 测试用例：创建医院 - 手机号格式错误
     */
    @Test
    @DisplayName("create: 手机号格式错误时返回400")
    void create_whenInvalidPhone_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/basic/hospital")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hospitalName", "测试医院",
                                "areaId", 111L,
                                "contact", "李医生",
                                "phone", "12345"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== update 测试 ====================

    /**
     * 测试用例：更新医院 - 成功场景
     */
    @Test
    @DisplayName("update: 更新成功返回200")
    void update_shouldSuccess() throws Exception {
        when(hospitalService.getHospitalById(1L)).thenReturn(buildTestVO(1L, "北京协和医院"));

        mockMvc.perform(put("/basic/hospital/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hospitalName", "北京协和医院-分院",
                                "contact", "王医生"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    // ==================== updateStatus 测试 ====================

    /**
     * 测试用例：修改医院状态 - 成功场景
     */
    @Test
    @DisplayName("updateStatus: 修改状态成功返回200")
    void updateStatus_shouldSuccess() throws Exception {
        mockMvc.perform(put("/basic/hospital/1/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));
    }

    /**
     * 测试用例：修改医院状态 - 状态值超范围时返回400
     */
    @Test
    @DisplayName("updateStatus: 状态值超范围时返回400")
    void updateStatus_whenInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(put("/basic/hospital/1/status")
                        .param("status", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== options 测试 ====================

    /**
     * 测试用例：获取医院下拉选项 - 成功场景
     */
    @Test
    @DisplayName("options: 获取下拉选项成功返回200")
    void options_shouldReturnOptions() throws Exception {
        when(hospitalService.listOptions(any()))
                .thenReturn(List.of(
                        buildTestVO(1L, "北京协和医院"),
                        buildTestVO(2L, "上海市第一人民医院")
                ));

        mockMvc.perform(get("/basic/hospital/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].hospitalName").value("北京协和医院"));
    }
}
