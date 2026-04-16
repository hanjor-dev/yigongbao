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
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.vo.DocItemVO;
import com.yigongbao.module.order.service.OrderMainService;
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
        @DisplayName("成功生成指令单（首次，无历史版本）")
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
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
            }
        }

        @Test
        @DisplayName("已封版时生成指令单（版本号递增）")
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
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/2", result.getVersion());
            }
        }

        @Test
        @DisplayName("未封版时重复生成（覆盖模板，版本号不变）")
        void generateInstruction_notSealed_overwritesTemplate() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                // 最新版未封版（revisedFileId 为 null）
                DesignInstructionEntity latestOpen = new DesignInstructionEntity();
                latestOpen.setId(1L);
                latestOpen.setVersionSeq(1);
                latestOpen.setInstructionCode("ZL-0001");
                latestOpen.setRevisedFileId(null);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getLatestVersion(PACKAGE_ID)).thenReturn(latestOpen);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                DocItemVO result = docService.generateInstruction(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                // 版本号不变
                assertEquals("A/1", result.getVersion());
                // 不应调用 save（只走 update）
                verify(instructionService, never()).save(any());
                verify(instructionService).updateById(any());
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
        @DisplayName("成功生成图纸")
        void generateDrawing_success() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(drawingService.getLatestVersion(PACKAGE_ID)).thenReturn(null);
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});
                when(fileService.uploadBytes(any(), any(), any())).thenReturn(mockFileVO);
                when(drawingService.save(any())).thenReturn(true);

                DocItemVO result = docService.generateDrawing(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertEquals("A/1", result.getVersion());
                assertEquals("file-001", result.getFileId());
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
        @DisplayName("成功上传修订版指令单")
        void uploadRevised_success() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                DesignInstructionEntity entity = new DesignInstructionEntity();
                entity.setId(1L);
                entity.setPackageId(PACKAGE_ID);
                when(instructionService.getById(1L)).thenReturn(entity);
                when(fileService.uploadFile(any(), any())).thenReturn(mockFileVO);
                when(instructionService.updateById(any())).thenReturn(true);

                assertDoesNotThrow(() -> docService.uploadRevisedInstruction(ORDER_ID, PACKAGE_ID, 1L,
                        mock(MultipartFile.class)));
            }
        }
    }
}
