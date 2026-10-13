package com.yigongbao.module.system.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.module.system.user.dto.UserPageDTO;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.vo.ManagedOrgSimpleVO;
import com.yigongbao.module.system.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户管理机构 JSON 响应契约")
class UserControllerManagedOrgResponseTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("区域管理员详情按快照顺序返回管理机构对象")
    void getById_regionalManager_shouldSerializeManagedOrgsInOrder() throws Exception {
        UserVO vo = new UserVO();
        vo.setId(1L);
        vo.setManagedOrgs(List.of(
                managedOrg(20L, "经销商甲"),
                managedOrg(30L, "")
        ));
        when(userService.getUserById(1L)).thenReturn(vo);

        mockMvc.perform(get("/system/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managedOrgs[0].id").value(20))
                .andExpect(jsonPath("$.data.managedOrgs[0].orgName").value("经销商甲"))
                .andExpect(jsonPath("$.data.managedOrgs[1].id").value(30))
                .andExpect(jsonPath("$.data.managedOrgs[1].orgName").value(""));
    }

    @Test
    @DisplayName("非区域管理员详情显式返回空数组")
    void getById_nonRegionalManager_shouldSerializeEmptyManagedOrgs() throws Exception {
        UserVO vo = new UserVO();
        vo.setId(1L);
        vo.setManagedOrgs(Collections.emptyList());
        when(userService.getUserById(1L)).thenReturn(vo);

        mockMvc.perform(get("/system/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.managedOrgs").isArray())
                .andExpect(jsonPath("$.data.managedOrgs").isEmpty());
    }

    @Test
    @DisplayName("账户列表不序列化 null 的 managedOrgs")
    void list_shouldOmitNullManagedOrgs() throws Exception {
        UserVO vo = new UserVO();
        vo.setId(1L);
        Page<UserVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1);
        when(userService.listUser(any(UserPageDTO.class))).thenReturn(page);

        mockMvc.perform(post("/system/user/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].managedOrgs").doesNotExist());
    }

    private ManagedOrgSimpleVO managedOrg(Long id, String orgName) {
        ManagedOrgSimpleVO org = new ManagedOrgSimpleVO();
        org.setId(id);
        org.setOrgName(orgName);
        return org;
    }
}
