package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.dto.draft.OrderDraftPageQueryDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.vo.draft.OrderDraftDetailVO;
import com.yigongbao.module.order.vo.draft.OrderDraftVO;

/**
 * 订单草稿 Service
 *
 * @author hanjor
 * @date 2026-03-31
 */
public interface OrderDraftService extends IService<OrderDraftEntity> {

    /**
     * 分页查询我的草稿列表（仅分页参数，按创建时间倒序）
     *
     * @param dto 分页查询参数
     * @return 草稿列表
     */
    IPage<OrderDraftVO> listDrafts(OrderDraftPageQueryDTO dto);

    /**
     * 查询草稿详情
     *
     * @param id 草稿ID
     * @return 草稿详情
     */
    OrderDraftDetailVO getDraftDetail(Long id);

    /**
     * 创建或更新草稿（包含重建项目列表）
     *
     * @param dto 创建/更新参数
     * @return 草稿ID
     */
    Long saveDraft(CreateOrderDraftDTO dto);

    /**
     * 删除草稿
     *
     * @param id 草稿ID
     */
    void removeDraft(Long id);

    /**
     * 校验草稿是否属于当前用户
     *
     * @param id 草稿ID
     * @param operatorId 操作员ID
     */
    void validateDraftOwner(Long id, Long operatorId);
}
