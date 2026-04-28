package com.yigongbao.module.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.CodeRuleConstants;
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
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.OrderPageDTO;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderModifyApplyService;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
 * - 仅允许 0→1 的变更（不需要→需要实体交付）
 * - 不允许 1→0 的变更（需要→不需要实体交付）
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
    private final OrderModifyApplyService orderModifyApplyService;

    /** 打破循环依赖：DesignerAssignmentServiceImpl 反向依赖 OrderMainService */
    @Lazy
    @Autowired
    private DesignerAssignmentService designerAssignmentService;

    // ==================== 私有方法 ====================

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID，未登录返回 null
     */
    private Long getCurrentUserId() {
        return orderQueryHelper.getCurrentUserId();
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
        log.info("分页查询订单列表，pageNum={}, pageSize={}, orderCode={}, hospitalId={}, phase={}, status={}",
                dto.getPageNum(), dto.getPageSize(), dto.getOrderCode(), dto.getHospitalId(), dto.getPhase(), dto.getStatus());
        try {
            Long currentUserId = getCurrentUserId();
            // 获取当前用户的数据权限类型（从角色表读取）
            DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
            log.info("当前用户数据权限，userId={}, scopeType={}", currentUserId, scopeType);

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
            wrapper.like(StrUtil.isNotBlank(dto.getOrderCode()), OrderMainEntity::getOrderCode, dto.getOrderCode())
                    .eq(Objects.nonNull(dto.getAreaId()), OrderMainEntity::getAreaId, dto.getAreaId())
                    .like(StrUtil.isNotBlank(dto.getDoctorName()), OrderMainEntity::getDoctorName, dto.getDoctorName())
                    .like(StrUtil.isNotBlank(dto.getPatientName()), OrderMainEntity::getPatientName, dto.getPatientName())
                    .eq(StrUtil.isNotBlank(dto.getBusinessType()), OrderMainEntity::getBusinessType, dto.getBusinessType())
                    .eq(Objects.nonNull(dto.getOperatorId()), OrderMainEntity::getOperatorId, dto.getOperatorId())
                    .ge(Objects.nonNull(dto.getCreateTimeStart()), OrderMainEntity::getCreateTime, dto.getCreateTimeStart())
                    .le(Objects.nonNull(dto.getCreateTimeEnd()), OrderMainEntity::getCreateTime, dto.getCreateTimeEnd());

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
            // 批量填充修改申请角标（避免 N+1）
            orderModifyApplyService.fillModifyApplyStatus(voList);

            // 构建返回页（复用分页元信息，替换 records）
            IPage<OrderListVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            ((Page<OrderListVO>) voPage).setRecords(voList);

            log.info("分页查询订单列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询订单列表异常", e);
            throw e;
        }
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
        log.info("查询订单详情，id={}", id);
        try {
            // 根据ID查询订单实体，校验存在性
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 数据权限校验：复用 buildDataScopeCondition 在同一 COUNT 查询中校验当前用户是否有权访问该订单
            // 防止横向越权（A 用户访问 B 用户权限范围外的订单）
            Long currentUserId = getCurrentUserId();
            DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
            LambdaQueryWrapper<OrderMainEntity> scopeWrapper = new LambdaQueryWrapper<>();
            scopeWrapper.eq(OrderMainEntity::getId, id);
            orderQueryHelper.buildDataScopeCondition(scopeWrapper, currentUserId, scopeType);
            if (count(scopeWrapper) == 0) {
                log.warn("订单不在当前用户数据权限范围内，id={}, userId={}, scopeType={}", id, currentUserId, scopeType);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
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
            log.info("查询订单详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询订单详情异常，id={}", id, e);
            throw e;
        }
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
        log.info("查询订单可执行动作，id={}", id);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 通过 FlowFacade 获取当前状态可执行的动作
            List<String> actions = flowFacade.getAvailableActions(id);
            log.info("查询订单可执行动作成功，id={}, actions={}", id, actions);
            return actions;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询订单可执行动作异常，id={}", id, e);
            throw e;
        }
    }

    // ==================== 修改操作 ====================

    /**
     * 更新订单信息
     * 仅公司管理员或提交后10分钟内的提单人/区域管理员可修改
     *
     * 【needsPhysicalDelivery 变更规则】
     * - 仅在订单阶段（phase=10）允许修改
     * - 仅允许 0→1 的变更（不需要→需要实体交付）
     * - 不允许 1→0 的变更（需要→不需要实体交付）
     *
     * @param id 订单ID
     * @param dto 更新参数
     * @throws BusinessException 订单不存在、变更规则不满足
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, UpdateOrderDTO dto) {
        log.info("更新订单，id={}", id);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 校验 needsPhysicalDelivery 变更规则（不在订单阶段不允许修改，不允许从需要改为不需要）
            validateNeedsPhysicalDeliveryChange(entity, dto);
            // 排除不可变更字段后复制属性
            BeanUtils.copyProperties(dto, entity, "id", "orderCode", "phase", "status", "createTime", "updateTime", "createBy", "updateBy", "version");
            // hospitalId 变更时同步更新地区冗余字段
            if (dto.getHospitalId() != null) {
                fillAreaFromHospital(entity, dto.getHospitalId());
            }
            // 更新订单
            updateById(entity);
            log.info("更新订单成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单异常，id={}", id, e);
            throw e;
        }
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
        // 如果值未变化，跳过校验
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        // 仅在订单阶段允许修改
        if (!Objects.equals(entity.getPhase(), FlowPhaseEnum.ORDER.getValue())) {
            log.warn("needsPhysicalDelivery 仅在订单阶段允许修改，orderId={}, phase={}", entity.getId(), entity.getPhase());
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN);
        }
        // 不允许 1→0 的变更（需要→不需要实体交付）
        if (Objects.equals(oldValue, 1) && Objects.equals(newValue, 0)) {
            log.warn("需要实体交付的订单不允许修改为不需要实体交付，orderId={}", entity.getId());
            throw new BusinessException(ErrorCodeEnum.ORDER_NEEDS_PHYSICAL_DELIVERY_CHANGE_FORBIDDEN);
        }
        // 0→1 是允许的变更，不做额外处理
        log.info("needsPhysicalDelivery 变更校验通过，orderId={}, oldValue={}, newValue={}", entity.getId(), oldValue, newValue);
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
        log.info("删除订单，id={}", id);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 只允许删除草稿状态（status=1010）的订单，正式提交后的订单不可删除
            if (!FlowStatusEnum.DRAFT.getValue().equals(entity.getStatus())) {
                log.warn("非草稿状态订单不允许删除，id={}, status={}", id, entity.getStatus());
                throw new BusinessException(ErrorCodeEnum.ORDER_CANNOT_DELETE);
            }
            // 删除订单主表（软删除）
            removeById(id);
            // 清理关联明细
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, id));
            // 清理关联文件记录（软删除）
            orderFileMapper.delete(new LambdaQueryWrapper<OrderFileEntity>().eq(OrderFileEntity::getOrderId, id));
            log.info("删除订单成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除订单异常，id={}", id, e);
            throw e;
        }
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
        log.info("提交订单，id={}, currentUserId={}", id, currentUserId);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 校验无阻断性修改申请
            orderModifyApplyService.validateNoBlockingModifyApply(id);
            // 通过 FlowFacade 执行提交动作，获取流转后的 phase 和 status
            TransitionResult result = flowFacade.executeFlow(
                    id, FlowActionEnum.SUBMIT_ORDER, FlowOperator.of(currentUserId, null));
            // 更新订单的阶段和状态
            entity.setPhase(result.getTargetPhase());
            entity.setStatus(result.getFinalStatus());
            updateById(entity);
            log.info("提交订单成功，id={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交订单异常，id={}", id, e);
            throw e;
        }
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
        log.info("撤回订单，id={}, currentUserId={}", id, currentUserId);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 校验无阻断性修改申请
            orderModifyApplyService.validateNoBlockingModifyApply(id);
            // 通过 FlowFacade 执行撤回动作
            TransitionResult result = flowFacade.executeFlow(
                    id, FlowActionEnum.WITHDRAW, FlowOperator.of(currentUserId, null));
            // 更新订单的阶段、状态和当前处理人
            entity.setPhase(result.getTargetPhase());
            entity.setStatus(result.getFinalStatus());
            entity.setCurrentHandlerId(currentUserId);
            updateById(entity);
            log.info("撤回订单成功，id={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("撤回订单异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 审核通过
     * 审核人确认订单数据无误，流程推进至下一阶段
     *
     * 【流转逻辑】
     * - 执行 DATA_AUDIT_PASS 动作，数据审核通过
     * - 根据 needsPhysicalDelivery 决定后续流程分支
     *
     * @param id 订单ID
     * @param dto 审核参数（含驳回原因remark）
     * @throws BusinessException 订单不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long id, AuditOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("审核通过，id={}, currentUserId={}", id, currentUserId);
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 校验无阻断性修改申请
            orderModifyApplyService.validateNoBlockingModifyApply(id);
            // 通过 FlowFacade 执行审核通过动作
            TransitionResult result = flowFacade.executeFlow(
                    id, FlowActionEnum.DATA_AUDIT_PASS, new FlowOperator(currentUserId, null, dto.getRemark()));
            // 更新订单的阶段、状态和当前处理人，同步写入审核时填写的预估费用和影像评估意见
            entity.setPhase(result.getTargetPhase());
            entity.setStatus(result.getFinalStatus());
            entity.setCurrentHandlerId(currentUserId);
            if (dto.getEstimatedCost() != null) {
                entity.setEstimatedCost(dto.getEstimatedCost());
            }
            if (StrUtil.isNotBlank(dto.getDataEvaluationOpinion())) {
                entity.setDataEvaluationOpinion(dto.getDataEvaluationOpinion());
            }
            updateById(entity);
            // 触发设计师分配（分配失败不影响审核结果）
            designerAssignmentService.triggerAssignmentAfterAudit(id);
            log.info("审核通过成功，id={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核通过异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 审核驳回
     * 审核人驳回订单，订单退回可编辑状态，申请人可修改后重新提交
     *
     * 【驳回规则】
     * - 必须填写驳回原因（remark）
     *
     * 【流转逻辑】
     * - 执行 DATA_AUDIT_REJECT 动作，订单退回草稿状态
     *
     * @param id 订单ID
     * @param dto 审核参数（含驳回原因remark）
     * @throws BusinessException 订单不存在、驳回原因未填写
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long id, AuditOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("审核驳回，id={}, currentUserId={}, remark={}", id, currentUserId, dto.getRemark());
        try {
            // 校验订单存在
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            // 驳回时必须填写驳回原因
            if (StrUtil.isBlank(dto.getRemark())) {
                log.warn("审核驳回时必须填写驳回原因");
                throw new BusinessException(ErrorCodeEnum.ORDER_AUDIT_REMARK_REQUIRED);
            }
            // 校验无阻断性修改申请
            orderModifyApplyService.validateNoBlockingModifyApply(id);
            // 通过 FlowFacade 执行审核驳回动作
            TransitionResult result = flowFacade.executeFlow(
                    id, FlowActionEnum.DATA_AUDIT_REJECT, new FlowOperator(currentUserId, null, dto.getRemark()));
            // 更新订单的阶段、状态、驳回原因和当前处理人
            entity.setPhase(result.getTargetPhase());
            entity.setStatus(result.getFinalStatus());
            entity.setAuditRemark(dto.getRemark());
            entity.setCurrentHandlerId(currentUserId);
            updateById(entity);
            log.info("审核驳回成功，id={}, phase={}, status={}", id, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核驳回异常，id={}", id, e);
            throw e;
        }
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
        log.info("从草稿创建正式订单，draftId={}", draft.getId());
        try {
            // Step 1：生成订单编号
            String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);
            log.info("生成订单编号，orderCode={}", orderCode);

            // Step 2：构建订单主表，从草稿复制字段，排除不可复用字段
            OrderMainEntity order = new OrderMainEntity();
            BeanUtils.copyProperties(draft, order, "id", "expiresAt", "status");
            order.setOrderCode(orderCode);
            order.setPhase(FlowPhaseEnum.ORDER.getValue());
            order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
            order.setVersion(0);
            // 从医院表补充地区冗余字段（草稿中已复制 hospitalId，此处补充 area 字段）
            fillAreaFromHospital(order, order.getHospitalId());

            // 提单人部门信息冗余写入（草稿提交时从提单人账号读取，创建后固化）
            String operatorName = null;
            if (draft.getOperatorId() != null) {
                UserEntity user = userService.getById(draft.getOperatorId());
                if (user != null) {
                    operatorName = user.getRealName();
                    order.setOperatorDeptId(user.getDeptId());
                    order.setOperatorDeptName(user.getDeptName());
                } else {
                    log.warn("草稿提交时提单人账号不存在，operatorDeptId/Name 将为 null，draftId={}, operatorId={}",
                            draft.getId(), draft.getOperatorId());
                }
            } else {
                log.warn("草稿无 operatorId，operatorDeptId/Name 将为 null，draftId={}", draft.getId());
            }

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
            log.info("创建订单明细，orderId={}, itemCount={}", orderId, draftItems.size());

            // Step 4：复制文件关联关系（从草稿关联迁移至订单关联）
            List<FileVO> draftImageData = fileService.listByBiz(FileBizTypeEnum.IMAGE_DATA.getDictCode(), draft.getId());
            List<FileVO> draftImageReport = fileService.listByBiz(FileBizTypeEnum.IMAGE_REPORT.getDictCode(), draft.getId());
            List<FileVO> draftFiles = new java.util.ArrayList<>(draftImageData);
            draftFiles.addAll(draftImageReport);
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
            log.info("从草稿创建正式订单成功，orderId={}, orderCode={}", orderId, orderCode);
            return orderId;
        } catch (Exception e) {
            log.error("从草稿创建正式订单异常，draftId={}", draft.getId(), e);
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
     *    - deptName: 从医院科室表查询覆盖（hospitalDeptName）
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
        log.info("直接创建正式订单，currentUserId={}", currentUserId);
        try {
            // Step 1：生成订单编号
            String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);
            log.info("生成订单编号，orderCode={}", orderCode);

            // Step 2：校验影像文件（根据系统配置判断是否必须上传）
            validateOrderFiles(dto);

            // Step 3：构建订单主表
            OrderMainEntity order = new OrderMainEntity();
            BeanUtils.copyProperties(dto, order);
            order.setOrderCode(orderCode);
            order.setPhase(FlowPhaseEnum.ORDER.getValue());
            order.setStatus(FlowStatusEnum.PENDING_DATA_AUDIT.getValue());
            order.setVersion(0);

            // 操作员信息强制从当前登录用户填充，不信任前端传入值
            UserEntity currentUser = userService.getById(currentUserId);
            if (currentUser == null) {
                log.warn("当前登录用户不存在，userId={}", currentUserId);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
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

            // 校验关联数据并覆盖所有冗余名称字段（orgName/hospitalName/area/hospitalDeptName/doctorId+Name+Phone）
            orderDataValidator.validateAndFillMasterForOrder(
                    order,
                    dto.getOrgId(), dto.getHospitalId(), dto.getHospitalDeptId(),
                    dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone(),
                    currentUserId, OrderDataValidator.ValidateMode.DIRECT);
            // 校验订单类型与机构资质是否匹配
            orderDataValidator.validateOrderType(currentUserId, dto.getOrderType());

            save(order);
            Long orderId = order.getId();
            log.info("创建订单主表，orderId={}, orderCode={}", orderId, orderCode);

            // Step 4：保存重建项目列表，校验并覆盖 bodyPartName/projectName/estimatedHours/projectDesc
            if (dto.getItems() != null && !dto.getItems().isEmpty()) {
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
                log.info("创建订单明细，orderId={}, itemCount={}", orderId, items.size());
            }

            // Step 5：保存影像文件关联
            saveOrderFiles(orderId, orderCode, dto.getImageDataFileIds(), FileBizTypeEnum.IMAGE_DATA.getDictCode());
            saveOrderFiles(orderId, orderCode, dto.getImageReportFileIds(), FileBizTypeEnum.IMAGE_REPORT.getDictCode());

            // Step 6：记录状态历史（CREATE 动作仅记录历史，不改变 phase/status）
            flowFacade.executeFlow(orderId, FlowActionEnum.CREATE,
                    new FlowOperator(currentUserId, currentUser.getRealName(), "直提创建"));
            log.info("直接创建正式订单成功，orderId={}, orderCode={}", orderId, orderCode);
            return orderId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("直接创建正式订单异常", e);
            throw e;
        }
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

        // ---- 影像报告 ----
        boolean hasImageReport = dto.getImageReportFileIds() != null && !dto.getImageReportFileIds().isEmpty();
        if (required && !hasImageReport) {
            log.warn("直提创建订单缺少影像报告，配置要求必须上传");
            throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "影像报告");
        }
        if (hasImageReport) {
            assertFilesExist(fileService.listByIds(dto.getImageReportFileIds()), dto.getImageReportFileIds(), "影像报告");
        }

        log.info("直提创建订单影像文件校验通过，imageDataCount={}, imageReportCount={}",
                hasImageData ? dto.getImageDataFileIds().size() : 0,
                hasImageReport ? dto.getImageReportFileIds().size() : 0);
    }

    /**
     * 校验文件列表中每个 fileId 都存在于查询结果中，否则抛出 ATTACHMENT_NOT_FOUND
     */
    private void assertFilesExist(List<FileVO> found, List<String> fileIds, String categoryName) {
        Set<String> foundIds = found.stream().map(FileVO::getId).collect(Collectors.toSet());
        for (String fileId : fileIds) {
            if (!foundIds.contains(fileId)) {
                log.warn("{} 文件不存在，fileId={}", categoryName, fileId);
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
        log.info("保存订单影像文件关联，orderId={}, fileCategory={}, count={}", orderId, fileCategory, fileIds.size());
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
        // 按文件类别分组
        List<OrderDetailVO.OrderFileVO> imageDataFiles = new java.util.ArrayList<>();
        List<OrderDetailVO.OrderFileVO> imageReportFiles = new java.util.ArrayList<>();
        for (OrderFileEntity orderFile : orderFiles) {
            FileVO fileVO = fileService.getById(orderFile.getFileId());
            if (fileVO == null) {
                continue;
            }
            OrderDetailVO.OrderFileVO file = toOrderFileVO(orderFile, fileVO);
            if (FileBizTypeEnum.IMAGE_DATA.getDictCode().equals(orderFile.getFileCategory())) {
                imageDataFiles.add(file);
            } else if (FileBizTypeEnum.IMAGE_REPORT.getDictCode().equals(orderFile.getFileCategory())) {
                imageReportFiles.add(file);
            }
        }
        vo.setImageDataFiles(imageDataFiles);
        vo.setImageReportFiles(imageReportFiles);
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
        log.info("获取当前用户列配置");
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
        log.info("保存用户列配置，userId={}", currentUserId);
        try {
            // 序列化为 JSON 字符串
            String json = objectMapper.writeValueAsString(config);
            // 更新 UserEntity.orderColumnSettings 字段
            UserEntity user = new UserEntity();
            user.setId(currentUserId);
            user.setOrderColumnSettings(json);
            userService.updateById(user);
            log.info("保存用户列配置成功，userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化列配置失败，userId={}", currentUserId, e);
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "列配置格式非法");
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
        log.info("重置用户列配置，userId={}", currentUserId);
        UserEntity user = new UserEntity();
        user.setId(currentUserId);
        user.setOrderColumnSettings(null);
        userService.updateById(user);
        log.info("重置用户列配置成功，userId={}", currentUserId);
    }
}
