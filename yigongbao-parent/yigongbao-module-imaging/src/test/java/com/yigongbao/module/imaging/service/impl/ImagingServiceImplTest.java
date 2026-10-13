package com.yigongbao.module.imaging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.service.DesignModelService;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import com.yigongbao.module.imaging.mapper.PartColorMapper;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.service.OrderFileService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ImagingServiceImpl 单元测试
 *
 * @author hanjor
 * @date 2026-04-20
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImagingServiceImplTest {

    @Mock
    private OrderFileService orderFileService;
    @Mock
    private DesignPackageService designPackageService;
    @Mock
    private DesignPackageFileService designPackageFileService;
    @Mock
    private DesignModelService designModelService;
    @Mock
    private PartColorMapper partColorMapper;
    @Mock
    private FileService fileService;

    @InjectMocks
    private ImagingServiceImpl imagingService;

    // ==================== getDcmPackages ====================

    @Nested
    @DisplayName("getDcmPackages - 获取DCM影像包列表")
    class GetDcmPackagesTest {

        @Test
        @DisplayName("返回订单的影像数据包列表")
        void shouldReturnDcmPackageList() {
            // given
            Long orderId = 1L;
            OrderFileEntity orderFile = new OrderFileEntity();
            orderFile.setOrderId(orderId);
            orderFile.setFileId("file001");
            orderFile.setPackageNo("PKG001");
            orderFile.setCreateTime(LocalDateTime.now());

            FileVO fileVO = new FileVO();
            fileVO.setId("file001");
            fileVO.setFileName("影像包.zip");
            fileVO.setFileUrl("https://example.com/file001");
            fileVO.setFileSize(1024L);

            when(orderFileService.listByOrderIdAndCategory(eq(orderId), any())).thenReturn(List.of(orderFile));
            when(fileService.listByIds(List.of("file001"))).thenReturn(List.of(fileVO));

            // when
            List<DcmPackageVO> result = imagingService.getDcmPackages(orderId);

            // then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("file001", result.get(0).getFileId());
            assertEquals("影像包.zip", result.get(0).getFileName());
            assertEquals("PKG001", result.get(0).getPackageNo());
        }

        @Test
        @DisplayName("订单无影像文件时返回空列表")
        void shouldReturnEmptyListWhenNoFiles() {
            // given
            when(orderFileService.listByOrderIdAndCategory(any(), any())).thenReturn(List.of());

            // when
            List<DcmPackageVO> result = imagingService.getDcmPackages(1L);

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(fileService, never()).listByIds(any());
        }
    }

    // ==================== getPackageModelFiles（数据包维度）====================

    @Nested
    @DisplayName("getPackageModelFiles - 数据包维度模型文件")
    class GetPackageModelFilesTest {

        @Test
        @DisplayName("返回数据包内文件列表并附带颜色透明度")
        void shouldReturnFilesWithColorAndOpacity() {
            // given
            Long packageId = 10L;
            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(101L);
            file.setPackageId(packageId);
            file.setFileName("右肺上叶.stl");
            file.setFileExt("stl");
            file.setFilePath("models/右肺上叶.stl");
            file.setFileSize(2048L);

            PartColorEntity color = new PartColorEntity();
            color.setPartDetail("右肺上叶");
            color.setColorCode("170,255,0");
            color.setOpacity(new BigDecimal("0.80"));

            when(designPackageFileService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of(color));

            // when
            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(packageId);

            // then
            assertEquals(1, result.size());
            PackageModelFileVO vo = result.get(0);
            assertEquals(101L, vo.getPackageFileId());
            assertEquals("右肺上叶.stl", vo.getFileName());
            assertEquals("170,255,0", vo.getColorCode());
            assertEquals(new BigDecimal("0.80"), vo.getOpacity());
        }

        @Test
        @DisplayName("文件名无法匹配颜色时 colorCode 和 opacity 为 null")
        void shouldReturnNullColorWhenNoMatch() {
            // given
            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(102L);
            file.setFileName("未知部位.stl");
            file.setFileExt("stl");

            when(designPackageFileService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of());

            // when
            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(1L);

            // then
            assertEquals(1, result.size());
            assertNull(result.get(0).getColorCode());
            assertNull(result.get(0).getOpacity());
        }

        @Test
        @DisplayName("数据包无文件时返回空列表")
        void shouldReturnEmptyListWhenNoFiles() {
            when(designPackageFileService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<PackageModelFileVO> result = imagingService.getPackageModelFiles(1L);

            assertTrue(result.isEmpty());
            verify(partColorMapper, never()).selectList(any());
        }
    }

    // ==================== getPackageModelFilesByOrder（订单维度）====================

    @Nested
    @DisplayName("getPackageModelFilesByOrder - 订单维度分组模型文件")
    class GetPackageModelFilesByOrderTest {

        @Test
        @DisplayName("返回按包分组的文件列表")
        void shouldReturnGroupedByPackage() {
            // given
            Long orderId = 1L;
            DesignPackageEntity pkg = new DesignPackageEntity();
            pkg.setId(10L);
            pkg.setOrderId(orderId);
            pkg.setPackageCode("ORD001-1");

            DesignPackageFileEntity file = new DesignPackageFileEntity();
            file.setId(101L);
            file.setPackageId(10L);
            file.setFileName("右肺上叶.stl");
            file.setFileExt("stl");
            file.setFilePath("models/右肺上叶.stl");
            file.setFileSize(2048L);

            when(designPackageService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(pkg));
            when(designPackageFileService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
            when(partColorMapper.selectList(any())).thenReturn(List.of());

            // when
            List<PackageModelGroupVO> result = imagingService.getPackageModelFilesByOrder(orderId);

            // then
            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).getPackageId());
            assertEquals("ORD001-1", result.get(0).getPackageCode());
            assertEquals(1, result.get(0).getFiles().size());
        }

        @Test
        @DisplayName("订单无数据包时返回空列表")
        void shouldReturnEmptyWhenNoPackages() {
            when(designPackageService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<PackageModelGroupVO> result = imagingService.getPackageModelFilesByOrder(1L);

            assertTrue(result.isEmpty());
        }
    }

    // ==================== getModels ====================

    @Nested
    @DisplayName("getModels - 可视化模型列表")
    class GetModelsTest {

        @Test
        @DisplayName("返回可视化模型列表并附带颜色透明度")
        void shouldReturnModelsWithColor() {
            // given
            Long orderId = 1L;
            DesignModelEntity model = new DesignModelEntity();
            model.setId(1L);
            model.setOrderId(orderId);
            model.setFileId("fileABC");

            FileVO fileVO = new FileVO();
            fileVO.setId("fileABC");
            fileVO.setFileName("整体模型.stl");
            fileVO.setFileUrl("https://example.com/fileABC");
            fileVO.setFileSize(4096L);

            PartColorEntity color = new PartColorEntity();
            color.setPartDetail("整体模型");
            color.setColorCode("255,0,0");
            color.setOpacity(new BigDecimal("0.90"));

            when(designModelService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(model));
            when(fileService.listByIds(List.of("fileABC"))).thenReturn(List.of(fileVO));
            when(partColorMapper.selectList(any())).thenReturn(List.of(color));

            // when
            List<ModelVO> result = imagingService.getModels(orderId);

            // then
            assertEquals(1, result.size());
            ModelVO vo = result.get(0);
            assertEquals(1L, vo.getModelId());
            assertEquals("fileABC", vo.getFileId());
            assertEquals("整体模型.stl", vo.getFileName());
            assertEquals("255,0,0", vo.getColorCode());
            assertEquals(new BigDecimal("0.90"), vo.getOpacity());
        }

        @Test
        @DisplayName("订单无模型时返回空列表")
        void shouldReturnEmptyWhenNoModels() {
            when(designModelService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            List<ModelVO> result = imagingService.getModels(1L);

            assertTrue(result.isEmpty());
        }
    }
}
