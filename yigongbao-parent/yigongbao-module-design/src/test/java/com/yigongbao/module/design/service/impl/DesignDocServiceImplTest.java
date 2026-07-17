package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.helper.DrawingExcelBuilder;
import com.yigongbao.module.design.helper.InstructionExcelBuilder;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileScreenshotMapper;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.config.service.ConfigService;
import jakarta.servlet.http.HttpServletResponse;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignDocServiceImpl 单元测试
 * <p>
 * 测试重点：
 * 1. ensureInstruction/ensureDrawing 的按需生成决策（4种场景）
 * 2. downloadInstruction/downloadDrawing（线下模式）
 * 3. getInstructionPreviewUrl/getDrawingPreviewUrl（在线模式）
 * 4. uploadRevised 自动确认
 * 5. confirm 手动确认
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignDocServiceImpl 单元测试")
class DesignDocServiceImplTest {

    @Mock private OrderMainService orderMainService;
    @Mock private DesignPackageService packageService;
    @Mock private DesignProductService productService;
    @Mock private DesignProductMapper designProductMapper;
    @Mock private DesignInstructionService instructionService;
    @Mock private DesignDrawingService drawingService;
    @Mock private InstructionExcelBuilder instructionBuilder;
    @Mock private DrawingExcelBuilder drawingBuilder;
    @Mock private CodeGeneratorService codeGeneratorService;
    @Mock private FileService fileService;
    @Mock private DesignProductFileService productFileService;
    @Mock private DesignScreenshotService screenshotService;
    @Mock private DesignQueryHelper designQueryHelper;
    @Mock private DesignPackageFileScreenshotMapper screenshotMapper;
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
    void setUp() throws IOException {
        doNothing().when(designQueryHelper).checkIsAssignedDesigner(any());
        when(screenshotMapper.getLatestUpdateTime(anyLong())).thenReturn(null);
        when(configService.getConfigValue(SystemConfigKeyEnum.IMAGING_VIEWER_BASE_URL.getKey()))
                .thenReturn("http://viewer/#/aiView");
        FileVO qrFileVO = new FileVO();
        qrFileVO.setId("qr-file-001");
        qrFileVO.setFileUrl("http://storage/test-qr.png");
        when(fileService.listByBiz(eq("10.21"), anyLong())).thenReturn(List.of(qrFileVO));
        doReturn(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})
                .when(fileService).downloadToBytes("qr-file-001");
        order = new OrderMainEntity();
        order.setId(ORDER_ID);
        order.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        order.setDesignerId(USER_ID);
        order.setOrderCode("ORD-001");

        pkg = new DesignPackageEntity();
        pkg.setId(PACKAGE_ID);
        pkg.setOrderId(ORDER_ID);
        pkg.setPackageCode("PKG-001");
        pkg.setUpdateTime(LocalDateTime.now().minusDays(1));

