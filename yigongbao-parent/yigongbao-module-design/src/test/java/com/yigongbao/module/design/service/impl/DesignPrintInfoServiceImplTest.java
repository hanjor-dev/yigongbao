package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
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
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.ColorGroupVO;
import com.yigongbao.module.design.vo.DesignProductVO;
import com.yigongbao.module.design.vo.PrintInfoOptionsVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
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
    @Mock private DesignProductFileService productFileService;
    @Mock private DictService dictService;
    @Mock private DesignInstructionMapper instructionMapper;
    @Mock private DesignDrawingMapper drawingMapper;

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

    @BeforeAll
    static void initLambdaCache() {
        // 初始化 MyBatis-Plus lambda 缓存（单元测试中无 Spring 容器，手动注册）
        Configuration configuration = new Configuration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, DesignProductEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignPackageFileEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignInstructionEntity.class);
        TableInfoHelper.initTableInfo(assistant, DesignDrawingEntity.class);
    }

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
        @DisplayName("获取选项数据：返回正确产品树、材质默认值、颜色分组，并回填包级字段")
        void getOptions_shouldReturnCorrectData() {
            when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
            when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

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

            // 颜色树（dictCode="16"），二级节点直接作为颜色选项，dictValue 存产品大类码
            DictVO colorLevel2 = new DictVO();
            colorLevel2.setDictCode("16.1");
            colorLevel2.setDictName("白色");
            colorLevel2.setDictValue("17.1");
            DictVO colorRoot = new DictVO();
            colorRoot.setDictCode("16");
            colorRoot.setDictName("打印颜色");
            colorRoot.setChildren(List.of(colorLevel2));
            when(dictService.listTreeByTypeCode("16")).thenReturn(List.of(colorRoot));

            PrintInfoOptionsVO result = printInfoService.getOptions(ORDER_ID, PACKAGE_ID);

            assertNotNull(result);
            assertEquals(1, result.getProducts().size());
            assertEquals(1, result.getProducts().get(0).getSpecs().size());
            assertEquals(1, result.getMaterials().size());
            assertTrue(result.getMaterials().get(0).getIsDefault()); // 15.1 是默认
            assertEquals(1, result.getColorGroups().size());
            ColorGroupVO group = result.getColorGroups().get(0);
            assertEquals("17.1", group.getCategoryCode());
            assertEquals(1, group.getColors().size());
            assertEquals("16.1", group.getColors().get(0).getCode());
        }

        @Test
        @DisplayName("获取选项数据：status=0 的规格不出现在 specs 列表")
        void getOptions_disabledSpecsExcluded() {
            when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
            when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

            ProductVO product = new ProductVO();
            product.setId(PRODUCT_ID);
            product.setProductName("膝关节假体");
            product.setStatus(1);
            product.setSpecs(Collections.emptyList()); // 无可用规格
            when(productService.listAllWithSpecs()).thenReturn(List.of(product));
            when(dictService.listByTypeCode("15")).thenReturn(Collections.emptyList());
            when(dictService.listTreeByTypeCode("16")).thenReturn(Collections.emptyList());

            PrintInfoOptionsVO result = printInfoService.getOptions(ORDER_ID, PACKAGE_ID);

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
        @DisplayName("查询打印信息列表成功，附带关联文件")
        void listPrintInfo_shouldReturnWithFiles() {
            when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);

            DesignProductEntity entity = new DesignProductEntity();
            entity.setId(1L);
            entity.setOrderId(ORDER_ID);
            entity.setPackageId(PACKAGE_ID);
            entity.setSortOrder(1);
            when(designProductService.list(any(Wrapper.class))).thenReturn(List.of(entity));

            DesignProductFileEntity fileEntity = new DesignProductFileEntity();
            fileEntity.setDesignProductId(1L);
            fileEntity.setPackageFileId(FILE_ID);
            fileEntity.setPackageFileName("左髋骨.stl");
            when(productFileService.listByProductIds(List.of(1L))).thenReturn(List.of(fileEntity));

            List<DesignProductVO> result = printInfoService.listPrintInfo(ORDER_ID, PACKAGE_ID);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(1, result.get(0).getFiles().size());
            assertEquals("左髋骨.stl", result.get(0).getFiles().get(0).getPackageFileName());
        }

        @Test
        @DisplayName("数据包不存在时抛出异常")
        void listPrintInfo_packageNotFound_shouldThrow() {
            when(packageService.getById(PACKAGE_ID)).thenReturn(null);

            assertThrows(BusinessException.class,
                    () -> printInfoService.listPrintInfo(ORDER_ID, PACKAGE_ID));
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
                // 旧产品行查询（select id）
                when(designProductService.list(any(Wrapper.class))).thenReturn(List.of());
                when(packageFileService.count(any(Wrapper.class))).thenReturn(1L);
                when(productService.getById(PRODUCT_ID)).thenReturn(testProduct);
                when(productSpecService.listByIds(any())).thenReturn(List.of(testSpec));
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
                when(designProductService.saveBatch(any())).thenReturn(true);
                when(packageFileService.getById(FILE_ID)).thenReturn(buildPackageFile());
                when(productFileService.saveBatch(any())).thenReturn(true);
                when(packageService.updateById(any())).thenReturn(true);

                SavePrintInfoDTO dto = buildSavePrintInfoDTO();
                printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto);

                verify(designProductService, times(1)).remove(any(Wrapper.class));
                verify(designProductService, times(1)).saveBatch(any());
                verify(productFileService, times(1)).saveBatch(any());
                verify(packageService, times(1)).updateById(any());
                verify(instructionMapper, times(1)).update(isNull(), any(Wrapper.class));
                verify(drawingMapper, times(1)).update(isNull(), any(Wrapper.class));
            }
        }

        @Test
        @DisplayName("保存打印信息后重置指令单和图纸的确认状态")
        void savePrintInfo_shouldResetConfirmedStatus() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(designProductService.list(any(Wrapper.class))).thenReturn(List.of());
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
                when(packageService.updateById(any())).thenReturn(true);

                SavePrintInfoDTO dto = new SavePrintInfoDTO();
                dto.setProductMark("LGC");
                dto.setItems(Collections.emptyList());
                printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto);

                // 确认状态被重置
                verify(instructionMapper, times(1)).update(isNull(), any(Wrapper.class));
                verify(drawingMapper, times(1)).update(isNull(), any(Wrapper.class));
            }
        }

        @Test
        @DisplayName("保存空列表时旧记录被清空，仍更新包级字段")
        void savePrintInfo_emptyItems_shouldClearOldRecords() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(designProductService.list(any(Wrapper.class))).thenReturn(List.of());
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
                when(packageService.updateById(any())).thenReturn(true);

                SavePrintInfoDTO dto = new SavePrintInfoDTO();
                dto.setProductMark("LGC");
                dto.setItems(Collections.emptyList());
                printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto);

                verify(designProductService, times(1)).remove(any(Wrapper.class));
                verify(designProductService, never()).saveBatch(any());
                verify(packageService, times(1)).updateById(any());
            }
        }

        @Test
        @DisplayName("specId 不属于传入 productId 时抛出 PARAM_ERROR")
        void savePrintInfo_specNotBelongToProduct_shouldThrow() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(designProductService.list(any(Wrapper.class))).thenReturn(List.of());
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
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
        @DisplayName("文件不属于该数据包时抛出 ORDER_FILE_NOT_FOUND")
        void savePrintInfo_fileNotInPackage_shouldThrow() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);
                when(packageService.getById(PACKAGE_ID)).thenReturn(testPackage);
                when(designProductService.list(any(Wrapper.class))).thenReturn(List.of());
                when(designProductService.remove(any(Wrapper.class))).thenReturn(true);
                // 文件不属于该包（count=0，但请求有 1 条）
                when(packageFileService.count(any(Wrapper.class))).thenReturn(0L);

                SavePrintInfoDTO dto = buildSavePrintInfoDTO();

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
                assertEquals(ErrorCodeEnum.ORDER_FILE_NOT_FOUND.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("权限校验：非设计师操作抛出 DESIGN_OPERATOR_NOT_ALLOWED（错误码741）")
        void savePrintInfo_notDesigner_shouldThrow740() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(888L); // 非设计师
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

                SavePrintInfoDTO dto = new SavePrintInfoDTO();
                dto.setProductMark("LGC");
                dto.setItems(Collections.emptyList());

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> printInfoService.savePrintInfo(ORDER_ID, PACKAGE_ID, dto));
                assertEquals(741, ex.getCode());
            }
        }
    }

    // ==================== deletePrintInfo 测试 ====================

    @Nested
    @DisplayName("deletePrintInfo 测试")
    class DeletePrintInfoTest {

        @Test
        @DisplayName("删除单条打印信息成功，先删文件关联再删产品行")
        void deletePrintInfo_shouldDeleteFilesThenProduct() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

                DesignProductEntity entity = new DesignProductEntity();
                entity.setId(1L);
                entity.setOrderId(ORDER_ID);
                entity.setPackageId(PACKAGE_ID);
                when(designProductService.getById(1L)).thenReturn(entity);
                when(designProductService.removeById(1L)).thenReturn(true);
                doNothing().when(productFileService).removeByProductId(1L);

                printInfoService.deletePrintInfo(ORDER_ID, PACKAGE_ID, 1L);

                // 验证先删文件关联，再删产品行
                verify(productFileService, times(1)).removeByProductId(1L);
                verify(designProductService, times(1)).removeById(1L);
                verify(instructionMapper, times(1)).update(isNull(), any(Wrapper.class));
                verify(drawingMapper, times(1)).update(isNull(), any(Wrapper.class));
            }
        }

        @Test
        @DisplayName("删除打印信息后重置指令单和图纸的确认状态")
        void deletePrintInfo_shouldResetConfirmedStatus() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(DESIGNER_ID);
                when(orderMainService.getById(ORDER_ID)).thenReturn(designInProgressOrder);

                DesignProductEntity entity = new DesignProductEntity();
                entity.setId(2L);
                entity.setOrderId(ORDER_ID);
                entity.setPackageId(PACKAGE_ID);
                when(designProductService.getById(2L)).thenReturn(entity);
                when(designProductService.removeById(2L)).thenReturn(true);
                doNothing().when(productFileService).removeByProductId(2L);

                printInfoService.deletePrintInfo(ORDER_ID, PACKAGE_ID, 2L);

                verify(instructionMapper, times(1)).update(isNull(), any(Wrapper.class));
                verify(drawingMapper, times(1)).update(isNull(), any(Wrapper.class));
            }
        }
    }

    // ==================== 辅助方法 ====================

    private SavePrintInfoDTO buildSavePrintInfoDTO() {
        SavePrintInfoItemDTO item = new SavePrintInfoItemDTO();
        item.setPackageFileIds(List.of(FILE_ID));   // 多文件 ID 列表
        item.setProductId(PRODUCT_ID);
        item.setSpecId(SPEC_ID);
        item.setQuantity(1);
        item.setIsUrgent(0);
        item.setSortOrder(1);

        SavePrintInfoDTO dto = new SavePrintInfoDTO();
        dto.setProductMark("LGC");   // 包级必填字段
        dto.setItems(List.of(item));
        return dto;
    }

    private DesignPackageFileEntity buildPackageFile() {
        DesignPackageFileEntity pf = new DesignPackageFileEntity();
        pf.setId(FILE_ID);
        pf.setPackageId(PACKAGE_ID);
        pf.setFileName("左髋骨.stl");
        return pf;
    }
}
