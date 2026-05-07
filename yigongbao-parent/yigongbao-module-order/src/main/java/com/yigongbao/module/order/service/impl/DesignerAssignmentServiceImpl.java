package com.yigongbao.module.order.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.rebuildProject.service.RebuildProjectService;
import com.yigongbao.module.order.dto.order.DesignerQueryDTO;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderItemMapper;
import com.yigongbao.module.order.service.DesignerAssignmentService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.DesignerVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 设计师分配 Service 实现
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Slf4j
@Service
public class DesignerAssignmentServiceImpl implements DesignerAssignmentService {

    private static final List<String> DESIGNER_ROLES = List.of("designer", "designer-manager");
    /** 专业方向二级编码严格白名单正则：仅允许 \d+\.\d+ 格式，如 7.1 */
    private static final Pattern SPECIALTY_PATTERN = Pattern.compile("^\\d+\\.\\d+$");

    private final OrderMainService orderMainService;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ConfigService configService;
    private final DictService dictService;
    private final RebuildProjectService rebuildProjectService;

    /**
     * 手写构造函数，对 OrderMainService 使用 @Lazy 打破循环依赖
     * （OrderMainServiceImpl 注入了 DesignerAssignmentService，
     *  DesignerAssignmentServiceImpl 反向注入 OrderMainService）
     */
    public DesignerAssignmentServiceImpl(
            @Lazy OrderMainService orderMainService,
            OrderItemMapper orderItemMapper,
            UserMapper userMapper,
            ConfigService configService,
            DictService dictService,
            RebuildProjectService rebuildProjectService) {
        this.orderMainService = orderMainService;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.configService = configService;
        this.dictService = dictService;
        this.rebuildProjectService = rebuildProjectService;
    }

