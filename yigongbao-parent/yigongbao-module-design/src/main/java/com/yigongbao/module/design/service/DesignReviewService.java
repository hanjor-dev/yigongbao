package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;

/**
 * 设计审核服务接口
 *
 * @author hanjor
 * @date 2026-04-17
 */
public interface DesignReviewService extends IService<DesignReviewEntity> {

    /**
     * 分页查询待审核工单列表
     * 固定 status=2040（设计审核中），复用工单查询逻辑
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO);

    /**
     * 获取审核详情（工单详情 + 审核历史）
     *
     * @param orderId 订单ID
     * @return 审核详情 VO
     */
    DesignReviewDetailVO getReviewDetail(Long orderId);

    /**
     * 审核通过
     * 状态流转：设计审核中(2040) → 设计审核通过(2050，不可见) → 待打印(3010) 或 待客户确认(7010)
     * flow 模块根据 needsPhysicalDelivery 自动完成分支跳转，无需业务层二次调用
     *
     * @param orderId 订单ID
     * @param dto     审核通过请求体
     */
    void reviewPass(Long orderId, ReviewPassDTO dto);

    /**
     * 审核驳回
     * 状态流转：设计审核中(2040) → 设计审核不通过(2060)
     *
     * @param orderId 订单ID
     * @param dto     审核驳回请求体（含必填驳回原因）
     */
    void reviewReject(Long orderId, ReviewRejectDTO dto);
}
