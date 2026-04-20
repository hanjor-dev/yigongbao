package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.enums.DesignModeEnum;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.config.service.ConfigService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignDocServiceImpl 单元测试")
class DesignDocServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private DesignPackageService packageService;
    @Mock private DesignProductService productService;
    @Mock private DesignInstructionService instructionService;
    @Mock private DesignDrawingService drawingService;
    @Mock private InstructionExcelBuilder instructionBuilder;
    @Mock private DrawingExcelBuilder drawingBuilder;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private FileService fileService;
    @Mock private DesignProductFileService productFileService;
    @Mock private DesignScreenshotService screenshotService;
    @Mock private ConfigService configService;

    @InjectMocks
    private DesignDocServiceImpl docService;

    private static final Long ORDER_ID = 1L;
    private static final Long PACKAGE_ID = 10L;
    private static final Long USER_ID = 100L;

    private OrderMainEntity order;
    private DesignPackageEntity pkg;
    private FileVO mockFileVO;

    @BeforeEach
    void setUp() {
        order = new OrderMainEntity();
        order.setId(ORDER_ID);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(USER_ID);
        order.setOrderCode("ORD-001");

        pkg = new DesignPackageEntity();
        pkg.setId(PACKAGE_ID);
        pkg.setOrderId(ORDER_ID);
        pkg.setPackageCode("PKG-001");

        mockFileVO = new FileVO();
        mockFileVO.setId("file-001");
        mockFileVO.setFileUrl("http://storage/test.xlsx");
    }

    @Nested
    @DisplayName("generateInstruction")
    class GenerateInstruction {

        @Test
        @DisplayName("成功生成指令单（首次，isConfirmed=0）")
        void generateInstruction_success() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                // 首次生成：无历史版本
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0001");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
                // 验证 isConfirmed=0
                verify(instructionService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("已封版时生成指令单（版本号递增，isConfirmed=0）")
        void generateInstruction_afterRevised_incrementsVersion() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 最新版已封版（revisedFileId 非空）
                DesignInstructionEntity latestSealed = new DesignInstructionEntity();
                latestSealed.setVersionSeq(1);
                latestSealed.setRevisedFileId("revised-001");

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latestSealed);
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0002");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/2", result.getVersion());
                verify(instructionService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("未封版时重复生成（覆盖模板，isConfirmed重置为0）")
        void generateInstruction_notSealed_overwritesTemplate() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 最新版未封版（revisedFileId 为 null）且已确认
                DesignInstructionEntity latestOpen = new DesignInstructionEntity();
                latestOpen.setId(1L);
                latestOpen.setVersionSeq(1);
                latestOpen.setInstructionCode("ZL-0001");
                latestOpen.setRevisedFileId(null);
                latestOpen.setIsConfirmed(1);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latestOpen);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                verify(instructionService, never()).save(any());
                // 重新生成重置 isConfirmed=0
                verify(instructionService).updateById(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("打印信息未填写时抛出 PRINT_INFO_REQUIRED")
        void generateInstruction_noPrintInfo_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(0L);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateInstruction(ORDER_ID, PACKAGE_ID));
                assertEquals(750, ex.getCode());
            }
        }

        @Test
        @DisplayName("非设计师时抛出 DESIGN_OPERATOR_NOT_ALLOWED")
        void generateInstruction_notDesigner_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateInstruction(ORDER_ID, PACKAGE_ID));
                assertEquals(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED.getCode(), ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("generateDrawing")
    class GenerateDrawing {

        @Test
        @DisplayName("成功生成图纸（首次，isConfirmed=0）")
        void generateDrawing_success() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(java.util.Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateDrawing(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
                // 验证生成的 entity 中 isConfirmed=0（通过 save 的参数捕获验证）
                verify(drawingService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("重新生成图纸时重置确认状态（isConfirmed=0）")
        void generateDrawing_resetsConfirmed() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 最新版已封版但已确认
                DesignDrawingEntity latestSealed = new DesignDrawingEntity();
                latestSealed.setId(1L);
                latestSealed.setPackageId(PACKAGE_ID);
                latestSealed.setVersionSeq(1);
                latestSealed.setRevisedFileId("revised-001");
                latestSealed.setIsConfirmed(1);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(latestSealed);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(java.util.Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                docService.generateDrawing(ORDER_ID, PACKAGE_ID);

                // 新版本 isConfirmed=0
                verify(drawingService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("覆盖未封版图纸时重置确认状态（isConfirmed=0）")
        void generateDrawing_overwrite_resetsConfirmed() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 最新版未封版但已确认
                DesignDrawingEntity latestOpen = new DesignDrawingEntity();
                latestOpen.setId(1L);
                latestOpen.setPackageId(PACKAGE_ID);
                latestOpen.setVersionSeq(1);
                latestOpen.setRevisedFileId(null);
                latestOpen.setIsConfirmed(1);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(latestOpen);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(java.util.Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.updateById(any())).thenReturn(true);

                docService.generateDrawing(ORDER_ID, PACKAGE_ID);

                // 覆盖时重置 isConfirmed=0
                verify(drawingService).updateById(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }
    }

    @Nested
    @DisplayName("uploadRevisedDrawing")
    class UploadRevisedDrawing {

        @Test
        @DisplayName("离线模式：上传修订版图纸后自动确认（isConfirmed=1）")
        void uploadRevisedDrawing_offlineAutoConfirm() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.OFFLINE.getCode()));

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(drawingService.getById(1L)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(drawingService.updateById(any())).thenReturn(true);

                docService.uploadRevisedDrawing(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                verify(drawingService).updateById(argThat(e -> Integer.valueOf(1).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("在线模式：上传修订版图纸不自动确认（isConfirmed保持0）")
        void uploadRevisedDrawing_onlineNoAutoConfirm() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.ONLINE.getCode()));

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(drawingService.getById(1L)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(drawingService.updateById(any())).thenReturn(true);

                docService.uploadRevisedDrawing(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                // 在线模式不自动确认：isConfirmed 仍为 0
                verify(drawingService).updateById(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }
    }

    @Nested
    @DisplayName("confirmDrawing")
    class ConfirmDrawing {

        @Test
        @DisplayName("确认图纸成功")
        void confirmDrawing_success() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(drawingService.getById(1L)).thenReturn(entity);
                when(drawingService.updateById(any())).thenReturn(true);

                assertDoesNotThrow(() -> docService.confirmDrawing(ORDER_ID, PACKAGE_ID, 1L));

                verify(drawingService).updateById(argThat(e ->
                        Integer.valueOf(1).equals(e.getIsConfirmed()) && e.getConfirmTime() != null));
            }
        }

        @Test
        @DisplayName("图纸版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void confirmDrawing_notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(drawingService.getById(999L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.confirmDrawing(ORDER_ID, PACKAGE_ID, 999L));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("图纸版本不属于当前数据包时抛出 DOC_VERSION_NOT_FOUND")
        void confirmDrawing_wrongPackage_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setId(1L);
                entity.setPackageId(999L); // 不属于当前 packageId
                when(drawingService.getById(1L)).thenReturn(entity);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.confirmDrawing(ORDER_ID, PACKAGE_ID, 1L));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("uploadRevisedInstruction")
    class UploadRevisedInstruction {

        @Test
        @DisplayName("版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void uploadRevised_notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(instructionService.getById(999L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 999L,
                                mock(MultipartFile.class)));
                assertEquals(751, ex.getCode());
            }
        }

        @Test
        @DisplayName("离线模式：上传修订版指令单后自动确认（isConfirmed=1）")
        void uploadRevised_offlineAutoConfirm() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.OFFLINE.getCode()));

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(instructionService.getById(1L)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                verify(instructionService).updateById(argThat(e -> Integer.valueOf(1).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("在线模式：上传修订版指令单不自动确认（isConfirmed保持0）")
        void uploadRevised_onlineNoAutoConfirm() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
                when(configService.getConfigValue(any())).thenReturn(String.valueOf(DesignModeEnum.ONLINE.getCode()));

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(instructionService.getById(1L)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                verify(instructionService).updateById(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }
    }

    @Nested
    @DisplayName("confirmInstruction")
    class ConfirmInstruction {

        @Test
        @DisplayName("确认指令单成功")
        void confirmInstruction_success() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                when(instructionService.getById(1L)).thenReturn(entity);
                when(instructionService.updateById(any())).thenReturn(true);

                assertDoesNotThrow(() -> docService.confirmInstruction(ORDER_ID, PACKAGE_ID, 1L));

                verify(instructionService).updateById(argThat(e ->
                        Integer.valueOf(1).equals(e.getIsConfirmed()) && e.getConfirmTime() != null));
            }
        }

        @Test
        @DisplayName("指令单版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void confirmInstruction_notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(instructionService.getById(999L)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.confirmInstruction(ORDER_ID, PACKAGE_ID, 999L));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("指令单版本不属于当前数据包时抛出 DOC_VERSION_NOT_FOUND")
        void confirmInstruction_wrongPackage_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(999L);
                when(instructionService.getById(1L)).thenReturn(entity);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.confirmInstruction(ORDER_ID, PACKAGE_ID, 1L));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }
    }

    // ==================== 辅助方法 ====================

    private DesignProductFileEntity buildProductFile() {
        DesignProductFileEntity f = new DesignProductFileEntity();
        f.setId(1L);
        f.setDesignProductId(1L);
        f.setPackageFileId(100L);
        f.setPackageFileName("左髋骨.stl");
        return f;
    }
}
