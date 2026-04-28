package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.entity.DesignPackageFileScreenshotEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileScreenshotMapper;
import com.yigongbao.module.design.service.DesignPackageFileService;
import com.yigongbao.module.design.service.DesignScreenshotService;
import com.yigongbao.module.design.vo.ScreenshotVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据包文件截图服务实现类
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignScreenshotServiceImpl
        extends ServiceImpl<DesignPackageFileScreenshotMapper, DesignPackageFileScreenshotEntity>
        implements DesignScreenshotService {

    private final DesignPackageFileService packageFileService;
    private final FileService fileService;

    /**
     * 保存截图（upsert：有则更新 fileId，无则插入）
     *
     * @param packageId     数据包ID（用于校验 packageFileId 归属）
     * @param packageFileId 数据包文件ID
     * @param file          截图文件（PNG/JPG）
     * @return 截图 VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScreenshotVO saveScreenshot(Long packageId, Long packageFileId, MultipartFile file) {
        log.info("保存截图，packageId={}，packageFileId={}", packageId, packageFileId);

        // 1. 校验 packageFileId 存在且归属 packageId
        validatePackageFile(packageId, packageFileId);

        // 2. 上传截图文件
        FileVO fileVO = fileService.uploadFile(file, FileBizTypeEnum.IMAGE_SCREENSHOT.getDictCode());
        log.info("截图文件上传成功，fileId={}，packageFileId={}", fileVO.getId(), packageFileId);

        // 3. upsert：查询是否已有截图记录（含逻辑删除过滤）
        DesignPackageFileScreenshotEntity existing = lambdaQuery()
                .eq(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileId)
                .one();

        if (existing != null) {
            // 有则更新 fileId，先记录旧文件ID以便删除
            String oldFileId = existing.getFileId();
            existing.setFileId(fileVO.getId());
            updateById(existing);
            // 删除旧截图文件，避免 OSS 泄漏
            if (oldFileId != null) {
                fileService.deleteById(oldFileId);
            }
            log.info("截图记录已更新，id={}，新 fileId={}", existing.getId(), fileVO.getId());
        } else {
            // 无则插入
            DesignPackageFileScreenshotEntity entity = new DesignPackageFileScreenshotEntity();
            entity.setPackageFileId(packageFileId);
            entity.setFileId(fileVO.getId());
            save(entity);
            log.info("截图记录已新增，packageFileId={}，fileId={}", packageFileId, fileVO.getId());
        }

        return toScreenshotVO(fileVO);
    }

    /**
     * 查询截图
     *
     * @param packageId     数据包ID
     * @param packageFileId 数据包文件ID
     * @return 截图 VO，不存在返回 null
     */
    @Override
    public ScreenshotVO getScreenshot(Long packageId, Long packageFileId) {
        log.info("查询截图，packageId={}，packageFileId={}", packageId, packageFileId);

        // 校验 packageFileId 存在且归属 packageId
        validatePackageFile(packageId, packageFileId);

        // 查询截图记录
        DesignPackageFileScreenshotEntity screenshot = lambdaQuery()
                .eq(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileId)
                .one();

        if (screenshot == null) {
            log.info("截图不存在，packageFileId={}", packageFileId);
            return null;
        }

        // 查询文件详情
        FileVO fileVO = fileService.getById(screenshot.getFileId());
        if (fileVO == null) {
            log.warn("截图关联文件不存在，fileId={}", screenshot.getFileId());
            return null;
        }

        return toScreenshotVO(fileVO);
    }

    /**
     * 按 packageFileId 列表批量查询截图文件ID（供 generateDrawing 使用）
     *
     * @param packageFileIds 数据包文件ID列表
     * @return packageFileId → fileId 的映射（无截图的不包含在结果中）
     */
    @Override
    public Map<Long, String> listFileIdsByPackageFileIds(List<Long> packageFileIds) {
        if (packageFileIds == null || packageFileIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DesignPackageFileScreenshotEntity> screenshots = lambdaQuery()
                .in(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileIds)
                .list();

        return screenshots.stream()
                .collect(Collectors.toMap(
                        DesignPackageFileScreenshotEntity::getPackageFileId,
                        DesignPackageFileScreenshotEntity::getFileId,
                        (v1, v2) -> v1
                ));
    }

    // ==================== 私有方法 ====================

    /**
     * 按 packageFileId 列表批量删除截图记录（不删除 OSS 文件，由调用方负责）
     *
     * @param packageFileIds 数据包文件ID列表
     */
    @Override
    public void deleteByPackageFileIds(List<Long> packageFileIds) {
        if (packageFileIds == null || packageFileIds.isEmpty()) {
            return;
        }
        lambdaUpdate()
                .in(DesignPackageFileScreenshotEntity::getPackageFileId, packageFileIds)
                .remove();
    }

    /**
     * 校验 packageFileId 存在且归属指定 packageId
     */
    private void validatePackageFile(Long packageId, Long packageFileId) {
        DesignPackageFileEntity packageFile = packageFileService.getById(packageFileId);
        if (packageFile == null) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_FILE_NOT_FOUND);
        }
        if (!packageId.equals(packageFile.getPackageId())) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_FILE_WRONG_PACKAGE);
        }
    }

    /**
     * FileVO 转 ScreenshotVO
     */
    private ScreenshotVO toScreenshotVO(FileVO fileVO) {
        ScreenshotVO vo = new ScreenshotVO();
        vo.setFileId(fileVO.getId());
        vo.setFileName(fileVO.getFileName());
        vo.setFileUrl(fileVO.getFileUrl());
        vo.setFileSize(fileVO.getFileSize());
        vo.setUploadTime(fileVO.getCreateTime());
        return vo;
    }
}
