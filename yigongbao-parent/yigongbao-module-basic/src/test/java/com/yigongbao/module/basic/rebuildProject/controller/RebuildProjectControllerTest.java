package com.yigongbao.module.basic.rebuildProject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.rebuildProject.dto.CreateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.dto.UpdateRebuildProjectDTO;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.basic.rebuildProject.vo.ProjectOptionItemVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectDetailVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectOptionVO;
import com.yigongbao.module.basic.rebuildProject.vo.RebuildProjectVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BasicTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RebuildProjectController 接口测试")
class RebuildProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RebuildProjectService rebuildProjectService;

    private RebuildProjectVO buildTestVO(Long id, String name, Long bodyPartId) {
        RebuildProjectVO vo = new RebuildProjectVO();
        vo.setId(id);
        vo.setBodyPartId(bodyPartId);
        vo.setBodyPartName("头部");
        vo.setParentId(0L);
        vo.setName(name);
        vo.setCode("RP_" + bodyPartId + "_001");
        vo.setLevel(1);
        vo.setStandardPrice(new BigDecimal("5000.00"));
        vo.setUrgentPrice(new BigDecimal("7500.00"));
        vo.setCategory("模型");
        vo.setEstimatedHours(new BigDecimal("8.5"));
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private RebuildProjectDetailVO buildTestDetailVO(Long id, String name, Long bodyPartId) {
        RebuildProjectDetailVO vo = new RebuildProjectDetailVO();
        vo.setId(id);
        vo.setBodyPartId(bodyPartId);
        vo.setBodyPartName("头部");
        vo.setParentId(0L);
        vo.setName(name);
        vo.setCode("RP_" + bodyPartId + "_001");
        vo.setLevel(1);
        vo.setStandardPrice(new BigDecimal("5000.00"));
        vo.setUrgentPrice(new BigDecimal("7500.00"));
        vo.setCategory("模型");
        vo.setEstimatedHours(new BigDecimal("8.5"));
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== tree 测试 ====================

    @Nested
    @DisplayName("tree 测试")
    class TreeTests {

        @Test
        @DisplayName("tree: 返回项目树形结构")
        void tree_shouldReturnTreeStructure() throws Exception {
            RebuildProjectVO parent = buildTestVO(1L, "颅骨重建", 1L);
            parent.setChildren(List.of(buildTestVO(2L, "颞骨重建", 1L)));
            when(rebuildProjectService.listTree(null)).thenReturn(List.of(parent));

            mockMvc.perform(get("/basic/rebuild-project/tree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data[0].name").value("颅骨重建"))
                    .andExpect(jsonPath("$.data[0].children[0].name").value("颞骨重建"));
        }

        @Test
        @DisplayName("tree: 空数据时返回空数组")
        void tree_whenEmpty_shouldReturnEmptyArray() throws Exception {
            when(rebuildProjectService.listTree(null)).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/basic/rebuild-project/tree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("tree: 按分类筛选")
        void tree_withCategory_shouldFilter() throws Exception {
            RebuildProjectVO project = buildTestVO(1L, "颅骨重建", 1L);
            project.setCategory("模型");
            when(rebuildProjectService.listTree("模型")).thenReturn(List.of(project));

            mockMvc.perform(get("/basic/rebuild-project/tree")
                            .param("category", "模型"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].category").value("模型"));
        }
    }

    // ==================== byBodyPart 测试 ====================

    @Nested
    @DisplayName("byBodyPart 测试")
    class ByBodyPartTests {

        @Test
        @DisplayName("byBodyPart: 返回该部位下的项目列表")
        void byBodyPart_shouldReturnProjects() throws Exception {
            when(rebuildProjectService.listByBodyPartId(1L, null)).thenReturn(List.of(buildTestVO(1L, "颅骨重建", 1L)));

            mockMvc.perform(get("/basic/rebuild-project/by-body-part/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].name").value("颅骨重建"));
        }

        @Test
        @DisplayName("byBodyPart: 部位不存在时返回错误")
        void byBodyPart_whenNotExists_shouldReturnError() throws Exception {
            when(rebuildProjectService.listByBodyPartId(999L, null))
                    .thenThrow(new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND));

            mockMvc.perform(get("/basic/rebuild-project/by-body-part/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(662));
        }

        @Test
        @DisplayName("byBodyPart: 按分类筛选")
        void byBodyPart_withCategory_shouldFilter() throws Exception {
            RebuildProjectVO project = buildTestVO(1L, "颅骨重建", 1L);
            project.setCategory("导板");
            when(rebuildProjectService.listByBodyPartId(1L, "导板")).thenReturn(List.of(project));

            mockMvc.perform(get("/basic/rebuild-project/by-body-part/1")
                            .param("category", "导板"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].category").value("导板"));
        }
    }

    // ==================== options 测试 ====================

    @Nested
    @DisplayName("options 测试")
    class OptionsTests {

        @Test
        @DisplayName("options: 返回下拉选项")
        void options_shouldReturnOptions() throws Exception {
            RebuildProjectOptionVO option = new RebuildProjectOptionVO();
            option.setBodyPartId(1L);
            option.setBodyPartName("头部");
            option.setChildren(new ArrayList<>());
            when(rebuildProjectService.listOptions(null, null)).thenReturn(List.of(option));

            mockMvc.perform(get("/basic/rebuild-project/options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data[0].bodyPartName").value("头部"));
        }

        @Test
        @DisplayName("options: 按部位筛选")
        void options_withBodyPartId_shouldFilter() throws Exception {
            when(rebuildProjectService.listOptions(1L, null)).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/basic/rebuild-project/options")
                            .param("bodyPartId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("options: 按分类筛选")
        void options_withCategory_shouldFilter() throws Exception {
            RebuildProjectOptionVO option = new RebuildProjectOptionVO();
            option.setBodyPartId(1L);
            option.setBodyPartName("头部");
            ProjectOptionItemVO item = new ProjectOptionItemVO();
            item.setId(1L);
            item.setParentId(0L);
            item.setName("颅骨重建");
            item.setLevel(1);
            item.setChildren(new ArrayList<>());
            option.setChildren(List.of(item));
            when(rebuildProjectService.listOptions(null, "模型")).thenReturn(List.of(option));

            mockMvc.perform(get("/basic/rebuild-project/options")
                            .param("category", "模型"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("options: 按部位和分类联合筛选")
        void options_withBodyPartIdAndCategory_shouldFilter() throws Exception {
            when(rebuildProjectService.listOptions(1L, "模型")).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/basic/rebuild-project/options")
                            .param("bodyPartId", "1")
                            .param("category", "模型"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在数据时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(rebuildProjectService.getDetailById(1L)).thenReturn(buildTestDetailVO(1L, "颅骨重建", 1L));

            mockMvc.perform(get("/basic/rebuild-project/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("颅骨重建"));
        }

        @Test
        @DisplayName("getById: 数据不存在时返回错误")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(rebuildProjectService.getDetailById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND));

            mockMvc.perform(get("/basic/rebuild-project/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(670));
        }
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 测试")
    class CreateTests {

        @Test
        @DisplayName("create: 创建成功返回200")
        void create_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "bodyPartId", 1,
                    "parentId", 0,
                    "name", "测试项目",
                    "standardPrice", 5000.00,
                    "category", "模型"
            );

            mockMvc.perform(post("/basic/rebuild-project")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("create: 缺少必填参数时返回400")
        void create_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "name", "测试项目"
            );

            mockMvc.perform(post("/basic/rebuild-project")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 测试")
    class UpdateTests {

        @Test
        @DisplayName("update: 更新成功返回200")
        void update_shouldSuccess() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "bodyPartId", 1,
                    "parentId", 0,
                    "name", "更新后的项目",
                    "standardPrice", 6000.00
            );

            mockMvc.perform(put("/basic/rebuild-project/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND))
                    .when(rebuildProjectService).updateProject(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "bodyPartId", 1,
                    "parentId", 0,
                    "name", "测试项目"
            );

            mockMvc.perform(put("/basic/rebuild-project/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(670))
                    .andExpect(jsonPath("$.message").value("项目不存在"));
        }

        @Test
        @DisplayName("update: 缺少必填参数时返回400")
        void update_whenMissingRequiredParam_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "standardPrice", 5000.00
            );

            mockMvc.perform(put("/basic/rebuild-project/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 测试")
    class DeleteTests {

        @Test
        @DisplayName("delete: 删除成功返回200")
        void delete_shouldSuccess() throws Exception {
            mockMvc.perform(delete("/basic/rebuild-project/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("delete: 存在子项目时返回错误")
        void delete_whenHasChildren_shouldReturnError() throws Exception {
            // removeProject 为 void，不能使用 when(...)，需使用 doThrow
            doThrow(new BusinessException(ErrorCodeEnum.DATA_HAS_CHILDREN))
                    .when(rebuildProjectService).removeProject(1L);

            mockMvc.perform(delete("/basic/rebuild-project/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(605));
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() throws Exception {
            mockMvc.perform(put("/basic/rebuild-project/1/status")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("updateStatus: 状态值不合法时返回400")
        void updateStatus_whenInvalidStatus_shouldReturnError() throws Exception {
            mockMvc.perform(put("/basic/rebuild-project/1/status")
                            .param("status", "99"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("must be less than or equal to 1"));
        }

        @Test
        @DisplayName("updateStatus: 数据不存在时返回错误")
        void updateStatus_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.REBUILD_PROJECT_NOT_FOUND))
                    .when(rebuildProjectService).updateStatus(eq(999L), any(Integer.class));

            mockMvc.perform(put("/basic/rebuild-project/999/status")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(670))
                    .andExpect(jsonPath("$.message").value("项目不存在"));
        }
    }
}
