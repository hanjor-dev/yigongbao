package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.order.OrderPhaseEnum;
import com.yigongbao.common.enums.order.OrderStatusEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.dto.order.AuditOrderDTO;
import com.yigongbao.module.order.dto.order.CreateOrderDTO;
import com.yigongbao.module.order.dto.order.UpdateOrderDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.entity.OrderMainEntity;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.service.OrderStateMachineService;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单主表 Service 实现类
 * 处理订单相关的业务逻辑，包括订单CRUD、状态流转、审核流程等
 *
 * @author hanjor
 * @date 2026-03-31
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
    private final OrderStateMachineService orderStateMachineService;
    private final ConfigService configService;
    private final UserService userService;

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID，未登录返回 null
     */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，可能未登录", e);
            return null;
        }
    }

    /**
     * 分页查询订单列表
     *
     * @param pageNum 页码（默认1）
     * @param pageSize 每页条数（默认10）
     * @param orderCode 订单编号（可选，模糊查询）
     * @param hospitalId 医院ID（可选）
     * @param status 状态（可选）
     * @return 分页后的订单列表
     */
    @Override
    public IPage<OrderListVO> listOrders(Integer pageNum, Integer pageSize, String orderCode, Long hospitalId, Integer status) {
        log.info("分页查询订单列表，pageNum={}, pageSize={}, orderCode={}, hospitalId={}, status={}",
                pageNum, pageSize, orderCode, hospitalId, status);
        try {
            Page<OrderMainEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(orderCode), OrderMainEntity::getOrderCode, orderCode)
                    .eq(Objects.nonNull(hospitalId), OrderMainEntity::getHospitalId, hospitalId)
                    .eq(Objects.nonNull(status), OrderMainEntity::getStatus, status)
                    .eq(OrderMainEntity::getPhase, 1)
                    .orderByDesc(OrderMainEntity::getCreateTime);
            IPage<OrderMainEntity> pageResult = page(page, wrapper);
            IPage<OrderListVO> voPage = pageResult.convert(this::toOrderListVO);
            log.info("分页查询订单列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询订单列表异常", e);
            throw e;
        }
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        log.info("查询订单详情，id={}", id);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            OrderDetailVO vo = toOrderDetailVO(entity);
            // 查询订单明细列表
            List<OrderItemEntity> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItemEntity>()
                            .eq(OrderItemEntity::getOrderId, id)
                            .eq(OrderItemEntity::getIsDeleted, 0)
                            .orderByAsc(OrderItemEntity::getSortOrder));
            vo.setItems(items.stream().map(this::toOrderItemVO).collect(Collectors.toList()));
            vo.setItemCount(items.size());
            // 查询可执行动作
            vo.setAvailableActions(orderStateMachineService.getAvailableActions(entity));
            log.info("查询订单详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询订单详情异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, UpdateOrderDTO dto) {
        log.info("更新订单，id={}", id);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            BeanUtils.copyProperties(dto, entity, "id", "orderCode", "phase", "status", "createTime", "updateTime", "createBy", "updateBy", "version");
            updateById(entity);
            log.info("更新订单成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrder(Long id) {
        log.info("删除订单，id={}", id);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            removeById(id);
            // 清理关联明细
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderId, id));
            log.info("删除订单成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除订单异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("提交订单，id={}, currentUserId={}", id, currentUserId);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            Integer targetStatus = orderStateMachineService.executeTransition(entity, "SUBMIT", currentUserId, null, null);
            entity.setStatus(targetStatus);
            updateById(entity);
            log.info("提交订单成功，id={}, targetStatus={}", id, targetStatus);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交订单异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawOrder(Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("撤回订单，id={}, currentUserId={}", id, currentUserId);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            Integer targetStatus = orderStateMachineService.executeTransition(entity, "WITHDRAW", currentUserId, null, null);
            entity.setStatus(targetStatus);
            updateById(entity);
            log.info("撤回订单成功，id={}, targetStatus={}", id, targetStatus);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("撤回订单异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long id, AuditOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("审核通过，id={}, currentUserId={}", id, currentUserId);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            Integer targetStatus = orderStateMachineService.executeTransition(entity, "AUDIT_PASS", currentUserId, null, dto.getRemark());
            entity.setStatus(targetStatus);
            entity.setCurrentHandlerId(currentUserId);
            updateById(entity);
            log.info("审核通过成功，id={}, targetStatus={}", id, targetStatus);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核通过异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long id, AuditOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("审核驳回，id={}, currentUserId={}, remark={}", id, currentUserId, dto.getRemark());
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            if (StrUtil.isBlank(dto.getRemark())) {
                log.warn("审核驳回时必须填写驳回原因");
                throw new BusinessException(ErrorCodeEnum.ORDER_AUDIT_REMARK_REQUIRED);
            }
            Integer targetStatus = orderStateMachineService.executeTransition(entity, "AUDIT_REJECT", currentUserId, null, dto.getRemark());
            entity.setStatus(targetStatus);
            entity.setAuditRemark(dto.getRemark());
            updateById(entity);
            log.info("审核驳回成功，id={}, targetStatus={}", id, targetStatus);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("审核驳回异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    public List<String> listAvailableActions(Long id) {
        log.info("查询订单可执行动作，id={}", id);
        try {
            OrderMainEntity entity = getById(id);
            if (entity == null) {
                log.warn("订单不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            List<String> actions = orderStateMachineService.getAvailableActions(entity);
            log.info("查询订单可执行动作成功，id={}, actions={}", id, actions);
            return actions;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询订单可执行动作异常，id={}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromDraft(OrderDraftEntity draft) {
        log.info("从草稿创建正式订单，draftId={}", draft.getId());
        try {
            // 生成订单编号
            String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);
            log.info("生成订单编号，orderCode={}", orderCode);

            // 构建订单主表
            OrderMainEntity order = new OrderMainEntity();
            BeanUtils.copyProperties(draft, order, "id", "expiresAt", "status");
            order.setOrderCode(orderCode);
            order.setPhase(OrderPhaseEnum.ORDER.getValue());
            order.setStatus(OrderStatusEnum.PENDING.getValue());
            order.setVersion(0);
            save(order);
            Long orderId = order.getId();
            log.info("创建订单主表，orderId={}, orderCode={}", orderId, orderCode);

            // 查询草稿明细并复制到订单明细
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

            // 复制文件关联关系
            List<FileVO> draftFiles = fileService.listByBiz("order_draft", draft.getId());
            for (FileVO file : draftFiles) {
                OrderFileEntity orderFile = new OrderFileEntity();
                orderFile.setOrderId(orderId);
                orderFile.setOrderCode(orderCode);
                orderFile.setFileId(file.getId());
                orderFile.setFileCategory(file.getBizType());
                orderFileMapper.insert(orderFile);
            }
            log.info("复制文件关联，orderId={}, fileCount={}", orderId, draftFiles.size());

            // 记录状态历史
            String operatorName = null;
            if (draft.getOperatorId() != null) {
                UserEntity user = userService.getById(draft.getOperatorId());
                if (user != null) {
                    operatorName = user.getRealName();
                }
            }
            orderStateMachineService.executeTransition(order, "CREATE", draft.getOperatorId(), operatorName, "从草稿创建");

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
     * @param dto 创建订单参数
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("直接创建正式订单，currentUserId={}", currentUserId);
        try {
            // 1. 生成订单编号
            String orderCode = codeGeneratorService.generate(CodeRuleConstants.ORDER_NO);
            log.info("生成订单编号，orderCode={}", orderCode);

            // 2. 构建订单主表
            OrderMainEntity order = new OrderMainEntity();
            BeanUtils.copyProperties(dto, order);
            order.setOrderCode(orderCode);
            order.setPhase(OrderPhaseEnum.ORDER.getValue());
            order.setStatus(OrderStatusEnum.PENDING.getValue());
            order.setVersion(0);
            save(order);
            Long orderId = order.getId();
            log.info("创建订单主表，orderId={}, orderCode={}", orderId, orderCode);

            // 3. 保存重建项目列表
            if (dto.getItems() != null && !dto.getItems().isEmpty()) {
                for (int i = 0; i < dto.getItems().size(); i++) {
                    var itemDTO = dto.getItems().get(i);
                    OrderItemEntity item = new OrderItemEntity();
                    item.setOrderId(orderId);
                    item.setOrderCode(orderCode);
                    item.setBodyPartId(itemDTO.getBodyPartId());
                    item.setBodyPartName(itemDTO.getBodyPartName());
                    item.setProjectId(itemDTO.getProjectId());
                    item.setProjectName(itemDTO.getProjectName());
                    item.setProjectEstimatedHours(itemDTO.getProjectEstimatedHours());
                    item.setProjectDesc(itemDTO.getProjectDesc());
                    item.setFormingRequirement(itemDTO.getFormingRequirement());
                    item.setOtherRequirement(itemDTO.getOtherRequirement());
                    item.setSortOrder(itemDTO.getSortOrder() != null ? itemDTO.getSortOrder() : i + 1);
                    orderItemMapper.insert(item);
                }
                log.info("创建订单明细，orderId={}, itemCount={}", orderId, dto.getItems().size());
            }

            // 4. 记录状态历史
            String operatorName = null;
            if (currentUserId != null) {
                UserEntity user = userService.getById(currentUserId);
                if (user != null) {
                    operatorName = user.getRealName();
                }
            }
            orderStateMachineService.executeTransition(order, "CREATE", currentUserId, operatorName, "直提创建");

            log.info("直接创建正式订单成功，orderId={}, orderCode={}", orderId, orderCode);
            return orderId;
        } catch (Exception e) {
            log.error("直接创建正式订单异常", e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 实体转换为订单列表 VO
     *
     * @param entity 订单实体
     * @return 订单列表 VO
     */
    private OrderListVO toOrderListVO(OrderMainEntity entity) {
        OrderListVO vo = new OrderListVO();
        vo.setId(entity.getId());
        vo.setOrderCode(entity.getOrderCode());
        vo.setOrderType(entity.getOrderType());
        vo.setBusinessType(entity.getBusinessType());
        vo.setHospitalName(entity.getHospitalName());
        vo.setPatientName(entity.getPatientName());
        vo.setPhase(entity.getPhase());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
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
        // 补充显示名称
        vo.setPatientGenderName(getGenderName(entity.getPatientGender()));
        return vo;
    }

    /**
     * 获取性别名称
     *
     * @param gender 性别编码
     * @return 性别名称
     */
    private String getGenderName(String gender) {
        if (StrUtil.isBlank(gender)) {
            return null;
        }
        return switch (gender) {
            case DictCodeConstants.PATIENT_GENDER_MALE -> "男";
            case DictCodeConstants.PATIENT_GENDER_FEMALE -> "女";
            default -> null;
        };
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
}
