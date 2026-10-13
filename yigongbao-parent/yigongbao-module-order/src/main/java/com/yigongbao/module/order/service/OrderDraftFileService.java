package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.entity.OrderDraftFileEntity;

import java.util.List;

/**
 * 订单草稿文件关联 Service
 *
 * @author hanjor
 * @date 2026-06-09
 */
public interface OrderDraftFileService extends IService<OrderDraftFileEntity> {

    /**
     * 保存草稿文件关联（先删除旧的，再插入新的）
     *
     * @param draftId 草稿ID
     * @param files 文件列表
     */
    void saveDraftFiles(Long draftId, List<OrderDraftFileEntity> files);

    /**
     * 查询草稿的文件关联列表
     *
     * @param draftId 草稿ID
     * @return 文件列表
     */
    List<OrderDraftFileEntity> listByDraftId(Long draftId);
}
