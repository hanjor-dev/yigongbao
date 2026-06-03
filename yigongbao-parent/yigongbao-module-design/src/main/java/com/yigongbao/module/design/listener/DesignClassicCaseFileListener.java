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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 经典案例文件迁移监听器（Design模块）
 * 处理7类设计文件的迁移，使用x-file-storage的move()方法真正迁移OSS/COS文件
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

    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
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

                        String newPath = newBasePath + oldFileInfo.getFilename();
                        FileInfo newFileInfo = fileStorageService.move(oldFileInfo)
                                .setPath(newPath)
                                .move();

                        FileDetail detail = fileRecorderService.toFileDetail(newFileInfo);
                        fileDetailMapper.updateById(detail);
                        updateDesignPackageFileUrls(fileId, newFileInfo.getUrl());
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
}
