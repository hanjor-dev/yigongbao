package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.constant.DesignConfigConstants;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.order.mapper.OrderMainMapper;
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
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DesignFileServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DesignFileServiceImpl 单元测试")
class DesignFileServiceImplTest {

    @Mock
    private OrderMainMapper orderMainMapper;

    @Mock
    private DesignPackageMapper packageMapper;

    @Mock
    private DesignPackageFileMapper packageFileMapper;

    @Mock
    private DesignModelMapper modelMapper;

    @Mock
    private DesignProductMapper productMapper;

    @Mock
    private FileService fileService;

    @Mock
    private CodeGeneratorService codeGeneratorService;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private DesignFileServiceImpl designFileService;

    private OrderMainEntity designingOrder;
    private final Long orderId = 1L;
    private final Long designerId = 100L;

    @BeforeEach
    void setUp() {
        // 设计中状态的订单
        designingOrder = new OrderMainEntity();
        designingOrder.setId(orderId);
        designingOrder.setOrderCode("202604150001");
        designingOrder.setStatus(FlowStatusEnum.DESIGN_IN_PROGRESS.getValue());
        designingOrder.setDesignerId(designerId);
    }

    @Nested
    @DisplayName("uploadPackage 测试")
    class UploadPackageTest {

        @Test
        @DisplayName("不支持的压缩包格式抛出异常")
        void shouldThrowExceptionForUnsupportedFormat() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "test.tar", "application/x-tar", new byte[10]);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.uploadPackage(orderId, file));

                assertEquals(ErrorCodeEnum.DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED.getCode(), exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("deletePackage 测试")
    class DeletePackageTest {

        @Test
        @DisplayName("数据包不存在抛出异常")
        void shouldThrowExceptionWhenPackageNotFound() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);
                when(packageMapper.selectById(999L)).thenReturn(null);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.deletePackage(orderId, 999L));

                assertEquals(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND.getCode(), exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("uploadModel 测试")
    class UploadModelTest {

        @Test
        @DisplayName("成功上传可视化模型")
        void shouldUploadModelSuccessfully() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                FileVO fileVO = new FileVO();
                fileVO.setId("file-456");
                fileVO.setFileUrl("http://example.com/model.stl");
                fileVO.setFileSize(2048L);
                when(fileService.uploadFile(any(), eq("10.7"))).thenReturn(fileVO);

                when(modelMapper.insert(any(DesignModelEntity.class))).thenAnswer(invocation -> {
                    DesignModelEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return 1;
                });

                MockMultipartFile file = new MockMultipartFile(
                        "file", "model.stl", "application/octet-stream", "content".getBytes());

                DesignModelVO result = designFileService.uploadModel(orderId, file);

                assertNotNull(result);
                assertEquals("file-456", result.getFileId());
                assertEquals("model.stl", result.getFileName());
                assertEquals("stl", result.getFileExt());

                verify(modelMapper).insert(any(DesignModelEntity.class));
            }
        }
    }

    @Nested
    @DisplayName("deleteModel 测试")
    class DeleteModelTest {

        @Test
        @DisplayName("成功删除可视化模型")
        void shouldDeleteModelSuccessfully() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                DesignModelEntity modelEntity = new DesignModelEntity();
                modelEntity.setId(1L);
                modelEntity.setOrderId(orderId);
                modelEntity.setFileId("file-456");
                when(modelMapper.selectById(1L)).thenReturn(modelEntity);

                designFileService.deleteModel(orderId, 1L);

                verify(modelMapper).deleteById(1L);
                verify(fileService).deleteById("file-456");
            }
        }

        @Test
        @DisplayName("模型不存在抛出异常")
        void shouldThrowExceptionWhenModelNotFound() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);
                when(modelMapper.selectById(999L)).thenReturn(null);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.deleteModel(orderId, 999L));

                assertEquals(ErrorCodeEnum.DESIGN_MODEL_NOT_FOUND.getCode(), exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("uploadReport 测试")
    class UploadReportTest {

        @Test
        @DisplayName("成功上传设计报告")
        void shouldUploadReportSuccessfully() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                // 无已有报告
                when(fileService.listByBiz("10.5", orderId)).thenReturn(Collections.emptyList());

                FileVO fileVO = new FileVO();
                fileVO.setId("file-789");
                fileVO.setFileName("report.pdf");
                when(fileService.uploadAndLink(any(), eq("10.5"), eq(orderId))).thenReturn(fileVO);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "report.pdf", "application/pdf", "content".getBytes());

                FileVO result = designFileService.uploadReport(orderId, file);

                assertNotNull(result);
                assertEquals("file-789", result.getId());
                verify(fileService).uploadAndLink(any(), eq("10.5"), eq(orderId));
            }
        }

        @Test
        @DisplayName("上传新报告时删除旧报告")
        void shouldDeleteOldReportWhenUploadNew() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                // 有已有报告
                FileVO oldReport = new FileVO();
                oldReport.setId("old-file");
                when(fileService.listByBiz("10.5", orderId)).thenReturn(List.of(oldReport));

                FileVO newFileVO = new FileVO();
                newFileVO.setId("new-file");
                when(fileService.uploadAndLink(any(), eq("10.5"), eq(orderId))).thenReturn(newFileVO);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "report.pdf", "application/pdf", "content".getBytes());

                designFileService.uploadReport(orderId, file);

                // 验证删除旧报告
                verify(fileService).deleteById("old-file");
                verify(fileService).uploadAndLink(any(), eq("10.5"), eq(orderId));
            }
        }
    }

    @Nested
    @DisplayName("权限校验测试")
    class PermissionTest {

        @Test
        @DisplayName("订单不存在抛出异常")
        void shouldThrowExceptionWhenOrderNotFound() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);
                when(orderMainMapper.selectById(999L)).thenReturn(null);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "test.zip", "application/zip", new byte[10]);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.uploadPackage(999L, file));

                assertEquals(ErrorCodeEnum.ORDER_NOT_FOUND.getCode(), exception.getCode());
            }
        }

        @Test
        @DisplayName("订单不在设计阶段抛出异常")
        void shouldThrowExceptionWhenNotInDesignPhase() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(designerId);

                OrderMainEntity orderInPrint = new OrderMainEntity();
                orderInPrint.setId(orderId);
                orderInPrint.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
                orderInPrint.setDesignerId(designerId);
                when(orderMainMapper.selectById(orderId)).thenReturn(orderInPrint);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "test.zip", "application/zip", new byte[10]);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.uploadPackage(orderId, file));

                assertEquals(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED.getCode(), exception.getCode());
            }
        }

        @Test
        @DisplayName("非设计师操作抛出异常")
        void shouldThrowExceptionWhenNotDesigner() {
            try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
                Long otherUserId = 999L;
                stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(otherUserId);
                when(orderMainMapper.selectById(orderId)).thenReturn(designingOrder);

                MockMultipartFile file = new MockMultipartFile(
                        "file", "test.zip", "application/zip", new byte[10]);

                BusinessException exception = assertThrows(BusinessException.class,
                        () -> designFileService.uploadPackage(orderId, file));

                assertEquals(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED.getCode(), exception.getCode());
            }
        }
    }

    @Nested
    @DisplayName("listPackages 测试")
    class ListPackagesTest {

        @Test
        @DisplayName("返回空列表当无数据包")
        void shouldReturnEmptyListWhenNoPackages() {
            when(packageMapper.selectList(any(Wrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<DesignPackageVO> result = designFileService.listPackages(orderId);

            assertTrue(result.isEmpty());
        }
    }

    /**
     * 创建测试用的 ZIP 文件
     */
    private byte[] createTestZip(List<String> fileNames) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String fileName : fileNames) {
                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);
                zos.write(("content of " + fileName).getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
