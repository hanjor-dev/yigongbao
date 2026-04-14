package com.yigongbao.module.basic.bodyPart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.BasicTestApplication;
import com.yigongbao.module.basic.bodyPart.service.BodyPartService;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartDetailVO;
import com.yigongbao.module.basic.bodyPart.vo.BodyPartVO;
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
@DisplayName("BodyPartController 接口测试")
class BodyPartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BodyPartService bodyPartService;

    private BodyPartVO buildTestVO(Long id, String name) {
        BodyPartVO vo = new BodyPartVO();
        vo.setId(id);
        vo.setName(name);
        vo.setCode("BP-" + String.format("%04d", id));
        vo.setSort(1);
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    private BodyPartDetailVO buildTestDetailVO(Long id, String name) {
        BodyPartDetailVO vo = new BodyPartDetailVO();
        vo.setId(id);
        vo.setName(name);
        vo.setCode("BP-" + String.format("%04d", id));
        vo.setSort(1);
        vo.setStatus(1);
        vo.setStatusName("正常");
        vo.setCreateTime(LocalDateTime.now());
        vo.setUpdateTime(LocalDateTime.now());
        return vo;
    }

    // ==================== list 测试 ====================

    @Nested
    @DisplayName("list 测试")
    class ListTests {

        @Test
        @DisplayName("list: 返回平级部位列表")
        void list_shouldReturnFlatList() throws Exception {
            BodyPartVO vo = buildTestVO(1L, "颅骨");
            when(bodyPartService.listAll()).thenReturn(List.of(vo));

            mockMvc.perform(post("/basic/body-part/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").value("颅骨"));
        }

        @Test
        @DisplayName("list: 空数据时返回空数组")
        void list_whenEmpty_shouldReturnEmptyArray() throws Exception {
            when(bodyPartService.listAll()).thenReturn(new ArrayList<>());

            mockMvc.perform(post("/basic/body-part/list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 测试")
    class GetByIdTests {

        @Test
        @DisplayName("getById: 存在数据时返回详情")
        void getById_whenExists_shouldReturnData() throws Exception {
            when(bodyPartService.getDetailById(1L)).thenReturn(buildTestDetailVO(1L, "颅骨"));

            mockMvc.perform(get("/basic/body-part/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("颅骨"))
                    .andExpect(jsonPath("$.data.code").value("BP-0001"));
        }

        @Test
        @DisplayName("getById: 数据不存在时返回错误码")
        void getById_whenNotExists_shouldReturnError() throws Exception {
            when(bodyPartService.getDetailById(999L))
                    .thenThrow(new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND));

            mockMvc.perform(get("/basic/body-part/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(662))
                    .andExpect(jsonPath("$.message").value("部位不存在"));
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
                    "name", "测试部位",
                    "sort", 1
            );

            mockMvc.perform(post("/basic/body-part")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("create: 名称为空时返回400")
        void create_whenNameBlank_shouldReturnError() throws Exception {
            Map<String, Object> requestBody = Map.of(
                    "name", ""
            );

            mockMvc.perform(post("/basic/body-part")
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
            mockMvc.perform(delete("/basic/body-part/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("delete: 数据不存在时返回错误码")
        void delete_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND))
                    .when(bodyPartService).removeBodyPart(999L);

            mockMvc.perform(delete("/basic/body-part/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(662));
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus: 修改状态成功")
        void updateStatus_shouldSuccess() throws Exception {
            mockMvc.perform(put("/basic/body-part/1/status")
                            .param("status", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("updateStatus: 状态值不合法时返回400")
        void updateStatus_whenInvalidStatus_shouldReturnError() throws Exception {
            mockMvc.perform(put("/basic/body-part/1/status")
                            .param("status", "99"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("must be less than or equal to 1"));
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
                    "name", "更新后的部位名称",
                    "sort", 2
            );

            mockMvc.perform(put("/basic/body-part/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }

        @Test
        @DisplayName("update: 数据不存在时返回错误码")
        void update_whenNotExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.BODY_PART_NOT_FOUND))
                    .when(bodyPartService).updateBodyPart(eq(999L), any());

            Map<String, Object> requestBody = Map.of(
                    "name", "测试部位"
            );

            mockMvc.perform(put("/basic/body-part/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(662))
                    .andExpect(jsonPath("$.message").value("部位不存在"));
        }

        @Test
        @DisplayName("update: 名称重复时返回错误码")
        void update_whenNameExists_shouldReturnError() throws Exception {
            doThrow(new BusinessException(ErrorCodeEnum.BODY_PART_NAME_EXISTS))
                    .when(bodyPartService).updateBodyPart(eq(1L), any());

            Map<String, Object> requestBody = Map.of(
                    "name", "重复部位名称"
            );

            mockMvc.perform(put("/basic/body-part/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(663));
        }
    }
}
