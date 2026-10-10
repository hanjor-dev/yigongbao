package com.yigongbao.module.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.service.FileDownloadUrlByUrlRequest;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.ArchiveFileInfo;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.service.DesignInstructionService;
import com.yigongbao.module.design.service.DesignDrawingService;
import com.yigongbao.module.design.service.DesignModelService;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignPackageService;
import com.yigongbao.module.design.entity.DesignPackageFileScreenshotEntity;
import com.yigongbao.module.design.entity.DesignPackageBatchEntity;
import com.yigongbao.module.design.enums.DesignPackageBatchStatus;
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.util.ArchiveParserUtil;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageFileVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import cn.dev33.satoken.stp.StpUtil;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计文件服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignFileServiceImpl implements DesignFileService {

    private final OrderMainService orderMainService;
    private final DesignPackageService packageService;
    private final DesignPackageFileService packageFileService;
    private final DesignModelService modelService;
    private final DesignProductService productService;
    private final DesignProductFileService productFileService;
    private final DesignScreenshotService screenshotService;
    private final DesignInstructionService instructionService;
    private final DesignDrawingService drawingService;
    private final FileService fileService;
    private final CodeGeneratorService codeGeneratorService;
    private final ConfigService configService;
    private final com.yigongbao.module.design.helper.DesignQueryHelper designQueryHelper;
    private final com.yigongbao.module.design.service.DesignPackageBatchService batchService;
    private final FlowFacade flowFacade;

    // ==================== 数据包 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignPackageVO uploadPackage(Long orderId, MultipartFile file) {
        return uploadPackageInternal(orderId, null, file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignPackageVO uploadPackage(Long orderId, Long batchId, MultipartFile file) {
        return uploadPackageInternal(orderId, batchId, file);
    }

    private DesignPackageVO uploadPackageInternal(Long orderId, Long batchId, MultipartFile file) {
        // 0. 校验文件非空
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "上传文件不能为空");
        }

        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许上传新的设计数据包
        orderMainService.checkNotClassicCase(orderId, "上传设计数据包");
        OrderMainEntity order = batchId == null ? checkDesignPhase(orderId) : checkAdditionalBatch(orderId, batchId);
        checkIsAssignedDesigner(order);

        // 2. 校验压缩包容器格式（由配置决定允许的格式）
        String fileName = file.getOriginalFilename();
        Set<String> archiveExts = fileService.parseAllowedExtensions(
                configService.getConfigValue(SystemConfigKeyEnum.DESIGN_PACKAGE_ARCHIVE_EXTENSIONS.getKey()),
                ".zip,.rar,.7z,.tar");
        String fileExt = fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : "";
        if (fileExt.isEmpty() || !archiveExts.contains(fileExt)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED);
        }

        // 3. 校验压缩包大小
        String maxSizeMbStr = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_PACKAGE_MAX_SIZE_MB.getKey());
        fileService.assertFileSizeAllowed(file.getSize(), maxSizeMbStr, 500, "打印文件包");

        // 4. 校验文件名格式并提取数据包编号
        // 规则：文件名（去扩展名）必须为 {订单编号}-{数字}，如 202607030001-1
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String expectedPrefix = order.getOrderCode() + "-";
        String seqPart = baseName.startsWith(expectedPrefix)
                ? baseName.substring(expectedPrefix.length()) : "";
        if (seqPart.isEmpty() || !seqPart.matches("\\d{1,9}")) {
            log.warn("数据包文件名格式不符合规则: fileName={}, orderCode={}", fileName, order.getOrderCode());
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NAME_INVALID);
        }
        String packageCode = baseName;
        int packageSeq = Integer.parseInt(seqPart);

        // 5. 校验数据包编号唯一性
        boolean codeExists = packageService.lambdaQuery()
                .eq(DesignPackageEntity::getPackageCode, packageCode)
                .exists();
        if (codeExists) {
            log.warn("数据包编号已存在: packageCode={}", packageCode);
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_CODE_EXISTS);
        }

        // 6. 写临时文件（流式，不占堆内存，避免大文件 OOM）
        File tempFile = null;
        try {
            // 创建临时文件
            try {
                tempFile = java.nio.file.Files.createTempFile("design_pkg_", fileExt).toFile();
            } catch (IOException e) {
                log.error("创建临时文件失败", e);
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
            }

            // 将上传文件写入临时文件
            try (InputStream in = file.getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                in.transferTo(out);
            } catch (IOException e) {
                log.error("写入临时文件失败", e);
                throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
            }

            // 7. 解析压缩包内文件列表（第一次读取临时文件）
            Set<String> allowedExtensions = getAllowedExtensions();
            List<ArchiveFileInfo> archiveFiles;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile)) {
                archiveFiles = ArchiveParserUtil.parse(fis, fileName, allowedExtensions);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("解析压缩包失败", e);
                throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
            }

            // 8. 校验是否有有效文件
            if (CollUtil.isEmpty(archiveFiles)) {
                throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_EMPTY);
            }

            // 9. 上传压缩包文件（第二次读取临时文件，流式上传）
            FileVO fileVO;
            try (java.io.FileInputStream fis = new java.io.FileInputStream(tempFile)) {
                fileVO = fileService.uploadStream(fis, tempFile.length(), fileName,
                        FileBizTypeEnum.PRINT_PACKAGE.getDictCode());
            } catch (IOException e) {
                log.error("读取临时文件失败", e);
                throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
            }

            // 10. 保存数据包记录
            DesignPackageEntity packageEntity = new DesignPackageEntity();
            packageEntity.setOrderId(orderId);
            packageEntity.setBatchId(batchId);
            packageEntity.setOrderCode(order.getOrderCode());
            packageEntity.setPackageCode(packageCode);
            packageEntity.setPackageSeq(packageSeq);
            packageEntity.setFileId(fileVO.getId());
            packageEntity.setFileName(fileName);
            packageEntity.setFileUrl(fileVO.getFileUrl());
            packageEntity.setFileSize(fileVO.getFileSize());
            packageEntity.setFileCount(archiveFiles.size());
            packageEntity.setUploadTime(LocalDateTime.now());
            packageService.save(packageEntity);

            // 11. 逐文件上传内部文件到 OSS，并保存包内文件记录
            List<DesignPackageFileEntity> fileEntities = new ArrayList<>();
            int sortOrder = 1;
            for (ArchiveFileInfo archiveFile : archiveFiles) {
                // 将包内文件独立上传到 OSS，获取独立访问 URL
                FileVO innerFileVO = fileService.uploadBytes(
                        archiveFile.getFileContent(),
                        archiveFile.getFileName(),
                        FileBizTypeEnum.PACKAGE_FILE.getDictCode());

                DesignPackageFileEntity fileEntity = new DesignPackageFileEntity();
                fileEntity.setPackageId(packageEntity.getId());
                fileEntity.setFileName(archiveFile.getFileName());
                fileEntity.setFileExt(archiveFile.getExtension().replace(".", ""));
                fileEntity.setFilePath(archiveFile.getFilePath());
                fileEntity.setFileSize(archiveFile.getFileSize());
                fileEntity.setSortOrder(sortOrder++);
                fileEntity.setFileId(innerFileVO.getId());
                fileEntity.setFileUrl(innerFileVO.getFileUrl());
                fileEntities.add(fileEntity);
            }
            // 批量插入
            packageFileService.saveBatch(fileEntities);

            // 首次成功上传追加数据包后，订单进入设计中；仅打开追加页面不改变订单状态。
            if (batchId != null && !Objects.equals(order.getStatus(), 2020)) {
                flowFacade.executeAdditionalDesignStart(orderId,
                        FlowOperator.of(StpUtil.getLoginIdAsLong(), null), order.getVersion());
            }

            // 12. 构建返回结果
            log.info("上传数据包: orderId={}, packageCode={}, fileCount={}", orderId, packageCode, archiveFiles.size());
            return buildPackageVO(packageEntity, fileEntities);
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                cn.hutool.core.io.FileUtil.del(tempFile);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long orderId, Long packageId) {
        deletePackageInternal(orderId, null, packageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long orderId, Long batchId, Long packageId) {
        deletePackageInternal(orderId, batchId, packageId);
    }

    private void deletePackageInternal(Long orderId, Long batchId, Long packageId) {
        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许删除设计数据包
        orderMainService.checkNotClassicCase(orderId, "删除设计数据包");
        OrderMainEntity order = batchId == null ? checkDesignPhase(orderId) : checkAdditionalBatch(orderId, batchId);
        checkIsAssignedDesigner(order);

        // 2. 查询数据包
        DesignPackageEntity packageEntity = packageService.getById(packageId);
        if (packageEntity == null || !packageEntity.getOrderId().equals(orderId)
                || (batchId != null && !batchId.equals(packageEntity.getBatchId()))) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        // 3. 检查是否有关联的打印产品
        long productCount = productService.countByPackageId(packageId);
        if (productCount > 0) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_HAS_PRODUCTS);
        }

        // 4. 检查是否已生成指令单或图纸，有则先清理 OSS 文件再删除记录
        List<com.yigongbao.module.design.entity.DesignInstructionEntity> instructions = instructionService.list(
                new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignInstructionEntity>()
                        .eq(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId, packageId));
        for (com.yigongbao.module.design.entity.DesignInstructionEntity inst : instructions) {
            if (inst.getTemplateFileId() != null) fileService.deleteById(inst.getTemplateFileId());
            if (inst.getRevisedFileId() != null) fileService.deleteById(inst.getRevisedFileId());
        }
        if (!instructions.isEmpty()) {
            instructionService.remove(new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignInstructionEntity>()
                    .eq(com.yigongbao.module.design.entity.DesignInstructionEntity::getPackageId, packageId));
        }

        List<com.yigongbao.module.design.entity.DesignDrawingEntity> drawings = drawingService.list(
                new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignDrawingEntity>()
                        .eq(com.yigongbao.module.design.entity.DesignDrawingEntity::getPackageId, packageId));
        for (com.yigongbao.module.design.entity.DesignDrawingEntity drawing : drawings) {
            if (drawing.getTemplateFileId() != null) fileService.deleteById(drawing.getTemplateFileId());
            if (drawing.getRevisedFileId() != null) fileService.deleteById(drawing.getRevisedFileId());
        }
        if (!drawings.isEmpty()) {
            drawingService.remove(new LambdaQueryWrapper<com.yigongbao.module.design.entity.DesignDrawingEntity>()
                    .eq(com.yigongbao.module.design.entity.DesignDrawingEntity::getPackageId, packageId));
        }

        // 4. 删除包内文件的独立 OSS 存储（先查出文件ID，再批量删除）
        List<DesignPackageFileEntity> innerFiles = packageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId)
                        .isNotNull(DesignPackageFileEntity::getFileId));

        // 4.1 删除包内文件关联的截图（OSS文件 + DB记录）
        if (!innerFiles.isEmpty()) {
            List<Long> packageFileIds = innerFiles.stream().map(DesignPackageFileEntity::getId).toList();
            Map<Long, String> screenshotFileIds = screenshotService.listFileIdsByPackageFileIds(packageFileIds);
            for (String fileId : screenshotFileIds.values()) {
                fileService.deleteById(fileId);
            }
            if (!screenshotFileIds.isEmpty()) {
                screenshotService.deleteByPackageFileIds(packageFileIds);
            }
        }

        // 4.2 删除包内文件本身的 OSS 存储
        for (DesignPackageFileEntity innerFile : innerFiles) {
            fileService.deleteById(innerFile.getFileId());
        }

        // 5. 删除包内文件记录
        packageFileService.remove(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId));

        // 6. 删除数据包记录
        packageService.removeById(packageId);

        // 7. 删除存储的压缩包文件
        fileService.deleteById(packageEntity.getFileId());

        log.info("删除数据包: packageId={}, orderId={}", packageId, orderId);
    }

    @Override
    public List<DesignPackageVO> listPackages(Long orderId) {
        designQueryHelper.checkOrderReadable(orderId);
        return listPackagesInternal(orderId);
    }

    @Override
    public List<DesignPackageVO> listPackages(Long orderId, Long batchId) {
        designQueryHelper.checkOrderReadable(orderId);
        if (batchId != null) {
            checkAdditionalBatch(orderId, batchId);
        }
        return listPackagesInternal(orderId, batchId);
    }

    @Override
    public List<DesignPackageVO> listPackagesForOrderDetail(Long orderId) {
        return listPackagesInternal(orderId);
    }

    private List<DesignPackageVO> listPackagesInternal(Long orderId) {
        return listPackagesInternal(orderId, null);
    }

    private List<DesignPackageVO> listPackagesInternal(Long orderId, Long currentBatchId) {
        // 1. 查询数据包列表
        List<DesignPackageEntity> packages = packageService.list(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .eq(currentBatchId != null, DesignPackageEntity::getBatchId, currentBatchId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq));

        if (CollUtil.isEmpty(packages)) {
            return Collections.emptyList();
        }
        OrderMainEntity order = orderMainService.getById(orderId);

        // 2. 批量查询包内文件
        List<Long> packageIds = packages.stream()
                .map(DesignPackageEntity::getId)
                .collect(Collectors.toList());
        List<DesignPackageFileEntity> allFiles = packageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .in(DesignPackageFileEntity::getPackageId, packageIds)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder));

        // 3. 按 packageId 分组
        Map<Long, List<DesignPackageFileEntity>> fileMap = allFiles.stream()
                .collect(Collectors.groupingBy(DesignPackageFileEntity::getPackageId));

        // 4. 查询已填写打印信息的文件ID集合
        Set<Long> filledFileIds = getFilledFileIds(packageIds);

        // 5. 构建返回结果
        return packages.stream()
                .map(pkg -> {
                    DesignPackageVO vo = buildPackageVO(pkg, fileMap.getOrDefault(pkg.getId(), Collections.emptyList()), filledFileIds,
                            order == null ? null : order.getPublicOrderCode());
                    if (currentBatchId != null) {
                        boolean current = currentBatchId.equals(pkg.getBatchId());
                        vo.setIsCurrentBatch(current);
                        if (current) {
                            boolean editable = !DesignPackageBatchStatus.COMPLETED.name().equals(vo.getBatchStatus())
                                    && !DesignPackageBatchStatus.CANCELLED.name().equals(vo.getBatchStatus());
                            vo.setEditable(editable);
                            vo.setCanDelete(editable);
                            vo.setCanEditPrintInfo(editable);
                            vo.setCanEditDocuments(editable);
                        } else {
                            vo.setEditable(false);
                            vo.setCanDelete(false);
                            vo.setCanEditPrintInfo(false);
                            vo.setCanEditDocuments(false);
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询数据包包内文件列表
     *
     * @param orderId   订单ID（校验数据包归属）
     * @param packageId 数据包ID
     * @return 包内文件 VO 列表，按 sortOrder 升序
     */
    @Override
    public List<DesignPackageFileVO> listPackageFiles(Long orderId, Long packageId) {
        designQueryHelper.checkOrderReadable(orderId);
        // 1. 校验数据包归属
        DesignPackageEntity pkg = packageService.getById(packageId);
        if (pkg == null || !pkg.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        // 2. 查询包内文件（全量返回，按 sortOrder 升序）
        List<DesignPackageFileEntity> files = packageFileService.list(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId)
                        .orderByAsc(DesignPackageFileEntity::getSortOrder));

        // 3. 构建 VO
        return files.stream()
                .map(f -> {
                    DesignPackageFileVO vo = new DesignPackageFileVO();
                    vo.setId(f.getId());
                    vo.setPackageId(f.getPackageId());
                    vo.setFileName(f.getFileName());
                    vo.setFileExt(f.getFileExt());
                    vo.setFilePath(f.getFilePath());
                    vo.setFileSize(f.getFileSize());
                    vo.setSortOrder(f.getSortOrder());
                    vo.setFileUrl(f.getFileUrl());
                    vo.setDownloadUrl(fileService.generateDownloadUrl(f.getFileUrl(), f.getFileName()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ==================== 可视化模型 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DesignModelVO> linkModels(Long orderId, List<String> fileIds) {
        log.info("批量关联可视化模型, orderId={}, fileIds={}", orderId, fileIds);
        if (CollUtil.isEmpty(fileIds)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "至少选择一个可视化模型文件");
        }
        List<String> requestedFileIds = fileIds.stream().distinct().toList();

        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许关联新的STL模型文件
        orderMainService.checkNotClassicCase(orderId, "关联STL模型");
        checkIsAssignedDesigner(checkDesignAttachmentMutation(orderId));

        // 2. 批量校验文件是否存在（类型和大小已在上传时由 FileService/Provider 校验）
        List<FileVO> fileVOs = fileService.listByIds(fileIds);
        if (fileVOs.size() != requestedFileIds.size()) {
            // 找出不存在的 fileId
            Set<String> foundIds = fileVOs.stream().map(FileVO::getId).collect(Collectors.toSet());
            List<String> notFoundIds = requestedFileIds.stream().filter(id -> !foundIds.contains(id)).toList();
            log.warn("部分文件不存在, notFoundIds={}", notFoundIds);
            cleanupOrphanFiles(fileVOs, requestedFileIds);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 去重检查：查询订单已关联的模型文件
        List<DesignModelEntity> existingModels = modelService.list(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId));
        Set<String> existingFileIds = existingModels.stream()
                .map(DesignModelEntity::getFileId)
                .collect(Collectors.toSet());

        // 过滤掉已关联的文件
        List<String> newFileIds = requestedFileIds.stream()
                .filter(fileId -> !existingFileIds.contains(fileId))
                .collect(Collectors.toList());

        if (newFileIds.isEmpty()) {
            log.warn("所有文件已关联, orderId={}, fileIds={}", orderId, fileIds);
            throw new BusinessException(ErrorCodeEnum.DESIGN_MODEL_ALREADY_EXISTS);
        }

        if (newFileIds.size() < requestedFileIds.size()) {
            List<String> duplicateIds = requestedFileIds.stream()
                    .filter(existingFileIds::contains)
                    .collect(Collectors.toList());
            log.warn("部分文件已关联将被忽略, orderId={}, duplicateIds={}", orderId, duplicateIds);
        }

        try {
            validateFilesForAttachment(fileVOs, newFileIds, FileBizTypeEnum.VISUAL_MODEL.getDictCode());
        } catch (RuntimeException ex) {
            cleanupOrphanFiles(fileVOs, newFileIds);
            throw ex;
        }

        // 4. 批量关联文件到业务，并保存模型记录
        Map<String, FileVO> fileMap = fileVOs.stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));
        try {
            newFileIds.forEach(fileId ->
                    fileService.linkFile(fileId, FileBizTypeEnum.VISUAL_MODEL.getDictCode(), orderId));

            List<DesignModelEntity> modelEntities = newFileIds.stream()
                    .map(fileId -> {
                        DesignModelEntity entity = new DesignModelEntity();
                        entity.setOrderId(orderId);
                        entity.setFileId(fileId);
                        return entity;
                    })
                    .collect(Collectors.toList());
            modelService.saveBatch(modelEntities);

            List<DesignModelVO> results = modelEntities.stream()
                    .map(entity -> buildModelVO(entity, fileMap.get(entity.getFileId())))
                    .collect(Collectors.toList());

            log.info("批量关联可视化模型: orderId={}, count={}", orderId, results.size());
            return results;
        } catch (RuntimeException ex) {
            cleanupOrphanFiles(fileVOs, newFileIds);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long orderId, Long modelId) {
        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许删除STL模型文件
        orderMainService.checkNotClassicCase(orderId, "删除STL模型");
        checkIsAssignedDesigner(checkDesignAttachmentMutation(orderId));

        // 2. 查询模型
        DesignModelEntity modelEntity = modelService.getById(modelId);
        if (modelEntity == null || !modelEntity.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_MODEL_NOT_FOUND);
        }

        // 3. 删除模型记录
        modelService.removeById(modelId);

        // 4. 删除存储的文件
        fileService.deleteById(modelEntity.getFileId());

        log.info("删除可视化模型: modelId={}, orderId={}", modelId, orderId);
    }

    @Override
    public List<DesignModelVO> listModels(Long orderId) {
        // 1. 查询模型记录
        List<DesignModelEntity> models = modelService.list(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .orderByDesc(DesignModelEntity::getCreateTime));

        if (CollUtil.isEmpty(models)) {
            return Collections.emptyList();
        }

        // 2. 批量查询文件信息
        List<String> fileIds = models.stream()
                .map(DesignModelEntity::getFileId)
                .collect(Collectors.toList());
        List<FileVO> fileVOs = fileService.listByIds(fileIds);
        Map<String, FileVO> fileMap = fileVOs.stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f, (a, b) -> a));

        // 3. 构建 VO
        return models.stream()
                .map(entity -> buildModelVO(entity, fileMap.get(entity.getFileId())))
                .collect(Collectors.toList());
    }

    // ==================== 设计报告 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileVO> linkReport(Long orderId, List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "至少选择一个设计报告文件");
        }
        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许关联新的设计报告文件
        orderMainService.checkNotClassicCase(orderId, "关联设计报告");
        checkIsAssignedDesigner(checkDesignAttachmentMutation(orderId));

        // 2. 批量校验文件是否存在、类型和现有关联
        List<String> requestedFileIds = fileIds.stream().distinct().toList();
        List<FileVO> fileVOs = fileService.listByIds(requestedFileIds);
        if (fileVOs.size() != requestedFileIds.size()) {
            cleanupOrphanFiles(fileVOs, requestedFileIds);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        List<FileVO> existingReports = fileService.listByBiz(FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId);
        Set<String> existingFileIds = existingReports.stream()
                .map(FileVO::getId)
                .collect(Collectors.toSet());
        List<String> newFileIds = requestedFileIds.stream()
                .filter(fileId -> !existingFileIds.contains(fileId))
                .collect(Collectors.toList());
        if (newFileIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            validateFilesForAttachment(fileVOs, newFileIds, FileBizTypeEnum.DESIGN_REPORT.getDictCode());
        } catch (RuntimeException ex) {
            cleanupOrphanFiles(fileVOs, newFileIds);
            throw ex;
        }

        // 3. 追加关联新报告，不删除既有报告
        try {
            List<FileVO> results = new ArrayList<>();
            for (String newFileId : newFileIds) {
                results.add(fileService.linkFile(
                        newFileId, FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId));
            }
            log.info("批量关联设计报告: orderId={}, count={}", orderId, results.size());
            return results;
        } catch (RuntimeException ex) {
            cleanupOrphanFiles(fileVOs, newFileIds);
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long orderId, String fileId) {
        // 1. 校验工单状态和操作权限
        // 经典案例保护：经典案例订单不允许删除设计报告文件
        orderMainService.checkNotClassicCase(orderId, "删除设计报告");
        checkIsAssignedDesigner(checkDesignAttachmentMutation(orderId));

        // 2. 校验文件归属
        FileVO fileVO = fileService.getById(fileId);
        if (fileVO == null || !FileBizTypeEnum.DESIGN_REPORT.getDictCode().equals(fileVO.getBizType())
                || !orderId.equals(fileVO.getBizId())) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 删除文件
        fileService.deleteById(fileId);

        log.info("删除设计报告: fileId={}, orderId={}", fileId, orderId);
    }

    @Override
    public List<FileVO> getReports(Long orderId) {
        List<FileVO> reports = fileService.listByBiz(FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId);
        return CollUtil.isEmpty(reports) ? Collections.emptyList() : reports;
    }

    // ==================== 私有方法 ====================

    private OrderMainEntity checkDesignPhase(Long orderId) {
        OrderMainEntity order = designQueryHelper.checkDesignPhase(orderId);
        ensureOriginalDesignMutation(order);
        return order;
    }

    private void ensureOriginalDesignMutation(OrderMainEntity order) {
        if (order == null || !Set.of(1030, 2010, 2020).contains(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
    }

    private OrderMainEntity checkDesignAttachmentMutation(Long orderId) {
        return designQueryHelper.checkDesignAttachmentMutation(orderId);
    }

    private void checkIsAssignedDesigner(OrderMainEntity order) {
        designQueryHelper.checkIsAssignedDesigner(order);
    }

    private void validateFilesForAttachment(List<FileVO> files, List<String> fileIds, String expectedBizType) {
        Map<String, FileVO> fileMap = files.stream()
                .collect(Collectors.toMap(FileVO::getId, file -> file, (first, ignored) -> first));
        for (String fileId : fileIds) {
            FileVO file = fileMap.get(fileId);
            if (file == null || !expectedBizType.equals(file.getBizType()) || file.getBizId() != null) {
                log.warn("文件业务类型或归属不合法, fileId={}, expectedBizType={}, actualBizType={}, bizId={}",
                        fileId, expectedBizType, file == null ? null : file.getBizType(),
                        file == null ? null : file.getBizId());
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_TYPE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 关联阶段失败时清理本次上传且尚未关联业务的文件，避免遗留孤儿文件。
     * 已归属其他业务的文件不在清理范围内。
     */
    private void cleanupOrphanFiles(List<FileVO> files, List<String> fileIds) {
        Map<String, FileVO> fileMap = files.stream()
                .collect(Collectors.toMap(FileVO::getId, file -> file, (first, ignored) -> first));
        List<String> orphanFileIds = fileIds.stream()
                .map(fileMap::get)
                .filter(Objects::nonNull)
                .filter(file -> file.getBizId() == null)
                .map(FileVO::getId)
                .distinct()
                .collect(Collectors.toList());
        if (orphanFileIds.isEmpty()) {
            return;
        }

        // 关联失败时外层事务可能已经更新了文件归属并持有行锁，必须等外层回滚释放锁后，
        // 再通过独立事务删除文件，否则 REQUIRES_NEW 可能与外层事务互相等待。
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteOrphanFiles(orphanFileIds);
                    }
                }
            });
            return;
        }

        // 无事务场景（例如单元测试或非事务调用）直接清理。
        deleteOrphanFiles(orphanFileIds);
    }

    private void deleteOrphanFiles(List<String> fileIds) {
        fileIds.forEach(fileId -> {
            try {
                fileService.deleteByIdAfterAssociationFailure(fileId);
            } catch (RuntimeException cleanupEx) {
                log.warn("清理附件孤儿文件失败, fileId={}", fileId, cleanupEx);
            }
        });
    }

    /**
     * 获取允许的文件扩展名集合
     */
    private Set<String> getAllowedExtensions() {
        String config = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_PACKAGE_ALLOWED_EXTENSIONS.getKey());
        if (StrUtil.isBlank(config)) {
            config = ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp";
        }
        return Arrays.stream(config.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 获取下一个数据包序号
     */
    private Integer getNextPackageSeq(Long orderId) {
        return packageService.getNextPackageSeq(orderId);
    }

    /**
     * 获取已填写打印信息的文件ID集合
     */
    private Set<Long> getFilledFileIds(List<Long> packageIds) {
        return productFileService.getFilledPackageFileIds(packageIds);
    }

    /**
     * 构建数据包 VO
     */
    private DesignPackageVO buildPackageVO(DesignPackageEntity entity, List<DesignPackageFileEntity> files) {
        OrderMainEntity order = orderMainService.getById(entity.getOrderId());
        return buildPackageVO(entity, files, Collections.emptySet(), order == null ? null : order.getPublicOrderCode());
    }

    /**
     * 构建数据包 VO
     */
    private DesignPackageVO buildPackageVO(DesignPackageEntity entity, List<DesignPackageFileEntity> files,
                                           Set<Long> filledFileIds) {
        OrderMainEntity order = orderMainService.getById(entity.getOrderId());
        return buildPackageVO(entity, files, filledFileIds, order == null ? null : order.getPublicOrderCode());
    }

    private DesignPackageVO buildPackageVO(DesignPackageEntity entity, List<DesignPackageFileEntity> files,
                                           Set<Long> filledFileIds, String publicOrderCode) {
        DesignPackageVO vo = new DesignPackageVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setBatchId(entity.getBatchId());
        if (entity.getBatchId() != null) {
            DesignPackageBatchEntity batch = batchService.getById(entity.getBatchId());
            if (batch != null) {
                vo.setBatchNo(batch.getBatchNo());
                vo.setBatchStatus(batch.getStatus());
                boolean editable = DesignPackageBatchStatus.COMPLETED.name().equals(batch.getStatus())
                        || DesignPackageBatchStatus.CANCELLED.name().equals(batch.getStatus());
                vo.setEditable(!editable);
                vo.setCanDelete(!editable);
                vo.setCanEditPrintInfo(!editable);
                vo.setCanEditDocuments(!editable);
            }
        }
        if (vo.getEditable() == null) {
            OrderMainEntity order = orderMainService.getById(entity.getOrderId());
            boolean designPhase = order != null && Set.of(1030, 2010, 2020).contains(order.getStatus());
            vo.setEditable(designPhase);
            vo.setCanDelete(designPhase);
            vo.setCanEditPrintInfo(designPhase);
            vo.setCanEditDocuments(designPhase);
        }
        vo.setOrderCode(entity.getOrderCode());
        vo.setPublicOrderCode(publicOrderCode);
        vo.setPackageCode(entity.getPackageCode());
        vo.setPackageSeq(entity.getPackageSeq());
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setFileUrl(entity.getFileUrl());
        vo.setDownloadUrl(fileService.generateDownloadUrl(entity.getFileUrl(), entity.getFileName()));
        vo.setFileSize(entity.getFileSize());
        vo.setFileCount(entity.getFileCount());
        vo.setUploadTime(entity.getUploadTime());
        long printInfoCount = productService.countByPackageId(entity.getId());
        vo.setPrintInfoCount((int) printInfoCount);
        vo.setPrintInfoCompleted(printInfoCount > 0);

        // 包内文件列表
        List<FileDownloadUrlByUrlRequest> requests = files.stream()
                .map(f -> new FileDownloadUrlByUrlRequest(f.getFileUrl(), f.getFileName()))
                .toList();
        List<String> packageDownloadUrls = fileService.generateDownloadUrls(requests);
        List<DesignPackageFileVO> fileVOs = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            DesignPackageFileEntity f = files.get(i);
                    DesignPackageFileVO fileVO = new DesignPackageFileVO();
                    fileVO.setId(f.getId());
                    fileVO.setPackageId(f.getPackageId());
                    fileVO.setFileName(f.getFileName());
                    fileVO.setFileExt(f.getFileExt());
                    fileVO.setFilePath(f.getFilePath());
                    fileVO.setFileSize(f.getFileSize());
                    fileVO.setSortOrder(f.getSortOrder());
                    fileVO.setHasPrintInfo(filledFileIds.contains(f.getId()));
                    // 历史数据包的文件仍可查看，但不能被追加打印信息操作选中。
                    fileVO.setSelectableForPrint(Boolean.TRUE.equals(vo.getCanEditPrintInfo())
                            && !filledFileIds.contains(f.getId()));
                    fileVO.setFileUrl(f.getFileUrl());
            fileVO.setDownloadUrl(i < packageDownloadUrls.size() ? packageDownloadUrls.get(i) : null);
            fileVOs.add(fileVO);
        }
        vo.setFiles(fileVOs);

        return vo;
    }

    private OrderMainEntity checkAdditionalBatch(Long orderId, Long batchId) {
        orderMainService.checkNotClassicCase(orderId, "操作追加设计批次");
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        Set<Integer> allowed = Set.of(2020, 2030, 3010, 3020, 3030, 3040, 4010, 5010, 5020,
                5030, 5040, 5050, 6010, 6020, 6030, 8010);
        if (!allowed.contains(order.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }
        DesignPackageBatchEntity batch = batchService.getOne(new LambdaQueryWrapper<DesignPackageBatchEntity>()
                .eq(DesignPackageBatchEntity::getId, batchId)
                .eq(DesignPackageBatchEntity::getOrderId, orderId)
                .last("FOR UPDATE"), false);
        if (batch == null || !orderId.equals(batch.getOrderId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        if (DesignPackageBatchStatus.COMPLETED.name().equals(batch.getStatus())
                || DesignPackageBatchStatus.CANCELLED.name().equals(batch.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED, "追加设计批次已锁定");
        }
        return order;
    }

    /**
     * 构建模型 VO
     */
    private DesignModelVO buildModelVO(DesignModelEntity entity, FileVO fileVO) {
        DesignModelVO vo = new DesignModelVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setFileId(entity.getFileId());
        vo.setCreateTime(entity.getCreateTime());

        // 从 FileVO 填充文件信息
        if (fileVO != null) {
            vo.setFileName(fileVO.getFileName());
            vo.setFileUrl(fileVO.getFileUrl());
            vo.setDownloadUrl(fileVO.getDownloadUrl());
            vo.setFileSize(fileVO.getFileSize());
            vo.setFileExt(fileVO.getFileExt());
        }
        return vo;
    }
}
