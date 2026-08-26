package com.yigongbao.module.order.convert;

import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.order.dto.diff.FieldDiff;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.dto.diff.OrderModificationDiff;
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
        assertFieldDiff(diffs, "needsPhysicalDelivery", "是否需要实物交付", "否", "是");
    }

    @Test
    void testNeedsPhysicalDeliveryDiff_FromNoToRemotePrinting() {
        OrderDraftEntity current = createBaseOrderDraft();
        current.setNeedsPhysicalDelivery(StatusConstants.NO);

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setNeedsPhysicalDelivery(2);

        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        assertFieldDiff(diffs, "needsPhysicalDelivery", "是否需要实物交付", "否", "异地打印");
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
        assertFieldDiff(diffs, "orderType", "订单类型", "医疗器械", "非医疗器械");
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
        assertFieldDiff(diffs, "businessType", "业务类型", "业务", "测试");
    }

    /**
     * 测试无变更场景：字段值相同时不应生成差异记录
     */
    @Test
    void testNoDiff_WhenFieldValuesAreSame() {
        // Arrange
        OrderDraftEntity current = createBaseOrderDraft();
        current.setNeedsPhysicalDelivery(StatusConstants.YES);
        current.setOrderType(1);
        current.setBusinessType("11.1");

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setNeedsPhysicalDelivery(StatusConstants.YES); // 相同值
        dto.setOrderType(1); // 相同值
        dto.setBusinessType("11.1"); // 相同值

        // Act
        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        // Assert
        assertNotNull(diffs);
        assertFalse(diffs.stream().anyMatch(d -> "needsPhysicalDelivery".equals(d.getFieldName())),
                "相同值不应生成 needsPhysicalDelivery 差异");
        assertFalse(diffs.stream().anyMatch(d -> "orderType".equals(d.getFieldName())),
                "相同值不应生成 orderType 差异");
        assertFalse(diffs.stream().anyMatch(d -> "businessType".equals(d.getFieldName())),
                "相同值不应生成 businessType 差异");
    }

    /**
     * 测试多字段同时变更场景
     */
    @Test
    void testMultipleFieldsChanged_Simultaneously() {
        // Arrange
        OrderDraftEntity current = createBaseOrderDraft();
        current.setNeedsPhysicalDelivery(StatusConstants.NO);
        current.setOrderType(1);
        current.setBusinessType("11.1");

        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setNeedsPhysicalDelivery(StatusConstants.YES);
        dto.setOrderType(2);
        dto.setBusinessType("11.2");

        // Act
        List<FieldDiff> diffs = invokeCalculateBasicInfoDiff(current, dto);

        // Assert
        assertNotNull(diffs);
        long changedCount = diffs.stream()
                .filter(d -> "needsPhysicalDelivery".equals(d.getFieldName())
                        || "orderType".equals(d.getFieldName())
                        || "businessType".equals(d.getFieldName()))
                .count();
        assertEquals(3, changedCount, "应生成3个字段的差异记录");

        assertFieldDiff(diffs, "needsPhysicalDelivery", "是否需要实物交付", "否", "是");
        assertFieldDiff(diffs, "orderType", "订单类型", "医疗器械", "非医疗器械");
        assertFieldDiff(diffs, "businessType", "业务类型", "业务", "测试");
    }

    @Test
    void calculateDiff_nullListsMeanNoChange() {
        OrderDraftEntity current = createBaseOrderDraft();
        OrderModifyFullDTO dto = createBaseModifyDTO();

        OrderModificationDiff diff = diffCalculator.calculateDiff(
                current,
                List.of(createItem(1L, "旧项目")),
                List.of(createFile(10L, "data-old", "10.1"), createFile(11L, "report-old", "10.2")),
                dto);

        assertNull(diff.getItems());
        assertNull(diff.getImageData());
        assertNull(diff.getImageReport());
    }

    @Test
    void calculateDiff_emptyOrChangedListsOnlyAffectProvidedCategory() {
        OrderDraftEntity current = createBaseOrderDraft();
        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setItems(List.of());
        dto.setImageDataFileIds(List.of("data-new"));

        OrderModificationDiff diff = diffCalculator.calculateDiff(
                current,
                List.of(createItem(1L, "旧项目")),
                List.of(createFile(10L, "data-old", "10.1"), createFile(11L, "report-old", "10.2")),
                dto);

        assertTrue(diff.getItems().isChanged());
        assertEquals(1, diff.getItems().getDeleted().size());
        assertEquals("旧项目", diff.getItems().getDeleted().get(0).getProjectName());
        assertTrue(diff.getImageData().isChanged());
        assertEquals(List.of("data-old"), diff.getImageData().getDeleted());
        assertEquals(List.of("data-new"), diff.getImageData().getAdded());
        assertNull(diff.getImageReport());
    }

    @Test
    void calculateDiff_emptyImageListMeansClearThatCategory() {
        OrderDraftEntity current = createBaseOrderDraft();
        OrderModifyFullDTO dto = createBaseModifyDTO();
        dto.setImageDataFileIds(List.of());

        OrderModificationDiff diff = diffCalculator.calculateDiff(
                current,
                List.of(),
                List.of(createFile(10L, "data-old", "10.1"), createFile(11L, "report-old", "10.2")),
                dto);

        assertEquals(List.of("data-old"), diff.getImageData().getDeleted());
        assertNull(diff.getImageReport());
    }

    private OrderItemEntity createItem(Long id, String projectName) {
        OrderItemEntity item = new OrderItemEntity();
        item.setId(id);
        item.setProjectName(projectName);
        return item;
    }

    private OrderFileEntity createFile(Long id, String fileId, String category) {
        OrderFileEntity file = new OrderFileEntity();
        file.setId(id);
        file.setFileId(fileId);
        file.setFileCategory(category);
        return file;
    }

    /**
     * 创建基础 OrderDraftEntity，包含所有必需字段的默认值
     */
    private OrderDraftEntity createBaseOrderDraft() {
        OrderDraftEntity entity = new OrderDraftEntity();
        populateCommonTestFields(entity);
        // OrderDraftEntity 特有字段
        entity.setHospitalName("测试医院");
        entity.setHospitalDeptName("测试科室");
        entity.setDoctorName("测试医生");
        return entity;
    }

    /**
     * 创建基础 OrderModifyFullDTO，包含所有必需字段的默认值
     */
    private OrderModifyFullDTO createBaseModifyDTO() {
        OrderModifyFullDTO dto = new OrderModifyFullDTO();
        populateCommonTestFields(dto);
        return dto;
    }

    /**
     * 填充测试对象的公共字段（避免代码重复）
     */
    private void populateCommonTestFields(Object target) {
        if (target instanceof OrderDraftEntity) {
            OrderDraftEntity entity = (OrderDraftEntity) target;
            entity.setPatientName("测试患者");
            entity.setPatientAge(30);
            entity.setPatientGender("1");
            entity.setHospitalId(1L);
            entity.setHospitalDeptId(1L);
            entity.setDoctorId(1L);
            entity.setIsUrgent(StatusConstants.NO);
            entity.setIsPostal(StatusConstants.NO);
            entity.setPostalAddress("测试地址");
            entity.setNeedsPhysicalDelivery(StatusConstants.YES);
            entity.setOrderType(1);
            entity.setBusinessType("11.1");
        } else if (target instanceof OrderModifyFullDTO) {
            OrderModifyFullDTO dto = (OrderModifyFullDTO) target;
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
        }
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

    /**
     * 断言字段差异存在且值正确
     *
     * @param diffs 差异列表
     * @param fieldName 字段名
     * @param expectedLabel 字段标签
     * @param expectedOldDisplay 旧值显示
     * @param expectedNewDisplay 新值显示
     */
    private void assertFieldDiff(List<FieldDiff> diffs, String fieldName, String expectedLabel,
                                  String expectedOldDisplay, String expectedNewDisplay) {
        assertNotNull(diffs);

        FieldDiff diff = diffs.stream()
                .filter(d -> fieldName.equals(d.getFieldName()))
                .findFirst()
                .orElse(null);

        assertNotNull(diff, "未找到字段差异: " + fieldName);
        assertEquals(expectedLabel, diff.getFieldLabel());
        assertEquals(expectedOldDisplay, diff.getOldDisplay());
        assertEquals(expectedNewDisplay, diff.getNewDisplay());
    }
}
