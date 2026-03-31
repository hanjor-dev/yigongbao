package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
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
     * 分页查询我的草稿列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param hospitalId 医院ID（可选）
     * @param status 草稿状态（可选）
     * @return 草稿列表
     */
    IPage<OrderDraftVO> listDrafts(Integer pageNum, Integer pageSize, Long hospitalId, Integer status);

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
     * 提交草稿，转为正式订单
     *
     * @param id 草稿ID
     * @return 正式订单ID
     */
    Long submitDraft(Long id);

    /**
     * 校验草稿是否属于当前用户
     *
     * @param id 草稿ID
     * @param operatorId 操作员ID
     */
    void validateDraftOwner(Long id, Long operatorId);
}
