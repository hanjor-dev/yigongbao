package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.AuditStatusConstants;
import com.yigongbao.common.event.AuditRejectedEvent;
import com.yigongbao.common.event.NotificationRemarkUpdateEvent;
import com.yigongbao.common.event.OrderCancelledEvent;
import com.yigongbao.common.event.OrderSubmittedEvent;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.RoleCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.constant.PhysicalDeliveryConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.validator.OrderDataScopeChecker;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderModificationLogEntity;
import com.yigongbao.module.order.entity.OrderModificationApplyEntity;
import com.yigongbao.module.order.enums.OrderDraftStatusEnum;
import com.yigongbao.module.order.enums.ApplyStatusEnum;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderModificationLogMapper;
import com.yigongbao.module.order.mapper.OrderModificationApplyMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.order.vo.order.OrderStatisticsVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 订单主表 Service 实现类
 * 处理订单相关的业务逻辑，包括订单CRUD、状态流转、审核流程等
 *
 * 【流程引擎调用说明】
 * 所有状态流转统一通过 FlowFacade 执行，包含以下操作：
 * - 查询当前可执行动作：{@link #listAvailableActions(Long)}
 * - 提交订单：SUBMIT_ORDER 动作
 * - 撤回订单：WITHDRAW 动作
 * - 审核通过：DATA_AUDIT_PASS 动作
 * - 审核驳回：DATA_AUDIT_REJECT 动作
 * - 创建记录：CREATE 动作（仅记录历史，不改变状态）
 *
 * 【needsPhysicalDelivery 变更规则】
 * - 仅在订单阶段（phase=10）允许修改
 * - 允许 0/2→1 的变更（非实体交付→需要实体交付）
 * - 不允许 1→0/2 的变更（需要实体交付→非实体交付）
 * 校验逻辑见 {@link #validateNeedsPhysicalDeliveryChange(OrderMainEntity, UpdateOrderDTO)}
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMainServiceImpl extends ServiceImpl<OrderMainMapper, OrderMainEntity> implements OrderMainService {

    private final OrderItemMapper orderItemMapper;
    private final OrderDraftMapper orderDraftMapper;
    private final OrderItemDraftMapper orderItemDraftMapper;
    private final OrderFileMapper orderFileMapper;
    private final OrderModificationLogMapper orderModificationLogMapper;
    private final OrderModificationApplyMapper orderModificationApplyMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final FileService fileService;
    private final OrgService orgService;
    private final FlowFacade flowFacade;
    private final ConfigService configService;
    private final UserService userService;
    private final UserHospitalService userHospitalService;
    private final OrderQueryHelper orderQueryHelper;
    private final ObjectMapper objectMapper;
    private final OrderDataValidator orderDataValidator;
    private final OrderDataScopeChecker orderDataScopeChecker;
    private final OrderModifyApplyService orderModifyApplyService;
    private final OrderCancelApplyService cancelApplyService;
    private final com.yigongbao.module.order.convert.OrderConvert orderConvert;
    private final ApplicationEventPublisher eventPublisher;

    /** 打破循环依赖：DesignerAssignmentServiceImpl 反向依赖 OrderMainService */
    @Lazy
    @Autowired
    private DesignerAssignmentService designerAssignmentService;

    /** 设计阶段阈值：订单阶段(phase=10)与设计阶段(phase=20)的分界点 */
    private static final int DESIGN_PHASE_THRESHOLD = 20;

    // ==================== 私有方法 ====================

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID，未登录返回 null
     */
    private Long getCurrentUserId() {
        return orderQueryHelper.getCurrentUserId();
    }

    /**
     * 获取当前登录用户的角色编码
     *
     * @return 角色编码（REGIONAL_ADMIN/DESIGN_ADMIN等），未找到返回 null
     */
    private String getCurrentUserRoleCode() {
        return orderQueryHelper.getCurrentUserRoleCode();
    }

    /**
     * 校验部门权限：区域管理员只能审核本部门的订单
     *
     * @param order 订单实体
     * @param currentUserDeptId 当前用户部门ID
     * @throws BusinessException 权限不足时抛出
     */
    private void validateDepartmentPermission(OrderMainEntity order, Long currentUserDeptId) {
        if (!Objects.equals(order.getOperatorDeptId(), currentUserDeptId)) {
            log.warn("无权审核非本部门订单: orderId={}, orderDeptId={}, userDeptId={}",
                order.getId(), order.getOperatorDeptId(), currentUserDeptId);
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
    }

    /**
     * 获取当前用户真实姓名
     *
     * @param userId 用户ID
     * @return 真实姓名，用户不存在返回 null
     */
    private String getUserRealName(Long userId) {
        UserEntity user = userService.getById(userId);
        return user != null ? user.getRealName() : null;
    }

    /**
     * 数据权限校验：当前用户是否有权访问该订单，无权则抛 ORDER_NOT_FOUND（不暴露存在性）
     */
    private void validateDataScope(Long orderId) {
        orderDataScopeChecker.checkOrderAccess(orderId);
    }

    /**
     * 校验订单是否为经典案例，如果是则抛出异常
     * <p>
     * 经典案例订单的所有数据（订单、设计、生产）和文件（影像、设计包、模型、报告等）
     * 均受保护，不允许进行修改、删除等操作。
     * </p>
     *
     * @param orderId 订单ID
     * @param operation 操作描述（用于日志和错误提示），如"上传设计数据包"、"删除STL模型"
     * @throws BusinessException 订单为经典案例时抛出 CLASSIC_CASE_PROTECTED 错误
     */
    @Override
    public void checkNotClassicCase(Long orderId, String operation) {
        // 查询订单实体
        OrderMainEntity order = getById(orderId);

        // 订单不存在时不做校验（后续业务逻辑会抛出 ORDER_NOT_FOUND）
        if (order == null) {
            return;
        }

        // 检查是否为经典案例（is_classic_case = 1）
        if (StatusConstants.YES == order.getIsClassicCase()) {
            log.warn("经典案例保护拦截: operation={}, orderId={}, orderCode={}",
                operation, orderId, order.getOrderCode());
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_PROTECTED);
        }

        // 校验通过，记录 DEBUG 日志
        log.debug("经典案例校验通过: operation={}, orderId={}, isClassicCase={}",
            operation, orderId, order.getIsClassicCase());
    }

    // ==================== 查询操作 ====================

    /**
     * 分页查询订单列表
     * 支持多维度筛选，查询结果受当前用户的数据权限（dataScopeType）控制：
     * - ALL：全部可见
     * - ORG：仅本机构
     * - HOSPITALS：仅关联医院
     * - SELF：仅自己创建的
     * phase/status 均为可选参数，不传则不限制阶段/状态。
     *
     * @param dto 查询参数
     * @return 分页后的订单列表
     */
    @Override
    public IPage<OrderListVO> listOrders(OrderPageDTO dto) {
        Long currentUserId = getCurrentUserId();
        // 获取当前用户的数据权限类型（从角色表读取）
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

            // phase/status 动态过滤（不传则不限制）
            wrapper.eq(Objects.nonNull(dto.getPhase()), OrderMainEntity::getPhase, dto.getPhase())
                   .eq(Objects.nonNull(dto.getStatus()), OrderMainEntity::getStatus, dto.getStatus());

            // 注入数据权限过滤条件
            orderQueryHelper.buildDataScopeCondition(wrapper, currentUserId, scopeType);

            // hospitalId 参数处理：HOSPITALS 类型需校验是否在权限范围内
            if (dto.getHospitalId() != null) {
                if (scopeType == DataScopeTypeEnum.HOSPITALS) {
                    List<Long> userHospitalIds = userHospitalService.getHospitalIdsByUserId(currentUserId);
                    if (!userHospitalIds.contains(dto.getHospitalId())) {
                        // 传入的 hospitalId 超出权限范围，返回空页，不报错
                        log.warn("传入的医院ID不在用户权限范围内，返回空页，userId={}, hospitalId={}", currentUserId, dto.getHospitalId());
                        Page<OrderListVO> emptyPage = new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
                        emptyPage.setRecords(new ArrayList<>());
                        return emptyPage;
                    }
                }
                wrapper.eq(OrderMainEntity::getHospitalId, dto.getHospitalId());
            }

            // 追加其他过滤条件
            // orderCode 参数：多字段模糊搜索（订单编号/机构名称/业务员姓名/医院名称/患者名字）
            if (StrUtil.isNotBlank(dto.getOrderCode())) {
                wrapper.and(w -> w.like(OrderMainEntity::getOrderCode, dto.getOrderCode())
                        .or().like(OrderMainEntity::getOrgName, dto.getOrderCode())
                        .or().like(OrderMainEntity::getOperatorName, dto.getOrderCode())
                        .or().like(OrderMainEntity::getHospitalName, dto.getOrderCode())
                        .or().like(OrderMainEntity::getPatientName, dto.getOrderCode()));
            }
            wrapper.eq(Objects.nonNull(dto.getAreaId()), OrderMainEntity::getAreaId, dto.getAreaId())
                    .like(StrUtil.isNotBlank(dto.getPatientName()), OrderMainEntity::getPatientName, dto.getPatientName())
                    .like(StrUtil.isNotBlank(dto.getDoctorName()), OrderMainEntity::getDoctorName, dto.getDoctorName())
                    .eq(StrUtil.isNotBlank(dto.getBusinessType()), OrderMainEntity::getBusinessType, dto.getBusinessType())
                    .eq(Objects.nonNull(dto.getOperatorId()), OrderMainEntity::getOperatorId, dto.getOperatorId())
                    .ge(Objects.nonNull(dto.getCreateTimeStart()), OrderMainEntity::getCreateTime, dto.getCreateTimeStart())
                    .lt(Objects.nonNull(dto.getCreateTimeEnd()), OrderMainEntity::getCreateTime, toExclusiveEndTime(dto.getCreateTimeEnd()));

            // bodyPartIds 过滤：先查 order_item 得到 orderIds，再用 MP in 条件（避免手写 SQL）
            if (dto.getBodyPartIds() != null && !dto.getBodyPartIds().isEmpty()) {
                List<Long> orderIdsByBodyPart = orderItemMapper.selectList(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderItemEntity>()
                                        .select("order_id")
                                        .in("body_part_id", dto.getBodyPartIds())
                                        .eq("is_deleted", 0))
                        .stream().map(OrderItemEntity::getOrderId).distinct().collect(Collectors.toList());
                if (orderIdsByBodyPart.isEmpty()) {
                    // 没有匹配明细，直接返回空页
                    Page<OrderListVO> emptyPage = new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
                    emptyPage.setRecords(new ArrayList<>());
                    return emptyPage;
                }
                wrapper.in(OrderMainEntity::getId, orderIdsByBodyPart);
            }

            // projectIds 过滤：同上，先查 order_item 得到 orderIds，再用 MP in 条件
            if (dto.getProjectIds() != null && !dto.getProjectIds().isEmpty()) {
                List<Long> orderIdsByProject = orderItemMapper.selectList(
                                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderItemEntity>()
                                        .select("order_id")
                                        .in("project_id", dto.getProjectIds())
                                        .eq("is_deleted", 0))
                        .stream().map(OrderItemEntity::getOrderId).distinct().collect(Collectors.toList());
                if (orderIdsByProject.isEmpty()) {
                    // 没有匹配明细，直接返回空页
                    Page<OrderListVO> emptyPage = new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
                    emptyPage.setRecords(new ArrayList<>());
                    return emptyPage;
                }
                wrapper.in(OrderMainEntity::getId, orderIdsByProject);
            }

            // 动态排序（白名单校验，防 SQL 注入）
            orderQueryHelper.applySort(wrapper, dto.getSortField(), dto.getSortOrder());

            // 执行分页查询
            Page<OrderMainEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            IPage<OrderMainEntity> pageResult = page(page, wrapper);

            // 转换为 VO（使用 OrderQueryHelper，含字段翻译）
            List<OrderListVO> voList = pageResult.getRecords().stream()
                    .map(orderQueryHelper::toOrderListVO)
                    .collect(Collectors.toList());

            // 批量填充重建项目列表（避免 N+1）
            orderQueryHelper.fillRebuildProjectList(voList);

            // 批量填充审核信息
            fillModifyAuditStatus(pageResult.getRecords(), voList);
            for (int i = 0; i < voList.size(); i++) {
                orderConvert.fillAuditInfo(pageResult.getRecords().get(i), voList.get(i));
            }

            // 构建返回页（复用分页元信息，替换 records）
            IPage<OrderListVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            ((Page<OrderListVO>) voPage).setRecords(voList);

            return voPage;
    }

    private LocalDateTime toExclusiveEndTime(LocalDateTime endTime) {
        return endTime == null ? null : endTime.toLocalDate().plusDays(1).atStartOfDay();
    }

    /**
     * 批量填充订单最新修改审核状态，避免逐条查询修改申请。
     *
     * @param entities 当前页订单实体
     * @param voList   当前页订单列表 VO
     */
    private void fillModifyAuditStatus(List<OrderMainEntity> entities, List<OrderListVO> voList) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        List<Long> orderIds = entities.stream()
                .map(OrderMainEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return;
        }

        List<OrderModificationApplyEntity> applies = orderModificationApplyMapper.selectList(
                new LambdaQueryWrapper<OrderModificationApplyEntity>()
                        .in(OrderModificationApplyEntity::getOrderId, orderIds)
                        .eq(OrderModificationApplyEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByDesc(OrderModificationApplyEntity::getApplyTime)
                        .orderByDesc(OrderModificationApplyEntity::getId));

        Map<Long, Integer> latestStatusMap = new HashMap<>();
        for (OrderModificationApplyEntity apply : applies) {
            latestStatusMap.putIfAbsent(apply.getOrderId(),
                    ApplyStatusEnum.APPROVED.getCode().equals(apply.getStatus()) ? 1 : 2);
        }

        for (int i = 0; i < entities.size(); i++) {
            voList.get(i).setModifyAuditStatus(latestStatusMap.getOrDefault(entities.get(i).getId(), 0));
        }
    }

    /**
     * 统计当前用户数据权限范围内的订单数量。
     */
    @Override
    public OrderStatisticsVO statistics() {
        Long currentUserId = getCurrentUserId();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

        OrderStatisticsVO statistics = new OrderStatisticsVO();
        statistics.setTotal(countOrders(currentUserId, scopeType, null));
        statistics.setPendingAudit(countOrders(currentUserId, scopeType,
                FlowStatusEnum.PENDING_DATA_AUDIT.getValue()));
        statistics.setDesigning(countOrders(currentUserId, scopeType,
                FlowStatusEnum.DESIGN_IN_PROGRESS.getValue()));
        return statistics;
    }

    private long countOrders(Long currentUserId, DataScopeTypeEnum scopeType, Integer status) {
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Objects.nonNull(status), OrderMainEntity::getStatus, status);
        orderQueryHelper.buildDataScopeCondition(wrapper, currentUserId, scopeType);
        return count(wrapper);
    }

    /**
     * 查询订单详情
     * 包含订单基本信息、明细列表、可执行动作列表。
     * 查询结果受当前用户数据权限控制：无权访问的订单返回 ORDER_NOT_FOUND（不暴露存在性）。
     *
     * @param id 订单ID
     * @return 订单详情 VO
     * @throws BusinessException 订单不存在或无权访问
     */
    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        // 根据ID查询订单实体，校验存在性
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 数据权限校验：防止横向越权
        validateDataScope(id);
        // 转换为详情 VO，补充性别名称等显示字段
        OrderDetailVO vo = toOrderDetailVO(entity);
        // 查询订单明细列表，按排序字段升序
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, id)
                        .eq(OrderItemEntity::getIsDeleted, 0)
                        .orderByAsc(OrderItemEntity::getSortOrder));
        vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
        vo.setItemCount(items.size());
        // 查询当前可执行的动作列表，用于前端按钮展示
        vo.setAvailableActions(flowFacade.getAvailableActions(entity.getId()));
        // 查询订单关联的影像文件列表
        fillOrderFiles(vo, id);
        // 填充审核信息
        orderConvert.fillAuditInfo(entity, vo);
        return vo;
    }

    /**
     * 构建订单详情（不进行数据权限校验）
     * <p>
     * 用于经典案例等公开场景，所有用户均可查看。
     * 直接构建 OrderDetailVO，跳过 validateDataScope() 校验。
     * </p>
     *
     * @param orderId 订单ID
     * @param order   订单实体（调用方已查询）
     * @return 订单详情 VO
     */
    @Override
    public OrderDetailVO buildOrderDetailWithoutPermissionCheck(Long orderId, OrderMainEntity order) {
        // 转换为详情 VO，补充性别名称等显示字段
        OrderDetailVO vo = toOrderDetailVO(order);
        // 查询订单明细列表，按排序字段升序
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
                        .eq(OrderItemEntity::getIsDeleted, 0)
                        .orderByAsc(OrderItemEntity::getSortOrder));
        vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
        vo.setItemCount(items.size());
        // 查询当前可执行的动作列表，用于前端按钮展示
        vo.setAvailableActions(flowFacade.getAvailableActions(order.getId()));
        // 查询订单关联的影像文件列表
        fillOrderFiles(vo, orderId);
        return vo;
    }

    /**
     * 查询订单可执行的动作列表
     *
     * @param id 订单ID
     * @return 可执行的动作编码列表
     * @throws BusinessException 订单不存在
     */
    @Override
    public List<String> listAvailableActions(Long id) {
        // 校验订单存在
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        validateDataScope(id);
        // 通过 FlowFacade 获取当前状态可执行的动作
        return flowFacade.getAvailableActions(id);
    }

    // ==================== 修改操作 ====================

    /**
     * 更新订单信息
     * 仅公司管理员或提交后10分钟内的提单人/区域管理员可修改
     *
     * 【needsPhysicalDelivery 变更规则】
     * - 仅在订单阶段（phase=10）允许修改
     * - 允许 0/2→1 的变更（非实体交付→需要实体交付）
     * - 不允许 1→0/2 的变更（需要实体交付→非实体交付）
     *
     * @param id 订单ID
     * @param dto 更新参数
     * @throws BusinessException 订单不存在、变更规则不满足
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, UpdateOrderDTO dto) {
        // 数据权限校验（含存在性校验）：无权访问时抛 ORDER_NOT_FOUND
        validateDataScope(id);
        OrderMainEntity entity = getById(id);
        // 经典案例保护校验
        checkNotClassicCase(id, "修改");
        // 校验 needsPhysicalDelivery 变更规则（不在订单阶段不允许修改，不允许从需要改为不需要）
        validateNeedsPhysicalDeliveryChange(entity, dto);
        // 排除不可变更字段后复制属性
        BeanUtils.copyProperties(dto, entity, "id", "orderCode", "phase", "status", "createTime", "updateTime", "createBy", "updateBy", "version");
        // hospitalId 变更时同步更新地区冗余字段
        if (dto.getHospitalId() != null) {
            Long currentUserId = getCurrentUserId();
            if (!userHospitalService.hasPermissionOnHospital(currentUserId, dto.getHospitalId())) {
                throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
            }
            fillAreaFromHospital(entity, dto.getHospitalId());
        }

        // 更新订单
        updateById(entity);
        log.info("更新订单: orderId={}", id);
    }

    /**
     * 校验是否需要实体交付的变更规则
     *
     * @param entity 订单实体（变更前的值）
     * @param dto 更新参数（变更后的值）
     * @throws BusinessException 违反变更规则
     */
    private void validateNeedsPhysicalDeliveryChange(OrderMainEntity entity, UpdateOrderDTO dto) {
        // 如果 DTO 中没有传入 needsPhysicalDelivery，跳过校验
        if (dto.getNeedsPhysicalDelivery() == null) {
            return;
        }
        Integer oldValue = entity.getNeedsPhysicalDelivery();
        Integer newValue = dto.getNeedsPhysicalDelivery();
        if (!PhysicalDeliveryConstants.isSupported(newValue)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_INVALID);
        }
        // 如果值未变化，跳过校验
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        // 仅在订单阶段允许修改
        if (!Objects.equals(entity.getPhase(), FlowPhaseEnum.ORDER.getValue())) {
            log.warn("needsPhysicalDelivery 仅在订单阶段允许修改，orderId={}, phase={}", entity.getId(), entity.getPhase());
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN);
        }
        // 不允许从需要实体交付改为任一非实体交付类型（1→0 或 1→2）
        if (PhysicalDeliveryConstants.needsProduction(oldValue)
                && PhysicalDeliveryConstants.isNoPhysicalDelivery(newValue)) {
            log.warn("需要实体交付的订单不允许修改为非实体交付类型，orderId={}, newValue={}", entity.getId(), newValue);
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN);
        }
        // 0/2→1 是允许的变更，不做额外处理
    }

    /**
     * 删除订单
     * 仅草稿状态的订单允许删除
     *
     * @param id 订单ID
     * @throws BusinessException 订单不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrder(Long id) {
        // 校验订单存在
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 经典案例保护校验
        checkNotClassicCase(id, "删除");
        // 数据权限校验：只有创建人可删除（草稿状态订单）
        Long currentUserId = getCurrentUserId();
        if (!Objects.equals(entity.getCreateBy(), currentUserId)) {
            log.warn("无权删除他人订单: orderId={}, createBy={}, currentUserId={}", id, entity.getCreateBy(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 只允许删除草稿状态（status=1010）的订单，正式提交后的订单不可删除
        if (!FlowStatusEnum.DRAFT.getValue().equals(entity.getStatus())) {
            log.warn("非草稿状态订单不允许删除: orderId={}, status={}", id, entity.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_CANNOT_DELETE);
        }
        // 删除订单主表（软删除）
        removeById(id);
        // 清理关联明细
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, id));
        // 清理关联文件记录（软删除）
        orderFileMapper.delete(new LambdaQueryWrapper<OrderFileEntity>().eq(OrderFileEntity::getOrderId, id));
        // 清理修改留痕日志（硬删除，不继承 BaseEntity）
        orderModificationLogMapper.delete(new LambdaQueryWrapper<OrderModificationLogEntity>().eq(OrderModificationLogEntity::getOrderId, id));
        // TODO: 流程历史记录清理需 FlowFacade 提供接口支持
        log.info("删除订单: orderId={}", id);
    }

    // ==================== 流程操作 ====================

    /**
     * 提交订单
     * 将草稿状态的订单正式提交，进入数据审核流程
     *
     * 【流转逻辑】
     * - 提交后 phase 推进到当前阶段，status 变为审核中状态
     * - 由 FlowFacade 执行 SUBMIT_ORDER 动作，返回流转结果
     *
     * @param id 订单ID
     * @throws BusinessException 订单不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long id) {
        Long currentUserId = getCurrentUserId();
        // 校验订单存在
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 数据权限校验：只有创建人可提交
        if (!Objects.equals(entity.getCreateBy(), currentUserId)) {
            log.warn("无权提交他人订单: orderId={}, createBy={}, currentUserId={}", id, entity.getCreateBy(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 通过 FlowFacade 执行提交动作，获取流转后的 phase 和 status
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.SUBMIT_ORDER, FlowOperator.of(currentUserId, null));
        // 更新订单的阶段和状态
        entity.setPhase(result.getTargetPhase());
        entity.setStatus(result.getFinalStatus());
        updateById(entity);
        eventPublisher.publishEvent(new OrderSubmittedEvent(this, entity.getId(), entity.getOrderCode(),
                entity.getBusinessType(), entity.getPatientName(), entity.getOrgName(), entity.getOperatorName(),
                entity.getHospitalId(), entity.getOrgId(), entity.getOperatorDeptId(), entity.getCreateBy()));
        log.info("提交订单: orderId={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
    }

    /**
     * 撤回订单
     * 将提交后的订单撤回至草稿状态，仅允许在审核前操作
     *
     * 【流转逻辑】
     * - 执行 WITHDRAW 动作，订单退回草稿状态
     *
     * @param id 订单ID
     * @throws BusinessException 订单不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawOrder(Long id) {
        Long currentUserId = getCurrentUserId();
        // 校验订单存在
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 数据权限校验：只有创建人可撤回
        if (!Objects.equals(entity.getCreateBy(), currentUserId)) {
            log.warn("无权撤回他人订单: orderId={}, createBy={}, currentUserId={}", id, entity.getCreateBy(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 通过 FlowFacade 执行撤回动作
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.WITHDRAW, FlowOperator.of(currentUserId, null));
        // 更新订单的阶段、状态和当前处理人
        entity.setPhase(result.getTargetPhase());
        entity.setStatus(result.getFinalStatus());
        entity.setCurrentHandlerId(currentUserId);
        updateById(entity);
        log.info("撤回订单: orderId={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
    }

    /**
     * 审核通过
     *
     * 【审核规则】
     * - 所有订单（含试用订单）：只有设计管理员可审核，直接调用 flowFacade 推进流程
     *
     * @param id 订单ID
     * @param dto 审核参数
     * @throws BusinessException 订单不存在、权限不足
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long id, AuditOrderDTO dto) {
        validateDataScope(id);
        Long currentUserId = getCurrentUserId();
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        if (orderModifyApplyService.hasPendingApply(id)) {
            log.warn("订单存在待审核的修改申请，不允许数据审核: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_PENDING);
        }

        if (cancelApplyService.hasPendingCancelApply(id)) {
            log.warn("订单存在待审核的取消申请，不允许数据审核: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        String roleCode = getCurrentUserRoleCode();

        // 所有订单统一：只有设计管理员可以审核（试用订单不再需要区域管理员审核）
        if (!RoleCodeConstants.DESIGN_ADMIN.equals(roleCode)) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        String operatorName = getUserRealName(currentUserId);
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.DATA_AUDIT_PASS, new FlowOperator(currentUserId, operatorName, dto.getRemark()),
                dto.getVersion());

        LambdaUpdateWrapper<OrderMainEntity> uw = new LambdaUpdateWrapper<>();
        uw.eq(OrderMainEntity::getId, id)
          .eq(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
          .set(OrderMainEntity::getPhase, result.getTargetPhase())
          .set(OrderMainEntity::getStatus, result.getFinalStatus())
          .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PASSED)
          .set(OrderMainEntity::getDesignAuditTime, LocalDateTime.now())
          .set(OrderMainEntity::getDesignAuditBy, currentUserId)
          .set(OrderMainEntity::getDesignAuditRemark, dto.getRemark())
          .set(OrderMainEntity::getCurrentHandlerId, currentUserId);
        if (dto.getEstimatedCost() != null) {
            uw.set(OrderMainEntity::getEstimatedCost, dto.getEstimatedCost());
        }
        if (StrUtil.isNotBlank(dto.getDataEvaluationOpinion())) {
            uw.set(OrderMainEntity::getDataEvaluationOpinion, dto.getDataEvaluationOpinion());
        }
        if (!update(uw)) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }

        designerAssignmentService.triggerAssignmentAfterAudit(id);
        eventPublisher.publishEvent(new NotificationRemarkUpdateEvent(
                this, "ORDER", id, "APPROVAL", "该订单已被" + operatorName + "审核通过"));
        log.info("订单审核通过: orderId={}, {} -> {}, designAuditBy={}",
            id, entity.getStatus(), result.getFinalStatus(), currentUserId);
    }

    /**
     * 审核驳回
     *
     * 【驳回规则】
     * - 必须填写驳回原因（remark）
     * - 所有订单（含试用订单）：只有设计管理员可驳回
     *
     * @param id 订单ID
     * @param dto 审核参数（含驳回原因remark）
     * @throws BusinessException 订单不存在、驳回原因未填写、权限不足
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long id, AuditOrderDTO dto) {
        validateDataScope(id);
        Long currentUserId = getCurrentUserId();
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (StrUtil.isBlank(dto.getRemark())) {
            log.warn("审核驳回时必须填写驳回原因");
            throw new BusinessException(ErrorCodeEnum.ORDER_AUDIT_REMARK_REQUIRED);
        }

        if (orderModifyApplyService.hasPendingApply(id)) {
            log.warn("订单存在待审核的修改申请，不允许数据审核: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_MODIFY_APPLY_PENDING);
        }

        if (cancelApplyService.hasPendingCancelApply(id)) {
            log.warn("订单存在待审核的取消申请，不允许数据审核: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        String roleCode = getCurrentUserRoleCode();

        // 所有订单统一：只有设计管理员可以驳回（试用订单不再需要区域管理员审核）
        if (!RoleCodeConstants.DESIGN_ADMIN.equals(roleCode)) {
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }

        String operatorName = getUserRealName(currentUserId);
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.DATA_AUDIT_REJECT, new FlowOperator(currentUserId, operatorName, dto.getRemark()),
                dto.getVersion());

        boolean updated = update(new LambdaUpdateWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getId, id)
                .eq(OrderMainEntity::getVersion, dto.getVersion())
                .eq(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
                .eq(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DATA_AUDIT.getValue())
                .set(OrderMainEntity::getPhase, result.getTargetPhase())
                .set(OrderMainEntity::getStatus, result.getFinalStatus())
                .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.REJECTED)
                .set(OrderMainEntity::getDesignAuditTime, LocalDateTime.now())
                .set(OrderMainEntity::getDesignAuditBy, currentUserId)
                .set(OrderMainEntity::getDesignAuditRemark, dto.getRemark())
                .set(OrderMainEntity::getAuditRemark, dto.getRemark())
                .set(OrderMainEntity::getCurrentHandlerId, currentUserId));

        if (!updated) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
        eventPublisher.publishEvent(new AuditRejectedEvent(this, id, entity.getOrderCode(),
                    entity.getPatientName(), entity.getHospitalName(), entity.getCreateBy(), dto.getRemark()));
        eventPublisher.publishEvent(new NotificationRemarkUpdateEvent(
                this, "ORDER", id, "APPROVAL", "该订单已被" + operatorName + "审核驳回"));
        log.warn("订单审核驳回: orderId={}, {} -> {}, designAuditBy={}, reason={}",
            id, entity.getStatus(), result.getFinalStatus(), currentUserId, dto.getRemark());
    }

    /**
     * 重新提交订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void     resubmit(Long id, Integer version) {
        Long currentUserId = getCurrentUserId();
        OrderMainEntity entity = getById(id);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(entity.getCreateBy(), currentUserId)) {
            log.warn("无权重新提交他人订单: orderId={}, createBy={}, currentUserId={}", id, entity.getCreateBy(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        String operatorName = getUserRealName(currentUserId);

        // 调用流程引擎流转状态：DATA_AUDIT_REJECTED → PENDING_DATA_AUDIT
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.RESUBMIT, new FlowOperator(currentUserId, operatorName, "重新提交"),
                version);

        // 所有订单统一：只重置设计审核字段（试用订单不再需要区域管理员审核）
        boolean updated = update(new LambdaUpdateWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getId, id)
                .eq(OrderMainEntity::getVersion, version)
                // 以流程状态作为重新提交前置条件，兼容历史上仅被区域管理员驳回的试用订单
                .eq(OrderMainEntity::getStatus, FlowStatusEnum.DATA_AUDIT_REJECTED.getValue())
                .set(OrderMainEntity::getDesignAuditStatus, AuditStatusConstants.PENDING)
                .set(OrderMainEntity::getDesignAuditTime, null)
                .set(OrderMainEntity::getDesignAuditBy, null)
                .set(OrderMainEntity::getDesignAuditRemark, null)
                .set(OrderMainEntity::getPhase, result.getTargetPhase())
                .set(OrderMainEntity::getStatus, result.getFinalStatus())
                .set(OrderMainEntity::getCurrentHandlerId, currentUserId));

        if (!updated) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
        log.info("重新提交订单: orderId={}, {} -> {}", id, entity.getStatus(), result.getFinalStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, Integer version) {
        validateDataScope(id);
        Long currentUserId = getCurrentUserId();
        // 校验订单存在
        OrderMainEntity order = getById(id);
        if (order == null) {
            log.warn("订单不存在: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 校验订单未取消
        if (order.getStatus().equals(FlowStatusEnum.CANCELLED.getValue())) {
            log.warn("订单已取消，不能重复取消: orderId={}", id);
            throw new BusinessException(ErrorCodeEnum.ORDER_ALREADY_CANCELLED);
        }

        // 检查阶段：订单阶段允许直接取消，设计阶段需提交申请
        if (order.getPhase() < DESIGN_PHASE_THRESHOLD) {
            // 订单阶段：直接取消
            directCancelOrder(id, order, currentUserId, version);
        } else {
            // 设计阶段：需提交取消申请
            log.warn("订单处于设计阶段，需提交取消申请: orderId={}, phase={}", id, order.getPhase());
            throw new BusinessException(ErrorCodeEnum.ORDER_NEED_CANCEL_APPLY);
        }
    }

    /**
     * 直接取消订单（仅订单阶段）
     *
     * @param id 订单ID
     * @param order 订单实体
     * @param currentUserId 当前用户ID
     * @param version 订单版本号（乐观锁）
     */
    private void directCancelOrder(Long id, OrderMainEntity order, Long currentUserId, Integer version) {
        // 获取当前用户姓名
        String operatorName = getUserRealName(currentUserId);
        // 通过 FlowFacade 执行取消动作
        TransitionResult result = flowFacade.executeFlow(
                id, FlowActionEnum.CANCEL, new FlowOperator(currentUserId, operatorName, null),
                version);
        // 使用乐观锁更新订单的阶段和状态
        boolean updated = update(new LambdaUpdateWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getId, id)
                .eq(OrderMainEntity::getVersion, version)
                .set(OrderMainEntity::getPhase, result.getTargetPhase())
                .set(OrderMainEntity::getStatus, result.getFinalStatus()));

        if (!updated) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
        eventPublisher.publishEvent(new OrderCancelledEvent(this, id));
        log.info("直接取消订单: orderId={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
    }

    /**
     * 手动完成订单（仅限不需要实体交付的订单）
     * 允许将设计完成的非实体交付订单直接标记为完成
     *
     * @param orderId 订单ID
     * @param version 订单版本号（乐观锁）
     * @throws BusinessException 订单不存在、状态错误或需要实体交付
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCompleteOrder(Long orderId, Integer version) {
        validateDataScope(orderId);
        Long currentUserId = getCurrentUserId();
        // 校验订单存在
        OrderMainEntity entity = getById(orderId);
        if (entity == null) {
            log.warn("订单不存在: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 校验必须是不需要实体交付的订单
        if (!PhysicalDeliveryConstants.isNoPhysicalDelivery(entity.getNeedsPhysicalDelivery())) {
            log.warn("订单需要实体交付，不允许手动完成: orderId={}, needsPhysicalDelivery={}",
                orderId, entity.getNeedsPhysicalDelivery());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_TRANSITION_ERROR);
        }
        // 校验状态必须为设计完成
        if (!Objects.equals(entity.getStatus(), FlowStatusEnum.DESIGN_COMPLETED.getValue())) {
            log.warn("订单状态不是设计完成，不允许手动完成: orderId={}, status={}",
                orderId, entity.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        // 获取当前用户姓名
        String operatorName = getUserRealName(currentUserId);
        // 通过 FlowFacade 执行完成动作
        TransitionResult result = flowFacade.executeFlow(
                orderId, FlowActionEnum.COMPLETE, new FlowOperator(currentUserId, operatorName, "手动完成"),
                version);
        // 使用乐观锁更新订单的阶段、状态和完成时间
        boolean updated = update(new LambdaUpdateWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getId, orderId)
                .eq(OrderMainEntity::getVersion, version)
                .eq(OrderMainEntity::getStatus, FlowStatusEnum.DESIGN_COMPLETED.getValue())
                .set(OrderMainEntity::getPhase, result.getTargetPhase())
                .set(OrderMainEntity::getStatus, result.getFinalStatus())
                .set(OrderMainEntity::getActualCompleteTime, LocalDateTime.now()));

        if (!updated) {
            throw new BusinessException(ErrorCodeEnum.ORDER_VERSION_CONFLICT);
        }
        log.info("手动完成订单: orderId={}, {} -> {}, operator={}",
            orderId, FlowStatusEnum.DESIGN_COMPLETED.getValue(), result.getFinalStatus(), currentUserId);
    }

    // ==================== 创建操作 ====================

    /**
     * 从草稿创建正式订单
     * 将草稿数据复制为正式订单，状态置为待数据审核
     *
     * 【执行步骤】
     * 1. 生成订单编号
     * 2. 从草稿复制主表数据，设置 phase=ORDER, status=PENDING_DATA_AUDIT
     * 3. 批量复制草稿明细到订单明细
     * 4. 复制文件关联关系
     * 5. 通过 FlowFacade 记录 CREATE 动作（仅记录历史，不改变状态）
     *
     * @param draft 草稿实体
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromDraft(OrderDraftEntity draft) {
        log.info("从草稿创建订单: draftId={}", draft.getId());
        try {
            // Step 1：生成订单编号
            String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);

            // Step 2：构建订单主表，从草稿复制字段，排除不可复用字段
            OrderMainEntity order = new OrderMainEntity();
            BeanUtils.copyProperties(draft, order, "id", "expiresAt", "status");
            order.setOrderCode(orderCode);
            order.setPhase(FlowPhaseEnum.ORDER.getValue());
            order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
            order.setVersion(0);
            // 所有类型订单统一初始化设计审核状态，试用订单不再经过区域管理员审核
            order.setDesignAuditStatus(AuditStatusConstants.PENDING);
            // 从医院表补充地区冗余字段（草稿中已复制 hospitalId，此处补充 area 字段）
            fillAreaFromHospital(order, order.getHospitalId());

            // 提单人部门信息冗余写入（草稿提交时从提单人账号读取，创建后固化）
            String operatorName = null;
            if (draft.getOperatorId() == null) {
                log.warn("草稿无 operatorId，无法校验订单所属机构，draftId={}", draft.getId());
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            UserEntity user = userService.getById(draft.getOperatorId());
            if (user == null) {
                log.warn("草稿提交时提单人账号不存在，draftId={}, operatorId={}",
                        draft.getId(), draft.getOperatorId());
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }
            if (!Objects.equals(order.getOrgId(), user.getOrgId())) {
                log.warn("草稿订单机构与提单人所属机构不一致: draftId={}, operatorId={}, draftOrgId={}, userOrgId={}",
                        draft.getId(), draft.getOperatorId(), order.getOrgId(), user.getOrgId());
                throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
            }
            if (order.getHospitalId() != null
                    && !userHospitalService.hasPermissionOnHospital(draft.getOperatorId(), order.getHospitalId())) {
                log.warn("草稿医院不在提单人当前可选范围内: draftId={}, operatorId={}, hospitalId={}",
                        draft.getId(), draft.getOperatorId(), order.getHospitalId());
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_SCOPE_DENIED);
            }
            operatorName = user.getRealName();
            order.setOperatorDeptId(user.getDeptId());
            order.setOperatorDeptName(user.getDeptName());

            // 校验订单类型与机构资质是否匹配（与直提流程保持一致）
            orderDataValidator.validateOrderType(draft.getOperatorId(), draft.getOrderType());

            save(order);
            Long orderId = order.getId();
            log.info("创建订单主表，orderId={}, orderCode={}", orderId, orderCode);

            // Step 3：查询草稿明细并批量复制到订单明细
            List<OrderItemDraftEntity> draftItems = orderItemDraftMapper.selectList(
                    new LambdaQueryWrapper<OrderItemDraftEntity>()
                            .eq(OrderItemDraftEntity::getDraftId, draft.getId())
                            .eq(OrderItemDraftEntity::getIsDeleted, 0)
                            .orderByAsc(OrderItemDraftEntity::getSortOrder));
            for (OrderItemDraftEntity draftItem : draftItems) {
                OrderItemEntity item = new OrderItemEntity();
                BeanUtils.copyProperties(draftItem, item, "id", "draftId");
                item.setOrderId(orderId);
                item.setOrderCode(orderCode);
                orderItemMapper.insert(item);
            }

            // Step 4：复制文件关联关系（从草稿关联迁移至订单关联）
            List<FileVO> draftImageData = fileService.listByBiz(FileBizTypeEnum.IMAGE_DATA.getDictCode(), draft.getId());
            List<FileVO> draftImageReport = fileService.listByBiz(FileBizTypeEnum.IMAGE_REPORT.getDictCode(), draft.getId());
            List<FileVO> draftApproval = fileService.listByBiz(FileBizTypeEnum.APPROVAL_FILE.getDictCode(), draft.getId());
            List<FileVO> draftFiles = new java.util.ArrayList<>(draftImageData);
            draftFiles.addAll(draftImageReport);
            draftFiles.addAll(draftApproval);
            for (FileVO file : draftFiles) {
                OrderFileEntity orderFile = new OrderFileEntity();
                orderFile.setOrderId(orderId);
                orderFile.setOrderCode(orderCode);
                orderFile.setFileId(file.getId());
                orderFile.setFileCategory(file.getBizType());
                orderFileMapper.insert(orderFile);
            }
            log.info("复制文件关联，orderId={}, fileCount={}", orderId, draftFiles.size());

            // Step 5：记录状态历史（CREATE 动作仅记录历史，不改变 phase/status）
            flowFacade.executeFlow(orderId, FlowActionEnum.CREATE, new FlowOperator(draft.getOperatorId(), operatorName, "从草稿创建"));
            log.info("从草稿创建订单: orderId={}, orderCode={}, itemCount={}", orderId, orderCode, draftItems.size());
            return orderId;
        } catch (Exception e) {
            log.error("从草稿创建订单异常: draftId={}", draft.getId(), e);
            throw e;
        }
    }

    /**
     * 直接创建正式订单（直提流程，不经过草稿）
     * 业务员直接填写完整信息后提交订单，跳过草稿保存步骤
     *
     * 【执行步骤】
     * 1. 生成订单编号
     * 2. 校验影像文件（根据系统配置判断是否必须上传）
     * 3. 构建订单主表，通过 OrderDataValidator 校验并覆盖所有关联名称字段
     *    - orgName: 从机构表查询覆盖
     *    - operatorId/operatorName/operatorPhone: 从当前登录用户填充，不信任前端传入值
     *    - hospitalName/areaId/areaName/fullAreaName: 从医院表查询覆盖
     *    - deptName: 科室字段已清理
     *    - doctorId/doctorName/doctorPhone: 已有 doctorId 则查询覆盖；仅有 doctorName 则 quickAdd 后填充
     *    - currentHandlerId/currentHandlerName: 设置为当前登录用户
     * 4. 保存重建项目明细，通过 OrderDataValidator 校验并覆盖 bodyPartName/projectName 等
     * 5. 通过 FlowFacade 记录 CREATE 动作（仅记录历史，不改变状态）
     *
     * @param dto 创建订单参数
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        // Step 0：如果传入了草稿ID，原子性更新草稿状态（防止并发重复提交）
        if (dto.getId() != null) {
            lockDraftForSubmission(dto.getId(), currentUserId);
        }

        // Step 1：生成订单编号
        String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);

        // Step 2：校验影像文件（根据系统配置判断是否必须上传）
        validateOrderFiles(dto);

        // Step 3：构建订单主表
        OrderMainEntity order = new OrderMainEntity();
        BeanUtils.copyProperties(dto, order, "id");
        order.setOrderCode(orderCode);
        order.setPhase(FlowPhaseEnum.ORDER.getValue());
        order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
        order.setVersion(0);
        // 所有类型订单统一初始化设计审核状态（试用订单不再需要区域管理员审核）
        order.setDesignAuditStatus(AuditStatusConstants.PENDING);

        // 操作员信息强制从当前登录用户填充，不信任前端传入值
        UserEntity currentUser = userService.getById(currentUserId);
        if (currentUser == null) {
            log.warn("当前登录用户不存在: userId={}", currentUserId);
            throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        }
        // 订单归属机构必须与当前登录用户一致，避免通过前端参数跨机构创建订单
        if (!Objects.equals(dto.getOrgId(), currentUser.getOrgId())) {
            log.warn("创建订单机构与当前用户所属机构不一致: userId={}, requestOrgId={}, userOrgId={}",
                    currentUserId, dto.getOrgId(), currentUser.getOrgId());
            throw new BusinessException(ErrorCodeEnum.PERMISSION_DENIED);
        }
        order.setOperatorId(currentUserId);
        order.setOperatorName(currentUser.getRealName());
        order.setOperatorPhone(currentUser.getPhone());
        // 创单时当前处理人即为提单人
        order.setCurrentHandlerId(currentUserId);
        order.setCurrentHandlerName(currentUser.getRealName());

        // 提单人部门信息冗余写入（创建时固化，后续不可修改）
        order.setOperatorDeptId(currentUser.getDeptId());
        order.setOperatorDeptName(currentUser.getDeptName());

        // 校验关联数据并覆盖所有冗余名称字段（orgName/hospitalName/area/doctorId+Name+Phone）
        orderDataValidator.validateAndFillMasterForOrder(
                order,
                dto.getOrgId(), dto.getHospitalId(), dto.getHospitalDeptId(),
                dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone(),
                currentUserId, OrderDataValidator.ValidateMode.DIRECT);
        // 校验订单类型与机构资质是否匹配
        orderDataValidator.validateOrderType(currentUserId, dto.getOrderType());
        // 校验业务类型限制及试用订单审批文件
        orderDataValidator.validateBusinessTypeRestrictions(currentUserId, dto.getBusinessType(), dto.getApprovalFileIds());

        save(order);
        Long orderId = order.getId();

        // Step 4：保存重建项目列表，校验并覆盖 bodyPartName/projectName/estimatedHours/projectDesc
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            // 校验重建项目去重：同一订单中不允许出现相同的（部位+项目）组合
            validateDuplicateItems(dto.getItems());
            List<OrderItemEntity> items = new ArrayList<>();
            for (int i = 0; i < dto.getItems().size(); i++) {
                var itemDTO = dto.getItems().get(i);
                OrderItemEntity item = new OrderItemEntity();
                item.setOrderId(orderId);
                item.setOrderCode(orderCode);
                item.setBodyPartId(itemDTO.getBodyPartId());
                item.setProjectId(itemDTO.getProjectId());
                item.setFormingRequirement(itemDTO.getFormingRequirement());
                item.setOtherRequirement(itemDTO.getOtherRequirement());
                item.setSortOrder(itemDTO.getSortOrder() != null ? itemDTO.getSortOrder() : i + 1);
                items.add(item);
            }
            // 通过校验器覆盖 bodyPartName/projectName/projectEstimatedHours/projectDesc
            orderDataValidator.validateAndFillItemsForOrder(items, OrderDataValidator.ValidateMode.DIRECT);
            for (OrderItemEntity item : items) {
                orderItemMapper.insert(item);
            }
        }

        // Step 5：保存影像文件关联
        saveOrderFiles(orderId, orderCode, dto.getImageDataFileIds(), FileBizTypeEnum.IMAGE_DATA.getDictCode());
        saveOrderFiles(orderId, orderCode, dto.getImageReportFileIds(), FileBizTypeEnum.IMAGE_REPORT.getDictCode());
        saveOrderFiles(orderId, orderCode, dto.getApprovalFileIds(), FileBizTypeEnum.APPROVAL_FILE.getDictCode());

        // Step 6：记录状态历史（CREATE 动作仅记录历史，不改变 phase/status）
        flowFacade.executeFlow(orderId, FlowActionEnum.CREATE,
                new FlowOperator(currentUserId, currentUser.getRealName(), "直提创建"));

        // Step 7：发布订单提交事件（触发消息通知）
        eventPublisher.publishEvent(new OrderSubmittedEvent(this, orderId, order.getOrderCode(),
                order.getBusinessType(), order.getPatientName(), order.getOrgName(), order.getOperatorName(),
                order.getHospitalId(), order.getOrgId(), order.getOperatorDeptId(), currentUserId));

        log.info("创建订单: orderId={}, orderCode={}, userId={}, itemCount={}",
                orderId, orderCode, currentUserId, dto.getItems() != null ? dto.getItems().size() : 0);
        return orderId;
    }

    // ==================== 私有方法 ====================

    /**
     * 从医院表读取地区信息，填充到订单实体的地区冗余字段
     * 创建订单和更新订单时调用，确保 area_id/area_name/full_area_name 与医院保持一致
     *
     * @param order      订单实体
     * @param hospitalId 医院ID，为 null 时跳过
     */
    private void fillAreaFromHospital(OrderMainEntity order, Long hospitalId) {
        if (hospitalId == null) {
            return;
        }
        OrgEntity org = orgService.getById(hospitalId);
        if (org != null) {
            order.setAreaId(org.getAreaId());
            order.setAreaName(org.getAreaName());
        }
    }

    /**
     * 校验直提订单的影像文件
     * 类型和大小在上传时已由 FileService（Provider 机制）校验，此处只做：
     * 1. 必填性校验（受 order.image.required 配置控制）
     * 2. 文件 ID 存在性校验
     */
    private void validateOrderFiles(CreateOrderDTO dto) {
        String imageRequired = configService.getConfigValue(SystemConfigKeyEnum.ORDER_IMAGE_REQUIRED.getKey());
        boolean required = "true".equalsIgnoreCase(imageRequired);

        // ---- 影像数据包 ----
        boolean hasImageData = dto.getImageDataFileIds() != null && !dto.getImageDataFileIds().isEmpty();
        if (required && !hasImageData) {
            log.warn("直提创建订单缺少影像数据，配置要求必须上传");
            throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "影像数据");
        }
        if (hasImageData) {
            // 只做存在性校验，类型/大小已在上传时由 FileService（Provider）校验
            assertFilesExist(fileService.listByIds(dto.getImageDataFileIds()), dto.getImageDataFileIds(), "影像数据包");
        }

        // ---- 免费业务审批文件（测试/试用业务类型必填）----
        boolean isTrialOrTest = DictCodeConstants.ORDER_BUSINESS_TYPE_TEST.equals(dto.getBusinessType())
                || DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL.equals(dto.getBusinessType());
        boolean hasApproval = dto.getApprovalFileIds() != null && !dto.getApprovalFileIds().isEmpty();
        if (isTrialOrTest && !hasApproval) {
            log.warn("直提创建订单缺少免费业务审批文件: businessType={}", dto.getBusinessType());
            throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "免费业务审批文件");
        }
        if (hasApproval) {
            assertFilesExist(fileService.listByIds(dto.getApprovalFileIds()), dto.getApprovalFileIds(), "免费业务审批文件");
        }
    }

    /**
     * 校验文件列表中每个 fileId 都存在于查询结果中，否则抛出 ATTACHMENT_NOT_FOUND
     */
    private void assertFilesExist(List<FileVO> found, List<String> fileIds, String categoryName) {
        Set<String> foundIds = found.stream().map(FileVO::getId).collect(Collectors.toSet());
        for (String fileId : fileIds) {
            if (!foundIds.contains(fileId)) {
                log.warn("{} 文件不存在: fileId={}", categoryName, fileId);
                throw new BusinessException(ErrorCodeEnum.ATTACHMENT_NOT_FOUND);
            }
        }
    }

    /**
     * 保存订单影像文件关联
     *
     * @param orderId 订单ID
     * @param orderCode 订单编号
     * @param fileIds 文件ID列表
     * @param fileCategory 文件类别（字典 dict_code）
     */
    private void saveOrderFiles(Long orderId, String orderCode, List<String> fileIds, String fileCategory) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            OrderFileEntity orderFile = new OrderFileEntity();
            orderFile.setOrderId(orderId);
            orderFile.setOrderCode(orderCode);
            orderFile.setFileId(fileId);
            orderFile.setFileCategory(fileCategory);
            orderFileMapper.insert(orderFile);
        }
    }

    /**
     * 填充订单文件列表
     * 根据订单ID查询 order_file 表关联文件，再通过 fileService 查询文件详情，分类填入 VO
     *
     * @param vo 订单详情 VO
     * @param orderId 订单ID
     */
    private void fillOrderFiles(OrderDetailVO vo, Long orderId) {
        // 查询订单关联的所有文件记录
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
                        .eq(OrderFileEntity::getIsDeleted, 0));
        if (orderFiles.isEmpty()) {
            return;
        }
        // 批量查询文件详情（避免 N+1）
        List<String> fileIds = orderFiles.stream().map(OrderFileEntity::getFileId).collect(Collectors.toList());
        Map<String, FileVO> fileMap = fileService.listByIds(fileIds).stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f));
        // 按文件类别分组
        List<OrderDetailVO.OrderFileVO> imageDataFiles = new java.util.ArrayList<>();
        List<OrderDetailVO.OrderFileVO> imageReportFiles = new java.util.ArrayList<>();
        List<OrderDetailVO.OrderFileVO> approvalFiles = new java.util.ArrayList<>();
        for (OrderFileEntity orderFile : orderFiles) {
            FileVO fileVO = fileMap.get(orderFile.getFileId());
            if (fileVO == null) {
                continue;
            }
            OrderDetailVO.OrderFileVO file = toOrderFileVO(orderFile, fileVO);
            if (FileBizTypeEnum.IMAGE_DATA.getDictCode().equals(orderFile.getFileCategory())) {
                imageDataFiles.add(file);
            } else if (FileBizTypeEnum.IMAGE_REPORT.getDictCode().equals(orderFile.getFileCategory())) {
                imageReportFiles.add(file);
            } else if (FileBizTypeEnum.APPROVAL_FILE.getDictCode().equals(orderFile.getFileCategory())) {
                approvalFiles.add(file);
            }
        }
        vo.setImageDataFiles(imageDataFiles);
        vo.setImageReportFiles(imageReportFiles);
        vo.setApprovalFiles(approvalFiles);
    }

    /**
     * 订单文件关联 + 文件详情转换为 VO
     *
     * @param orderFile 订单文件关联实体
     * @param fileVO 文件详情 VO
     * @return 订单文件 VO
     */
    private OrderDetailVO.OrderFileVO toOrderFileVO(OrderFileEntity orderFile, FileVO fileVO) {
        OrderDetailVO.OrderFileVO vo = new OrderDetailVO.OrderFileVO();
        vo.setFileId(orderFile.getFileId());
        vo.setFileName(fileVO.getFileName());
        vo.setFileCategory(orderFile.getFileCategory());
        vo.setFileCategoryName(getFileCategoryName(orderFile.getFileCategory()));
        vo.setFileUrl(fileVO.getFileUrl());
        vo.setThUrl(fileVO.getThUrl());
        vo.setFileSize(fileVO.getFileSize());
        vo.setFileSizeText(fileVO.getFileSizeText());
        vo.setFileExt(fileVO.getFileExt());
        return vo;
    }

    /**
     * 获取文件类别名称
     *
     * @param fileCategory 文件类别（字典 dict_code）
     * @return 文件类别名称
     */
    private String getFileCategoryName(String fileCategory) {
        if (StrUtil.isBlank(fileCategory)) {
            return null;
        }
        FileBizTypeEnum fileBizType = FileBizTypeEnum.getByDictCode(fileCategory);
        return fileBizType != null ? fileBizType.getName() : null;
    }

    /**
     * 实体转换为订单详情 VO
     *
     * @param entity 订单实体
     * @return 订单详情 VO
     */
    private OrderDetailVO toOrderDetailVO(OrderMainEntity entity) {
        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(entity, vo);
        // 补充展示名称字段（通过 OrderQueryHelper 统一翻译，与列表 VO 保持一致）
        orderQueryHelper.fillDisplayNames(entity, vo);
        return vo;
    }

    /**
     * 实体转换为订单明细 VO
     *
     * @param entity 订单明细实体
     * @return 订单明细 VO
     */
    private OrderDetailVO.OrderItemVO toOrderItemVO(OrderItemEntity entity) {
        OrderDetailVO.OrderItemVO vo = new OrderDetailVO.OrderItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    // ==================== 列配置 ====================

    /**
     * 获取当前用户的列配置（个人配置 > 系统默认）
     *
     * @return 列配置 VO，均未配置时返回 null
     */
    @Override
    public OrderColumnConfigVO getColumnConfig() {
        return orderQueryHelper.getColumnConfig();
    }

    /**
     * 保存当前用户的列配置
     * 将配置序列化为 JSON，存入 sys_user.column_settings 字段
     *
     * @param config 列配置 VO
     * @throws BusinessException 序列化失败时抛出
     */
    @Override
    public void saveColumnConfig(OrderColumnConfigVO config) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        try {
            // 序列化为 JSON 字符串
            String json = objectMapper.writeValueAsString(config);
            // 更新 UserEntity.orderColumnSettings 字段
            UserEntity user = new UserEntity();
            user.setId(currentUserId);
            user.setOrderColumnSettings(json);
            userService.updateById(user);
        } catch (JsonProcessingException e) {
            log.error("序列化列配置失败: userId={}", currentUserId, e);
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER);
        }
    }

    /**
     * 重置当前用户列配置（删除个人配置，恢复系统默认）
     */
    @Override
    public void resetColumnConfig() {
        Long currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        UserEntity user = new UserEntity();
        user.setId(currentUserId);
        user.setOrderColumnSettings(null);
        userService.updateById(user);
    }

    /**
     * 原子性锁定草稿用于提交（防止并发重复提交）
     *
     * @param draftId 草稿ID
     * @param currentUserId 当前用户ID
     * @throws BusinessException 草稿不存在/不属于当前用户/已提交
     */
    private void lockDraftForSubmission(Long draftId, Long currentUserId) {
        LambdaUpdateWrapper<OrderDraftEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(OrderDraftEntity::getId, draftId)
                .eq(OrderDraftEntity::getOperatorId, currentUserId)
                .ne(OrderDraftEntity::getStatus, OrderDraftStatusEnum.SUBMITTED.getCode())
                .set(OrderDraftEntity::getStatus, OrderDraftStatusEnum.SUBMITTED.getCode());
        int updated = orderDraftMapper.update(null, updateWrapper);
        if (updated == 0) {
            log.warn("草稿锁定失败: draftId={}, userId={}", draftId, currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
        }
    }

    /**
     * 校验重建项目去重：同一订单中不允许出现相同的（部位+项目）组合
     *
     * @param items 重建项目列表
     * @throws BusinessException 存在重复项目时抛出
     */
    private void validateDuplicateItems(List<com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO> items) {
        Set<Integer> seen = new HashSet<>();
        for (var item : items) {
            int key = Objects.hash(item.getBodyPartId(), item.getProjectId());
            if (!seen.add(key)) {
                log.warn("订单中存在重复的重建项目: bodyPartId={}, projectId={}", item.getBodyPartId(), item.getProjectId());
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "同一订单中不允许重复添加相同的部位和项目组合");
            }
        }
    }
}
