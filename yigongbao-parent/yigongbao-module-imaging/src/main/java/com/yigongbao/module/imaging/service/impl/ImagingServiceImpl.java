package com.yigongbao.module.imaging.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import com.yigongbao.module.imaging.mapper.PartColorMapper;
import com.yigongbao.module.imaging.service.ImagingService;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 影像阅览服务实现
 * 只读操作：聚合订单影像文件和设计模型文件，为前端阅览提供数据
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingServiceImpl implements ImagingService {

    private final OrderFileMapper orderFileMapper;
    private final DesignPackageMapper designPackageMapper;
    private final DesignPackageFileMapper designPackageFileMapper;
    private final DesignModelMapper designModelMapper;
    private final PartColorMapper partColorMapper;
    private final FileService fileService;

    /**
     * DCM影像文件的字典编码
     */
    private static final String FILE_CATEGORY_DCM = "10.1";

    @Override
    public List<DcmPackageVO> getDcmPackages(Long orderId) {
        log.info("查询DCM影像包列表, orderId={}", orderId);

        // 1. 查询订单下的影像文件记录
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
                        .eq(OrderFileEntity::getFileCategory, FILE_CATEGORY_DCM)
                        .orderByAsc(OrderFileEntity::getId)
        );

        if (CollUtil.isEmpty(orderFiles)) {
            return new ArrayList<>();
        }

        // 2. 批量查询文件详情，避免 N+1
        List<String> fileIds = orderFiles.stream()
                .map(OrderFileEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 组装 VO
        return orderFiles.stream().map(of -> {
            DcmPackageVO vo = new DcmPackageVO();
            FileVO fileVO = fileMap.get(of.getFileId());
            vo.setFileId(of.getFileId());
            vo.setPackageNo(of.getPackageNo());
            vo.setUploadTime(of.getCreateTime());
            if (fileVO != null) {
                vo.setFileName(fileVO.getFileName());
                vo.setFileUrl(fileVO.getFileUrl());
                vo.setFileSize(fileVO.getFileSize());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PackageModelFileVO> getPackageModelFiles(Long packageId) {
        log.info("查询数据包内模型文件, packageId={}", packageId);

        // 1. 查询包内文件列表
        List<DesignPackageFileEntity> files = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder)
        );

        if (CollUtil.isEmpty(files)) {
            return new ArrayList<>();
        }

        // 2. 批量查询颜色透明度
        Map<String, PartColorEntity> colorMap = batchQueryColors(files.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 3. 组装 VO
        return files.stream()
                .map(f -> toPackageModelFileVO(f, colorMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<PackageModelGroupVO> getPackageModelFilesByOrder(Long orderId) {
        log.info("查询订单所有数据包模型文件（按包分组）, orderId={}", orderId);

        // 1. 查询该订单的所有数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq)
        );

        if (CollUtil.isEmpty(packages)) {
            return new ArrayList<>();
        }

        // 2. 批量查询所有包内文件
        List<Long> packageIds = packages.stream()
                .map(DesignPackageEntity::getId)
                .collect(Collectors.toList());
        List<DesignPackageFileEntity> allFiles = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .in(DesignPackageFileEntity::getPackageId, packageIds)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder)
        );

        // 3. 批量查询颜色透明度（一次 IN 查询，避免 N+1）
        Map<String, PartColorEntity> colorMap = batchQueryColors(allFiles.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 4. 按包分组组装 VO
        Map<Long, List<DesignPackageFileEntity>> filesByPackage = allFiles.stream()
                .collect(Collectors.groupingBy(DesignPackageFileEntity::getPackageId));

        return packages.stream().map(pkg -> {
            PackageModelGroupVO group = new PackageModelGroupVO();
            group.setPackageId(pkg.getId());
            group.setPackageCode(pkg.getPackageCode());
            List<DesignPackageFileEntity> pkgFiles = filesByPackage.getOrDefault(pkg.getId(), List.of());
            group.setFiles(pkgFiles.stream()
                    .map(f -> toPackageModelFileVO(f, colorMap))
                    .collect(Collectors.toList()));
            return group;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ModelVO> getModels(Long orderId) {
        log.info("查询可视化模型列表, orderId={}", orderId);

        // 1. 查询设计模型记录
        List<DesignModelEntity> models = designModelMapper.selectList(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .orderByAsc(DesignModelEntity::getId)
        );

        if (CollUtil.isEmpty(models)) {
            return new ArrayList<>();
        }

        // 2. 批量查询文件详情
        List<String> fileIds = models.stream()
                .map(DesignModelEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 批量查询颜色透明度（按文件名匹配）
        List<String> fileNames = fileMap.values().stream()
                .map(FileVO::getFileName)
                .collect(Collectors.toList());
        Map<String, PartColorEntity> colorMap = batchQueryColors(fileNames);

        // 4. 组装 VO
        return models.stream().map(m -> {
            ModelVO vo = new ModelVO();
            vo.setModelId(m.getId());
            vo.setFileId(m.getFileId());
            FileVO fileVO = fileMap.get(m.getFileId());
            if (fileVO != null) {
                vo.setFileName(fileVO.getFileName());
                vo.setFileUrl(fileVO.getFileUrl());
                vo.setFileSize(fileVO.getFileSize());
                // 颜色透明度按文件名去扩展名精确匹配（Hutool FileUtil.mainName）
                String nameWithoutExt = FileUtil.mainName(fileVO.getFileName());
                PartColorEntity color = colorMap.get(nameWithoutExt);
                if (color != null) {
                    vo.setColorCode(color.getColorCode());
                    vo.setOpacity(color.getOpacity());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 批量查询颜色透明度（一次 IN 查询）
     * key = partDetail（文件名去扩展名，使用 Hutool FileUtil.mainName）
     */
    private Map<String, PartColorEntity> batchQueryColors(List<String> fileNames) {
        if (CollUtil.isEmpty(fileNames)) {
            return Map.of();
        }
        // 提取所有文件名（去扩展名）作为查询条件，使用 Hutool FileUtil.mainName()
        Set<String> partDetails = fileNames.stream()
                .map(FileUtil::mainName)
                .collect(Collectors.toSet());

        List<PartColorEntity> colors = partColorMapper.selectList(
                new LambdaQueryWrapper<PartColorEntity>()
                        .in(PartColorEntity::getPartDetail, partDetails)
        );
        return colors.stream()
                .collect(Collectors.toMap(PartColorEntity::getPartDetail, c -> c,
                        (existing, replacement) -> existing)); // 同名取第一条
    }

    /**
     * 将 DesignPackageFileEntity 转换为 PackageModelFileVO，填充颜色透明度
     */
    private PackageModelFileVO toPackageModelFileVO(DesignPackageFileEntity f,
                                                     Map<String, PartColorEntity> colorMap) {
        PackageModelFileVO vo = new PackageModelFileVO();
        vo.setPackageFileId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileExt(f.getFileExt());
        vo.setFilePath(f.getFilePath());
        vo.setFileSize(f.getFileSize());
        // 精确匹配颜色（使用 Hutool FileUtil.mainName() 去扩展名）
        String nameWithoutExt = FileUtil.mainName(f.getFileName());
        PartColorEntity color = colorMap.get(nameWithoutExt);
        if (color != null) {
            vo.setColorCode(color.getColorCode());
            vo.setOpacity(color.getOpacity());
        }
        return vo;
    }
}
