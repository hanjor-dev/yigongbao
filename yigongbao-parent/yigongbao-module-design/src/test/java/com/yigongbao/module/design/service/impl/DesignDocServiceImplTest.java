package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
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
import com.yigongbao.module.design.vo.GenerateDocsResultVO;
import com.yigongbao.module.order.service.OrderMainService;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
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
    @Mock private FileStorageService fileStorageService;

    @Spy
    @InjectMocks
    private DesignDocServiceImpl docService;

    private static final Long ORDER_ID = 1L;
    private static final Long PACKAGE_ID = 10L;
    private static final Long USER_ID = 100L;

    private OrderMainEntity order;
    private DesignPackageEntity pkg;

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
    }

    @Nested
    @DisplayName("generateDocs")
    class GenerateDocs {

        @Test
        @DisplayName("成功生成指令单和图纸")
        void generateDocs_success() throws Exception {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(2L);
                when(instructionService.getMaxVersionSeq(PACKAGE_ID)).thenReturn(0);
                when(drawingService.getMaxVersionSeq(PACKAGE_ID)).thenReturn(0);
                when(codeGeneratorService.generate(any())).thenReturn("ZL-0001");
                when(productService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(new DesignProductEntity()));
                when(instructionBuilder.build(any())).thenReturn(new byte[]{1, 2, 3});
                when(drawingBuilder.build(any())).thenReturn(new byte[]{4, 5, 6});

                // uploadBytes 是 protected 方法，用 spy + doReturn 绕过 FileStorageService 链式调用
                FileInfo mockFileInfo = new FileInfo();
                mockFileInfo.setId("file-001");
                mockFileInfo.setUrl("http://storage/test.xlsx");
                doReturn(mockFileInfo).when(docService).uploadBytes(any(), any(), any(), any());

                when(instructionService.save(any())).thenReturn(true);
                when(drawingService.save(any())).thenReturn(true);

                GenerateDocsResultVO result = docService.generateDocs(ORDER_ID, PACKAGE_ID);

                assertNotNull(result);
                assertNotNull(result.getInstruction());
                assertEquals("A/1", result.getInstruction().getVersion());
            }
        }

        @Test
        @DisplayName("打印信息未填写时抛出 PRINT_INFO_REQUIRED")
        void generateDocs_noPrintInfo_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);
                when(productService.count(any())).thenReturn(0L); // 无打印信息

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateDocs(ORDER_ID, PACKAGE_ID));
                assertEquals(750, ex.getCode());
            }
        }

        @Test
        @DisplayName("非设计师时抛出 DESIGN_OPERATOR_NOT_ALLOWED")
        void generateDocs_notDesigner_throwsException() {
            try (MockedStatic<StpUtil> stpMock = mockStatic(StpUtil.class)) {
                stpMock.when(StpUtil::getLoginIdAsLong).thenReturn(999L); // 非设计师

                when(orderMainService.getById(ORDER_ID)).thenReturn(order);
                when(packageService.getById(PACKAGE_ID)).thenReturn(pkg);

                BusinessException ex = assertThrows(BusinessException.class,
                        () -> docService.generateDocs(ORDER_ID, PACKAGE_ID));
                assertEquals(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED.getCode(), ex.getCode());
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
    }
}
