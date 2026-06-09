package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.service.FlowOrderService;
import com.yigongbao.module.order.convert.OrderDiffCalculator;
import com.yigongbao.module.order.dto.apply.ApplyListQueryDTO;
import com.yigongbao.module.order.dto.apply.AuditApplyDTO;
import com.yigongbao.module.order.dto.diff.OrderModificationDiff;
import com.yigongbao.module.order.dto.modify.ModificationLogPageQueryDTO;
import com.yigongbao.module.order.dto.modify.OrderModifyFullDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.service.OrderModifyFullService;
import com.yigongbao.module.order.utils.OrderModifyTimeWindowChecker;
import com.yigongbao.module.order.vo.apply.ApplyDetailVO;
import com.yigongbao.module.order.vo.apply.ApplyListItemVO;
import com.yigongbao.module.order.vo.modify.ModificationLogVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单修改申请 Service 实现类
 *
 * 【设计说明】
 * - 不注入 OrderMainService，直接注入 OrderMainMapper（规避循环依赖）
 * - 医院/科室/医生的冗余字段同步统一走 OrderDataValidator.validateAndFillForModify
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderModifyApplyServiceImpl implements OrderModifyApplyService {

    private final OrderModificationLogMapper orderModificationLogMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderFileMapper orderFileMapper;
    private final UserService userService;
    private final FlowFacade flowFacade;
    private final FlowOrderService flowOrderService;
    private final OrderModificationApplyMapper orderModificationApplyMapper;
    private final OrderModifyFullService orderModifyFullService;
    private final OrderDiffCalculator diffCalculator;
    private final OrderModifyTimeWindowChecker timeWindowChecker;
    private final ConfigService configService;

    // ==================== 申请审核流程方法 ====================

    /**
     * 提交修改申请（超过时间窗口时使用）
     *
     * @param orderId 订单ID
     * @param dto     完整订单修改数据
     * @return 申请ID
     * @author hanjor
     * @date 2026-06-08
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitApply(Long orderId, OrderModifyFullDTO dto) {
        // 检查权限：设计师禁止提交修改申请
        checkModifyPermission();

        if (dto == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "修改内容不能为空");
        }

        // 查询订单是否存在
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 检查订单所有权：只有订单创建者可以提交修改申请
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getOperatorId())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_BELONG_TO_USER);
        }

        // 检查是否存在待审核申请，避免重复提交
        List<OrderModificationApplyEntity> pendingApplies = orderModificationApplyMapper.selectList(
                new LambdaQueryWrapper<OrderModificationApplyEntity>()
                        .eq(OrderModificationApplyEntity::getOrderId, orderId)
                        .eq(OrderModificationApplyEntity::getStatus, ApplyStatusEnum.PENDING.getCode())
        );
        if (CollUtil.isNotEmpty(pendingApplies)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_PENDING);
        }

        // 加载订单当前数据，用于计算变更差异
        List<OrderItemEntity> currentItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
        );
        List<OrderFileEntity> currentFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
        );

        // 计算新旧数据差异(用于审核预览)
        OrderDraftEntity draftOrder = new OrderDraftEntity();
        BeanUtils.copyProperties(order, draftOrder);
        OrderModificationDiff diff = diffCalculator.calculateDiff(
                draftOrder, currentItems, currentFiles, dto
        );

        // 创建申请记录
        OrderModificationApplyEntity apply = new OrderModificationApplyEntity();
        apply.setOrderId(orderId);
        apply.setOrderCode(order.getOrderCode());
        apply.setApplyType(ModifyApplyTypeEnum.FULL.getCode()); // 全量修改类型
        apply.setModificationContent(JSONUtil.toJsonStr(dto));
        apply.setModificationDiff(JSONUtil.toJsonStr(diff));

        // 设置申请人信息
        Long userId = StpUtil.getLoginIdAsLong();
        String userName = getCurrentUserName();
        apply.setApplyUserId(userId);
        apply.setApplyUserName(userName);
        apply.setApplyTime(LocalDateTime.now());

        // 设置过期时间（从配置读取，默认10分钟）
        String expireMinutesStr = configService.getConfigValue(
                SystemConfigKeyEnum.ORDER_MODIFY_APPLY_EXPIRE_MINUTES.getKey()
        );
        int expireMinutes = 10;
        if (StrUtil.isNotBlank(expireMinutesStr)) {
            try {
                expireMinutes = Integer.parseInt(expireMinutesStr);
            } catch (NumberFormatException e) {
                log.warn("订单修改申请过期时间配置格式错误，使用默认值: configValue={}, defaultValue=10", expireMinutesStr);
            }
        }
        apply.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        apply.setStatus(ApplyStatusEnum.PENDING.getCode()); // 待审核状态

        orderModificationApplyMapper.insert(apply);

        log.info("提交修改申请: applyId={}, orderId={}, userId={}", apply.getId(), orderId, userId);

        return apply.getId();
    }

    /**
     * 审核修改申请
     *
     * @param applyId 申请ID
     * @param dto     审核结果
     * @author hanjor
     * @date 2026-06-08
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditApply(Long applyId, AuditApplyDTO dto) {
        // 检查审核权限：仅设计管理员可审核
        checkAuditPermission();

        if (dto == null || dto.getResult() == null) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "审核结果不能为空");
        }

        // 查询申请记录
        OrderModificationApplyEntity apply = orderModificationApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 校验申请状态必须为待审核
        if (!ApplyStatusEnum.PENDING.getCode().equals(apply.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_NOT_PENDING);
        }

        // 校验申请是否过期，过期则更新状态并拒绝审核
        if (apply.getExpireTime().isBefore(LocalDateTime.now())) {
            apply.setStatus(ApplyStatusEnum.EXPIRED.getCode());
            orderModificationApplyMapper.updateById(apply);
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_EXPIRED);
        }

        // 根据审核结果处理：1=通过，2=驳回
        if (ApplyStatusEnum.APPROVED.getCode().equals(dto.getResult())) {
            // 审核通过：先执行订单修改，成功后再更新申请状态
            OrderModifyFullDTO modifyDto;
            try {
                modifyDto = JSONUtil.toBean(apply.getModificationContent(), OrderModifyFullDTO.class);
            } catch (Exception e) {
                log.error("申请内容JSON解析失败: applyId={}, content={}", applyId, apply.getModificationContent(), e);
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "申请内容格式错误");
            }

            // 执行订单修改（如果失败会抛出异常，事务回滚，申请状态不会更新）
            orderModifyFullService.modifyOrderFull(apply.getOrderId(), modifyDto);

            // 订单修改成功后才更新申请状态
            apply.setStatus(ApplyStatusEnum.APPROVED.getCode());
        } else if (ApplyStatusEnum.REJECTED.getCode().equals(dto.getResult())) {
            // 审核驳回：记录驳回原因
            apply.setStatus(ApplyStatusEnum.REJECTED.getCode());
            apply.setAuditRemark(dto.getRemark());
        } else {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "审核结果无效");
        }

        // 记录审核人信息
        Long userId = StpUtil.getLoginIdAsLong();
        String userName = getCurrentUserName();
        apply.setAuditUserId(userId);
        apply.setAuditUserName(userName);
        apply.setAuditTime(LocalDateTime.now());

        orderModificationApplyMapper.updateById(apply);

        log.info("审核修改申请: applyId={}, result={}, auditUserId={}", applyId, dto.getResult(), userId);
    }

    /**
     * 检查修改权限（业务员允许，设计师禁止）
     */
    private void checkModifyPermission() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        if (RoleCodeEnum.DESIGNER.getCode().equals(user.getRoleCode())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_DESIGNER_NOT_ALLOWED);
        }
    }

    /**
     * 检查审核权限（设计管理员允许）
     */
    private void checkAuditPermission() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        if (!RoleCodeEnum.DESIGNER_MANAGER.getCode().equals(user.getRoleCode())) {
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_AUDIT_NO_PERMISSION);
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 查询修改申请列表（设计管理员）
     *
     * @param dto 查询条件
     * @return 分页列表
     */
    @Override
    public IPage<ApplyListItemVO> listApplies(ApplyListQueryDTO dto) {
        // 检查权限：仅设计管理员可查询所有申请
        checkAuditPermission();

        // 构建分页查询条件
        Page<OrderModificationApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModificationApplyEntity> wrapper = new LambdaQueryWrapper<OrderModificationApplyEntity>()
                .eq(dto.getStatus() != null, OrderModificationApplyEntity::getStatus, dto.getStatus())
                .like(StrUtil.isNotBlank(dto.getOrderCode()), OrderModificationApplyEntity::getOrderCode, dto.getOrderCode())
                .like(StrUtil.isNotBlank(dto.getApplyUserName()), OrderModificationApplyEntity::getApplyUserName, dto.getApplyUserName())
                .ge(dto.getApplyTimeStart() != null, OrderModificationApplyEntity::getApplyTime, dto.getApplyTimeStart())
                .le(dto.getApplyTimeEnd() != null, OrderModificationApplyEntity::getApplyTime, dto.getApplyTimeEnd())
                .orderByDesc(OrderModificationApplyEntity::getApplyTime);

        // 执行查询并转换为VO
        IPage<OrderModificationApplyEntity> entityPage = orderModificationApplyMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toApplyListItemVO);
    }

    /**
     * 查询修改申请详情
     *
     * @param applyId 申请ID
     * @return 申请详情
     */
    @Override
    public ApplyDetailVO getApplyDetail(Long applyId) {
        // 检查权限：仅设计管理员可查看申请详情
        checkAuditPermission();

        // 查询申请记录
        OrderModificationApplyEntity apply = orderModificationApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 组装详情VO，包含变更差异对比
        ApplyDetailVO vo = new ApplyDetailVO();
        vo.setApplyId(apply.getId());
        vo.setOrderId(apply.getOrderId());
        vo.setOrderCode(apply.getOrderCode());
        vo.setApplyUserName(apply.getApplyUserName());
        vo.setApplyTime(apply.getApplyTime());
        vo.setExpireTime(apply.getExpireTime());
        vo.setStatus(apply.getStatus());
        vo.setStatusDesc(getStatusDesc(apply.getStatus()));

        try {
            vo.setDiff(JSONUtil.toBean(apply.getModificationDiff(), OrderModificationDiff.class));
        } catch (Exception e) {
            log.error("申请差异JSON解析失败: applyId={}, diff={}", applyId, apply.getModificationDiff(), e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "申请数据格式错误");
        }

        return vo;
    }

    /**
     * 查询我的修改申请列表（业务员）
     *
     * @param dto 查询条件
     * @return 分页列表
     */
    @Override
    public IPage<ApplyListItemVO> myListApplies(ApplyListQueryDTO dto) {
        // 获取当前用户ID，仅查询本人提交的申请
        Long userId = StpUtil.getLoginIdAsLong();

        // 构建查询条件：过滤申请人为当前用户
        Page<OrderModificationApplyEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModificationApplyEntity> wrapper = new LambdaQueryWrapper<OrderModificationApplyEntity>()
                .eq(OrderModificationApplyEntity::getApplyUserId, userId)
                .eq(dto.getStatus() != null, OrderModificationApplyEntity::getStatus, dto.getStatus())
                .like(StrUtil.isNotBlank(dto.getOrderCode()), OrderModificationApplyEntity::getOrderCode, dto.getOrderCode())
                .ge(dto.getApplyTimeStart() != null, OrderModificationApplyEntity::getApplyTime, dto.getApplyTimeStart())
                .le(dto.getApplyTimeEnd() != null, OrderModificationApplyEntity::getApplyTime, dto.getApplyTimeEnd())
                .orderByDesc(OrderModificationApplyEntity::getApplyTime);

        // 执行查询并转换为VO
        IPage<OrderModificationApplyEntity> entityPage = orderModificationApplyMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toApplyListItemVO);
    }

    /**
     * 根据ID查询申请实体（内部使用）
     *
     * @param applyId 申请ID
     * @return 申请实体
     */
    @Override
    public OrderModificationApplyEntity getApplyEntityById(Long applyId) {
        return orderModificationApplyMapper.selectById(applyId);
    }

    /**
     * 全量修改订单（带时间窗口检查）
     */
    @Override
    public void modifyOrderFullV2(Long orderId, OrderModifyFullDTO dto) {
        checkModifyPermission();

        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        if (timeWindowChecker.isWithinTimeWindow(order.getCreateTime())) {
            log.info("订单在时间窗口内，直接修改: orderId={}, createTime={}", orderId, order.getCreateTime());
            orderModifyFullService.modifyOrderFull(orderId, dto);
        } else {
            log.warn("订单超出时间窗口，需提交申请: orderId={}, createTime={}", orderId, order.getCreateTime());
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_TIME_WINDOW_EXCEEDED);
        }
    }

    /**
     * 查询订单的修改留痕记录（分页）
     *
     * @param orderId 订单ID
     * @param dto     查询参数
     * @return 分页列表
     */
    @Override
    public IPage<ModificationLogVO> listModificationLogs(Long orderId, ModificationLogPageQueryDTO dto) {
        Page<OrderModificationLogEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderModificationLogEntity> wrapper =
                new LambdaQueryWrapper<OrderModificationLogEntity>()
                        .eq(OrderModificationLogEntity::getOrderId, orderId)
                        .eq(StrUtil.isNotBlank(dto.getFieldName()),
                                OrderModificationLogEntity::getFieldName, dto.getFieldName())
                        .orderByDesc(OrderModificationLogEntity::getCreateTime);
        IPage<OrderModificationLogEntity> entityPage = orderModificationLogMapper.selectPage(page, wrapper);
        return entityPage.convert(this::toLogVO);
    }

    // ==================== 通用辅助方法 ====================

    /**
     * 记录修改留痕
     */
    private void recordModificationLog(Long orderId, String orderCode, Long applyId,
            String fieldName, String fieldLabel, Object oldValue, Object newValue,
            Long modifierId, String modifierName) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        OrderModificationLogEntity logEntity = new OrderModificationLogEntity();
        logEntity.setOrderId(orderId);
        logEntity.setOrderCode(orderCode);
        logEntity.setApplyId(applyId);
        logEntity.setFieldName(fieldName);
        logEntity.setFieldLabel(fieldLabel);
        logEntity.setOldValue(oldValue != null ? oldValue.toString() : null);
        logEntity.setNewValue(newValue != null ? newValue.toString() : null);
        logEntity.setModifierId(modifierId);
        logEntity.setModifierName(modifierName);
        orderModificationLogMapper.insert(logEntity);
    }

    /**
     * 获取当前登录用户姓名
     */
    private String getCurrentUserName() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            UserEntity user = userService.getById(userId);
            return user != null ? user.getRealName() : null;
        } catch (Exception e) {
            log.warn("获取当前用户姓名失败", e);
            return null;
        }
    }

    // ==================== Entity → VO 转换 ====================

    private ModificationLogVO toLogVO(OrderModificationLogEntity entity) {
        ModificationLogVO vo = new ModificationLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private ApplyListItemVO toApplyListItemVO(OrderModificationApplyEntity entity) {
        ApplyListItemVO vo = new ApplyListItemVO();
        vo.setApplyId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setOrderCode(entity.getOrderCode());
        vo.setApplyUserName(entity.getApplyUserName());
        vo.setApplyTime(entity.getApplyTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(getStatusDesc(entity.getStatus()));

        if (StrUtil.isNotBlank(entity.getModificationDiff())) {
            try {
                OrderModificationDiff diff =
                    JSONUtil.toBean(entity.getModificationDiff(), OrderModificationDiff.class);
                int changeCount = 0;
                if (diff.getInfoFields() != null) changeCount += diff.getInfoFields().size();
                if (diff.getItems() != null && diff.getItems().isChanged()) changeCount++;
                if (diff.getImageData() != null && diff.getImageData().isChanged()) changeCount++;
                if (diff.getImageReport() != null && diff.getImageReport().isChanged()) changeCount++;
                vo.setChangeCount(changeCount);
                vo.setChangeSummary(buildChangeSummary(diff));
            } catch (Exception e) {
                log.warn("申请差异JSON解析失败，跳过统计: applyId={}, diff={}", entity.getId(), entity.getModificationDiff());
                vo.setChangeCount(0);
                vo.setChangeSummary("数据格式错误");
            }
        }

        return vo;
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已驳回";
            case 3 -> "已过期";
            default -> "未知";
        };
    }

    private String buildChangeSummary(OrderModificationDiff diff) {
        List<String> parts = new ArrayList<>();
        if (diff.getInfoFields() != null && !diff.getInfoFields().isEmpty()) {
            parts.add("基础信息");
        }
        if (diff.getImageData() != null && diff.getImageData().isChanged()) {
            parts.add("影像数据");
        }
        if (diff.getImageReport() != null && diff.getImageReport().isChanged()) {
            parts.add("影像报告");
        }
        if (diff.getItems() != null && diff.getItems().isChanged()) {
            parts.add("重建项目");
        }
        return parts.isEmpty() ? "无变更" : String.join("、", parts);
    }
}
