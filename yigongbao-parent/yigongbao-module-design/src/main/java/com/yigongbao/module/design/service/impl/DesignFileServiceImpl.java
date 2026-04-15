package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.ArchiveFileInfo;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.util.ArchiveParserUtil;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageFileVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.order.mapper.OrderMainMapper;
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

    private final OrderMainMapper orderMainMapper;
    private final DesignPackageMapper packageMapper;
    private final DesignPackageFileMapper packageFileMapper;
    private final DesignModelMapper modelMapper;
    private final DesignProductMapper productMapper;
    private final FileService fileService;
    private final CodeGeneratorService codeGeneratorService;
    private final ConfigService configService;

    /**
     * 设计报告业务类型（对应 FileBizTypeEnum.DESIGN_REPORT = "10.5"）
     */
    private static final String BIZ_TYPE_DESIGN_REPORT = "10.5";

    /**
     * 打印文件包业务类型（对应 FileBizTypeEnum.PRINT_PACKAGE = "10.4"）
     */
    private static final String BIZ_TYPE_DESIGN_PACKAGE = "10.4";

    /**
     * 可视化模型业务类型（对应 FileBizTypeEnum.VISUAL_MODEL = "10.6"）
     */
    private static final String BIZ_TYPE_DESIGN_MODEL = "10.6";

    // ==================== 数据包 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DesignPackageVO uploadPackage(Long orderId, MultipartFile file) {
        log.info("上传数据包, orderId={}, fileName={}", orderId, file.getOriginalFilename());

        // 1. 校验工单状态和操作权限
        OrderMainEntity order = checkOrderAndPermission(orderId);

        // 2. 校验压缩包格式
        String fileName = file.getOriginalFilename();
        if (!ArchiveParserUtil.isSupported(fileName)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_FORMAT_NOT_SUPPORTED);
        }

        // 3. 上传压缩包文件
        FileVO fileVO = fileService.uploadFile(file, BIZ_TYPE_DESIGN_PACKAGE);
        log.info("压缩包上传成功, fileId={}", fileVO.getId());

        // 4. 解析压缩包内文件列表
        Set<String> allowedExtensions = getAllowedExtensions();
        List<ArchiveFileInfo> archiveFiles;
        try {
            archiveFiles = ArchiveParserUtil.parse(file.getInputStream(), fileName, allowedExtensions);
        } catch (IOException e) {
            log.error("读取压缩包流失败", e);
            // 删除已上传的文件
            fileService.deleteById(fileVO.getId());
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_PARSE_FAILED, e.getMessage());
        }

        // 5. 校验是否有有效文件
        if (CollUtil.isEmpty(archiveFiles)) {
            fileService.deleteById(fileVO.getId());
            throw new BusinessException(ErrorCodeEnum.DESIGN_ARCHIVE_EMPTY);
        }
        log.info("压缩包解析成功, 有效文件数={}", archiveFiles.size());

        // 6. 生成数据包编号
        String packageCode = codeGeneratorService.generateWithSeqSuffix(
                CodeRuleConstants.DATA_PACKAGE_NO, order.getOrderCode());

        // 7. 计算序号
        Integer packageSeq = getNextPackageSeq(orderId);

        // 8. 保存数据包记录
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
        packageMapper.insert(packageEntity);
        log.info("数据包记录保存成功, packageId={}, packageCode={}", packageEntity.getId(), packageCode);

        // 9. 保存包内文件记录
        List<DesignPackageFileEntity> fileEntities = new ArrayList<>();
        int sortOrder = 1;
        for (ArchiveFileInfo archiveFile : archiveFiles) {
            DesignPackageFileEntity fileEntity = new DesignPackageFileEntity();
            fileEntity.setPackageId(packageEntity.getId());
            fileEntity.setFileName(archiveFile.getFileName());
            fileEntity.setFileExt(archiveFile.getExtension().replace(".", ""));
            fileEntity.setFilePath(archiveFile.getFilePath());
            fileEntity.setFileSize(archiveFile.getFileSize());
            fileEntity.setSortOrder(sortOrder++);
            fileEntities.add(fileEntity);
        }
        // 批量插入
        for (DesignPackageFileEntity fileEntity : fileEntities) {
            packageFileMapper.insert(fileEntity);
        }
        log.info("包内文件记录保存成功, count={}", fileEntities.size());

        // 10. 构建返回结果
        return buildPackageVO(packageEntity, fileEntities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(Long orderId, Long packageId) {
        log.info("删除数据包, orderId={}, packageId={}", orderId, packageId);

        // 1. 校验工单状态和操作权限
        checkOrderAndPermission(orderId);

        // 2. 查询数据包
        DesignPackageEntity packageEntity = packageMapper.selectById(packageId);
        if (packageEntity == null || !packageEntity.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }

        // 3. 检查是否有关联的打印产品
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getPackageId, packageId));
        if (productCount > 0) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_HAS_PRODUCTS);
        }

        // 4. 删除包内文件记录
        packageFileMapper.delete(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, packageId));

        // 5. 删除数据包记录
        packageMapper.deleteById(packageId);

        // 6. 删除存储的压缩包文件
        fileService.deleteById(packageEntity.getFileId());

        log.info("数据包删除成功, packageId={}", packageId);
    }

    @Override
    public List<DesignPackageVO> listPackages(Long orderId) {
        // 1. 查询数据包列表
        List<DesignPackageEntity> packages = packageMapper.selectList(
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
        List<DesignPackageFileEntity> allFiles = packageFileMapper.selectList(
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

    // ==================== 可视化模型 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DesignModelVO> linkModels(Long orderId, List<String> fileIds) {
        log.info("批量关联可视化模型, orderId={}, fileIds={}", orderId, fileIds);

        // 1. 校验工单状态和操作权限
        checkOrderAndPermission(orderId);

        // 2. 批量校验文件是否存在
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
            fileService.linkFile(fileId, BIZ_TYPE_DESIGN_MODEL, orderId);

            // 保存模型记录
            DesignModelEntity modelEntity = new DesignModelEntity();
            modelEntity.setOrderId(orderId);
            modelEntity.setFileId(fileId);
            modelMapper.insert(modelEntity);

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
        checkOrderAndPermission(orderId);

        // 2. 查询模型
        DesignModelEntity modelEntity = modelMapper.selectById(modelId);
        if (modelEntity == null || !modelEntity.getOrderId().equals(orderId)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_MODEL_NOT_FOUND);
        }

        // 3. 删除模型记录
        modelMapper.deleteById(modelId);

        // 4. 删除存储的文件
        fileService.deleteById(modelEntity.getFileId());

        log.info("可视化模型删除成功, modelId={}", modelId);
    }

    @Override
    public List<DesignModelVO> listModels(Long orderId) {
        // 1. 查询模型记录
        List<DesignModelEntity> models = modelMapper.selectList(
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
        checkOrderAndPermission(orderId);

        // 2. 校验文件是否存在
        FileVO fileVO = fileService.getById(fileId);
        if (fileVO == null) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 删除旧报告（每工单仅一份）
        List<FileVO> existingReports = fileService.listByBiz(BIZ_TYPE_DESIGN_REPORT, orderId);
        for (FileVO existing : existingReports) {
            fileService.deleteById(existing.getId());
            log.info("删除旧设计报告, fileId={}", existing.getId());
        }

        // 4. 关联新文件到业务
        return fileService.linkFile(fileId, BIZ_TYPE_DESIGN_REPORT, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Long orderId, String fileId) {
        log.info("删除设计报告, orderId={}, fileId={}", orderId, fileId);

        // 1. 校验工单状态和操作权限
        checkOrderAndPermission(orderId);

        // 2. 校验文件归属
        FileVO fileVO = fileService.getById(fileId);
        if (fileVO == null || !BIZ_TYPE_DESIGN_REPORT.equals(fileVO.getBizType())
                || !orderId.equals(fileVO.getBizId())) {
            throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
        }

        // 3. 删除文件
        fileService.deleteById(fileId);

        log.info("设计报告删除成功, fileId={}", fileId);
    }

    @Override
    public FileVO getReport(Long orderId) {
        List<FileVO> reports = fileService.listByBiz(BIZ_TYPE_DESIGN_REPORT, orderId);
        return CollUtil.isEmpty(reports) ? null : reports.get(0);
    }

    // ==================== 私有方法 ====================

    /**
     * 校验工单状态和操作权限
     *
     * @param orderId 订单ID
     * @return 订单实体
     */
    private OrderMainEntity checkOrderAndPermission(Long orderId) {
        // 1. 查询订单
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 2. 校验阶段（必须在设计阶段）
        FlowStatusEnum status = FlowStatusEnum.getByValue(order.getStatus());
        if (status == null || !status.belongsTo(FlowPhaseEnum.DESIGN)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }

        // 3. 校验状态（设计中或设计审核不通过才能操作）
        if (status != FlowStatusEnum.DESIGN_IN_PROGRESS
                && status != FlowStatusEnum.DESIGN_REVIEW_REJECTED) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_ORDER_STATUS_NOT_ALLOWED);
        }

        // 4. 校验操作人（必须是当前设计师）
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_OPERATOR_NOT_ALLOWED);
        }

        return order;
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
        Integer maxSeq = packageMapper.selectList(
                        new LambdaQueryWrapper<DesignPackageEntity>()
                                .eq(DesignPackageEntity::getOrderId, orderId)
                                .select(DesignPackageEntity::getPackageSeq))
                .stream()
                .map(DesignPackageEntity::getPackageSeq)
                .max(Integer::compareTo)
                .orElse(0);
        return maxSeq + 1;
    }

    /**
     * 获取已填写打印信息的文件ID集合
     */
    private Set<Long> getFilledFileIds(List<Long> packageIds) {
        if (CollUtil.isEmpty(packageIds)) {
            return Collections.emptySet();
        }
        List<DesignProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .in(DesignProductEntity::getPackageId, packageIds)
                        .select(DesignProductEntity::getPackageFileId));
        return products.stream()
                .map(DesignProductEntity::getPackageFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
