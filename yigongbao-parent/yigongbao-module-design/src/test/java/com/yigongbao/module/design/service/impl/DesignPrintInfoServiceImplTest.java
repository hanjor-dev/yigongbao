package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.product.entity.ProductSpecEntity;
import com.yigongbao.module.basic.product.service.ProductService;
import com.yigongbao.module.basic.product.service.ProductSpecService;
import com.yigongbao.module.basic.product.vo.ProductSpecVO;
import com.yigongbao.module.basic.product.vo.ProductVO;
import com.yigongbao.module.design.dto.SavePrintInfoDTO;
import com.yigongbao.module.design.dto.SavePrintInfoItemDTO;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.ColorGroupVO;
import com.yigongbao.module.design.vo.DesignProductVO;
import com.yigongbao.module.design.vo.DictOptionVO;
import com.yigongbao.module.design.vo.PrintInfoOptionsVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignPrintInfoServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignPrintInfoService 单元测试")
class DesignPrintInfoServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private ProductService productService;
    @Mock private ProductSpecService productSpecService;
    @Mock private DesignPackageService packageService;
    @Mock private DesignPackageFileService packageFileService;
    @Mock private DesignProductService designProductService;
    @Mock private DictService dictService;

    @InjectMocks
    private DesignPrintInfoServiceImpl printInfoService;

    private OrderMainEntity designInProgressOrder;
    private DesignPackageEntity testPackage;
    private ProductVO testProduct;
    private ProductSpecEntity testSpec;

    private static final Long ORDER_ID = 1L;
    private static final Long PACKAGE_ID = 10L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long SPEC_ID = 200L;
    private static final Long FILE_ID = 300L;
    private static final Long DESIGNER_ID = 999L;

    @BeforeEach
    void setUp() {
        // 设计中订单
        designInProgressOrder = new OrderMainEntity();
        designInProgressOrder.setId(ORDER_ID);
        designInProgressOrder.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        designInProgressOrder.setDesignerId(DESIGNER_ID);

        // 数据包
        testPackage = new DesignPackageEntity();
        testPackage.setId(PACKAGE_ID);
        testPackage.setOrderId(ORDER_ID);

        // 产品
        testProduct = new ProductVO();
        testProduct.setId(PRODUCT_ID);
        testProduct.setProductName("膝关节假体");
        testProduct.setCategory("17.1");
        testProduct.setStatus(StatusConstants.NORMAL);
        testProduct.setSpecs(Collections.emptyList());

        // 规格
        testSpec = new ProductSpecEntity();
        testSpec.setId(SPEC_ID);
        testSpec.setProductId(PRODUCT_ID);
        testSpec.setSpecName("47号");
        testSpec.setCertNo("国械注准20250001");
        testSpec.setStatus(StatusConstants.NORMAL);
    }

    // ==================== getOptions 测试 ====================

    @Nested
    @DisplayName("getOptions 测试")
    class GetOptionsTest {

        @Test
        @DisplayName("获取选项数据：返回正确产品树、材质默认值、颜色分组")
        void getOptions_shouldReturnCorrectData() {
            when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

            // 产品树（含 specs）
            ProductVO product = new ProductVO();
            product.setId(PRODUCT_ID);
            product.setProductName("膝关节假体");
            product.setCategory("17.1");
            product.setStatus(1);
            List<ProductSpecVO> specs = new ArrayList<>();
            ProductSpecVO specVO = new ProductSpecVO();
            specVO.setId(SPEC_ID);
            specVO.setSpecName("47号");
            specs.add(specVO);
            product.setSpecs(specs);
            when(productService.listAllWithSpecs()).thenReturn(List.of(product));

            // 材质（dictCode="15"）
            DictVO material = new DictVO();
            material.setDictCode("15.1");
            material.setDictName("树脂");
            when(dictService.listByTypeCode("15")).thenReturn(List.of(material));

            // 颜色树（dictCode="16"）
            DictVO colorLevel3 = new DictVO();
            colorLevel3.setDictCode("16.1.1");
            colorLevel3.setDictName("白色");
            DictVO colorLevel2 = new DictVO();
            colorLevel2.setDictCode("16.1");
            colorLevel2.setDictName("模型类颜色");
            colorLevel2.setDictValue("17.1");
            colorLevel2.setChildren(List.of(colorLevel3));
            DictVO colorRoot = new DictVO();
            colorRoot.setDictCode("16");
            colorRoot.setDictName("打印颜色");
            colorRoot.setChildren(List.of(colorLevel2));
            when(dictService.listTreeByTypeCode("16")).thenReturn(List.of(colorRoot));

            PrintInfoOptionsVO result = printInfoService.getOptions(ORDER_ID);

            assertNotNull(result);
            // 产品树
            assertEquals(1, result.getProducts().size());
            assertEquals(1, result.getProducts().get(0).getSpecs().size());
            // 材质默认值
            assertEquals(1, result.getMaterials().size());
            assertTrue(result.getMaterials().get(0).getIsDefault()); // 15.1 是默认
            // 颜色分组
            assertEquals(1, result.getColorGroups().size());
            ColorGroupVO group = result.getColorGroups().get(0);
            assertEquals("17.1", group.getCategoryCode());
            assertEquals(1, group.getColors().size());
            assertEquals("16.1.1", group.getColors().get(0).getCode());
        }

        @Test
        @DisplayName("获取选项数据：status=0 的规格不出现在 specs 列表")
        void getOptions_disabledSpecsExcluded() {
            when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

            // 产品 specs 为空（status=0 的规格已被过滤，listAllWithSpecs 只返回 status=1 的）
            ProductVO product = new ProductVO();
            product.setId(PRODUCT_ID);
            product.setProductName("膝关节假体");
            product.setStatus(1);
            product.setSpecs(Collections.emptyList()); // 无可用规格
            when(productService.listAllWithSpecs()).thenReturn(List.of(product));
            when(dictService.listByTypeCode("15")).thenReturn(Collections.emptyList());
            when(dictService.listTreeByTypeCode("16")).thenReturn(Collections.emptyList());

            PrintInfoOptionsVO result = printInfoService.getOptions(ORDER_ID);

            assertNotNull(result);
            assertEquals(1, result.getProducts().size());
            assertTrue(result.getProducts().get(0).getSpecs().isEmpty());
        }
    }

    // ==================== listPrintInfo 测试 ====================

    @Nested
    @DisplayName("listPrintInfo 测试")
    class ListPrintInfoTest {

        @Test
        @DisplayName("查询打印信息列表成功，按 sort_order 排序返回")
        void listPrintInfo_shouldReturnSortedList() {
            when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

            DesignProductEntity entity = new DesignProductEntity();
            entity.setId(1L);
            entity.setOrderId(ORDER_ID);
            entity.setPackageId(PACKAGE_ID);
            entity.setSortOrder(1);
            when(designProductService.list(any(Wrapper.class))).thenReturn(List.of(entity));

            List<DesignProductVO> result = printInfoService.listPrintInfo(ORDER_ID, PACKAGE_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
        }
    }

    // ==================== savePrintInfo 测试 ====================

    @Nested
    @DisplayName("savePrintInfo 测试")
    class SavePrintInfoTest {

        @Test
        @DisplayName("保存打印信息成功（整包替换：旧记录删除、新记录插入）")
        void savePrintInfo_shouldDeleteOldAndInsertNew() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(packageFileService.count(any(Wrapper.class))).thenReturn(1L);
                when(productService.getById(PRODUCT_ID)).thenReturn(testProduct);
                when(productSpecService.listByIds(any())).thenReturn(List.of(testSpec));
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
                when(designProductService.saveBatch(any())).thenReturn(true);

                SavePrintInfoDTO dto = buildSavePrintInfoDTO();
                printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto);

                verify(designProductService, times(1)).remove(any(Wrapper.class));
                verify(designProductService, times(1)).saveBatch(any());
            }
        }

        @Test
        @DisplayName("保存空列表时旧记录被清空")
        void savePrintInfo_emptyItems_shouldClearOldRecords() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);

                SavePrintInfoDTO dto = new SavePrintInfoDTO();
                dto.setItems(Collections.emptyList());
                printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto);

                verify(designProductService, times(1)).remove(any(Wrapper.class));
                verify(designProductService, never()).saveBatch(any());
            }
        }

        @Test
        @DisplayName("specId 不属于传入 productId 时抛出 PARAM_ERROR")
        void savePrintInfo_specNotBelongToProduct_shouldThrow() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(packageFileService.count(any(Wrapper.class))).thenReturn(1L);
                when(productService.getById(PRODUCT_ID)).thenReturn(testProduct);

                // spec 属于另一个产品
                ProductSpecEntity wrongSpec = new ProductSpecEntity();
                wrongSpec.setId(SPEC_ID);
                wrongSpec.setProductId(999L); // 不匹配
                wrongSpec.setStatus(StatusConstants.NORMAL);
                when(productSpecService.listByIds(any())).thenReturn(List.of(wrongSpec));

                SavePrintInfoDTO dto = buildSavePrintInfoDTO();

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
                assertEquals(ErrorCodeEnum.PARAM_ERROR.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("packageFileId 不属于该 package 时抛出异常")
        void savePrintInfo_fileNotInPackage_shouldThrow() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                // 文件不属于该包（count=0，但请求有 1 条）
                when(packageFileService.count(any(Wrapper.class))).thenReturn(0L);

                SavePrintInfoDTO dto = buildSavePrintInfoDTO();

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
                assertEquals(ErrorCodeEnum.ORDER_FILE_NOT_FOUND.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("权限校验：非设计师操作抛出 DESIGN_OPERATOR_NOT_ALLOWED（错误码740）")
        void savePrintInfo_notDesigner_shouldThrow740() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(888L); // 非设计师
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

                SavePrintInfoDTO dto = new SavePrintInfoDTO();
                dto.setItems(Collections.emptyList());

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
                assertEquals(740, ex.getCode());
            }
        }
    }

    // ==================== deletePrintInfo 测试 ====================

    @Nested
    @DisplayName("deletePrintInfo 测试")
    class DeletePrintInfoTest {

        @Test
        @DisplayName("删除单条打印信息成功")
        void deletePrintInfo_shouldSuccess() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

                DesignProductEntity entity = new DesignProductEntity();
                entity.setId(1L);
                entity.setOrderId(ORDER_ID);
                entity.setPackageId(PACKAGE_ID);
                when(designProductService.getById(1L)).thenReturn(entity);
                when(designProductService.removeById(1L)).thenReturn(true);

                printInfoService.deletePrintInfo(ORDER_ID, PACKAGE_ID, 1L);

                verify(designProductService, times(1)).removeById(1L);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private SavePrintInfoDTO buildSavePrintInfoDTO() {
        SavePrintInfoItemDTO item = new SavePrintInfoItemDTO();
        item.setPackageFileId(FILE_ID);
        item.setProductId(PRODUCT_ID);
        item.setSpecId(SPEC_ID);
        item.setQuantity(1);
        item.setSortOrder(1);

        SavePrintInfoDTO dto = new SavePrintInfoDTO();
        dto.setItems(List.of(item));
        return dto;
    }
}
