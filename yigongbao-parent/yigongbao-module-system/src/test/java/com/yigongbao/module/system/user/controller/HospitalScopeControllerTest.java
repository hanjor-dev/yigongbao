package com.yigongbao.module.system.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 医院权限范围 Controller 接口测试
 *
 * @author hanjor
 * @date 2026-03-20
 */
@SpringBootTest(classes = com.yigongbao.module.system.SystemTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HospitalScopeController 接口测试")
class HospitalScopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleService roleService;

    @MockBean
    private UserHospitalService userHospitalService;

    private UserEntity testUser;
    private RoleEntity testRole;
    private HospitalVO testHospital;

    @BeforeEach
    void setUp() {
        // 准备测试用户
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRealName("测试用户");
        testUser.setOrgId(1L);
        testUser.setRoleId(1L);

        // 准备测试角色
        testRole = new RoleEntity();
        testRole.setId(1L);
        testRole.setRoleName("业务员");
        testRole.setRoleCode("salesman");
        testRole.setDataScopeType("hospitals"); // 医院范围权限

        // 准备测试医院
        testHospital = new HospitalVO();
        testHospital.setId(1L);
        testHospital.setHospitalName("测试医院");
        testHospital.setHospitalCode("HOS-001");
    }

    /**
     * 测试用例：用户不存在时返回空列表
     */
    @Test
    @DisplayName("getMyHospitals: 用户不存在时返回空列表")
    void getMyHospitals_whenUserNotExists_shouldReturnEmptyList() throws Exception {
        when(userMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试用例：用户没有角色时返回空列表
     */
    @Test
    @DisplayName("getMyHospitals: 用户没有角色时返回空列表")
    void getMyHospitals_whenNoRole_shouldReturnEmptyList() throws Exception {
        testUser.setRoleId(null);
        when(userMapper.selectById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试用例：角色的 dataScopeType=hospitals 时返回用户关联的医院
     */
    @Test
    @DisplayName("getMyHospitals: dataScopeType=hospitals 返回用户关联医院")
    void getMyHospitals_whenDataScopeTypeHospitals_shouldReturnUserHospitals() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(1L)).thenReturn(testRole);
        when(userHospitalService.getHospitalsByUserId(1L))
                .thenReturn(List.of(testHospital));

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].hospitalName").value("测试医院"));
    }

    /**
     * 测试用例：角色的 dataScopeType!=hospitals 时返回空列表
     */
    @Test
    @DisplayName("getMyHospitals: dataScopeType=org 返回空列表")
    void getMyHospitals_whenDataScopeTypeOrg_shouldReturnEmptyList() throws Exception {
        testRole.setDataScopeType("org");
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(1L)).thenReturn(testRole);

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试用例：用户没有关联医院时返回空列表
     */
    @Test
    @DisplayName("getMyHospitals: 用户没有关联医院时返回空列表")
    void getMyHospitals_whenNoHospitals_shouldReturnEmptyList() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(1L)).thenReturn(testRole);
        when(userHospitalService.getHospitalsByUserId(1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    /**
     * 测试用例：角色不存在时返回空列表
     */
    @Test
    @DisplayName("getMyHospitals: 角色不存在时返回空列表")
    void getMyHospitals_whenRoleNotExists_shouldReturnEmptyList() throws Exception {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(roleService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/system/hospital-scope/my-hospitals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
