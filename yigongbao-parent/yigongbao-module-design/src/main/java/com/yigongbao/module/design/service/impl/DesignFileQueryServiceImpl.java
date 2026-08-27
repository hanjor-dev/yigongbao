package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DesignPackageFileVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.order.service.DesignFileQueryService;
import com.yigongbao.module.order.vo.order.DesignFileDetailVO;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为订单详情提供设计阶段文件信息。
 */
@Service
public class DesignFileQueryServiceImpl implements DesignFileQueryService {

    private final DesignFileService designFileService;
    private final DesignDocService designDocService;
    private final DesignProductMapper designProductMapper;

    public DesignFileQueryServiceImpl(@Lazy DesignFileService designFileService,
                                      DesignDocService designDocService,
                                      DesignProductMapper designProductMapper) {
        this.designFileService = designFileService;
        this.designDocService = designDocService;
        this.designProductMapper = designProductMapper;
    }

    @Override
    public DesignFileDetailVO getDesignFiles(Long orderId) {
        List<DesignPackageVO> packages = designFileService.listPackages(orderId);
        enrichLatestDocuments(packages);

        DesignFileDetailVO result = new DesignFileDetailVO();
        result.setPackageList(packages.stream()
                .map(this::toOrderPackage)
                .collect(Collectors.toList()));
        result.setReport(designFileService.getReport(orderId));
        return result;
    }

    private DesignFileDetailVO.DesignPackageVO toOrderPackage(DesignPackageVO source) {
        DesignFileDetailVO.DesignPackageVO target = new DesignFileDetailVO.DesignPackageVO();
        target.setId(source.getId());
        target.setOrderId(source.getOrderId());
        target.setOrderCode(source.getOrderCode());
        target.setPackageCode(source.getPackageCode());
        target.setPackageSeq(source.getPackageSeq());
        target.setFileId(source.getFileId());
        target.setFileName(source.getFileName());
        target.setFileUrl(source.getFileUrl());
        target.setDownloadUrl(source.getDownloadUrl());
        target.setFileSize(source.getFileSize());
        target.setFileCount(source.getFileCount());
        target.setUploadTime(source.getUploadTime());
        target.setFiles(source.getFiles() == null ? Collections.emptyList() : source.getFiles().stream()
                .map(this::toOrderPackageFile)
                .collect(Collectors.toList()));
        target.setLatestInstruction(toOrderDoc(source.getLatestInstruction()));
        target.setLatestDrawing(toOrderDoc(source.getLatestDrawing()));
        target.setLatestDrawings(source.getLatestDrawings() == null ? Collections.emptyList()
                : source.getLatestDrawings().stream().map(this::toOrderDoc).collect(Collectors.toList()));
        return target;
    }

    private DesignFileDetailVO.DesignPackageFileVO toOrderPackageFile(DesignPackageFileVO source) {
        DesignFileDetailVO.DesignPackageFileVO target = new DesignFileDetailVO.DesignPackageFileVO();
        target.setId(source.getId());
        target.setPackageId(source.getPackageId());
        target.setFileName(source.getFileName());
        target.setFileExt(source.getFileExt());
        target.setFilePath(source.getFilePath());
        target.setFileSize(source.getFileSize());
        target.setSortOrder(source.getSortOrder());
        target.setHasPrintInfo(source.getHasPrintInfo());
        target.setFileUrl(source.getFileUrl());
        target.setDownloadUrl(source.getDownloadUrl());
        return target;
    }

    private DesignFileDetailVO.DesignDocVersionVO toOrderDoc(DesignDocVersionVO source) {
        if (source == null) {
            return null;
        }
        DesignFileDetailVO.DesignDocVersionVO target = new DesignFileDetailVO.DesignDocVersionVO();
        target.setId(source.getId());
        target.setVersion(source.getVersion());
        target.setVersionSeq(source.getVersionSeq());
        target.setSourceType(source.getSourceType());
        target.setTemplateFileId(source.getTemplateFileId());
        target.setTemplateFileUrl(source.getTemplateFileUrl());
        target.setTemplateDownloadUrl(source.getTemplateDownloadUrl());
        target.setRevisedFileId(source.getRevisedFileId());
        target.setRevisedFileUrl(source.getRevisedFileUrl());
        target.setRevisedDownloadUrl(source.getRevisedDownloadUrl());
        target.setGenerateTime(source.getGenerateTime());
        target.setRevisedUploadTime(source.getRevisedUploadTime());
        target.setIsConfirmed(source.getIsConfirmed());
        target.setProductCategory(source.getProductCategory());
        target.setConfirmTime(source.getConfirmTime());
        return target;
    }

    private void enrichLatestDocuments(List<DesignPackageVO> packages) {
        if (packages == null || packages.isEmpty()) {
            return;
        }

        Set<Long> packageIds = packages.stream()
                .map(DesignPackageVO::getId)
                .collect(Collectors.toSet());
        Map<Long, DesignDocVersionVO> latestInstructions =
                designDocService.getLatestInstructionMap(packageIds);
        Map<Long, List<DesignDocVersionVO>> latestDrawings =
                designDocService.getLatestDrawingGroups(packageIds);
        List<DesignProductEntity> products = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .in(DesignProductEntity::getPackageId, packageIds));
        Map<Long, Set<String>> productCategories = products.stream()
                .filter(product -> product.getProductCategory() != null
                        && !product.getProductCategory().isBlank())
                .collect(Collectors.groupingBy(DesignProductEntity::getPackageId,
                        Collectors.mapping(DesignProductEntity::getProductCategory, Collectors.toSet())));

        for (DesignPackageVO source : packages) {
            source.setLatestInstruction(latestInstructions.get(source.getId()));
            List<DesignDocVersionVO> drawings = latestDrawings.getOrDefault(
                    source.getId(), Collections.emptyList());
            source.setLatestDrawings(drawings);
            int categoryCount = productCategories.getOrDefault(
                    source.getId(), Collections.emptySet()).size();
            source.setLatestDrawing(categoryCount <= 1 && drawings.size() == 1
                    ? drawings.get(0) : null);
        }
    }
}
