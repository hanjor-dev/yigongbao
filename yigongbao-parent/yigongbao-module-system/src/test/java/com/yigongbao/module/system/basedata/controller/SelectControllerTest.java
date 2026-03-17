package com.yigongbao.module.system.basedata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统一下拉查询接口测试
 * 测试 SelectController 提供的地区和字典下拉接口
 *
 * @author hanjor
 * @date 2026-03-17
 */
@SpringBootTest(classes = com.yigongbao.module.system.SystemTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("SelectController 接口测试")
class SelectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== 地区树形接口测试 ====================

    @Test
    @DisplayName("tree: 地区树形接口 - 查询省份列表（parentId=0）")
    void tree_area_whenProvinces_shouldReturnTreeStructure() throws Exception {
        // 父级行政代码 0 表示查询省份
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "area")
                        .param("parentId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].value").exists());
    }

    @Test
    @DisplayName("tree: 地区树形接口 - 查询某省下的市（parentId=330000）")
    void tree_area_whenCity_shouldReturnCityTree() throws Exception {
        // 浙江省 area_code = 330000
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "area")
                        .param("parentId", "330000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("tree: 地区树形接口 - 查询某市下的区（parentId=330100）")
    void tree_area_whenDistrict_shouldReturnDistrictTree() throws Exception {
        // 杭州市 area_code = 330100
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "area")
                        .param("parentId", "330100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("tree: 地区树形接口 - 无数据时返回空数组")
    void tree_area_whenNoData_shouldReturnEmptyArray() throws Exception {
        // 一个不存在的父级代码
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "area")
                        .param("parentId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== 地区下拉选项接口测试 ====================

    @Test
    @DisplayName("options: 地区下拉接口 - 查询省份下拉列表")
    void options_area_whenProvinces_shouldReturnOptions() throws Exception {
        mockMvc.perform(get("/api/system/select/options")
                        .param("type", "area")
                        .param("parentId", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("options: 地区下拉接口 - 查询某省下的市")
    void options_area_whenCities_shouldReturnOptions() throws Exception {
        mockMvc.perform(get("/api/system/select/options")
                        .param("type", "area")
                        .param("parentId", "330000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 字典树形接口测试 ====================

    @Test
    @DisplayName("tree: 字典树形接口 - 查询字典类型下的树")
    void tree_dict_whenExists_shouldReturnTree() throws Exception {
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "dict")
                        .param("code", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("机构类型"));
    }

    @Test
    @DisplayName("tree: 字典树形接口 - 缺少code参数时返回错误")
    void tree_dict_whenMissingCode_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "dict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 字典下拉选项接口测试 ====================

    @Test
    @DisplayName("options: 字典下拉接口 - 查询字典叶子节点")
    void options_dict_whenExists_shouldReturnOptions() throws Exception {
        mockMvc.perform(get("/api/system/select/options")
                        .param("type", "dict")
                        .param("code", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("options: 字典下拉接口 - 缺少code参数时返回错误")
    void options_dict_whenMissingCode_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/select/options")
                        .param("type", "dict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== 错误类型测试 ====================

    @Test
    @DisplayName("tree: 不支持的类型返回错误")
    void tree_whenUnsupportedType_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/select/tree")
                        .param("type", "unsupported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的数据类型：unsupported"));
    }

    @Test
    @DisplayName("options: 不支持的类型返回错误")
    void options_whenUnsupportedType_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/select/options")
                        .param("type", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的数据类型：unknown"));
    }
}
