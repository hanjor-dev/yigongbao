package com.yigongbao.module.basic.hospitalGroupTemplate.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.common.mapper.OrgQueryMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.CreateHospitalGroupTemplateDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.dto.HospitalGroupTemplatePageDTO;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateDetailEntity;
import com.yigongbao.module.basic.hospitalGroupTemplate.entity.HospitalGroupTemplateEntity;
import com.yigongbao.module.basic.hospitalGroupTemplate.mapper.HospitalGroupTemplateDetailMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.mapper.HospitalGroupTemplateMapper;
import com.yigongbao.module.basic.hospitalGroupTemplate.service.HospitalGroupTemplateService;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateSimpleVO;
import com.yigongbao.module.basic.hospitalGroupTemplate.vo.HospitalGroupTemplateVO;
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
@DisplayName("HospitalGroupTemplateServiceImpl 单元测试")
class HospitalGroupTemplateServiceImplTest {

    @Mock
    private HospitalGroupTemplateMapper templateMapper;

    @Mock
    private HospitalGroupTemplateDetailMapper detailMapper;

    @Mock
    private OrgQueryMapper orgQueryMapper;

    @InjectMocks
    private HospitalGroupTemplateServiceImpl templateService;

    private HospitalGroupTemplateEntity templateEntity;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(templateService, templateMapper);

        templateEntity = new HospitalGroupTemplateEntity();
        templateEntity.setId(1L);
        templateEntity.setTemplateName("测试模板");
        templateEntity.setTemplateCode("TPL-HOS-001");
        templateEntity.setStatus(1);
    }

    // ==================== getTemplateById 测试 ====================

    @Test
    @DisplayName("getTemplateById: 存在数据时返回VO")
    void getTemplateById_whenExists_shouldReturnData() {
        when(templateMapper.selectById(1L)).thenReturn(templateEntity);
        when(detailMapper.countByTemplateId(1L)).thenReturn(2L);
        when(detailMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        HospitalGroupTemplateVO result = templateService.getTemplateById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试模板", result.getTemplateName());
        verify(templateMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("getTemplateById: 数据不存在时抛出异常")
    void getTemplateById_whenNotExists_shouldThrowException() {
        when(templateMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.getTemplateById(999L));
        assertEquals(ErrorCodeEnum.TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== createTemplate 测试 ====================

    @Test
    @DisplayName("createTemplate: 模板名称已存在时抛出异常")
    void createTemplate_whenNameExists_shouldThrowException() {
        CreateHospitalGroupTemplateDTO dto = new CreateHospitalGroupTemplateDTO();
        dto.setTemplateName("测试模板");
        dto.setHospitalIds(List.of(1L, 2L));
        when(templateMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.createTemplate(dto));
        assertEquals(ErrorCodeEnum.TEMPLATE_EXISTS.getCode(), ex.getCode());
    }

    // ==================== removeTemplate 测试 ====================

    @Test
    @DisplayName("removeTemplate: 数据不存在时抛出异常")
    void removeTemplate_whenNotExists_shouldThrowException() {
        when(templateMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.removeTemplate(999L));
        assertEquals(ErrorCodeEnum.TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== updateStatus 测试 ====================

    @Test
    @DisplayName("updateStatus: 状态值不合法时抛出异常")
    void updateStatus_whenInvalidStatus_shouldThrowException() {
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.updateStatus(1L, 99));
        assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("updateStatus: 数据不存在时抛出异常")
    void updateStatus_whenNotExists_shouldThrowException() {
        when(templateMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> templateService.updateStatus(999L, 0));
        assertEquals(ErrorCodeEnum.TEMPLATE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== listTemplate 测试 ====================

    @Test
    @DisplayName("listTemplate: 返回分页数据")
    void listTemplate_shouldReturnPageData() {
        Page<HospitalGroupTemplateEntity> page = new Page<>(1, 10);
        page.setRecords(List.of(templateEntity));
        page.setTotal(1);
        when(templateMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(detailMapper.countByTemplateId(1L)).thenReturn(2L);
        var result = templateService.listTemplate(new HospitalGroupTemplatePageDTO());
        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }
}
