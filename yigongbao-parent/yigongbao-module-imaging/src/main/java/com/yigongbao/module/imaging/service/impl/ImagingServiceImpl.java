package com.yigongbao.module.imaging.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.service.DesignModelService;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.imaging.entity.PartColorEntity;
import com.yigongbao.module.imaging.mapper.PartColorMapper;
import com.yigongbao.module.imaging.service.ImagingService;
import com.yigongbao.module.imaging.vo.DcmPackageVO;
import com.yigongbao.module.imaging.vo.ModelVO;
import com.yigongbao.module.imaging.vo.PackageModelFileVO;
import com.yigongbao.module.imaging.vo.PackageModelGroupVO;
import org.springframework.web.multipart.MultipartFile;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.service.OrderFileService;
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
 * <p>
 * 只读聚合服务：跨 order / design 模块查询影像文件和设计模型文件，
 * 为前端阅览页提供统一的数据视图。依赖各模块 Service 接口，不直接操作跨模块 Mapper。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImagingServiceImpl implements ImagingService {

    /** 订单文件服务（order 模块）：查询 order_file 表 */
    private final OrderFileService orderFileService;

    /** 设计数据包服务（design 模块）：查询 design_package 表 */
    private final DesignPackageService designPackageService;

    /** 设计数据包文件服务（design 模块）：查询 design_package_file 表 */
    private final DesignPackageFileService designPackageFileService;

    /** 设计模型服务（design 模块）：查询 design_model 表 */
    private final DesignModelService designModelService;

    /** 部位颜色 Mapper（imaging 模块自有表，无对应 Service）*/
    private final PartColorMapper partColorMapper;

    /** 文件服务（basic 模块）：通过文件ID批量查询文件详情 */
    private final FileService fileService;

    // ==================== 公开接口实现 ====================

    /**
     * 获取订单的 DCM 影像数据包列表。
     * <p>
     * 流程：查询 order_file（fileCategory=10.1）→ 批量查文件详情 → 组装 VO。
     * </p>
     *
     * @param orderId 订单ID
     * @return DCM 影像包 VO 列表，无数据时返回空列表
     */
    @Override
    public List<DcmPackageVO> getDcmPackages(Long orderId) {
        log.info("查询DCM影像包列表, orderId={}", orderId);

        // 1. 通过 OrderFileService 查询订单下的 DCM 影像文件记录
        List<OrderFileEntity> orderFiles = orderFileService.listByOrderIdAndCategory(
                orderId, DictCodeConstants.ORDER_FILE_CATEGORY_DCM);

        if (CollUtil.isEmpty(orderFiles)) {
            return new ArrayList<>();
        }

        // 2. 收集所有 fileId，批量查询文件详情，避免 N+1
        List<String> fileIds = orderFiles.stream()
                .map(OrderFileEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 组装 VO：将 orderFile 和 fileVO 合并为返回视图
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

    /**
     * 获取指定数据包内的模型文件列表（含颜色透明度）。
     * <p>
     * 流程：查询 design_package_file → 批量查颜色透明度（一次 IN）→ 内存匹配组装 VO。
     * </p>
     *
     * @param packageId 数据包ID
     * @return 模型文件 VO 列表，无数据时返回空列表
     */
    @Override
    public List<PackageModelFileVO> getPackageModelFiles(Long packageId) {
        log.info("查询数据包内模型文件, packageId={}", packageId);

        // 1. 通过 DesignPackageFileService 查询包内文件，按 sortOrder 排序
        List<DesignPackageFileEntity> files = designPackageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder));

        if (CollUtil.isEmpty(files)) {
            return new ArrayList<>();
        }

        // 2. 收集文件名，批量查颜色透明度（避免逐文件查询）
        Map<String, PartColorEntity> colorMap = batchQueryColors(files.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 3. 将每个文件记录转换为 VO，内存中按文件名（去扩展名）匹配颜色
        return files.stream()
                .map(f -> toPackageModelFileVO(f, colorMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取订单所有数据包内的模型文件，按包分组（含颜色透明度）。
     * <p>
     * 流程：查订单所有包 → 批量查全部包内文件（一次 IN）→ 批量查颜色透明度 → 按包 ID 分组组装。
     * 整个过程只有 3 次 DB 查询，无 N+1。
     * </p>
     *
     * @param orderId 订单ID
     * @return 按包分组的模型文件 VO 列表，无数据时返回空列表
     */
    @Override
    public List<PackageModelGroupVO> getPackageModelFilesByOrder(Long orderId) {
        log.info("查询订单所有数据包模型文件（按包分组）, orderId={}", orderId);

        // 1. 通过 DesignPackageService 查询该订单的所有数据包，按序号排序
        List<DesignPackageEntity> packages = designPackageService.list(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq));

        if (CollUtil.isEmpty(packages)) {
            return new ArrayList<>();
        }

        // 2. 收集所有包ID，一次 IN 查询取出全部包内文件
        List<Long> packageIds = packages.stream()
                .map(DesignPackageEntity::getId)
                .collect(Collectors.toList());
        List<DesignPackageFileEntity> allFiles = designPackageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .in(DesignPackageFileEntity::getPackageId, packageIds)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder));

        // 3. 批量查颜色透明度（一次 IN，覆盖所有包的文件名）
        Map<String, PartColorEntity> colorMap = batchQueryColors(allFiles.stream()
                .map(DesignPackageFileEntity::getFileName)
                .collect(Collectors.toList()));

        // 4. 将文件列表按 packageId 分组，方便后续按包装配
        Map<Long, List<DesignPackageFileEntity>> filesByPackage = allFiles.stream()
                .collect(Collectors.groupingBy(DesignPackageFileEntity::getPackageId));

        // 5. 按原包顺序组装分组 VO
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

    /**
     * 获取订单的可视化模型列表（含颜色透明度）。
     * <p>
     * 流程：查询 design_model → 批量查文件详情 → 批量查颜色透明度 → 按文件名匹配组装 VO。
     * </p>
     *
     * @param orderId 订单ID
     * @return 可视化模型 VO 列表，无数据时返回空列表
     */
    @Override
    public List<ModelVO> getModels(Long orderId) {
        log.info("查询可视化模型列表, orderId={}", orderId);

        // 1. 通过 DesignModelService 查询设计模型记录
        List<DesignModelEntity> models = designModelService.list(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .orderByAsc(DesignModelEntity::getId));

        if (CollUtil.isEmpty(models)) {
            return new ArrayList<>();
        }

        // 2. 收集 fileId，批量查文件详情
        List<String> fileIds = models.stream()
                .map(DesignModelEntity::getFileId)
                .collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        // 3. 从文件名提取部位名称，批量查颜色透明度（按文件名匹配）
        List<String> fileNames = fileMap.values().stream()
                .map(FileVO::getFileName)
                .collect(Collectors.toList());
        Map<String, PartColorEntity> colorMap = batchQueryColors(fileNames);

        // 4. 逐条组装 VO，颜色透明度按文件名去扩展名精确匹配
        return models.stream().map(m -> {
            ModelVO vo = new ModelVO();
            vo.setModelId(m.getId());
            vo.setFileId(m.getFileId());
            FileVO fileVO = fileMap.get(m.getFileId());
            if (fileVO != null) {
                vo.setFileName(fileVO.getFileName());
                vo.setFileUrl(fileVO.getFileUrl());
                vo.setFileSize(fileVO.getFileSize());
                // 颜色匹配：文件名去扩展名后与 part_colors.part_detail 精确比对
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

    // ==================== 私有辅助方法 ====================

    /**
     * 批量查询部位颜色透明度（一次 IN 查询）。
     * <p>
     * 将文件名列表去扩展名后作为 {@code part_detail} 的查询条件，
     * 返回以 {@code partDetail} 为 key 的 Map，供调用方 O(1) 查找。
     * 同一 partDetail 有多条记录时取第一条。
     * </p>
     *
     * @param fileNames 文件名列表（含扩展名）
     * @return partDetail → PartColorEntity 的映射，fileNames 为空时返回空 Map
     */
    private Map<String, PartColorEntity> batchQueryColors(List<String> fileNames) {
        if (CollUtil.isEmpty(fileNames)) {
            return Map.of();
        }
        // 使用 Hutool FileUtil.mainName() 去扩展名，去重后作为 IN 查询条件
        Set<String> partDetails = fileNames.stream()
                .map(FileUtil::mainName)
                .collect(Collectors.toSet());

        List<PartColorEntity> colors = partColorMapper.selectList(
                new LambdaQueryWrapper<PartColorEntity>()
                        .in(PartColorEntity::getPartDetail, partDetails));

        // 同名 partDetail 存在多条时取第一条（颜色配置表应保持唯一，此处作容错处理）
        return colors.stream()
                .collect(Collectors.toMap(PartColorEntity::getPartDetail, c -> c,
                        (existing, replacement) -> existing));
    }

    /**
     * 将 {@link DesignPackageFileEntity} 转换为 {@link PackageModelFileVO}，
     * 并填充颜色透明度（从预查询的 colorMap 中按文件名匹配）。
     *
     * @param f        数据包文件记录
     * @param colorMap partDetail → PartColorEntity 的颜色映射
     * @return 填充完整的 PackageModelFileVO
     */
    private PackageModelFileVO toPackageModelFileVO(DesignPackageFileEntity f,
                                                     Map<String, PartColorEntity> colorMap) {
        PackageModelFileVO vo = new PackageModelFileVO();
        vo.setPackageFileId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setFileExt(f.getFileExt());
        vo.setFilePath(f.getFilePath());
        vo.setFileUrl(f.getFileUrl());
        vo.setFileSize(f.getFileSize());
        // 使用 Hutool FileUtil.mainName() 去扩展名后，精确匹配颜色配置
        String nameWithoutExt = FileUtil.mainName(f.getFileName());
        PartColorEntity color = colorMap.get(nameWithoutExt);
        if (color != null) {
            vo.setColorCode(color.getColorCode());
            vo.setOpacity(color.getOpacity());
        }
        return vo;
    }
}
