package com.yigongbao.module.production.listener;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.event.ClassicCaseMarkedEvent;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.impl.FileRecorderService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 经典案例文件迁移监听器（Production模块）
 * <p>
 * 处理production_record.flow_card_file_url的URL更新，使用x-file-storage的move()方法真正迁移OSS/COS文件。
 * 同步执行（非@Async），确保文件迁移在标记事务中完成，失败时自动回滚。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionClassicCaseFileListener {

    private final ProductionRecordMapper productionRecordMapper;
    private final FileStorageService fileStorageService;
    private final FileRecorderService fileRecorderService;

    @EventListener
    public void handleClassicCaseMarked(ClassicCaseMarkedEvent event) {
        log.info("Production模块开始处理经典案例文件迁移: orderId={}, orderCode={}",
            event.getOrderId(), event.getOrderCode());

        try {
            String newBasePath = "classic-cases/" + event.getOrderCode() + "/";

            List<ProductionRecordEntity> records = productionRecordMapper.selectList(
                    new LambdaQueryWrapper<ProductionRecordEntity>()
                            .eq(ProductionRecordEntity::getOrderId, event.getOrderId()));

            int successCount = 0;
            for (ProductionRecordEntity record : records) {
                if (StrUtil.isNotBlank(record.getFlowCardFileUrl())) {
                    try {
                        String oldUrl = record.getFlowCardFileUrl();
                        FileInfo oldFileInfo = fileRecorderService.getByUrl(oldUrl);
                        if (oldFileInfo == null) {
                            log.warn("文件不存在，跳过: url={}", oldUrl);
                            continue;
                        }

                        String newPath = newBasePath + oldFileInfo.getFilename();
                        FileInfo newFileInfo = fileStorageService.move(oldFileInfo)
                                .setPath(newPath)
                                .move();

                        record.setFlowCardFileUrl(newFileInfo.getUrl());
                        productionRecordMapper.updateById(record);
                        successCount++;

                    } catch (Exception e) {
                        log.error("迁移流转卡文件失败: recordId={}, orderId={}", record.getId(), event.getOrderId(), e);
                        throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_FILE_MIGRATE_FAILED);
                    }
                }
            }

            log.info("Production模块文件迁移完成: orderId={}, successCount={}", event.getOrderId(), successCount);
        } catch (Exception e) {
            log.error("Production模块文件迁移失败: orderId={}, orderCode={}",
                event.getOrderId(), event.getOrderCode(), e);
            throw e;
        }
    }
}
