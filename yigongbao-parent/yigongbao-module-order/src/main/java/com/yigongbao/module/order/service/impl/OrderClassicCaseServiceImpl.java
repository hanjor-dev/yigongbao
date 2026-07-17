package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.event.ClassicCaseMarkedEvent;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.order.convert.ClassicCaseConvert;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单经典案例服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClassicCaseServiceImpl implements IOrderClassicCaseService {

    private final OrderMainMapper orderMainMapper;
    private final OrderMainService orderMainService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.yigongbao.module.order.helper.OrderQueryHelper orderQueryHelper;
    private final com.yigongbao.module.system.user.service.UserService userService;

    /**
     * 将订单标记为经典案例
     * <p>
     * 只有处于"已完成"状态（phase=80）的订单才能标记为经典案例。
     * 标记后会同步执行文件迁移，确保操作原子性，避免异步导致的数据不一致。
     * 如果文件迁移失败，整个事务回滚，订单不会被标记。
     * </p>
     *
     * @param dto 标记请求参数（包含订单ID和备注）
     * @throws BusinessException 订单不存在、未完成或已标记时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsClassicCase(MarkClassicCaseDTO dto) {
        // 查询订单实体
        OrderMainEntity order = orderMainMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 校验订单状态是否允许标记为经典案例
        // 1. 需要实物交付的订单：必须处于"已完成"（phase=80）或"已出库"（status=6030）
        // 2. 不需要实物交付的订单：设计完成（status=2030）即可标记
        boolean isCompleted = FlowPhaseEnum.COMPLETED.getValue().equals(order.getPhase());
        boolean isWarehouseOut = FlowStatusEnum.WAREHOUSE_OUT.getValue().equals(order.getStatus());
        boolean isDesignCompleted = FlowStatusEnum.DESIGN_COMPLETED.getValue().equals(order.getStatus());
        boolean needsPhysicalDelivery = Integer.valueOf(StatusConstants.YES).equals(order.getNeedsPhysicalDelivery());

        boolean canMark = isCompleted || isWarehouseOut ||
                         (isDesignCompleted && !needsPhysicalDelivery);

        if (!canMark) {
            log.warn("订单状态不允许标记为经典案例: orderId={}, phase={}, status={}, needsPhysicalDelivery={}",
                dto.getOrderId(), order.getPhase(), order.getStatus(), order.getNeedsPhysicalDelivery());
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ORDER_NOT_COMPLETED);
        }

        // 校验订单是否已经标记为经典案例
        if (StatusConstants.YES == order.getIsClassicCase()) {
            log.warn("订单已是经典案例，不可重复标记: orderId={}, orderCode={}",
                dto.getOrderId(), order.getOrderCode());
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ALREADY_MARKED);
        }

        // 更新订单的经典案例相关字段
        order.setIsClassicCase(StatusConstants.YES);
        order.setClassicCaseTime(LocalDateTime.now());
        order.setClassicCaseBy(StpUtil.getLoginIdAsLong());
        order.setClassicCaseRemark(dto.getRemark());
        orderMainMapper.updateById(order);

        log.info("标记订单为经典案例: orderId={}, orderCode={}, operator={}, remark={}",
                order.getId(), order.getOrderCode(), order.getClassicCaseBy(), dto.getRemark());

        // 发布经典案例标记事件，同步执行文件迁移（非异步）
        // 如果文件迁移失败，事务回滚，订单不会被标记
        eventPublisher.publishEvent(new ClassicCaseMarkedEvent(this, order.getId(), order.getOrderCode()));

        log.info("经典案例标记及文件迁移完成: orderId={}, orderCode={}", order.getId(), order.getOrderCode());
    }

    /**
     * 分页查询经典案例列表
     * <p>
     * 支持多条件筛选：
     * - keyword：模糊匹配订单编号、患者姓名、医院名称、机构名称、业务员姓名
     * - hospitalId：医院筛选
     * - startTime/endTime：订单创建时间范围筛选
     * 结果按订单创建时间倒序排列。
     * </p>
     *
     * @param dto 查询条件（包含分页参数和筛选条件）
     * @return 经典案例分页列表
     */
    @Override
    public IPage<ClassicCaseVO> listClassicCases(ClassicCaseQueryDTO dto) {
        Page<OrderMainEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

        // 只查询标记为经典案例的订单（is_classic_case = 1）
        wrapper.eq(OrderMainEntity::getIsClassicCase, StatusConstants.YES);

        // 关键词模糊查询（订单编号、患者姓名、医院名称、机构名称、业务员姓名）
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w.like(OrderMainEntity::getOrderCode, dto.getKeyword())
                    .or().like(OrderMainEntity::getPatientName, dto.getKeyword())
                    .or().like(OrderMainEntity::getHospitalName, dto.getKeyword())
                    .or().like(OrderMainEntity::getOrgName, dto.getKeyword())
                    .or().like(OrderMainEntity::getOperatorName, dto.getKeyword()));
        }

        // 医院筛选
        if (dto.getHospitalId() != null) {
            wrapper.eq(OrderMainEntity::getHospitalId, dto.getHospitalId());
        }

        // 订单创建时间范围筛选
        if (dto.getStartTime() != null) {
            wrapper.ge(OrderMainEntity::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(OrderMainEntity::getCreateTime, dto.getEndTime());
        }

        // 按订单创建时间倒序排列（最新创建的排在前面）
        wrapper.orderByDesc(OrderMainEntity::getCreateTime);

        IPage<OrderMainEntity> entityPage = orderMainMapper.selectPage(page, wrapper);

        // 收集所有标记人ID并批量查询用户信息
        List<Long> userIds = entityPage.getRecords().stream()
                .map(OrderMainEntity::getClassicCaseBy)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> userNameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<com.yigongbao.module.system.user.entity.UserEntity> users = userService.listByIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(
                            com.yigongbao.module.system.user.entity.UserEntity::getId,
                            com.yigongbao.module.system.user.entity.UserEntity::getRealName,
                            (a, b) -> a));
        }

        // 使用 OrderQueryHelper 转换为完整的 OrderListVO，然后转换为 ClassicCaseVO
        Map<Long, String> finalUserNameMap = userNameMap;
        List<ClassicCaseVO> voList = entityPage.getRecords().stream()
                .map(entity -> {
                    // 先使用 OrderQueryHelper 获取包含所有翻译字段的 OrderListVO
                    OrderListVO orderListVO = orderQueryHelper.toOrderListVO(entity);
                    // 再转换为 ClassicCaseVO，添加经典案例特有字段（列表查询不包含订单明细和文件）
                    ClassicCaseVO vo = ClassicCaseConvert.toClassicCaseVOFromList(orderListVO, entity);
                    // 填充标记人姓名
                    if (entity.getClassicCaseBy() != null) {
                        vo.setClassicCaseByName(finalUserNameMap.get(entity.getClassicCaseBy()));
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        // 构建分页结果
        Page<ClassicCaseVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 查询经典案例详情
     * <p>
     * 根据订单ID查询经典案例的详细信息，包含完整的订单信息、订单明细、文件列表等。
     * 经典案例是公开的示例内容，不受数据权限限制，所有用户均可查看。
     * 如果订单不存在或未标记为经典案例，则抛出异常。
     * </p>
     *
     * @param orderId 订单ID
     * @return 经典案例详情VO（包含订单明细、文件列表、可执行动作等完整信息）
     * @throws BusinessException 订单不存在或非经典案例时抛出 DATA_NOT_FOUND
     */
    @Override
    public ClassicCaseVO getClassicCaseDetail(Long orderId) {
        // 查询订单实体，校验存在性和经典案例标记
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null || StatusConstants.YES != order.getIsClassicCase()) {
            log.warn("查询经典案例详情失败，订单不存在或非经典案例: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 直接构建订单详情，不调用 orderMainService.getOrderDetail()，避免数据权限校验
        OrderDetailVO orderDetail = orderMainService.buildOrderDetailWithoutPermissionCheck(orderId, order);

        // 转换为 ClassicCaseVO，继承订单详情的所有字段并添加经典案例特有字段
        return ClassicCaseConvert.toClassicCaseVO(orderDetail, order);
    }

    /**
     * 判断订单是否为经典案例
     * <p>
     * 根据订单ID查询订单，检查 is_classic_case 字段是否为 1。
     * 订单不存在时返回 false。
     * </p>
     *
     * @param orderId 订单ID
     * @return true=是经典案例，false=不是经典案例或订单不存在
     */
    @Override
    public boolean isClassicCase(Long orderId) {
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        return order != null && StatusConstants.YES == order.getIsClassicCase();
    }

    /**
     * 取消经典案例标记（文件迁移失败时的补偿操作）
     * <p>
     * 当文件迁移失败时，需要回滚订单的经典案例标记，避免数据不一致。
     * 此方法将订单的 is_classic_case 重置为 0，并清空相关字段。
     * </p>
     *
     * @param orderId 订单ID
     * @param reason  取消原因（用于日志记录）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelClassicCaseMark(Long orderId, String reason) {
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null) {
            log.warn("取消经典案例标记失败，订单不存在: orderId={}", orderId);
            return;
        }

        // 只有已标记为经典案例的订单才需要取消
        if (StatusConstants.YES != order.getIsClassicCase()) {
            log.debug("订单未标记为经典案例，无需取消: orderId={}", orderId);
            return;
        }

        // 回滚经典案例标记
        order.setIsClassicCase(StatusConstants.NO);
        order.setClassicCaseTime(null);
        order.setClassicCaseBy(null);
        order.setClassicCaseRemark(null);
        orderMainMapper.updateById(order);

        log.warn("取消经典案例标记: orderId={}, orderCode={}, reason={}",
                orderId, order.getOrderCode(), reason);
    }
}
