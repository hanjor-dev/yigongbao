package com.yigongbao.module.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.entity.FileDetail;
import com.yigongbao.module.basic.file.mapper.FileDetailMapper;
import com.yigongbao.module.basic.file.service.impl.FileRecorderService;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.service.IClassicCaseFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 经典案例文件迁移服务实现
 * 处理order_file的文件迁移，使用x-file-storage的move()方法真正迁移OSS/COS文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassicCaseFileServiceImpl implements IClassicCaseFileService {

    private final OrderFileMapper orderFileMapper;
    private final FileDetailMapper fileDetailMapper;
    private final FileStorageService fileStorageService;
    private final FileRecorderService fileRecorderService;

    /**
     * 收集订单关联的所有文件ID
     * <p>
     * 从 order_file 表中查询该订单的所有文件关联记录，提取文件ID并去重。
     * 返回的文件ID列表将用于后续的文件迁移操作。
     * </p>
     *
     * @param orderId 订单ID
     * @return 文件ID列表（已去重，不包含空值）
     */
    @Override
    public List<String> collectOrderFileIds(Long orderId) {
        // 查询订单关联的所有文件记录
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>().eq(OrderFileEntity::getOrderId, orderId));

        // 提取文件ID，过滤空值并去重
        return orderFiles.stream()
                .map(OrderFileEntity::getFileId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将订单文件迁移到经典案例专用目录
     * <p>
     * 使用 x-file-storage 的 move() 方法真正迁移 OSS/COS 文件到经典案例目录。
     * 迁移后的文件路径格式：classic-cases/{orderCode}/{filename}
     * </p>
     * <p>
     * 注意：移除 @Transactional 避免死锁。x-file-storage 框架内部已管理事务，
     * 外层大事务与其他模块并发执行时会在 file_detail 表上产生锁冲突。
     * </p>
     *
     * @param orderId 订单ID
     * @param orderCode 订单编号（用于构建目标路径）
     * @throws BusinessException 文件迁移失败时抛出 CLASSIC_CASE_FILE_MIGRATE_FAILED
     */
    @Override
    public void migrateFilesToClassicCase(Long orderId, String orderCode) {
        log.info("开始迁移经典案例文件: orderId={}, orderCode={}", orderId, orderCode);

        // 构建经典案例文件的目标基础路径
        String newBasePath = "classic-cases/" + orderCode + "/";

        // 收集该订单关联的所有文件ID
        List<String> fileIds = collectOrderFileIds(orderId);
        log.debug("收集到文件ID列表: orderId={}, fileCount={}", orderId, fileIds.size());

        if (!fileIds.isEmpty()) {
            int successCount = 0;
            // 逐个迁移文件
            for (String fileId : fileIds) {
                try {
                    // 根据文件ID查询文件信息
                    FileInfo oldFileInfo = fileRecorderService.getById(fileId);
                    if (oldFileInfo == null) {
                        log.warn("文件记录不存在，跳过迁移: fileId={}, orderId={}", fileId, orderId);
                        continue;
                    }

                    // 修复：只设置目录路径，不包含文件名
                    String newPath = newBasePath;

                    // 调用 x-file-storage 的 move() 方法迁移文件
                    FileInfo newFileInfo = fileStorageService.move(oldFileInfo)
                            .setPath(newPath)
                            .move();

                    // 修复：move()操作会创建新的file_id，需要更新order_file表的关联
                    String oldFileId = fileId;
                    String newFileId = newFileInfo.getId();
                    updateOrderFileId(orderId, oldFileId, newFileId);
                    successCount++;

                    log.info("文件迁移成功: orderId={}, oldFileId={}, newFileId={}, newUrl={}",
                        orderId, oldFileId, newFileId, newFileInfo.getUrl());

                } catch (Exception e) {
                    log.error("迁移文件失败: fileId={}, orderId={}, errorMsg={}", fileId, orderId, e.getMessage(), e);
                    throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_FILE_MIGRATE_FAILED);
                }
            }
            log.info("order_file 迁移完成: orderId={}, totalCount={}, successCount={}",
                orderId, fileIds.size(), successCount);
        } else {
            log.info("订单无关联文件，跳过迁移: orderId={}", orderId);
        }

        log.info("经典案例文件迁移完成: orderId={}, orderCode={}, totalCount={}",
            orderId, orderCode, fileIds.size());
    }

    /**
     * 更新order_file表的file_id
     * move()操作会创建新的file_id，需要更新业务表的关联
     *
     * @param orderId 订单ID
     * @param oldFileId 旧文件ID
     * @param newFileId 新文件ID
     */
    private void updateOrderFileId(Long orderId, String oldFileId, String newFileId) {
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
                        .eq(OrderFileEntity::getFileId, oldFileId));
        for (OrderFileEntity orderFile : orderFiles) {
            orderFile.setFileId(newFileId);
            orderFileMapper.updateById(orderFile);
        }
        if (!orderFiles.isEmpty()) {
            log.info("更新order_file的file_id: orderId={}, oldFileId={}, newFileId={}, count={}",
                orderId, oldFileId, newFileId, orderFiles.size());
        }
    }
}

