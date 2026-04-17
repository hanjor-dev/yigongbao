package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.ReviewPassDTO;
import com.yigongbao.module.design.dto.ReviewRejectDTO;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignReviewService;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignReviewDetailVO;
import com.yigongbao.module.design.vo.DesignReviewHistoryVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设计审核服务实现类
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesignReviewServiceImpl extends ServiceImpl<DesignReviewMapper, DesignReviewEntity>
        implements DesignReviewService {

    private final OrderMainService orderMainService;
    private final UserService userService;
    private final DesignWorkorderService designWorkorderService;
    private final FlowFacade flowFacade;

    /**
     * 分页查询待审核工单列表
     * 强制覆盖 status 为 2040（设计审核中），复用工单查询逻辑
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO) {
        log.info("查询待审核工单列表，queryDTO={}", queryDTO);
        // 强制覆盖 status 为 2040，前端传入值无效
        queryDTO.setStatus(FlowStatusEnum.DESIGN_REVIEWING.getValue());
        return designWorkorderService.listWorkorders(queryDTO);
    }

    /**
     * 获取审核详情
     * 在工单详情基础上追加审核历史记录列表（时间倒序）
     *
     * @param orderId 订单ID
     * @return 审核详情 VO
     */
    @Override
    public DesignReviewDetailVO getReviewDetail(Long orderId) {
        log.info("查询审核详情，orderId={}", orderId);

        // 1. 获取工单详情（复用现有逻辑）
        DesignWorkorderDetailVO workorderDetail = designWorkorderService.getWorkorderDetail(orderId);

        // 2. 构建审核详情 VO，复制工单详情字段
        DesignReviewDetailVO detailVO = new DesignReviewDetailVO();
        BeanUtils.copyProperties(workorderDetail, detailVO);

        // 3. 查询审核历史记录（时间倒序，追加写入不覆盖）
        List<DesignReviewEntity> reviews = list(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .eq(DesignReviewEntity::getOrderId, orderId)
                        .orderByDesc(DesignReviewEntity::getReviewTime));
        detailVO.setReviewHistory(reviews.stream()
                .map(this::toHistoryVO)
                .collect(Collectors.toList()));

        return detailVO;
    }

    /**
     * 审核通过
     * 调用一次 DESIGN_REVIEW_PASS，flow 模块内部根据 needsPhysicalDelivery 自动跳转到
     * 待打印(3010) 或 待客户确认(7010)，TransitionResult 返回最终状态落库
     *
     * @param orderId 订单ID
     * @param dto     审核通过请求体（可为 null）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPass(Long orderId, ReviewPassDTO dto) {
        log.info("审核通过，orderId={}", orderId);

        // 1. 校验订单存在且状态为 2040
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.DESIGN_REVIEWING.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许审核通过，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 获取当前审核人信息
        Long reviewerId = StpUtil.getLoginIdAsLong();
        UserEntity reviewer = userService.getById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : null;

        // 3. 写入审核记录（result=1 通过）
        DesignReviewEntity reviewRecord = new DesignReviewEntity();
        reviewRecord.setOrderId(orderId);
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setReviewerName(reviewerName);
        reviewRecord.setReviewResult(1);
        reviewRecord.setComment(dto != null ? dto.getComment() : null);
        reviewRecord.setReviewTime(LocalDateTime.now());
        save(reviewRecord);

        // 4. 执行状态流转：DESIGN_REVIEWING → (2050 不可见) → 3010 或 7010
        // flow 模块内部根据 order.needsPhysicalDelivery 自动完成分支跳转
        TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_PASS,
                FlowOperator.of(reviewerId, reviewerName));

        // 5. 回写订单表（最终状态，清空当前处理人）
        OrderMainEntity update = new OrderMainEntity();
        update.setId(orderId);
        update.setPhase(result.getTargetPhase());
        update.setStatus(result.getFinalStatus());
        update.setCurrentHandlerId(null);
        update.setCurrentHandlerName(null);
        orderMainService.updateById(update);

        log.info("审核通过成功，orderId={}, finalPhase={}, finalStatus={}",
                orderId, result.getTargetPhase(), result.getFinalStatus());
    }

    /**
     * 审核驳回
     * 状态流转：DESIGN_REVIEWING(2040) → DESIGN_REVIEW_REJECTED(2060)
     * 驳回原因写入 order_main.design_review_remark 供列表/详情展示
     *
     * @param orderId 订单ID
     * @param dto     审核驳回请求体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewReject(Long orderId, ReviewRejectDTO dto) {
        log.info("审核驳回，orderId={}", orderId);

        // 1. 校验订单存在且状态为 2040
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.DESIGN_REVIEWING.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许审核驳回，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 获取当前审核人信息
        Long reviewerId = StpUtil.getLoginIdAsLong();
        UserEntity reviewer = userService.getById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : null;

        // 3. 写入审核记录（result=0 驳回）
        DesignReviewEntity reviewRecord = new DesignReviewEntity();
        reviewRecord.setOrderId(orderId);
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setReviewerName(reviewerName);
        reviewRecord.setReviewResult(0);
        reviewRecord.setRejectReason(dto.getRejectReason());
        reviewRecord.setReviewTime(LocalDateTime.now());
        save(reviewRecord);

        // 4. 执行状态流转：DESIGN_REVIEWING → DESIGN_REVIEW_REJECTED(2060)
        FlowOperator operator = FlowOperator.of(reviewerId, reviewerName);
        operator.setRemark(dto.getRejectReason());
        TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_REJECT, operator);

        // 5. 回写订单表（含驳回原因快照，恢复分配设计师为当前处理人）
        OrderMainEntity update = new OrderMainEntity();
        update.setId(orderId);
        update.setPhase(result.getTargetPhase());
        update.setStatus(result.getFinalStatus());
        update.setDesignReviewRemark(dto.getRejectReason());
        update.setCurrentHandlerId(order.getDesignerId());
        update.setCurrentHandlerName(order.getDesignerName());
        orderMainService.updateById(update);

        log.info("审核驳回成功，orderId={}, phase={}, status={}",
                orderId, result.getTargetPhase(), result.getFinalStatus());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将审核记录实体转换为历史 VO
     * 注意：reviewResult 是 Integer，用 equals 比较避免自动拆箱风险
     */
    private DesignReviewHistoryVO toHistoryVO(DesignReviewEntity entity) {
        DesignReviewHistoryVO vo = new DesignReviewHistoryVO();
        vo.setId(entity.getId());
        vo.setReviewerName(entity.getReviewerName());
        vo.setReviewResult(entity.getReviewResult());
        vo.setReviewResultName(Integer.valueOf(1).equals(entity.getReviewResult()) ? "通过" : "驳回");
        vo.setComment(entity.getComment());
        vo.setRejectReason(entity.getRejectReason());
        vo.setReviewTime(entity.getReviewTime());
        return vo;
    }
}