        mockFileVO = new FileVO();
        mockFileVO.setId("file-001");
        mockFileVO.setFileUrl("http://storage/test.xlsx");
    }

    @Test
    void listDrawingVersions_validatesContextAndMapsVersionEntities() {
        DesignDrawingEntity drawing = new DesignDrawingEntity();
        drawing.setId(5L);
        drawing.setPackageId(PACKAGE_ID);
        drawing.setVersion("A/1");
        drawing.setVersionSeq(1);
        drawing.setTemplateFileId("drawing-1");
        drawing.setTemplateFileUrl("http://storage/drawing.xlsx");
        drawing.setIsConfirmed(1);
        when(orderMainService.getById(ORDER_ID)).thenReturn(order);
        when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
        when(drawingService.listVersions(PACKAGE_ID)).thenReturn(List.of(drawing));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
            var versions = docService.listDrawingVersions(ORDER_ID, PACKAGE_ID);

            assertEquals(1, versions.size());
            assertEquals("A/1", versions.get(0).getVersion());
            assertEquals("drawing-1", versions.get(0).getTemplateFileId());
        }
    }

    // ==================== getInstructionPreviewUrl（在线模式） ====================

    @Nested
    @DisplayName("getInstructionPreviewUrl（在线模式）")
    class GetInstructionPreviewUrl {

        @Test
        @DisplayName("场景1：首次调用，自动生成 A/1，isConfirmed=0")
        void firstTime_generatesA1() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(LocalDateTime.now().minusHours(1));
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0001");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
                assertEquals(0, result.getIsConfirmed());
                verify(instructionService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
            }
        }

        @Test
        @DisplayName("场景4：打印信息未变化，复用已有版本，不触发重新生成")
        void dataUnchanged_reusesExisting() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 已有记录，generateTime 在数据变化时间之后（数据未变）
                LocalDateTime generateTime = LocalDateTime.now().minusMinutes(10);
                LocalDateTime dataTime = generateTime.minusMinutes(5); // 数据更新在生成之前

                DesignInstructionEntity latest = new DesignInstructionEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setGenerateTime(generateTime);
                latest.setTemplateFileId("file-001");
                latest.setTemplateFileUrl("http://storage/test.xlsx");
                latest.setIsConfirmed(1);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(dataTime);

                DocItemVO result = docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals(1, result.getIsConfirmed());
                // 未触发重新生成
                verify(instructionService, never()).save(any());
                verify(instructionService, never()).updateById(any());
                verify(instructionBuilder, never()).build(any());
            }
        }

        @Test
        @DisplayName("场景2：数据变化且未封版，覆盖当前版本，isConfirmed 重置为 0")
        void dataChanged_notSealed_overwritesAndResetsConfirmed() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusHours(2);
                LocalDateTime dataTime = LocalDateTime.now().minusHours(1); // 数据更新在生成之后

                DesignInstructionEntity latest = new DesignInstructionEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setInstructionCode("ZL-0001");
                latest.setGenerateTime(generateTime);
                latest.setRevisedFileId(null); // 未封版
                latest.setIsConfirmed(1); // 已确认，但数据变了，应重置

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(generateTime.minusMinutes(30)); // 包更新时间早于产品更新时间
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                DocItemVO result = docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion()); // 版本号不变
                // 覆盖时 isConfirmed 重置为 0
                verify(instructionService).updateById(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())));
                verify(instructionService, never()).save(any()); // 覆盖，不新建
            }
        }

        @Test
        @DisplayName("场景3：数据变化且已封版，新建下一版本（A/2），isConfirmed=0")
        void dataChanged_sealed_createsNewVersion() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusHours(2);
                LocalDateTime dataTime = LocalDateTime.now().minusHours(1);

                DesignInstructionEntity latest = new DesignInstructionEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setGenerateTime(generateTime);
                latest.setRevisedFileId("revised-001"); // 已封版
                latest.setSourceType(com.yigongbao.common.constant.StatusConstants.SOURCE_TYPE_MANUAL);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(generateTime.minusMinutes(30));
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0002");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/2", result.getVersion()); // 新版本
                verify(instructionService).save(argThat(e ->
                        Integer.valueOf(0).equals(e.getIsConfirmed()) && e.getVersionSeq() == 2));
            }
        }

        @Test
        @DisplayName("打印信息未填写时抛出 PRINT_INFO_REQUIRED")
        void noPrintInfo_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(0L);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID));
                assertEquals(ErrorCodeEnum.PRINT_INFO_REQUIRED.getCode(), ex.getCode());
            }
        }

        @Test
        @DisplayName("非订单设计师时抛出 DESIGN_OPERATOR_NOT_ALLOWED")
        void notDesigner_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L); // 非设计师

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                assertThrows(BusinessException.class,
                        () -> docService.getInstructionPreviewUrl(ORDER_ID, PACKAGE_ID));
            }
        }
    }

    // ==================== getDrawingPreviewUrl（在线模式） ====================

    @Nested
    @DisplayName("getDrawingPreviewUrl（在线模式）")
    class GetDrawingPreviewUrl {

        @Test
        @DisplayName("场景1：首次调用，自动生成 A/1，isConfirmed=0")
        void firstTime_generatesA1() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(LocalDateTime.now().minusHours(1));
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                DocItemVO result = docService.getDrawingPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
                assertEquals(0, result.getIsConfirmed());
                verify(drawingService).save(argThat(e -> Integer.valueOf(0).equals(e.getIsConfirmed())
                        && "qr-file-001".equals(e.getQrFileId())));
                verify(drawingBuilder).build(argThat(ctx ->
                        "FRONTEND_FILE".equals(ctx.getQrSource())
                                && ctx.getQrBytes() != null
                                && ctx.getQrBytes().length > 0));
                verify(fileService).downloadToBytes("qr-file-001");
            }
        }

        @Test
        @DisplayName("首次生成但没有前端二维码时使用后端兜底二维码")
        void firstTimeWithoutQr_usesBackendFallback() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(1L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(fileService.listByBiz("10.21", ORDER_ID)).thenReturn(Collections.emptyList());
                when(productService.list(any(LambdaQueryWrapper.class)))
                        .thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(Collections.emptyList());
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                DocItemVO result = docService.getDrawingPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertEquals("file-001", result.getFileId());
                verify(drawingBuilder).build(argThat(ctx ->
                        "BACKEND_FALLBACK".equals(ctx.getQrSource())
                                && ctx.getQrBytes() != null
                                && ctx.getQrBytes().length > 0));
                verify(drawingService).save(argThat(e -> e.getQrFileId() == null));
                verify(fileService, never()).downloadToBytes(anyString());
            }
        }

        @Test
        @DisplayName("场景4：打印信息未变化，复用已有版本，不触发重新生成")
        void dataUnchanged_reusesExisting() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusMinutes(10);
                LocalDateTime dataTime = generateTime.minusMinutes(5);

                DesignDrawingEntity latest = new DesignDrawingEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setGenerateTime(generateTime);
                latest.setTemplateFileId("file-001");
                latest.setTemplateFileUrl("http://storage/test.xlsx");
                latest.setQrFileId("qr-file-001");
                latest.setIsConfirmed(1);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(dataTime);

                DocItemVO result = docService.getDrawingPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals(1, result.getIsConfirmed());
                verify(drawingService, never()).save(any());
                verify(drawingService, never()).updateById(any());
                verify(drawingBuilder, never()).build(any());
            }
        }

        @Test
        @DisplayName("当前二维码替换后，自动图纸重新生成并记录新二维码快照")
        void qrChanged_regeneratesAutoDrawing() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusMinutes(10);
                LocalDateTime dataTime = generateTime.minusMinutes(5);
                DesignDrawingEntity latest = new DesignDrawingEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setGenerateTime(generateTime);
                latest.setTemplateFileId("old-file");
                latest.setQrFileId("old-qr-file");

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(1L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(dataTime);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(Collections.emptyList());
                when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(Collections.emptyMap());
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.updateById(any())).thenReturn(true);

                docService.getDrawingPreviewUrl(ORDER_ID, PACKAGE_ID);

                verify(drawingService).updateById(argThat(e -> "qr-file-001".equals(e.getQrFileId())));
                verify(fileService).downloadToBytes("qr-file-001");
            }
        }

        @Test
        @DisplayName("当前二维码替换后，手工图纸保持原版本不自动重生成")
        void qrChanged_keepsManualDrawing() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusMinutes(10);
                LocalDateTime dataTime = generateTime.minusMinutes(5);
                DesignDrawingEntity latest = new DesignDrawingEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setSourceType(com.yigongbao.common.constant.StatusConstants.SOURCE_TYPE_MANUAL);
                latest.setGenerateTime(generateTime);
                latest.setTemplateFileId("manual-file");
                latest.setQrFileId("old-qr-file");

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(1L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(dataTime);

                DocItemVO result = docService.getDrawingPreviewUrl(ORDER_ID, PACKAGE_ID);

                assertEquals("manual-file", result.getFileId());
                verify(drawingBuilder, never()).build(any());
                verify(drawingService, never()).updateById(any());
                verify(drawingService, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("downloadDrawing（线下模式）")
    class DownloadDrawing {

        @Test
        void firstTime_generatesWithCurrentQrAndDownloadsDrawing() throws Exception {
            when(orderMainService.getById(ORDER_ID)).thenReturn(order);
            when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
            when(productService.count(any())).thenReturn(1L);
            when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
            when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
            when(productFileService.listByProductIds(any())).thenReturn(Collections.emptyList());
            when(screenshotService.listFileIdsByPackageFileIds(any())).thenReturn(Collections.emptyMap());
            when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
            when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
            when(drawingService.save(any())).thenReturn(true);

            docService.downloadDrawing(ORDER_ID, PACKAGE_ID, mock(HttpServletResponse.class));

            verify(fileService).download(eq("file-001"), any(HttpServletResponse.class));
            verify(drawingService).save(argThat(e -> "qr-file-001".equals(e.getQrFileId())));
        }
    }

    // ==================== downloadInstruction（线下模式） ====================

    @Nested
    @DisplayName("downloadInstruction（线下模式）")
    class DownloadInstruction {

        @Test
        @DisplayName("打印信息未变化时，复用已有文件流式下载，不重新生成")
        void dataUnchanged_downloadsExistingFile() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                LocalDateTime generateTime = LocalDateTime.now().minusMinutes(10);
                LocalDateTime dataTime = generateTime.minusMinutes(5);

                DesignInstructionEntity latest = new DesignInstructionEntity();
                latest.setId(1L);
                latest.setVersionSeq(1);
                latest.setVersion("A/1");
                latest.setGenerateTime(generateTime);
                latest.setTemplateFileId("file-001");
                latest.setIsConfirmed(0);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latest);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(dataTime);
                pkg.setUpdateTime(dataTime);
                doNothing().when(fileService).download(eq("file-001"), any());

                HttpServletResponse response = mock(HttpServletResponse.class);
                assertDoesNotThrow(() -> docService.downloadInstruction(ORDER_ID, PACKAGE_ID, response));

                verify(fileService).download(eq("file-001"), any());
                verify(instructionBuilder, never()).build(any()); // 未重新生成
            }
        }

        @Test
        @DisplayName("首次下载时自动生成并流式返回")
        void firstTime_generatesAndDownloads() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(designProductMapper.getLatestUpdateTime(PACKAGE_ID)).thenReturn(LocalDateTime.now().minusHours(1));
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0001");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(productFileService.listByProductIds(any())).thenReturn(List.of(buildProductFile()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);
                doNothing().when(fileService).download(eq("file-001"), any());

                HttpServletResponse response = mock(HttpServletResponse.class);
                assertDoesNotThrow(() -> docService.downloadInstruction(ORDER_ID, PACKAGE_ID, response));

                verify(instructionBuilder).build(any());
                verify(fileService).download(eq("file-001"), any());
            }
        }
    }

    // ==================== uploadRevisedInstruction ====================

    @Nested
    @DisplayName("uploadRevisedInstruction")
    class UploadRevisedInstruction {

        @Test
        @DisplayName("上传修订版指令单后自动确认（isConfirmed=1）")
        void uploadRevised_autoConfirms() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                entity.setVersionSeq(1);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                verify(instructionService).save(argThat(e ->
                        Integer.valueOf(1).equals(e.getIsConfirmed())
                                && "A/2".equals(e.getVersion())
                                && e.getTemplateFileId() != null));
            }
        }

        @Test
        @DisplayName("版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 999L,
                                mock(MultipartFile.class)));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }
    }

    // ==================== uploadRevisedDrawing ====================

    @Nested
    @DisplayName("uploadRevisedDrawing")
    class UploadRevisedDrawing {

        @Test
        @DisplayName("上传修订版图纸后自动确认（isConfirmed=1）")
        void uploadRevised_autoConfirms() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignDrawingEntity entity = new DesignDrawingEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                entity.setIsConfirmed(0);
                entity.setVersionSeq(1);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                docService.uploadRevisedDrawing(ORDER_ID, PACKAGE_ID, 1L, mock(MultipartFile.class));

                verify(drawingService).save(argThat(e ->
                        Integer.valueOf(1).equals(e.getIsConfirmed())
                                && "A/2".equals(e.getVersion())
                                && e.getTemplateFileId() != null));
            }
        }

        @Test
        @DisplayName("版本不存在时抛出 DOC_VERSION_NOT_FOUND")
        void notFound_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.uploadRevisedDrawing(ORDER_ID, PACKAGE_ID, 999L,
                                mock(MultipartFile.class)));
                assertEquals(ErrorCodeEnum.DOC_VERSION_NOT_FOUND.getCode(), ex.getCode());
            }
        }
    }

    // ==================== confirmDrawing ====================

    @Nested
    @DisplayName("confirmDrawing")
    class ConfirmDrawing {

        @Test
        @DisplayName("确认图纸成功（isConfirmed=1，confirmTime 已填充）")
        void confirm_success() {
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
        void notFound_throwsException() {
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
        void wrongPackage_throwsException() {
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

    // ==================== confirmInstruction ====================

    @Nested
    @DisplayName("confirmInstruction")
    class ConfirmInstruction {

        @Test
        @DisplayName("确认指令单成功（isConfirmed=1，confirmTime 已填充）")
        void confirm_success() {
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
        void notFound_throwsException() {
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
        void wrongPackage_throwsException() {
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
