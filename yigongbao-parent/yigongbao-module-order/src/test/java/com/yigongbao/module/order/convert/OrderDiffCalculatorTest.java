package com.yigongbao.module.order.convert;

import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.order.dto.diff.FieldDiff;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * OrderDiffCalculator 单元测试
 * 测试订单修改申请新增的三个字段差异计算功能
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-13
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderDiffCalculatorTest {

    @Mock
    private DictService dictService;

    @InjectMocks
    private OrderDiffCalculator diffCalculator;

    @BeforeEach
    void setUp() {
        // Mock 字典服务返回
        DictVO orderTypeDict1 = new DictVO();
        orderTypeDict1.setDictCode("1");
        orderTypeDict1.setDictName("医疗器械");

        DictVO orderTypeDict2 = new DictVO();
        orderTypeDict2.setDictCode("2");
        orderTypeDict2.setDictName("非医疗器械");

        DictVO businessTypeDict1 = new DictVO();
        businessTypeDict1.setDictCode("11.1");
        businessTypeDict1.setDictName("业务");

        DictVO businessTypeDict2 = new DictVO();
        businessTypeDict2.setDictCode("11.2");
        businessTypeDict2.setDictName("测试");

        when(dictService.getByDictCode("1")).thenReturn(orderTypeDict1);
        when(dictService.getByDictCode("2")).thenReturn(orderTypeDict2);
        when(dictService.getByDictCode("11.1")).thenReturn(businessTypeDict1);
        when(dictService.getByDictCode("11.2")).thenReturn(businessTypeDict2);
    }

    /**
     * 测试 needsPhysicalDelivery 字段差异计算
     * 场景：从"不需要"改为"需要"
     */
    @Test
    void testNeedsPhysicalDeliveryDiff_FromNoToYes() {
        // Arrange
        OrderDraftEntity current = createBaseOrderDraft();
        current.setNeedsPhysicalDelivery(StatusConstants.NO);

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setNeedsPhysicalDelivery(StatusConstants.YES);

        // Act
        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        // Assert
        assertNotNull(diffs);
        assertTrue(diffs.stream().anyMatch(d -> "needsPhysicalDelivery".equals(d.getFieldName())));

        FieldDiff diff = diffs.stream()
                .filter(d -> "needsPhysicalDelivery".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(diff);
        assertEquals("是否需要实物交付", diff.getFieldLabel());
        assertEquals("否", diff.getOldDisplay());
        assertEquals("是", diff.getNewDisplay());
    }

    /**
     * 测试 orderType 字段差异计算
     * 场景：从"医疗器械"改为"非医疗器械"
     */
    @Test
    void testOrderTypeDiff_FromMedicalToNonMedical() {
        // Arrange
        OrderDraftEntity current = createBaseOrderDraft();
        current.setOrderType(1); // 医疗器械

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setOrderType(2); // 非医疗器械

        // Act
        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        // Assert
        assertNotNull(diffs);
        assertTrue(diffs.stream().anyMatch(d -> "orderType".equals(d.getFieldName())));

        FieldDiff diff = diffs.stream()
                .filter(d -> "orderType".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(diff);
        assertEquals("订单类型", diff.getFieldLabel());
        assertEquals("医疗器械", diff.getOldDisplay());
        assertEquals("非医疗器械", diff.getNewDisplay());
    }

    /**
     * 测试 businessType 字段差异计算
     * 场景：从"业务"改为"测试"
     */
    @Test
    void testBusinessTypeDiff_FromBusinessToTest() {
        // Arrange
        OrderDraftEntity current = createBaseOrderDraft();
        current.setBusinessType("11.1"); // 业务

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setBusinessType("11.2"); // 测试

        // Act
        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        // Assert
        assertNotNull(diffs);
        assertTrue(diffs.stream().anyMatch(d -> "businessType".equals(d.getFieldName())));

        FieldDiff diff = diffs.stream()
                .filter(d -> "businessType".equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(diff);
        assertEquals("业务类型", diff.getFieldLabel());
        assertEquals("业务", diff.getOldDisplay());
        assertEquals("测试", diff.getNewDisplay());
    }

    /**
     * 创建基础 OrderDraftEntity，包含所有必需字段的默认值
     */
    private OrderDraftEntity createBaseOrderDraft() {
        OrderDraftEntity entity = new OrderDraftEntity();
        entity.setPatientName("测试患者");
        entity.setPatientAge(30);
        entity.setPatientGender("1");
        entity.setHospitalId(1L);
        entity.setHospitalName("测试医院");
        entity.setHospitalDeptId(1L);
        entity.setHospitalDeptName("测试科室");
        entity.setDoctorId(1L);
        entity.setDoctorName("测试医生");
        entity.setIsUrgent(StatusConstants.NO);
        entity.setIsPostal(StatusConstants.NO);
        entity.setPostalAddress("测试地址");
        entity.setNeedsPhysicalDelivery(StatusConstants.YES);
        entity.setOrderType(1);
        entity.setBusinessType("11.1");
        return entity;
    }

    /**
     * 创建基础 OrderModifyFullDTO，包含所有必需字段的默认值
     */
    private OrderModifyFullDTO createBaseModifyDTO() {
        OrderModifyFullDTO dto = new OrderModifyFullDTO();
        dto.setPatientName("测试患者");
        dto.setPatientAge(30);
        dto.setPatientGender("1");
        dto.setHospitalId(1L);
        dto.setHospitalDeptId(1L);
        dto.setDoctorId(1L);
        dto.setIsUrgent(StatusConstants.NO);
        dto.setIsPostal(StatusConstants.NO);
        dto.setPostalAddress("测试地址");
        dto.setNeedsPhysicalDelivery(StatusConstants.YES);
        dto.setOrderType(1);
        dto.setBusinessType("11.1");
        return dto;
    }

    /**
     * 使用反射调用 private 方法 calculateBasicInfoDiff
     */
    private List<FieldDiff> invokeCalculateBasicInfoDiff(OrderDraftEntity current, OrderModifyFullDTO dto) {
        try {
            java.lang.reflect.Method method = OrderDiffCalculator.class
                    .getDeclaredMethod("calculateBasicInfoDiff", OrderDraftEntity.class, OrderModifyFullDTO.class);
            method.setAccessible(true);
            return (List<FieldDiff>) method.invoke(diffCalculator, current, dto);
        } catch (Exception e) {
            throw new RuntimeException("反射调用失败", e);
        }
    }
}
