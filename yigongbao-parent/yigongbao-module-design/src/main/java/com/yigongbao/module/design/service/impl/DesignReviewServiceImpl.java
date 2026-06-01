package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.event.DesignReviewPassedEvent;
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
import com.yigongbao.module.design.enums.ReviewResultEnum;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 分页查询待审核工单列表
     * 强制覆盖 status 为 2040（设计审核中），复用工单查询逻辑
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listReviewWorkorders(DesignWorkorderQueryDTO queryDTO) {
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
        // 1. 获取工单详情（复用现有逻辑）
        DesignWorkorderDetailVO workorderDetail = designWorkorderService.getWorkorderDetail(orderId);
        if (workorderDetail == null) {
            log.warn("订单不存在: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

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

        // 3. 先执行状态流转：DESIGN_REVIEWING → (2050 不可见) → 3010 或 7010
        // 优先流转：确保状态已变更后再写审核记录，防止并发重复审核写入多条记录
        // flow 模块内部根据 order.needsPhysicalDelivery 自动完成分支跳转
        try {
            TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_PASS,
                    FlowOperator.of(reviewerId, reviewerName), dto != null ? dto.getVersion() : null);

            // 4. 写入审核记录（result=1 通过）
            DesignReviewEntity reviewRecord = new DesignReviewEntity();
            reviewRecord.setOrderId(orderId);
            reviewRecord.setReviewerId(reviewerId);
            reviewRecord.setReviewerName(reviewerName);
            reviewRecord.setReviewResult(ReviewResultEnum.PASS.getCode());
            reviewRecord.setComment(dto != null ? dto.getComment() : null);
            reviewRecord.setReviewTime(LocalDateTime.now());
            save(reviewRecord);

            // 5. 回写订单表（最终状态，清空当前处理人）
            OrderMainEntity update = new OrderMainEntity();
            update.setId(orderId);
            update.setPhase(result.getTargetPhase());
            update.setStatus(result.getFinalStatus());
            update.setCurrentHandlerId(null);
            update.setCurrentHandlerName(null);
            orderMainService.updateById(update);

            // 6. 发布设计审核通过事件（触发生产模块自动创建流转卡）
            eventPublisher.publishEvent(new DesignReviewPassedEvent(this, orderId));

            log.info("设计审核通过: orderId={}, {} -> {}, reviewerId={}",
                orderId, FlowStatusEnum.DESIGN_REVIEWING.getName(),
                FlowStatusEnum.DESIGN_REVIEW_PASSED.getName(), reviewerId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核通过异常: orderId={}", orderId, e);
            throw e;
        }
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
        // 1. 校验订单存在且状态为 2040
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.DESIGN_REVIEWING.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许审核驳回，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 2. 校验设计师已分配（驳回后需恢复处理人，若设计师为空则无法恢复）
        if (order.getDesignerId() == null) {
            log.warn("订单未分配设计师，无法驳回，orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.DESIGNER_NOT_ASSIGNED);
        }

        // 3. 获取当前审核人信息
        Long reviewerId = StpUtil.getLoginIdAsLong();
        UserEntity reviewer = userService.getById(reviewerId);
        String reviewerName = reviewer != null ? reviewer.getRealName() : null;

        // 4. 先执行状态流转：DESIGN_REVIEWING → DESIGN_REVIEW_REJECTED(2060)
        // 优先流转：确保状态已变更后再写审核记录，防止并发重复驳回写入多条记录
        FlowOperator operator = FlowOperator.of(reviewerId, reviewerName);
        operator.setRemark(dto.getRejectReason());
        try {
            TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.DESIGN_REVIEW_REJECT,
                    operator, dto.getVersion());

            // 5. 写入审核记录（result=0 驳回）
            DesignReviewEntity reviewRecord = new DesignReviewEntity();
            reviewRecord.setOrderId(orderId);
            reviewRecord.setReviewerId(reviewerId);
            reviewRecord.setReviewerName(reviewerName);
            reviewRecord.setReviewResult(ReviewResultEnum.REJECT.getCode());
            reviewRecord.setRejectReason(dto.getRejectReason());
            reviewRecord.setReviewTime(LocalDateTime.now());
            save(reviewRecord);

            // 6. 回写订单表（含驳回原因快照，恢复分配设计师为当前处理人）
            OrderMainEntity update = new OrderMainEntity();
            update.setId(orderId);
            update.setPhase(result.getTargetPhase());
            update.setStatus(result.getFinalStatus());
            update.setDesignReviewRemark(dto.getRejectReason());
            update.setCurrentHandlerId(order.getDesignerId());
            update.setCurrentHandlerName(order.getDesignerName());
            orderMainService.updateById(update);

            log.info("设计审核驳回: orderId={}, {} -> {}, reviewerId={}, reason={}",
                orderId, FlowStatusEnum.DESIGN_REVIEWING.getName(), FlowStatusEnum.DESIGN_REVIEW_REJECTED.getName(),
                reviewerId, dto.getRejectReason());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核驳回异常: orderId={}", orderId, e);
            throw e;
        }
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
        // 明确判断三个分支：1=通过，0=驳回，null=未知（避免 null 时默认显示"驳回"的逻辑错误）
        Integer reviewResult = entity.getReviewResult();
        String resultName;
        if (ReviewResultEnum.PASS.getCode().equals(reviewResult)) {
            resultName = "通过";
        } else if (ReviewResultEnum.REJECT.getCode().equals(reviewResult)) {
            resultName = "驳回";
        } else {
            resultName = "未知";
        }
        vo.setReviewResultName(resultName);
        vo.setComment(entity.getComment());
        vo.setRejectReason(entity.getRejectReason());
        vo.setReviewTime(entity.getReviewTime());
        return vo;
    }
}
