package com.yigongbao.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.OrderDraftFileEntity;
import com.yigongbao.module.order.mapper.OrderDraftFileMapper;
import com.yigongbao.module.order.service.OrderDraftFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单草稿文件关联 Service 实现
 *
 * @author hanjor
 * @date 2026-06-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDraftFileServiceImpl extends ServiceImpl<OrderDraftFileMapper, OrderDraftFileEntity>
        implements OrderDraftFileService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDraftFiles(Long draftId, List<OrderDraftFileEntity> files) {
        // 删除旧的文件关联
        remove(new LambdaQueryWrapper<OrderDraftFileEntity>()
                .eq(OrderDraftFileEntity::getDraftId, draftId));

        // 插入新的文件关联
        if (files != null && !files.isEmpty()) {
            saveBatch(files);
            log.info("保存草稿文件关联: draftId={}, fileCount={}", draftId, files.size());
        }
    }

    @Override
    public List<OrderDraftFileEntity> listByDraftId(Long draftId) {
        return list(new LambdaQueryWrapper<OrderDraftFileEntity>()
                .eq(OrderDraftFileEntity::getDraftId, draftId)
                .orderByAsc(OrderDraftFileEntity::getCreateTime));
    }
}
