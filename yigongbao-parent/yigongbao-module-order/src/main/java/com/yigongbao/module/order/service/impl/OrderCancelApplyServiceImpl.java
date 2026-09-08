package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.order.convert.OrderCancelApplyConvert;
import com.yigongbao.module.order.dto.order.CancelApplyPageQueryDTO;
import com.yigongbao.module.order.dto.order.AuditCancelApplyDTO;
import com.yigongbao.module.order.dto.order.CancelOrderApplyDTO;
import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.common.event.CancelApplyApprovedEvent;
import com.yigongbao.common.event.CancelApplyRejectedEvent;
import com.yigongbao.common.event.CancelApplySubmittedEvent;
import com.yigongbao.module.order.mapper.OrderCancelApplyMapper;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单取消申请 Service 实现
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Slf4j
@Service
public class OrderCancelApplyServiceImpl extends ServiceImpl<OrderCancelApplyMapper, OrderCancelApplyEntity>
        implements OrderCancelApplyService {

    private final OrderMainService orderMainService;
    private final FlowFacade flowFacade;
    private final OrderCancelApplyConvert cancelApplyConvert;
    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;

    /**
     * 构造器注入，使用@Lazy打破循环依赖
     */
    public OrderCancelApplyServiceImpl(
            @Lazy OrderMainService orderMainService,
            FlowFacade flowFacade,
            OrderCancelApplyConvert cancelApplyConvert,
            ApplicationEventPublisher eventPublisher,
            UserService userService) {
        this.orderMainService = orderMainService;
        this.flowFacade = flowFacade;
        this.cancelApplyConvert = cancelApplyConvert;
        this.eventPublisher = eventPublisher;
        this.userService = userService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitCancelApply(CancelOrderApplyDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Long orderId = dto.getOrderId();

        // 1. 验证订单存在且未取消
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            log.warn("提交取消申请失败，订单不存在: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        if (FlowStatusEnum.CANCELLED.getValue().equals(order.getStatus())) {
            log.warn("提交取消申请失败，订单已取消: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_CANCELLED);
        }

        // 2. 验证订单阶段≥20
        if (order.getPhase() == null || order.getPhase() < 20 || order.getPhase() >= 80) {
            log.warn("提交取消申请失败，订单阶段不允许: orderId={}, phase={}", orderId, order.getPhase());
            throw new BusinessException(ErrorCodeEnum.ORDER_PHASE_NOT_ALLOW_APPLY);
        }

        // 3. 验证无待审核的取消申请
        if (Integer.valueOf(StatusConstants.YES).equals(order.getHasPendingCancelApply())) {
            log.warn("提交取消申请失败，存在待审核申请: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        // 4. 权限校验：仅订单创建人或设计师可提交
        boolean isCreator = currentUserId.equals(order.getCreateBy());
        boolean isDesigner = currentUserId.equals(order.getDesignerId());
        if (!isCreator && !isDesigner) {
            log.warn("提交取消申请失败，无权限: orderId={}, userId={}, creator={}, designer={}",
                    orderId, currentUserId, order.getCreateBy(), order.getDesignerId());
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        // 5. 原子抢占订单待审核标记，防止并发提交多个申请
        boolean markerClaimed = orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getId, orderId)
                .and(wrapper -> wrapper
                        .eq(OrderMainEntity::getHasPendingCancelApply, StatusConstants.NO)
                        .or()
                        .isNull(OrderMainEntity::getHasPendingCancelApply))
                .set(OrderMainEntity::getHasPendingCancelApply, StatusConstants.YES));
        if (!markerClaimed) {
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        // 6. 创建取消申请记录
        OrderCancelApplyEntity apply = new OrderCancelApplyEntity();
        apply.setOrderId(orderId);
        apply.setApplyBy(currentUserId);
        apply.setApplyReason(dto.getReason());
        apply.setAuditStatus(ApplyStatusEnum.PENDING.getCode());
        save(apply);

        // 7. 发布申请提交事件
        eventPublisher.publishEvent(new CancelApplySubmittedEvent(this, apply.getId(), orderId, currentUserId));

        log.info("提交取消申请: applyId={}, orderId={}, applyBy={}, reason={}",
                apply.getId(), orderId, currentUserId, dto.getReason());

        return apply.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCancelApply(Long applyId, AuditCancelApplyDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        // 1. 权限校验：仅设计管理员可审核
        String roleCode = getCurrentUserRoleCode();
        if (!RoleCodeConstants.DESIGN_ADMIN.equals(roleCode)) {
            log.warn("审核取消申请失败，无权限: applyId={}, userId={}, roleCode={}",
                    applyId, currentUserId, roleCode);
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        // 2. 验证申请存在
        OrderCancelApplyEntity apply = getById(applyId);
        if (apply == null) {
            log.warn("审核取消申请失败，申请不存在: applyId={}", applyId);
            throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_NOT_FOUND);
        }

        // 3. 验证申请状态为待审核
        if (!ApplyStatusEnum.PENDING.getCode().equals(apply.getAuditStatus())) {
            log.warn("审核取消申请失败，申请已审核: applyId={}, auditStatus={}",
                    applyId, apply.getAuditStatus());
            throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_ALREADY_AUDITED);
        }

        Long orderId = apply.getOrderId();
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            log.error("审核取消申请失败，订单不存在: applyId={}, orderId={}", applyId, orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        String auditorName = getUserRealName(currentUserId);

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        boolean claimed = update(new LambdaUpdateWrapper<OrderCancelApplyEntity>()
                .eq(OrderCancelApplyEntity::getId, applyId)
                .eq(OrderCancelApplyEntity::getAuditStatus, ApplyStatusEnum.PENDING.getCode())
                .set(OrderCancelApplyEntity::getAuditStatus,
                        approved ? ApplyStatusEnum.APPROVED.getCode() : ApplyStatusEnum.REJECTED.getCode())
                .set(OrderCancelApplyEntity::getAuditBy, currentUserId)
                .set(OrderCancelApplyEntity::getAuditReason, dto.getReason())
                .set(OrderCancelApplyEntity::getAuditTime, LocalDateTime.now()));
        if (!claimed) {
            throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_ALREADY_AUDITED);
        }

        if (approved) {
            // 审核通过：执行订单取消流程
            try {
                TransitionResult result = flowFacade.executeFlow(
                        orderId,
                        FlowActionEnum.CANCEL,
                        new FlowOperator(currentUserId, auditorName, dto.getReason()));

                // 更新订单状态和标记
                Integer version = order.getVersion() == null ? 0 : order.getVersion();
                boolean orderUpdated = orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
                        .eq(OrderMainEntity::getId, orderId)
                        .eq(OrderMainEntity::getVersion, version)
                        .set(OrderMainEntity::getPhase, result.getTargetPhase())
                        .set(OrderMainEntity::getStatus, result.getFinalStatus())
                        .set(OrderMainEntity::getHasPendingCancelApply, StatusConstants.NO));
                if (!orderUpdated) {
                    throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
                }

                // 发布审核通过事件
                eventPublisher.publishEvent(new CancelApplyApprovedEvent(
                        this, applyId, orderId, currentUserId, apply.getApplyBy()));

                log.info("取消申请审核通过: applyId={}, orderId={}, auditBy={}, phase={}, status={}",
                        applyId, orderId, currentUserId, result.getTargetPhase(), result.getFinalStatus());

            } catch (Exception e) {
                log.error("取消申请审核通过失败: applyId={}, orderId={}", applyId, orderId, e);
                throw e;
            }

        } else {
            // 审核驳回：更新申请记录并清除订单标记
            // 清除订单待审核标记
            Integer version = order.getVersion() == null ? 0 : order.getVersion();
            boolean orderUpdated = orderMainService.update(new LambdaUpdateWrapper<OrderMainEntity>()
                    .eq(OrderMainEntity::getId, orderId)
                    .eq(OrderMainEntity::getVersion, version)
                    .set(OrderMainEntity::getHasPendingCancelApply, StatusConstants.NO));
            if (!orderUpdated) {
                throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
            }

            // 发布审核驳回事件
            eventPublisher.publishEvent(new CancelApplyRejectedEvent(
                    this, applyId, orderId, currentUserId, apply.getApplyBy()));

            log.info("取消申请审核驳回: applyId={}, orderId={}, auditBy={}, reason={}",
                    applyId, orderId, currentUserId, dto.getReason());
        }
    }

    @Override
    public CancelApplyVO getCancelApplyDetail(Long applyId) {
        OrderCancelApplyEntity apply = getById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.CANCEL_APPLY_NOT_FOUND);
        }

        OrderMainEntity order = orderMainService.getById(apply.getOrderId());
        ensureCanView(apply, order);
        return buildCancelApplyVO(apply);
    }

    @Override
    public IPage<CancelApplyVO> listPendingApplies(CancelApplyPageQueryDTO dto) {
        requireDesignAdmin();
        Page<OrderCancelApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        IPage<OrderCancelApplyEntity> result = baseMapper.selectPendingPage(page, dto);

        return result.convert(this::buildCancelApplyVO);
    }

    @Override
    public boolean hasPendingCancelApply(Long orderId) {
        return count(new LambdaQueryWrapper<OrderCancelApplyEntity>()
                .eq(OrderCancelApplyEntity::getOrderId, orderId)
                .eq(OrderCancelApplyEntity::getAuditStatus, ApplyStatusEnum.PENDING.getCode())
                .eq(OrderCancelApplyEntity::getIsDeleted, StatusConstants.NOT_DELETED)) > 0;
    }

    @Override
    public IPage<CancelApplyVO> listMyApplies(CancelApplyPageQueryDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Page<OrderCancelApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        IPage<OrderCancelApplyEntity> result = baseMapper.selectMyPage(page, dto, currentUserId);

        return result.convert(this::buildCancelApplyVO);
    }

    @Override
    public List<CancelApplyVO> getCancelApplyHistory(Long orderId) {
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!isDesignAdmin() && !Objects.equals(currentUserId, order.getCreateBy())
                && !Objects.equals(currentUserId, order.getDesignerId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
        LambdaQueryWrapper<OrderCancelApplyEntity> qw = new LambdaQueryWrapper<>();
        qw.eq(OrderCancelApplyEntity::getOrderId, orderId)
                .orderByDesc(OrderCancelApplyEntity::getCreateTime);

        List<OrderCancelApplyEntity> applies = list(qw);

        return applies.stream()
                .map(this::buildCancelApplyVO)
                .collect(Collectors.toList());
    }

    /**
     * 构建取消申请VO（包含用户名称和订单编号）
     *
     * @param entity 取消申请实体
     * @return 取消申请VO
     */
    private CancelApplyVO buildCancelApplyVO(OrderCancelApplyEntity entity) {
        if (entity == null) {
            return null;
        }

        String applyByName = getUserRealName(entity.getApplyBy());
        String auditByName = entity.getAuditBy() != null ? getUserRealName(entity.getAuditBy()) : null;

        OrderMainEntity order = orderMainService.getById(entity.getOrderId());
        String orderCode = order != null ? order.getOrderCode() : null;
        String publicOrderCode = order != null ? order.getPublicOrderCode() : null;

        return cancelApplyConvert.toVO(entity, applyByName, auditByName, orderCode, publicOrderCode);
    }

    /**
     * 获取用户真实姓名
     *
     * @param userId 用户ID
     * @return 用户真实姓名
     */
    private String getUserRealName(Long userId) {
        if (userId == null) {
            return null;
        }
        UserEntity user = userService.getById(userId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * 获取当前用户角色编码
     *
     * @return 角色编码
     */
    private String getCurrentUserRoleCode() {
        return userService.getCurrentUserRoleCode();
    }

    private void requireDesignAdmin() {
        if (!isDesignAdmin()) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
    }

    private boolean isDesignAdmin() {
        return RoleCodeConstants.DESIGN_ADMIN.equals(getCurrentUserRoleCode());
    }

    private void ensureCanView(OrderCancelApplyEntity apply, OrderMainEntity order) {
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!isDesignAdmin() && !Objects.equals(currentUserId, apply.getApplyBy())
                && !Objects.equals(currentUserId, order.getCreateBy())
                && !Objects.equals(currentUserId, order.getDesignerId())) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
    }
}
