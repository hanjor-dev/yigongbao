package com.yigongbao.module.basic.area.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.basic.area.entity.AreaEntity;
import com.yigongbao.module.basic.area.mapper.AreaMapper;
import com.yigongbao.module.basic.area.vo.AreaVO;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 地区 Service 单元测试
 * 表结构与 cnarea_2023 一致，使用 parent_code / area_code
 *
 * @author hanjor
 * @date 2026-03-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Slf4j
@DisplayName("AreaServiceImpl 单元测试")
class AreaServiceImplTest {

    @Mock
    private AreaMapper areaMapper;

    @InjectMocks
    private AreaServiceImpl areaService;

    private AreaEntity province;
    private AreaEntity city;
    private AreaEntity district;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(areaService, areaMapper);

        // 浙江省（parent_code=0, area_code=330000）
        province = new AreaEntity();
        province.setId(1L);
        province.setParentCode(0L);
        province.setAreaCode(330000L);
        province.setName("浙江省");
        province.setLevel(1);
        province.setPinyin("zhejiang");

        // 杭州市（parent_code=330000, area_code=330100）
        city = new AreaEntity();
        city.setId(2L);
        city.setParentCode(330000L);
        city.setAreaCode(330100L);
        city.setName("杭州市");
        city.setLevel(2);
        city.setPinyin("hangzhou");

        // 上城区（parent_code=330100, area_code=330102）
        district = new AreaEntity();
        district.setId(3L);
        district.setParentCode(330100L);
        district.setAreaCode(330102L);
        district.setName("上城区");
        district.setLevel(3);
        district.setPinyin("shangcheng");
    }

    // ==================== listTree 测试 ====================

    @Test
    @DisplayName("listTree: 查询省份列表（parentCode=0）")
    void listTree_whenProvinces_shouldReturnProvinceList() {
        List<AreaEntity> provinceList = Arrays.asList(province);
        when(areaMapper.selectList(any())).thenReturn(provinceList);

        List<AreaVO> result = areaService.listTree(0L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("浙江省", result.get(0).getName());
        assertEquals(0L, result.get(0).getParentCode());
        assertEquals(330000L, result.get(0).getAreaCode());
        assertNull(result.get(0).getChildren());
        verify(areaMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("listTree: 查询完整树形结构（省->市->区）")
    void listTree_whenFullTree_shouldReturnTreeStructure() {
        List<AreaEntity> fullList = Arrays.asList(province, city, district);
        when(areaMapper.selectList(any())).thenReturn(fullList);

        List<AreaVO> result = areaService.listTree(0L);

        assertNotNull(result);
        assertEquals(1, result.size());

        AreaVO provinceVO = result.get(0);
        assertEquals("浙江省", provinceVO.getName());
        assertEquals(330000L, provinceVO.getAreaCode());
        assertNotNull(provinceVO.getChildren());
        assertEquals(1, provinceVO.getChildren().size());

        AreaVO cityVO = provinceVO.getChildren().get(0);
        assertEquals("杭州市", cityVO.getName());
        assertEquals(330100L, cityVO.getAreaCode());
        assertNotNull(cityVO.getChildren());
        assertEquals(1, cityVO.getChildren().size());

        AreaVO districtVO = cityVO.getChildren().get(0);
        assertEquals("上城区", districtVO.getName());
        assertEquals(330102L, districtVO.getAreaCode());
    }

    @Test
    @DisplayName("listTree: 无数据时返回空列表")
    void listTree_whenNoData_shouldReturnEmptyList() {
        when(areaMapper.selectList(any())).thenReturn(List.of());

        List<AreaVO> result = areaService.listTree(0L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== listByParentId 测试 ====================

    @Test
    @DisplayName("listByParentId: 按父级行政代码查询子地区（省下的市）")
    void listByParentId_shouldReturnCityList() {
        List<AreaEntity> cityList = Arrays.asList(city);
        when(areaMapper.selectList(any())).thenReturn(cityList);

        List<AreaVO> result = areaService.listByParentId(330000L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("杭州市", result.get(0).getName());
        assertEquals(330000L, result.get(0).getParentCode());
        assertEquals(330100L, result.get(0).getAreaCode());
    }

    @Test
    @DisplayName("listByParentId: 无子节点时返回空列表")
    void listByParentId_whenNoChildren_shouldReturnEmptyList() {
        when(areaMapper.selectList(any())).thenReturn(List.of());

        List<AreaVO> result = areaService.listByParentId(999999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== listProvinces 测试 ====================

    @Test
    @DisplayName("listProvinces: 查询所有省份")
    void listProvinces_shouldReturnAllProvinces() {
        AreaEntity province2 = new AreaEntity();
        province2.setId(2L);
        province2.setParentCode(0L);
        province2.setAreaCode(310000L);
        province2.setName("上海市");
        province2.setLevel(1);
        province2.setPinyin("shanghai");

        List<AreaEntity> provinces = Arrays.asList(province, province2);
        when(areaMapper.selectList(any())).thenReturn(provinces);

        List<AreaVO> result = areaService.listProvinces();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("浙江省", result.get(0).getName());
        assertEquals("上海市", result.get(1).getName());
    }

    @Test
    @DisplayName("listProvinces: 无省份数据时返回空列表")
    void listProvinces_whenNoData_shouldReturnEmptyList() {
        when(areaMapper.selectList(any())).thenReturn(List.of());

        List<AreaVO> result = areaService.listProvinces();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