    /**
     * 审核通过后触发分配（根据系统配置决定自动或跳过；分配失败不影响审核结果）
     *
     * @param orderId 订单ID
     */
    @Override
    public void triggerAssignmentAfterAudit(Long orderId) {
        log.info("触发设计师分配，orderId={}", orderId);
        String mode = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_ASSIGN_MODE.getKey());
        if (!"auto".equals(mode)) {
            // 手动模式：跳过自动分配，订单保持 PENDING_DESIGN 状态等待管理员手动分配
            log.info("当前为手动分配模式，跳过自动分配，orderId={}", orderId);
            return;
        }
        try {
            Long designerId = autoAssignDesigner(orderId);
            if (designerId == null) {
                // 无可用设计师（专业方向无人覆盖或无在职设计师），订单保持 PENDING_DESIGN 状态
                // 管理员可通过筛选 status=PENDING_DESIGN & designerId=null 找到此类待分配订单
                log.warn("自动分配未找到合适设计师，订单进入待分配状态，orderId={}", orderId);
            } else {
                log.info("自动分配成功，orderId={}, designerId={}", orderId, designerId);
            }
        } catch (Exception e) {
            // 技术异常（DB 故障等），分配失败不阻断审核结果，管理员后续手动分配
            log.error("自动分配异常，订单保持 PENDING_DESIGN 状态，orderId={}", orderId, e);
        }
    }

    /**
     * 自动分配设计师（专业方向匹配 + 负载均衡，取负载最低的第一位）
     * 两级查询：1) 精确匹配订单专业方向；2) 兜底查询通用专业方向
     *
     * @param orderId 订单ID
     * @return 分配到的设计师用户ID，无可分配时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoAssignDesigner(Long orderId) {
        log.info("自动分配设计师，orderId={}", orderId);
        // 1. 获取订单专业方向
        String specialty = getOrderSpecialty(orderId);
        if (StrUtil.isBlank(specialty)) {
            log.warn("订单无可用专业方向，跳过自动分配，orderId={}", orderId);
            return null;
        }
        // 2. 第一次查询：精确匹配订单专业方向
        List<UserEntity> candidates = userMapper.selectAvailableDesigners(specialty);
        if (CollUtil.isEmpty(candidates)) {
            log.info("精确匹配无结果，尝试通用专业方向兜底，specialty={}", specialty);
            // 3. 第二次查询：使用通用专业方向兜底
            candidates = userMapper.selectAvailableDesigners(DictCodeConstants.USER_SPECIALTY_GENERAL);
            if (CollUtil.isEmpty(candidates)) {
                log.warn("通用专业方向兜底仍无结果，specialty={}", specialty);
                return null;
            }
            log.info("通用专业方向兜底成功，匹配到 {} 位设计师", candidates.size());
        }
        // 4. 取负载最低的第一位
        UserEntity designer = candidates.getFirst();
        // 5. 更新订单 designerId / designerName
        OrderMainEntity order = orderMainService.getById(orderId);
        updateOrderDesigner(order, designer);
        log.info("自动分配成功，orderId={}, designerId={}, specialty={}", orderId, designer.getId(), specialty);
        return designer.getId();
    }

    /**
     * 手动分配设计师（仅管理员；订单必须处于 PENDING_DESIGN 状态）
     *
     * @param orderId    订单ID
     * @param designerId 设计师用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualAssignDesigner(Long orderId, Long designerId) {
        log.info("手动分配设计师，orderId={}, designerId={}", orderId, designerId);
        // 1. 校验订单存在且状态为 PENDING_DESIGN
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!FlowStatusEnum.PENDING_DESIGN.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许分配，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }
        // 2. 校验设计师存在、角色合法、状态正常
        UserEntity designer = userMapper.selectById(designerId);
        if (designer == null || designer.getIsDeleted() == StatusConstants.DELETED) {
            throw new BusinessException(ErrorCodeEnum.DESIGNER_NOT_FOUND);
        }
        if (!DESIGNER_ROLES.contains(designer.getRoleCode())) {
            log.warn("用户角色不合法，designerId={}, roleCode={}", designerId, designer.getRoleCode());
            throw new BusinessException(ErrorCodeEnum.DESIGNER_ROLE_INVALID);
        }
        if (designer.getStatus() != StatusConstants.NORMAL) {
            log.warn("设计师已禁用，designerId={}", designerId);
            throw new BusinessException(ErrorCodeEnum.DESIGNER_DISABLED);
        }
        // 3. 校验设计师 specialty 包含订单项目方向
        String orderSpecialty = getOrderSpecialty(orderId);
        if (StrUtil.isNotBlank(orderSpecialty) && !isSpecialtyMatch(designer.getSpecialty(), orderSpecialty)) {
            log.warn("设计师专业方向不匹配，designerId={}, designerSpecialty={}, orderSpecialty={}",
                    designerId, designer.getSpecialty(), orderSpecialty);
            throw new BusinessException(ErrorCodeEnum.DESIGNER_SPECIALTY_MISMATCH);
        }
        // 4. 更新订单
        updateOrderDesigner(order, designer);
        log.info("手动分配成功，orderId={}, designerId={}", orderId, designerId);
    }

    /**
     * 查询可分配设计师列表（按专业方向过滤 + 负载排序）
     *
     * @param dto 查询条件
     * @return 匹配的设计师列表
     */
    @Override
    public List<DesignerVO> listAvailableDesigners(DesignerQueryDTO dto) {
        log.info("查询可分配设计师，specialties={}", dto.getSpecialties());
        List<String> specialties = dto.getSpecialties();
        if (CollUtil.isEmpty(specialties)) {
            return List.of();
        }
        // 严格白名单校验：只允许 \d+\.\d+ 格式，长度 ≤ 16，防止 SQL 注入
        String condition = specialties.stream()
                .filter(s -> StrUtil.isNotBlank(s)
                        && s.length() <= 16
                        && SPECIALTY_PATTERN.matcher(s).matches())
                .map(s -> String.format("FIND_IN_SET('%s', specialty) > 0", s))
                .collect(Collectors.joining(" OR "));
        if (StrUtil.isBlank(condition)) {
            log.warn("所有专业方向编码均未通过白名单校验，返回空列表");
            return List.of();
        }
        List<UserEntity> users = userMapper.selectDesignersBySpecialties(condition);
        return users.stream().map(this::toDesignerVO).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 获取订单的专业方向（通过 order_item.projectId → rebuild_project.specialty）
     */
    private String getOrderSpecialty(Long orderId) {
        List<OrderItemEntity> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItemEntity>()
                        .eq(OrderItemEntity::getOrderId, orderId)
                        .eq(OrderItemEntity::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (CollUtil.isEmpty(items)) {
            return null;
        }
        Long projectId = items.get(0).getProjectId();
        if (projectId == null) {
            return null;
        }
        return rebuildProjectService.getSpecialtyByProjectId(projectId);
    }

    /**
     * 检查设计师 specialty（逗号拼接多值）是否包含指定 orderSpecialty
     */
    private boolean isSpecialtyMatch(String designerSpecialty, String orderSpecialty) {
        if (StrUtil.isBlank(designerSpecialty)) {
            return false;
        }
        // StrUtil.split 在 Hutool 5.8 中返回 List<String>
        return StrUtil.split(designerSpecialty, ',').contains(orderSpecialty);
    }

    /**
     * 更新订单的设计师冗余字段
     */
    private void updateOrderDesigner(OrderMainEntity order, UserEntity designer) {
        order.setDesignerId(designer.getId());
        order.setDesignerName(designer.getRealName());
        orderMainService.updateById(order);
    }

    /**
     * 将 UserEntity 转换为 DesignerVO，填充专业方向名称列表
     */
    private DesignerVO toDesignerVO(UserEntity user) {
        DesignerVO vo = new DesignerVO();
        vo.setUserId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setCurrentLoad(user.getCurrentLoad() != null ? user.getCurrentLoad() : 0);
        if (StrUtil.isNotBlank(user.getSpecialty())) {
            List<String> specList = StrUtil.split(user.getSpecialty(), ',');
            vo.setSpecialtyList(specList);
            List<String> nameList = specList.stream()
                    .map(code -> {
                        var dict = dictService.getByDictCode(code);
                        return dict != null ? dict.getDictName() : code;
                    })
                    .collect(Collectors.toList());
            vo.setSpecialtyNameList(nameList);
        }
        return vo;
    }
}
