package com.yigongbao.module.basic.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.area.entity.AreaEntity;
import com.yigongbao.module.basic.area.service.AreaService;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HospitalServiceImpl 单元测试")
class HospitalServiceImplTest {

    @Mock
    private HospitalMapper hospitalMapper;

    @Mock
    private AreaService areaService;

    @Mock
    private CodeGeneratorService codeGeneratorService;

    @InjectMocks
    private HospitalServiceImpl hospitalService;

    private HospitalEntity testEntity;
    private CreateHospitalDTO createDTO;
    private AreaEntity areaEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(hospitalService, hospitalMapper);

        areaEntity = new AreaEntity();
        areaEntity.setId(1L);
        areaEntity.setName("东城区");
        areaEntity.setMergerName("中国,北京,北京市,东城区");

        testEntity = new HospitalEntity();
        testEntity.setId(1L);
        testEntity.setHospitalName("测试医院");
        testEntity.setHospitalCode("HOS-001");
        testEntity.setAreaId(1L);
        testEntity.setAreaName("东城区");
        testEntity.setFullAreaName("中国,北京,北京市,东城区");
        testEntity.setContact("张医生");
        testEntity.setPhone("13800138001");
        testEntity.setStatus(1);

        createDTO = new CreateHospitalDTO();
        createDTO.setHospitalName("新医院");
        createDTO.setAreaId(1L);
        createDTO.setContact("李医生");
        createDTO.setPhone("13900139001");
    }

    // ==================== getHospitalById 测试 ====================

    @Test
    @DisplayName("getHospitalById: 存在数据时返回VO")
    void getHospitalById_whenExists_shouldReturnData() {
        when(hospitalMapper.selectById(1L)).thenReturn(testEntity);
        HospitalVO result = hospitalService.getHospitalById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试医院", result.getHospitalName());
        verify(hospitalMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("getHospitalById: 数据不存在时抛出异常")
    void getHospitalById_whenNotExists_shouldThrowException() {
        when(hospitalMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> hospitalService.getHospitalById(999L));
        assertEquals(ErrorCodeEnum.HOSPITAL_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== createHospital 测试 ====================

    @Test
    @DisplayName("createHospital: 医院名称已存在时抛出异常")
    void createHospital_whenNameExists_shouldThrowException() {
        when(hospitalMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> hospitalService.createHospital(createDTO));
        assertEquals(ErrorCodeEnum.HOSPITAL_EXISTS.getCode(), ex.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 状态值不合法时抛出异常")
    void updateStatus_whenInvalidStatus_shouldThrowException() {
        BusinessException ex = assertThrows(BusinessException.class, () -> hospitalService.updateStatus(1L, 99));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updateStatus: 数据不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        when(hospitalMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> hospitalService.updateStatus(999L, 0));
        assertEquals(ErrorCodeEnum.HOSPITAL_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== listHospital 测试 ====================

    // ==================== createHospital 测试 ====================

    @Test
    @DisplayName("createHospital: 创建成功")
    void createHospital_shouldSuccess() {
        when(hospitalMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(areaService.getById(1L)).thenReturn(areaEntity);
        when(codeGeneratorService.generate("HOSPITAL_NO")).thenReturn("HOS-0001");
        when(hospitalMapper.insert(any(HospitalEntity.class))).thenReturn(1);

        hospitalService.createHospital(createDTO);

        verify(hospitalMapper, times(1)).insert(any(HospitalEntity.class));
    }

}
