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
import com.yigongbao.module.design.service.DesignProductFileService;
import com.yigongbao.module.design.service.DesignProductService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.util.ArchiveParserUtil;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageFileVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    // ==================== 数据包 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignPackageVO uploadPackage(Long orderId, MultipartFile file) {
        log.info("上传数据包, orderId={}, fileName={}", orderId, file.getOriginalFilename());

        // 0. 校验文件非空
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "上传文件不能为空");
        }

        // 1. 校验工单状态和操作权限
        OrderMainEntity order = checkDesignPhase(orderId);
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

        // 4. 先读取文件流到内存，避免 MultipartFile InputStream 被上传步骤消费后无法重新读取
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件流失败", e);
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
        }

        // 5. 解析压缩包内文件列表（使用缓存的 byte[]，避免流被消费后为空）
        Set<String> allowedExtensions = getAllowedExtensions();
        List<ArchiveFileInfo> archiveFiles;
        try {
            archiveFiles = ArchiveParserUtil.parse(new java.io.ByteArrayInputStream(fileBytes),
                    fileName, allowedExtensions);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取压缩包流失败", e);
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
        }

        // 6. 校验是否有有效文件
        if (CollUtil.isEmpty(archiveFiles)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_EMPTY);
        }
        log.info("压缩包解析成功, 有效文件数={}", archiveFiles.size());

        // 7. 上传压缩包文件（使用缓存的 byte[]）
        FileVO fileVO = fileService.uploadBytes(fileBytes, fileName, FileBizTypeEnum.PRINT_PACKAGE.getDictCode());
        log.info("压缩包上传成功, fileId={}", fileVO.getId());

        // 8. 生成数据包编号
        String packageCode = codeGeneratorService.generateWithSeqSuffix(
                CodeRuleConstants.DATA_PACKAGE_NO, order.getOrderCode());

        // 9. 计算序号
        Integer packageSeq = getNextPackageSeq(orderId);

        // 10. 保存数据包记录
        DesignPackageEntity packageEntity = new DesignPackageEntity();
        packageEntity.setOrderId(orderId);
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
        log.info("数据包记录保存成功, packageId={}, packageCode={}", packageEntity.getId(), packageCode);

        // 11. 逐文件上传内部文件到 OSS，并保存包内文件记录
        List<DesignPackageFileEntity> fileEntities = new ArrayList<>();
        int sortOrder = 1;
        for (ArchiveFileInfo archiveFile : archiveFiles) {
            // 将包内文件独立上传到 OSS，获取独立访问 URL
            FileVO innerFileVO = fileService.uploadBytes(
                    archiveFile.getFileContent(),
                    archiveFile.getFileName(),
                    FileBizTypeEnum.PACKAGE_FILE.getDictCode());
            log.info("包内文件上传成功, fileName={}, fileId={}", archiveFile.getFileName(), innerFileVO.getId());

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
        log.info("包内文件记录保存成功, count={}", fileEntities.size());

        // 12. 构建返回结果
        return buildPackageVO(packageEntity, fileEntities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long orderId, Long packageId) {
        log.info("删除数据包, orderId={}, packageId={}", orderId, packageId);

        // 1. 校验工单状态和操作权限
        checkIsAssignedDesigner(checkDesignPhase(orderId));

        // 2. 查询数据包
        DesignPackageEntity packageEntity = packageService.getById(packageId);
        if (packageEntity == null || !packageEntity.getOrderId().equals(orderId)) {
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
        log.info("指令单/图纸 OSS 文件及记录清理完成, packageId={}", packageId);

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
            log.info("包内文件截图删除完成, count={}", screenshotFileIds.size());
        }

        // 4.2 删除包内文件本身的 OSS 存储
        for (DesignPackageFileEntity innerFile : innerFiles) {
            fileService.deleteById(innerFile.getFileId());
        }
        log.info("包内文件 OSS 存储删除完成, count={}", innerFiles.size());

        // 5. 删除包内文件记录
        packageFileService.remove(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId));

        // 6. 删除数据包记录
        packageService.removeById(packageId);

        // 7. 删除存储的压缩包文件
        fileService.deleteById(packageEntity.getFileId());

        log.info("数据包删除成功, packageId={}", packageId);
    }

    @Override
    public List<DesignPackageVO> listPackages(Long orderId) {
        designQueryHelper.checkOrderReadable(orderId);
        // 1. 查询数据包列表
        List<DesignPackageEntity> packages = packageService.list(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .orderByAsc(DesignPackageEntity::getPackageSeq));

        if (CollUtil.isEmpty(packages)) {
            return Collections.emptyList();
        }

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
                .map(pkg -> buildPackageVO(pkg, fileMap.getOrDefault(pkg.getId(), Collections.emptyList()), filledFileIds))
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
        log.info("查询包内文件列表，orderId={}，packageId={}", orderId, packageId);
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
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ==================== 可视化模型 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DesignModelVO> linkModels(Long orderId, List<String> fileIds) {
        log.info("批量关联可视化模型, orderId={}, fileIds={}", orderId, fileIds);

        // 1. 校验工单状态和操作权限
        checkIsAssignedDesigner(checkDesignPhase(orderId));

        // 2. 批量校验文件是否存在（类型和大小已在上传时由 FileService/Provider 校验）
        List<FileVO> fileVOs = fileService.listByIds(fileIds);
        if (fileVOs.size() != fileIds.size()) {
            // 找出不存在的 fileId
            Set<String> foundIds = fileVOs.stream().map(FileVO::getId).collect(Collectors.toSet());
            List<String> notFoundIds = fileIds.stream().filter(id -> !foundIds.contains(id)).toList();
            log.warn("部分文件不存在, notFoundIds={}", notFoundIds);
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 批量关联文件到业务，并保存模型记录
        Map<String, FileVO> fileMap = fileVOs.stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));

        List<DesignModelVO> results = new ArrayList<>();
        for (String fileId : fileIds) {
            // 关联文件到业务
            fileService.linkFile(fileId, FileBizTypeEnum.VISUAL_MODEL.getDictCode(), orderId);

            // 保存模型记录
            DesignModelEntity modelEntity = new DesignModelEntity();
            modelEntity.setOrderId(orderId);
            modelEntity.setFileId(fileId);
            modelService.save(modelEntity);

            results.add(buildModelVO(modelEntity, fileMap.get(fileId)));
        }

        log.info("批量关联可视化模型成功, orderId={}, count={}", orderId, results.size());
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long orderId, Long modelId) {
        log.info("删除可视化模型, orderId={}, modelId={}", orderId, modelId);

        // 1. 校验工单状态和操作权限
        checkIsAssignedDesigner(checkDesignPhase(orderId));

        // 2. 查询模型
        DesignModelEntity modelEntity = modelService.getById(modelId);
        if (modelEntity == null || !modelEntity.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_MODEL_NOT_FOUND);
        }

        // 3. 删除模型记录
        modelService.removeById(modelId);

        // 4. 删除存储的文件
        fileService.deleteById(modelEntity.getFileId());

        log.info("可视化模型删除成功, modelId={}", modelId);
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
    public FileVO linkReport(Long orderId, String fileId) {
        log.info("关联设计报告, orderId={}, fileId={}", orderId, fileId);

        // 1. 校验工单状态和操作权限
        checkIsAssignedDesigner(checkDesignPhase(orderId));

        // 2. 校验文件是否存在（类型和大小已在上传时由 FileService/Provider 校验）
        FileVO fileVO = fileService.getById(fileId);
        if (fileVO == null) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 删除旧报告（每工单仅一份）
        List<FileVO> existingReports = fileService.listByBiz(FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId);
        for (FileVO existing : existingReports) {
            fileService.deleteById(existing.getId());
            log.info("删除旧设计报告, fileId={}", existing.getId());
        }

        // 5. 关联新文件到业务
        return fileService.linkFile(fileId, FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long orderId, String fileId) {
        log.info("删除设计报告, orderId={}, fileId={}", orderId, fileId);

        // 1. 校验工单状态和操作权限
        checkIsAssignedDesigner(checkDesignPhase(orderId));

        // 2. 校验文件归属
        FileVO fileVO = fileService.getById(fileId);
        if (fileVO == null || !FileBizTypeEnum.DESIGN_REPORT.getDictCode().equals(fileVO.getBizType())
                || !orderId.equals(fileVO.getBizId())) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 删除文件
        fileService.deleteById(fileId);

        log.info("设计报告删除成功, fileId={}", fileId);
    }

    @Override
    public FileVO getReport(Long orderId) {
        List<FileVO> reports = fileService.listByBiz(FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId);
        return CollUtil.isEmpty(reports) ? null : reports.get(0);
    }

    // ==================== 私有方法 ====================

    private OrderMainEntity checkDesignPhase(Long orderId) {
        return designQueryHelper.checkDesignPhase(orderId);
    }

    private void checkIsAssignedDesigner(OrderMainEntity order) {
        designQueryHelper.checkIsAssignedDesigner(order);
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
        return buildPackageVO(entity, files, Collections.emptySet());
    }

    /**
     * 构建数据包 VO
     */
    private DesignPackageVO buildPackageVO(DesignPackageEntity entity, List<DesignPackageFileEntity> files,
                                           Set<Long> filledFileIds) {
        DesignPackageVO vo = new DesignPackageVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setOrderCode(entity.getOrderCode());
        vo.setPackageCode(entity.getPackageCode());
        vo.setPackageSeq(entity.getPackageSeq());
        vo.setFileId(entity.getFileId());
        vo.setFileName(entity.getFileName());
        vo.setFileUrl(entity.getFileUrl());
        vo.setFileSize(entity.getFileSize());
        vo.setFileCount(entity.getFileCount());
        vo.setUploadTime(entity.getUploadTime());

        // 包内文件列表
        List<DesignPackageFileVO> fileVOs = files.stream()
                .map(f -> {
                    DesignPackageFileVO fileVO = new DesignPackageFileVO();
                    fileVO.setId(f.getId());
                    fileVO.setPackageId(f.getPackageId());
                    fileVO.setFileName(f.getFileName());
                    fileVO.setFileExt(f.getFileExt());
                    fileVO.setFilePath(f.getFilePath());
                    fileVO.setFileSize(f.getFileSize());
                    fileVO.setSortOrder(f.getSortOrder());
                    fileVO.setHasPrintInfo(filledFileIds.contains(f.getId()));
                    fileVO.setFileUrl(f.getFileUrl());
                    return fileVO;
                })
                .collect(Collectors.toList());
        vo.setFiles(fileVOs);

        return vo;
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
            vo.setFileSize(fileVO.getFileSize());
            vo.setFileExt(fileVO.getFileExt());
        }
        return vo;
    }
}
