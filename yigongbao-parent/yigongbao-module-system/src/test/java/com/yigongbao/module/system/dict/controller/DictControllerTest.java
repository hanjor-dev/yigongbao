package com.yigongbao.module.system.dict.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.dict.dto.CreateDictDTO;
import com.yigongbao.module.system.dict.dto.UpdateDictDTO;
import com.yigongbao.module.system.dict.entity.DictEntity;
import com.yigongbao.module.system.dict.mapper.DictMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 字典接口测试
 * 使用 MockMvc 进行 HTTP 接口测试
 *
 * @author hanjor
 * @date 2026-03-16
 */
@SpringBootTest(classes = com.yigongbao.module.system.SystemTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("字典接口测试")
class DictControllerTest {

    private static final Long ROOT_PARENT_ID = 0L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DictMapper dictMapper;

    private Long tempRootId;
    private Long tempChildId;

    @BeforeEach
    void setUp() {
        // 使用与初始数据不同的 dictCode，避免冲突
        DictEntity root = new DictEntity();
        root.setParentId(ROOT_PARENT_ID);
        root.setDictCode("100");
        root.setDictName("接口测试-根节点");
        root.setDictValue(null);
        root.setLevel(1);
        root.setSort(0);
        root.setStatus(1);
        root.setRemark("接口测试");
        dictMapper.insert(root);
        tempRootId = root.getId();

        DictEntity child = new DictEntity();
        child.setParentId(tempRootId);
        child.setDictCode("100.1");
        child.setDictName("接口测试-子节点");
        child.setDictValue("value");
        child.setLevel(2);
        child.setSort(0);
        child.setStatus(1);
        child.setRemark("接口测试");
        dictMapper.insert(child);
        tempChildId = child.getId();
    }

    @Test
    @DisplayName("listType: 查询字典类型列表")
    void listType_shouldReturnTypeList() throws Exception {
        mockMvc.perform(get("/api/system/dict/type/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("listByTypeCode: 根据类型编码查询字典数据")
    void listByTypeCode_shouldReturnDataList() throws Exception {
        mockMvc.perform(get("/api/system/dict/data/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("listByTypeCode: 类型不存在时返回错误码")
    void listByTypeCode_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/dict/data/NOT_EXISTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("listTree: 查询完整树形结构")
    void listTree_shouldReturnTreeStructure() throws Exception {
        mockMvc.perform(get("/api/system/dict/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("listTreeByTypeCode: 查询指定类型的树形结构")
    void listTreeByTypeCode_shouldReturnTreeStructure() throws Exception {
        mockMvc.perform(get("/api/system/dict/tree/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].dictCode").value("1"));
    }

    @Test
    @DisplayName("listOptions: 查询下拉选项（叶子节点）")
    void listOptions_shouldReturnLeafNodes() throws Exception {
        mockMvc.perform(get("/api/system/dict/options/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("getById: 查询字典详情")
    void getById_shouldReturnDict() throws Exception {
        mockMvc.perform(get("/api/system/dict/" + tempRootId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(tempRootId));
    }

    @Test
    @DisplayName("getById: 查询不存在的字典")
    void getById_whenNotExists_shouldReturnError() throws Exception {
        mockMvc.perform(get("/api/system/dict/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("create: 创建字典（根节点）")
    void create_shouldSuccess() throws Exception {
        CreateDictDTO dto = new CreateDictDTO();
        dto.setParentId(ROOT_PARENT_ID);
        dto.setDictName("接口测试-新增根节点");
        dto.setDictValue(null);
        dto.setSort(0);
        dto.setStatus(1);

        mockMvc.perform(post("/api/system/dict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        DictEntity created = dictMapper.selectOne(new LambdaQueryWrapper<DictEntity>()
                .eq(DictEntity::getParentId, ROOT_PARENT_ID)
                .eq(DictEntity::getDictName, "接口测试-新增根节点"));
        assertNotNull(created);
    }

    @Test
    @DisplayName("update: 更新字典")
    void update_shouldSuccess() throws Exception {
        UpdateDictDTO dto = new UpdateDictDTO();
        dto.setDictName("接口测试-更新后的名称");
        dto.setDictValue("updated_value");
        dto.setSort(5);
        dto.setStatus(1);

        mockMvc.perform(put("/api/system/dict/" + tempChildId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        DictEntity updated = dictMapper.selectById(tempChildId);
        assertNotNull(updated);
        assertEquals("接口测试-更新后的名称", updated.getDictName());
    }

    @Test
    @DisplayName("updateStatus: 更新字典状态")
    void updateStatus_shouldSuccess() throws Exception {
        mockMvc.perform(put("/api/system/dict/" + tempRootId + "/status")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        DictEntity updated = dictMapper.selectById(tempRootId);
        assertNotNull(updated);
        assertEquals(0, updated.getStatus());
    }

    @Test
    @DisplayName("remove: 删除字典（无子节点）")
    void remove_shouldSuccess() throws Exception {
        mockMvc.perform(delete("/api/system/dict/" + tempChildId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        DictEntity deleted = dictMapper.selectById(tempChildId);
        assertNull(deleted);
    }

    @Test
    @DisplayName("remove: 删除有子节点的字典")
    void remove_whenHasChildren_shouldReturnError() throws Exception {
        mockMvc.perform(delete("/api/system/dict/" + tempRootId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(200)));
    }
}

