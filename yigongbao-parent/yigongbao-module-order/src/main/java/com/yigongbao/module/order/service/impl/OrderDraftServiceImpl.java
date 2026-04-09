package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.order.dto.draft.CreateOrderDraftDTO;
import com.yigongbao.module.order.dto.draft.OrderDraftPageQueryDTO;
import com.yigongbao.module.order.dto.draft.OrderItemDraftItemDTO;
import com.yigongbao.module.order.entity.OrderDraftEntity;
import com.yigongbao.module.order.entity.OrderItemDraftEntity;
import com.yigongbao.module.order.mapper.OrderDraftMapper;
import com.yigongbao.module.order.mapper.OrderItemDraftMapper;
import com.yigongbao.module.order.service.OrderDraftService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.validator.OrderDataValidator;
import com.yigongbao.module.order.vo.draft.OrderDraftDetailVO;
import com.yigongbao.module.order.vo.draft.OrderDraftVO;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单草稿 Service 实现类
 * 处理订单草稿相关的业务逻辑，包括草稿CRUD、提交转正式订单等
 *
 * 【重要】草稿状态与订单状态是两个完全独立的状态体系：
 * - 草稿状态（order_draft.status）：1=有效，2=已提交，3=已过期（仅用于草稿生命周期管理）
 * - 订单状态（order_main.status）：OrderStatusEnum 定义的 10-80 范围（用于订单阶段流转）
 * 这两个字段没有关联，不能混淆使用
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDraftServiceImpl extends ServiceImpl<OrderDraftMapper, OrderDraftEntity> implements OrderDraftService {

    private final OrderItemDraftMapper orderItemDraftMapper;
    private final FileService fileService;
    private final OrderMainService orderMainService;
    private final com.yigongbao.module.system.config.service.ConfigService configService;
    private final OrderDataValidator orderDataValidator;
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
     * 分页查询我的草稿列表（仅分页参数，按创建时间倒序）
     *
     * @param dto 分页查询参数
     * @return 分页后的草稿列表
     */
    @Override
    public IPage<OrderDraftVO> listDrafts(OrderDraftPageQueryDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("分页查询我的草稿列表，pageNum={}, pageSize={}, currentUserId={}",
                dto.getPageNum(), dto.getPageSize(), currentUserId);
        try {
            Page<OrderDraftEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
            LambdaQueryWrapper<OrderDraftEntity> wrapper = new LambdaQueryWrapper<>();
            // 仅查询当前用户的草稿，排除已提交的草稿，按创建时间倒序
            wrapper.eq(currentUserId != null, OrderDraftEntity::getOperatorId, currentUserId)
                    .ne(OrderDraftEntity::getStatus, 2)
                    .orderByDesc(OrderDraftEntity::getCreateTime);
            IPage<OrderDraftEntity> pageResult = page(page, wrapper);

            // 查询每个草稿的明细数量
            List<OrderDraftEntity> records = pageResult.getRecords();
            Map<Long, Long> itemCountMap = new java.util.HashMap<>();
            if (!records.isEmpty()) {
                List<Long> draftIds = records.stream().map(OrderDraftEntity::getId).collect(Collectors.toList());
                itemCountMap = orderItemDraftMapper.selectList(
                                new LambdaQueryWrapper<OrderItemDraftEntity>()
                                        .in(OrderItemDraftEntity::getDraftId, draftIds)
                                        .eq(OrderItemDraftEntity::getIsDeleted, 0))
                        .stream()
                        .collect(Collectors.groupingBy(OrderItemDraftEntity::getDraftId, Collectors.counting()));
            }

            // 在 convert lambda 中填充计算字段，确保数据正确传入 VO
            final Map<Long, Long> finalItemCountMap = itemCountMap;
            IPage<OrderDraftVO> voPage = pageResult.convert(entity -> {
                OrderDraftVO vo = toOrderDraftVO(entity);
                vo.setItemCount(finalItemCountMap.getOrDefault(entity.getId(), 0L).intValue());
                vo.setStatusName(getDraftStatusName(entity.getStatus()));
                return vo;
            });
            log.info("分页查询我的草稿列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询我的草稿列表异常", e);
            throw e;
        }
    }

    /**
     * 查询草稿详情
     *
     * @param id 草稿ID
     * @return 草稿详情（包含重建项目列表）
     * @throws BusinessException 草稿不存在
     */
    @Override
    public OrderDraftDetailVO getDraftDetail(Long id) {
        log.info("查询草稿详情，id={}", id);
        try {
            OrderDraftEntity entity = getById(id);
            if (entity == null) {
                log.warn("草稿不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
            }
            OrderDraftDetailVO vo = toOrderDraftDetailVO(entity);
            // 查询重建项目列表
            List<OrderItemDraftEntity> items = orderItemDraftMapper.selectList(
                    new LambdaQueryWrapper<OrderItemDraftEntity>()
                            .eq(OrderItemDraftEntity::getDraftId, id)
                            .eq(OrderItemDraftEntity::getIsDeleted, 0)
                            .orderByAsc(OrderItemDraftEntity::getSortOrder));
            List<OrderDraftDetailVO.OrderItemDraftVO> itemVOs = items.stream()
                    .map(this::toOrderItemDraftVO)
                    .collect(Collectors.toList());
            vo.setItems(itemVOs);
            vo.setItemCount(itemVOs.size());
            log.info("查询草稿详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询草稿详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 保存草稿（新增或更新）
     * 新增时自动设置过期时间（默认30天），更新时保留原过期时间
     * 所有关联名称字段（orgName/hospitalName/deptName/doctorName等）通过 OrderDataValidator 从数据库查询覆盖
     * 操作员信息（operatorName/operatorPhone）强制从当前登录用户填充
     *
     * @param dto 创建或更新草稿的请求参数
     * @return 草稿ID
     * @throws BusinessException 草稿不存在/无权限/已提交
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(CreateOrderDraftDTO dto) {
        Long currentUserId = getCurrentUserId();
        log.info("保存草稿，currentUserId={}, draftId={}", currentUserId, dto.getId());
        try {
            // 校验业务类型
            validateBusinessType(dto.getBusinessType());
            // 判断是新增还是更新
            OrderDraftEntity entity;
            if (dto.getId() != null) {
                // 更新：校验草稿存在且属于当前用户
                entity = getById(dto.getId());
                if (entity == null) {
                    log.warn("草稿不存在，id={}", dto.getId());
                    throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
                }
                if (!Objects.equals(currentUserId, entity.getOperatorId())) {
                    log.warn("只能修改自己的草稿，id={}, operatorId={}, currentUserId={}",
                            dto.getId(), entity.getOperatorId(), currentUserId);
                    throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_MINE);
                }
                // 草稿已提交后不能再修改
                if (entity.getStatus() != null && entity.getStatus() == 2) {
                    log.warn("草稿已提交，不能修改，id={}", dto.getId());
                    throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_ALREADY_SUBMITTED);
                }
            } else {
                // 新增
                entity = new OrderDraftEntity();
                // 操作员ID固定为当前登录用户，不允许前端传入
                entity.setOperatorId(currentUserId);
                // 操作员姓名和电话强制从当前登录用户填充，不信任前端传入值
                UserEntity currentUser = userService.getById(currentUserId);
                if (currentUser != null) {
                    entity.setOperatorName(currentUser.getRealName());
                    entity.setOperatorPhone(currentUser.getPhone());
                }
                // 设置过期时间：从系统配置获取，默认30天
                String expireDaysStr = configService.getConfigValue(SystemConfigKeyEnum.ORDER_DRAFT_EXPIRE_DAYS.getKey());
                int expireDays = StrUtil.isNotBlank(expireDaysStr) ? Integer.parseInt(expireDaysStr) : 30;
                entity.setExpiresAt(LocalDateTime.now().plusDays(expireDays));
                entity.setStatus(1);
            }
            // 复制 DTO 中允许前端设置的纯业务字段（排除关联名称类字段和 id/items）
            BeanUtils.copyProperties(dto, entity, "id", "items",
                    "orgName", "operatorName", "operatorPhone",
                    "hospitalName", "hospitalDeptName",
                    "doctorName", "doctorPhone");

            // 校验关联数据并覆盖所有冗余名称字段（DRAFT 模式：仅校验已填写的字段）
            orderDataValidator.validateAndFillMaster(
                    entity,
                    dto.getOrgId(), dto.getHospitalId(), dto.getHospitalDeptId(),
                    dto.getDoctorId(), dto.getDoctorName(), dto.getDoctorPhone(),
                    currentUserId, OrderDataValidator.ValidateMode.DRAFT);

            saveOrUpdate(entity);
            Long draftId = entity.getId();

            // 保存重建项目列表，校验并覆盖 bodyPartName/projectName 等
            if (dto.getItems() != null && !dto.getItems().isEmpty()) {
                // 先删除旧明细
                orderItemDraftMapper.delete(
                        new LambdaQueryWrapper<OrderItemDraftEntity>()
                                .eq(OrderItemDraftEntity::getDraftId, draftId));
                // 构建明细实体列表（仅设置 ID 类字段和业务字段，名称字段由 validator 覆盖）
                List<OrderItemDraftEntity> itemEntities = new java.util.ArrayList<>();
                for (int i = 0; i < dto.getItems().size(); i++) {
                    OrderItemDraftItemDTO itemDTO = dto.getItems().get(i);
                    OrderItemDraftEntity itemEntity = new OrderItemDraftEntity();
                    itemEntity.setDraftId(draftId);
                    itemEntity.setBodyPartId(itemDTO.getBodyPartId());
                    itemEntity.setProjectId(itemDTO.getProjectId());
                    itemEntity.setFormingRequirement(itemDTO.getFormingRequirement());
                    itemEntity.setOtherRequirement(itemDTO.getOtherRequirement());
                    itemEntity.setSortOrder(itemDTO.getSortOrder() != null ? itemDTO.getSortOrder() : i + 1);
                    itemEntities.add(itemEntity);
                }
                // 通过校验器覆盖 bodyPartName/projectName/projectEstimatedHours/projectDesc
                orderDataValidator.validateAndFillItems(itemEntities, OrderDataValidator.ValidateMode.DRAFT);
                for (OrderItemDraftEntity itemEntity : itemEntities) {
                    orderItemDraftMapper.insert(itemEntity);
                }
            }
            log.info("保存草稿成功，id={}", draftId);
            return draftId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存草稿异常，draftId={}", dto.getId(), e);
            throw e;
        }
    }

    /**
     * 删除草稿
     * 只能删除自己的草稿，且草稿未提交
     *
     * @param id 草稿ID
     * @throws BusinessException 草稿不存在/无权限/已提交
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDraft(Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("删除草稿，id={}, currentUserId={}", id, currentUserId);
        try {
            OrderDraftEntity entity = getById(id);
            if (entity == null) {
                log.warn("草稿不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
            }
            // 校验权限：只有创建人能删除
            if (currentUserId == null) {
                log.warn("未登录，无法删除草稿，id={}", id);
                throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
            }
            if (!currentUserId.equals(entity.getOperatorId())) {
                log.warn("只能删除自己的草稿，id={}, operatorId={}, currentUserId={}",
                        id, entity.getOperatorId(), currentUserId);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_MINE);
            }
            // 草稿已提交后不能删除
            if (entity.getStatus() != null && entity.getStatus() == 2) {
                log.warn("草稿已提交，不能删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_ALREADY_SUBMITTED);
            }
            // 先删除明细
            orderItemDraftMapper.delete(
                    new LambdaQueryWrapper<OrderItemDraftEntity>()
                            .eq(OrderItemDraftEntity::getDraftId, id));
            // 再删除草稿
            removeById(id);
            log.info("删除草稿成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除草稿异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 提交草稿，转化为正式订单
     * 校验内容：草稿存在/未过期/业务类型合法/有重建项目/文件完整
     *
     * @param id 草稿ID
     * @return 生成的正式订单ID
     * @throws BusinessException 草稿不存在/已过期/业务类型不合法/明细为空/文件缺失
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitDraft(Long id) {
        Long currentUserId = getCurrentUserId();
        log.info("提交草稿，id={}, currentUserId={}", id, currentUserId);
        try {
            OrderDraftEntity entity = getById(id);
            if (entity == null) {
                log.warn("草稿不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
            }
            // 草稿已提交后不能再提交
            if (entity.getStatus() != null && entity.getStatus() == 2) {
                log.warn("草稿已提交，不能重复提交，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_ALREADY_SUBMITTED);
            }
            // 校验是否过期
            if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("草稿已过期，id={}, expiresAt={}", id, entity.getExpiresAt());
                throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_EXPIRED);
            }
            // 校验业务类型
            validateBusinessType(entity.getBusinessType());
            // 校验重建项目至少1条
            long itemCount = orderItemDraftMapper.selectCount(
                    new LambdaQueryWrapper<OrderItemDraftEntity>()
                            .eq(OrderItemDraftEntity::getDraftId, id)
                            .eq(OrderItemDraftEntity::getIsDeleted, 0));
            if (itemCount == 0) {
                log.warn("重建项目明细为空，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORDER_ITEM_EMPTY);
            }
            // 校验文件
            validateDraftFiles(id);
            // 转为正式订单
            Long orderId = orderMainService.createFromDraft(entity);
            // 更新草稿状态为已提交
            entity.setStatus(2);
            updateById(entity);
            log.info("提交草稿成功，draftId={}, orderId={}", id, orderId);
            return orderId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交草稿异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 校验草稿是否属于指定操作员
     *
     * @param id 草稿ID
     * @param operatorId 操作员ID
     * @throws BusinessException 草稿不存在/不属于该操作员
     */
    @Override
    public void validateDraftOwner(Long id, Long operatorId) {
        OrderDraftEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_FOUND);
        }
        if (!operatorId.equals(entity.getOperatorId())) {
            log.warn("只能查看自己的草稿，id={}, operatorId={}, requestOperatorId={}",
                    id, entity.getOperatorId(), operatorId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DRAFT_NOT_MINE);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 校验业务类型
     */
    private void validateBusinessType(String businessType) {
        if (StrUtil.isBlank(businessType)) {
            return;
        }
        // 校验是否为合法的字典 dict_code
        if (!businessType.equals(DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS)
                && !businessType.equals(DictCodeConstants.ORDER_BUSINESS_TYPE_TEST)
                && !businessType.equals(DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL)
                && !businessType.equals(DictCodeConstants.ORDER_BUSINESS_TYPE_AGENT)) {
            log.warn("业务类型不合法，businessType={}", businessType);
            throw new BusinessException(ErrorCodeEnum.ORDER_BUSINESS_TYPE_INVALID);
        }
    }

    /**
     * 校验草稿文件
     * 根据系统配置 order.image.required 判断影像文件是否必填
     */
    private void validateDraftFiles(Long draftId) {
        // 通过 FileService 查询草稿关联的文件
        // bizType 使用 FileBizTypeEnum.ORDER_DRAFT.getCode() 作为业务标识
        List<FileVO> files = fileService.listByBiz(FileBizTypeEnum.ORDER_DRAFT.getCode(), draftId);
        // 获取系统配置：是否必须上传影像文件（ConfigService 已内置兜底逻辑）
        String imageRequired = configService.getConfigValue(SystemConfigKeyEnum.ORDER_IMAGE_REQUIRED.getKey());
        if ("true".equalsIgnoreCase(imageRequired)) {
            // 校验影像数据（dict_code=10.1）
            boolean hasImageData = files.stream()
                    .anyMatch(f -> FileBizTypeEnum.IMAGE_DATA.getDictCode().equals(f.getBizType()));
            if (!hasImageData) {
                log.warn("草稿缺少影像数据，draftId={}", draftId);
                throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "影像数据（CT/MRI等）");
            }
            // 校验影像报告（dict_code=10.2）
            boolean hasImageReport = files.stream()
                    .anyMatch(f -> FileBizTypeEnum.IMAGE_REPORT.getDictCode().equals(f.getBizType()));
            if (!hasImageReport) {
                log.warn("草稿缺少影像报告，draftId={}", draftId);
                throw new BusinessException(ErrorCodeEnum.ORDER_FILE_REQUIRED, "影像报告");
            }
        }
    }

    /**
     * 获取配置值（含兜底默认值）
     * 注意：ConfigService.getConfigValue() 已内置兜底逻辑，此方法提供额外的一层保护
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private String getConfigValue(String configKey, String defaultValue) {
        String value = configService.getConfigValue(configKey);
        return StrUtil.isNotBlank(value) ? value : defaultValue;
    }

    /**
     * 将草稿实体转换为 VO（列表展示用）
     *
     * @param entity 草稿实体
     * @return 草稿 VO
     */
    private OrderDraftVO toOrderDraftVO(OrderDraftEntity entity) {
        OrderDraftVO vo = new OrderDraftVO();
        vo.setId(entity.getId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOrderType(entity.getOrderType());
        vo.setOrderTypeName(getOrderTypeName(entity.getOrderType()));
        vo.setNeedsPhysicalDelivery(entity.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessTypeName(getBusinessTypeName(entity.getBusinessType()));
        vo.setOrgId(entity.getOrgId());
        vo.setOrgName(entity.getOrgName());
        vo.setHospitalId(entity.getHospitalId());
        vo.setHospitalName(entity.getHospitalName());
        vo.setPatientName(entity.getPatientName());
        vo.setPatientGender(entity.getPatientGender());
        vo.setPatientGenderName(getGenderName(entity.getPatientGender()));
        vo.setIsUrgent(entity.getIsUrgent());
        vo.setIsPostal(entity.getIsPostal());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 将草稿实体转换为详情 VO（包含完整字段）
     *
     * @param entity 草稿实体
     * @return 草稿详情 VO
     */
    private OrderDraftDetailVO toOrderDraftDetailVO(OrderDraftEntity entity) {
        OrderDraftDetailVO vo = new OrderDraftDetailVO();
        vo.setId(entity.getId());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setOrderType(entity.getOrderType());
        vo.setOrderTypeName(getOrderTypeName(entity.getOrderType()));
        vo.setNeedsPhysicalDelivery(entity.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessTypeName(getBusinessTypeName(entity.getBusinessType()));
        vo.setOrgId(entity.getOrgId());
        vo.setOrgName(entity.getOrgName());
        vo.setOperatorPhone(entity.getOperatorPhone());
        vo.setHospitalId(entity.getHospitalId());
        vo.setHospitalName(entity.getHospitalName());
        vo.setHospitalDeptId(entity.getHospitalDeptId());
        vo.setHospitalDeptName(entity.getHospitalDeptName());
        vo.setDoctorId(entity.getDoctorId());
        vo.setDoctorName(entity.getDoctorName());
        vo.setDoctorPhone(entity.getDoctorPhone());
        vo.setPatientName(entity.getPatientName());
        vo.setPatientAge(entity.getPatientAge());
        vo.setPatientGender(entity.getPatientGender());
        vo.setPatientGenderName(getGenderName(entity.getPatientGender()));
        vo.setIsUrgent(entity.getIsUrgent());
        vo.setIsPostal(entity.getIsPostal());
        vo.setPostalAddress(entity.getPostalAddress());
        vo.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        vo.setExpiresAt(entity.getExpiresAt());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getDraftStatusName(entity.getStatus()));
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * 将草稿明细实体转换为 VO
     *
     * @param entity 草稿明细实体
     * @return 草稿明细 VO
     */
    private OrderDraftDetailVO.OrderItemDraftVO toOrderItemDraftVO(OrderItemDraftEntity entity) {
        OrderDraftDetailVO.OrderItemDraftVO vo = new OrderDraftDetailVO.OrderItemDraftVO();
        vo.setId(entity.getId());
        vo.setBodyPartId(entity.getBodyPartId());
        vo.setBodyPartName(entity.getBodyPartName());
        vo.setProjectId(entity.getProjectId());
        vo.setProjectName(entity.getProjectName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setProjectEstimatedHours(entity.getProjectEstimatedHours());
        vo.setProjectDesc(entity.getProjectDesc());
        vo.setFormingRequirement(entity.getFormingRequirement());
        vo.setOtherRequirement(entity.getOtherRequirement());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String getOrderTypeName(Integer orderType) {
        if (orderType == null) return null;
        return switch (orderType) {
            case 1 -> "医疗器械";
            case 2 -> "非医疗器械";
            default -> null;
        };
    }

    /**
     * 获取是否需要实体交付名称
     *
     * @param needsPhysicalDelivery 是否需要实体交付
     * @return 显示名称
     */
    private String getNeedsPhysicalDeliveryName(Integer needsPhysicalDelivery) {
        if (needsPhysicalDelivery == null) return null;
        return switch (needsPhysicalDelivery) {
            case 0 -> "不需要实体交付";
            case 1 -> "需要实体交付";
            default -> null;
        };
    }

    private String getBusinessTypeName(String businessType) {
        if (StrUtil.isBlank(businessType)) return null;
        return switch (businessType) {
            case DictCodeConstants.ORDER_BUSINESS_TYPE_BUSINESS -> "业务";
            case DictCodeConstants.ORDER_BUSINESS_TYPE_TEST -> "测试";
            case DictCodeConstants.ORDER_BUSINESS_TYPE_TRIAL -> "试用";
            case DictCodeConstants.ORDER_BUSINESS_TYPE_AGENT -> "代理";
            default -> null;
        };
    }

    private String getGenderName(String patientGender) {
        if (StrUtil.isBlank(patientGender)) return null;
        return switch (patientGender) {
            case DictCodeConstants.PATIENT_GENDER_MALE -> "男";
            case DictCodeConstants.PATIENT_GENDER_FEMALE -> "女";
            default -> null;
        };
    }

    private String getDraftStatusName(Integer status) {
        if (status == null) return null;
        return switch (status) {
            case 1 -> "有效";
            case 2 -> "已提交";
            case 3 -> "已过期";
            default -> null;
        };
    }
}
