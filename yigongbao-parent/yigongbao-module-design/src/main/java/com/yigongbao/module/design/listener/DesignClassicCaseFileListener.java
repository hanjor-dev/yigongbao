package com.yigongbao.module.design.listener;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.event.ClassicCaseMarkedEvent;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.basic.file.service.impl.FileRecorderService;
import com.yigongbao.module.design.entity.*;
import com.yigongbao.module.design.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 经典案例文件迁移监听器（Design模块）
 * <p>
 * 处理7类设计文件的迁移，使用x-file-storage的move()方法真正迁移OSS/COS文件。
 * 同步执行（非@Async），确保文件迁移在标记事务中完成，失败时自动回滚。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesignClassicCaseFileListener {

    private final DesignPackageMapper designPackageMapper;
    private final DesignPackageFileMapper designPackageFileMapper;
    private final DesignPackageFileScreenshotMapper designPackageFileScreenshotMapper;
    private final DesignModelMapper designModelMapper;
    private final DesignInstructionMapper designInstructionMapper;
    private final DesignDrawingMapper designDrawingMapper;
    private final FileDetailMapper fileDetailMapper;
    private final FileStorageService fileStorageService;
    private final FileRecorderService fileRecorderService;

    @EventListener
    public void handleClassicCaseMarked(ClassicCaseMarkedEvent event) {
        log.info("Design模块开始处理经典案例文件迁移: orderId={}, orderCode={}",
            event.getOrderId(), event.getOrderCode());

        try {
            String newBasePath = "classic-cases/" + event.getOrderCode() + "/";
            List<String> fileIds = collectDesignFileIds(event.getOrderId());

            if (!fileIds.isEmpty()) {
                int successCount = 0;
                for (String fileId : fileIds) {
                    try {
                        FileInfo oldFileInfo = fileRecorderService.getById(fileId);
                        if (oldFileInfo == null) {
                            log.warn("文件不存在，跳过: fileId={}", fileId);
                            continue;
                        }

                        // 修复：只设置目录路径，不包含文件名
                        String newPath = newBasePath;
                        FileInfo newFileInfo = fileStorageService.move(oldFileInfo)
                                .setPath(newPath)
                                .move();

                        // 修复：move()操作会创建新的file_id，需要更新业务表的关联
                        String oldFileId = fileId;
                        String newFileId = newFileInfo.getId();

                        // 更新design_model表的file_id
                        updateDesignModelFileId(oldFileId, newFileId);
                        // 更新design_package_file表的file_url
                        updateDesignPackageFileUrls(oldFileId, newFileInfo.getUrl());

                        log.info("文件迁移成功: oldFileId={}, newFileId={}, newUrl={}",
                            oldFileId, newFileId, newFileInfo.getUrl());
                        successCount++;

                    } catch (Exception e) {
                        log.error("迁移文件失败: fileId={}, orderId={}", fileId, event.getOrderId(), e);
                        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_FILE_MIGRATE_FAILED);
                    }
                }
                log.info("Design模块文件迁移完成: orderId={}, successCount={}", event.getOrderId(), successCount);
            }
        } catch (Exception e) {
            log.error("Design模块文件迁移失败: orderId={}, orderCode={}",
                event.getOrderId(), event.getOrderCode(), e);
            throw e;
        }
    }

    private List<String> collectDesignFileIds(Long orderId) {
        List<String> fileIds = new ArrayList<>();

        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>().eq(DesignPackageEntity::getOrderId, orderId));
        packages.forEach(p -> fileIds.add(p.getFileId()));

        List<Long> packageIds = packages.stream().map(DesignPackageEntity::getId).collect(Collectors.toList());
        if (!packageIds.isEmpty()) {
            List<DesignPackageFileEntity> packageFiles = designPackageFileMapper.selectList(
                    new LambdaQueryWrapper<DesignPackageFileEntity>().in(DesignPackageFileEntity::getPackageId, packageIds));
            packageFiles.forEach(f -> fileIds.add(f.getFileId()));

            List<Long> packageFileIds = packageFiles.stream().map(DesignPackageFileEntity::getId).collect(Collectors.toList());
            if (!packageFileIds.isEmpty()) {
                List<DesignPackageFileScreenshotEntity> screenshots = designPackageFileScreenshotMapper.selectList(
                        new LambdaQueryWrapper<DesignPackageFileScreenshotEntity>().in(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileIds));
                screenshots.forEach(s -> fileIds.add(s.getFileId()));
            }

            List<DesignInstructionEntity> instructions = designInstructionMapper.selectList(
                    new LambdaQueryWrapper<DesignInstructionEntity>().in(DesignInstructionEntity::getPackageId, packageIds));
            instructions.forEach(i -> {
                fileIds.add(i.getTemplateFileId());
                fileIds.add(i.getRevisedFileId());
            });

            List<DesignDrawingEntity> drawings = designDrawingMapper.selectList(
                    new LambdaQueryWrapper<DesignDrawingEntity>().in(DesignDrawingEntity::getPackageId, packageIds));
            drawings.forEach(d -> {
                fileIds.add(d.getTemplateFileId());
                fileIds.add(d.getRevisedFileId());
            });
        }

        List<DesignModelEntity> models = designModelMapper.selectList(
                new LambdaQueryWrapper<DesignModelEntity>().eq(DesignModelEntity::getOrderId, orderId));
        models.forEach(m -> fileIds.add(m.getFileId()));

        return fileIds.stream().filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
    }

    private void updateDesignPackageFileUrls(String fileId, String newUrl) {
        List<DesignPackageFileEntity> files = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>().eq(DesignPackageFileEntity::getFileId, fileId));
        for (DesignPackageFileEntity file : files) {
            file.setFileUrl(newUrl);
            designPackageFileMapper.updateById(file);
        }
    }

    /**
     * 更新design_model表的file_id
     * move()操作会创建新的file_id，需要更新业务表的关联
     *
     * @param oldFileId 旧文件ID
     * @param newFileId 新文件ID
     */
    private void updateDesignModelFileId(String oldFileId, String newFileId) {
        List<DesignModelEntity> models = designModelMapper.selectList(
                new LambdaQueryWrapper<DesignModelEntity>().eq(DesignModelEntity::getFileId, oldFileId));
        for (DesignModelEntity model : models) {
            model.setFileId(newFileId);
            designModelMapper.updateById(model);
        }
        if (!models.isEmpty()) {
            log.info("更新design_model的file_id: oldFileId={}, newFileId={}, count={}",
                oldFileId, newFileId, models.size());
        }
    }
}
