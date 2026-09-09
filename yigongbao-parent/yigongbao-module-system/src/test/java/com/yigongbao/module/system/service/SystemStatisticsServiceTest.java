package com.yigongbao.module.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dept.dto.DeptStatisticsQueryDTO;
import com.yigongbao.module.system.dept.entity.DeptEntity;
import com.yigongbao.module.system.dept.mapper.DeptMapper;
import com.yigongbao.module.system.dept.service.impl.DeptServiceImpl;
import com.yigongbao.module.system.dept.vo.DeptStatisticsVO;
import com.yigongbao.module.system.org.dto.OrgStatisticsQueryDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.service.impl.OrgServiceImpl;
import com.yigongbao.module.system.org.vo.OrgStatisticsVO;
import com.yigongbao.module.system.user.dto.UserStatisticsQueryDTO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemStatisticsServiceTest {

    @Mock private OrgMapper orgMapper;
    @Mock private DeptMapper deptMapper;
    @Mock private UserMapper userMapper;
    @Mock private ConfigService configService;

    @InjectMocks private OrgServiceImpl orgService;
    @InjectMocks private DeptServiceImpl deptService;
    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void injectBaseMappers() throws Exception {
        setBaseMapper(orgService, orgMapper);
        setBaseMapper(deptService, deptMapper);
        setBaseMapper(userService, userMapper);
        org.mockito.Mockito.lenient().when(configService.getConfigValue(any())).thenReturn(null);
    }

    @Test
    void orgStatistics_returnsTypeCounts() {
        OrgStatisticsVO expected = new OrgStatisticsVO();
        expected.setTotal(8L);
        expected.setDistributor(2L);
        expected.setServiceProvider(3L);
        expected.setMedicalInstitution(3L);
        when(orgMapper.selectStatistics(any(Wrapper.class))).thenReturn(expected);

        OrgStatisticsVO actual = orgService.getStatistics(new OrgStatisticsQueryDTO());

        assertEquals(8L, actual.getTotal());
        assertEquals(2L, actual.getDistributor());
        assertEquals(3L, actual.getServiceProvider());
        assertEquals(3L, actual.getMedicalInstitution());
    }

    @Test
    void deptStatistics_returnsTypeCounts() {
        DeptStatisticsVO expected = new DeptStatisticsVO();
        expected.setTotal(5L);
        expected.setEnterprise(2L);
        expected.setBusiness(3L);
        when(deptMapper.selectStatistics(any(Wrapper.class))).thenReturn(expected);

        DeptStatisticsVO actual = deptService.getStatistics(new DeptStatisticsQueryDTO());

        assertEquals(5L, actual.getTotal());
        assertEquals(2L, actual.getEnterprise());
        assertEquals(3L, actual.getBusiness());
    }

    @Test
    void userStatistics_returnsAccountTypeCounts() {
        com.yigongbao.module.system.user.vo.UserStatisticsVO expected =
                new com.yigongbao.module.system.user.vo.UserStatisticsVO();
        expected.setTotal(11L);
        expected.setEnterprise(4L);
        expected.setBusiness(7L);
        when(userMapper.selectStatistics(any(Wrapper.class))).thenReturn(expected);

        com.yigongbao.module.system.user.vo.UserStatisticsVO actual =
                userService.getStatistics(new UserStatisticsQueryDTO());

        assertEquals(11L, actual.getTotal());
        assertEquals(4L, actual.getEnterprise());
        assertEquals(7L, actual.getBusiness());
    }

    private void setBaseMapper(Object service, Object mapper) throws Exception {
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }
}
