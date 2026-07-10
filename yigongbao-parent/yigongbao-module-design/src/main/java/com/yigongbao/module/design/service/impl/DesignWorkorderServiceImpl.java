package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.FileBizTypeEnum;
import com.yigongbao.common.enums.OrderTypeEnum;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.file.service.FileService;
import com.yigongbao.module.basic.file.vo.FileVO;
import com.yigongbao.module.design.dto.DesignWorkorderQueryDTO;
import com.yigongbao.module.design.dto.SaveDesignColumnConfigDTO;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.helper.DesignQueryHelper;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignDocService;
import com.yigongbao.module.design.service.DesignFileService;
import com.yigongbao.module.design.service.DesignWorkorderService;
import com.yigongbao.module.design.vo.DesignColumnConfigVO;
import com.yigongbao.module.design.vo.DesignDocVersionVO;
import com.yigongbao.module.design.vo.DesignModelVO;
import com.yigongbao.module.design.vo.DesignPackageVO;
import com.yigongbao.module.design.vo.DesignWorkorderDetailVO;
import com.yigongbao.module.design.vo.DesignWorkorderListVO;
import com.yigongbao.module.design.vo.SubmitCheckVO;
import com.yigongbao.module.order.entity.OrderFileEntity;
import com.yigongbao.module.order.entity.OrderItemEntity;
import com.yigongbao.module.order.mapper.OrderFileMapper;
import com.yigongbao.module.order.service.OrderCancelApplyService;
import com.yigongbao.module.order.service.OrderItemService;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设计工单查询服务实现类
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DesignWorkorderServiceImpl implements DesignWorkorderService {

    private final OrderMainService orderMainService;
    private final OrderItemService orderItemService;
    private final FileService fileService;
    private final DesignPackageMapper designPackageMapper;
    private final DesignProductMapper designProductMapper;
    private final DesignInstructionMapper designInstructionMapper;
    private final DesignDrawingMapper designDrawingMapper;
    private final DesignModelMapper designModelMapper;
    private final DesignReviewMapper designReviewMapper;
    private final UserService userService;
    private final UserHospitalService userHospitalService;
    private final DesignQueryHelper designQueryHelper;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final FlowFacade flowFacade;
    private final DesignFileService designFileService;
    private final DesignDocService designDocService;
    private final OrderFileMapper orderFileMapper;
    private final com.yigongbao.module.order.mapper.OrderDesignerAssignmentLogMapper assignmentLogMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final com.yigongbao.module.order.service.OrderCancelApplyService cancelApplyService;

    /**
     * 分页查询设计工单列表
     * 按当前用户的数据权限范围过滤，固定查询设计阶段（phase=20）的工单
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listWorkorders(DesignWorkorderQueryDTO queryDTO) {
        // 获取当前用户信息和数据权限类型
        Long currentUserId = designQueryHelper.getCurrentUserId();
        UserEntity currentUser = designQueryHelper.getCurrentUser();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

        // 构建查询条件
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

        // 注入数据权限过滤（按 designer_id）
        designQueryHelper.buildDataScopeCondition(wrapper, currentUser, scopeType);

        // 动态筛选条件
        // orderCode 参数：多字段模糊搜索（订单编号/机构名称/业务员姓名/医院名称/患者名字）
        if (StrUtil.isNotBlank(queryDTO.getOrderCode())) {
            wrapper.and(w -> w.like(OrderMainEntity::getOrderCode, queryDTO.getOrderCode())
                    .or().like(OrderMainEntity::getOrgName, queryDTO.getOrderCode())
                    .or().like(OrderMainEntity::getOperatorName, queryDTO.getOrderCode())
                    .or().like(OrderMainEntity::getHospitalName, queryDTO.getOrderCode())
                    .or().like(OrderMainEntity::getPatientName, queryDTO.getOrderCode()));
        }
        // 状态筛选
        if (queryDTO.getStatus() != null) {
            // 设计完成状态：查询设计完成及后续所有状态（排除已取消）
            if (FlowStatusEnum.DESIGN_COMPLETED.getValue().equals(queryDTO.getStatus())) {
                wrapper.ge(OrderMainEntity::getStatus, FlowStatusEnum.DESIGN_COMPLETED.getValue())
                       .lt(OrderMainEntity::getStatus, FlowStatusEnum.CANCELLED.getValue());
            } else {
                // 其他状态：精确匹配
                wrapper.eq(OrderMainEntity::getStatus, queryDTO.getStatus());
            }
        } else {
            // 未传状态：查询从待设计到后续所有状态（排除已取消）
            wrapper.ge(OrderMainEntity::getStatus, FlowStatusEnum.PENDING_DESIGN.getValue())
                   .lt(OrderMainEntity::getStatus, FlowStatusEnum.CANCELLED.getValue());
        }
        wrapper.eq(queryDTO.getIsUrgent() != null, OrderMainEntity::getIsUrgent, queryDTO.getIsUrgent());
        wrapper.eq(queryDTO.getHospitalId() != null, OrderMainEntity::getHospitalId, queryDTO.getHospitalId());
        wrapper.eq(StrUtil.isNotBlank(queryDTO.getBusinessType()), OrderMainEntity::getBusinessType, queryDTO.getBusinessType());
        wrapper.ge(queryDTO.getCreateTimeStart() != null, OrderMainEntity::getCreateTime, queryDTO.getCreateTimeStart());
        wrapper.le(queryDTO.getCreateTimeEnd() != null, OrderMainEntity::getCreateTime, queryDTO.getCreateTimeEnd());

        // 排序
        designQueryHelper.applySort(wrapper, queryDTO.getSortField(), queryDTO.getSortOrder());

        // 分页参数校验（pageSize 最大 100）
        int pageSize = queryDTO.getPageSize() == null ? 10 : Math.min(queryDTO.getPageSize(), 100);
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        IPage<OrderMainEntity> entityPage = orderMainService.page(new Page<>(pageNum, pageSize), wrapper);

        // 转换为列表 VO
        List<OrderMainEntity> entities = entityPage.getRecords();
        List<DesignWorkorderListVO> voList = entities.stream()
                .map(this::toWorkorderListVO)
                .collect(Collectors.toList());

        // 批量填充重建项目摘要（避免 N+1 问题）
        fillRebuildProjectSummary(voList);

        // 批量填充数据包数量
        fillPackageCount(voList);

        // 批量填充打印信息状态
        fillPrintInfoStatus(voList);

        // 批量填充驳回原因
        fillRejectReason(voList);

        // 构建返回分页对象
        IPage<DesignWorkorderListVO> resultPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 获取工单详情
     *
     * @param orderId 订单ID
     * @return 工单详情 VO
     */
    @Override
    public DesignWorkorderDetailVO getWorkorderDetail(Long orderId) {
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        DesignWorkorderDetailVO vo = new DesignWorkorderDetailVO();

        // 基本信息
        vo.setId(order.getId());
        vo.setOrderCode(order.getOrderCode());
        vo.setStatus(order.getStatus());
        vo.setStatusName(designQueryHelper.getStatusName(order.getStatus()));
        vo.setPhase(order.getPhase());
        vo.setPhaseName(designQueryHelper.getPhaseName(order.getPhase()));

        // 订单类型
        vo.setOrderType(order.getOrderType());
        vo.setOrderTypeName(designQueryHelper.getOrderTypeName(order.getOrderType()));
        vo.setNeedsPhysicalDelivery(order.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(designQueryHelper.getNeedsPhysicalDeliveryName(order.getNeedsPhysicalDelivery()));
        vo.setBusinessType(order.getBusinessType());
        vo.setBusinessTypeName(designQueryHelper.getDictName(order.getBusinessType()));

        // 机构信息
        vo.setOrgId(order.getOrgId());
        vo.setOrgName(order.getOrgName());
        vo.setOperatorId(order.getOperatorId());
        vo.setOperatorName(order.getOperatorName());
        vo.setOperatorPhone(order.getOperatorPhone());

        // 医院信息
        vo.setHospitalId(order.getHospitalId());
        vo.setHospitalName(order.getHospitalName());
        vo.setHospitalDeptName(order.getHospitalDeptName());
        vo.setAreaName(order.getAreaName());
        vo.setFullAreaName(order.getFullAreaName());

        // 医生/患者信息
        vo.setDoctorName(order.getDoctorName());
        vo.setDoctorPhone(order.getDoctorPhone());
        vo.setPatientName(order.getPatientName());
        vo.setPatientAge(order.getPatientAge());
        vo.setPatientGender(order.getPatientGender());
        vo.setPatientGenderName(designQueryHelper.getGenderName(order.getPatientGender()));

        // 业务信息
        vo.setIsUrgent(order.getIsUrgent());
        vo.setIsPostal(order.getIsPostal());
        vo.setPostalAddress(order.getPostalAddress());
        vo.setExpectedDeliveryDate(order.getExpectedDeliveryDate());

        // 设计信息
        vo.setDesignerId(order.getDesignerId());
        vo.setDesignerName(order.getDesignerName());
        vo.setDesignStartTime(order.getDesignStartTime());
        vo.setDesignSubmitTime(order.getDesignSubmitTime());
        vo.setVersion(order.getVersion());

        // 最近一次驳回原因
        vo.setRejectReason(getLatestRejectReason(orderId));

        // 重建项目列表
        vo.setRebuildProjectList(buildRebuildProjectList(orderId));

        // 提交校验状态（读取系统配置的设计模式）
        Integer designMode = getDesignMode();
        vo.setDesignMode(designMode);
        vo.setSubmitCheck(buildSubmitCheck(orderId, designMode));

        // 订单影像文件（订单阶段上传）
        fillOrderImageFiles(vo, orderId);

        // 设计阶段文件（数据包、模型、报告）
        fillDesignFiles(vo, orderId);

        return vo;
    }

    /**
     * 获取当前用户列配置
     *
     * @return 列配置 VO
     */
    @Override
    public DesignColumnConfigVO getColumnConfig() {
        return designQueryHelper.getColumnConfig();
    }

    /**
     * 保存用户列配置到 sys_user.design_column_settings
     *
     * @param dto 列配置参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumnConfig(SaveDesignColumnConfigDTO dto) {
        Long currentUserId = designQueryHelper.getCurrentUserId();
        UserEntity user = userService.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 将列配置序列化为 JSON 写入 design_column_settings 字段
        DesignColumnConfigVO configVO = new DesignColumnConfigVO();
        if (dto.getColumns() != null) {
            List<DesignColumnConfigVO.ColumnItemVO> columnItems = dto.getColumns().stream()
                    .map(item -> {
                        DesignColumnConfigVO.ColumnItemVO colVO = new DesignColumnConfigVO.ColumnItemVO();
                        colVO.setField(item.getField());
                        colVO.setLabel(item.getLabel());
                        colVO.setVisible(item.getVisible());
                        colVO.setSort(item.getSort());
                        colVO.setWidth(item.getWidth());
                        colVO.setFixed(item.getFixed());
                        return colVO;
                    })
                    .collect(Collectors.toList());
            configVO.setColumns(columnItems);
        }

        try {
            String configJson = objectMapper.writeValueAsString(configVO);
            UserEntity update = new UserEntity();
            update.setId(currentUserId);
            update.setDesignColumnSettings(configJson);
            userService.updateById(update);
            log.info("保存用户设计列配置: userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化列配置失败: userId={}", currentUserId, e);
            throw new BusinessException(ErrorCodeEnum.SERVER_ERROR, "列配置保存失败");
        }
    }

    /**
     * 设计师开始设计
     * <p>
     * 校验规则：订单必须处于 PENDING_DESIGN 状态，且当前登录用户是该订单的分配设计师。
     * 执行后更新：phase、status、designStartTime、currentHandlerId、currentHandlerName。
     * </p>
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startDesign(Long orderId) {
        // 1. 校验订单存在
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 检查是否存在待审核的取消申请
        if (cancelApplyService.hasPendingCancelApply(orderId)) {
            log.warn("订单存在待审核的取消申请，不允许开始设计: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        // 2. 校验订单状态（必须是待设计）
        if (!FlowStatusEnum.PENDING_DESIGN.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许开始设计，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 3. 校验当前登录用户是该订单的分配设计师
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            log.warn("非分配设计师，无权开始设计，orderId={}, designerId={}, currentUserId={}",
                    orderId, order.getDesignerId(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
        }

        // 4. 查询当前用户姓名（用于冗余字段回写）
        UserEntity currentUser = userService.getById(currentUserId);
        String currentUserName = currentUser != null ? currentUser.getRealName() : null;

        // 5. 通过 FlowFacade 执行状态流转（PENDING_DESIGN → DESIGN_IN_PROGRESS）
        try {
            TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.START_DESIGN,
                    FlowOperator.of(currentUserId, currentUserName));

            // 6. 将流转结果和设计开始时间写回订单表
            OrderMainEntity update = new OrderMainEntity();
            update.setId(orderId);
            update.setPhase(result.getTargetPhase());
            update.setStatus(result.getFinalStatus());
            update.setDesignStartTime(LocalDateTime.now());
            update.setCurrentHandlerId(currentUserId);
            update.setCurrentHandlerName(currentUserName);
            orderMainService.updateById(update);

            log.info("设计师开始设计: orderId={}, {} -> {}, designerId={}",
                orderId, FlowStatusEnum.PENDING_DESIGN.getName(), FlowStatusEnum.DESIGN_IN_PROGRESS.getName(), currentUserId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("开始设计异常: orderId={}", orderId, e);
            throw e;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 填充订单影像文件（影像数据 10.1 和影像报告 10.2）
     * 从 order_file 表查询关联记录，再通过 fileService 批量获取文件详情
     *
     * @param vo      工单详情 VO
     * @param orderId 订单ID
     */
    private void fillOrderImageFiles(DesignWorkorderDetailVO vo, Long orderId) {
        List<OrderFileEntity> orderFiles = orderFileMapper.selectList(
                new LambdaQueryWrapper<OrderFileEntity>()
                        .eq(OrderFileEntity::getOrderId, orderId)
                        .eq(OrderFileEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByAsc(OrderFileEntity::getId));
        if (orderFiles.isEmpty()) {
            vo.setImageDataFiles(Collections.emptyList());
            vo.setImageReportFiles(Collections.emptyList());
            return;
        }
        // 批量查询文件详情，避免 N+1
        List<String> fileIds = orderFiles.stream().map(OrderFileEntity::getFileId).collect(Collectors.toList());
        List<FileVO> fileVOs = fileService.listByIds(fileIds);
        Map<String, FileVO> fileVOMap = fileVOs.stream()
                .collect(Collectors.toMap(FileVO::getId, f -> f, (v1, v2) -> v1));

        List<OrderDetailVO.OrderFileVO> imageDataFiles = new ArrayList<>();
        List<OrderDetailVO.OrderFileVO> imageReportFiles = new ArrayList<>();
        for (OrderFileEntity orderFile : orderFiles) {
            FileVO fileVO = fileVOMap.get(orderFile.getFileId());
            if (fileVO == null) {
                continue;
            }
            OrderDetailVO.OrderFileVO item = toOrderFileVO(orderFile, fileVO);
            if (FileBizTypeEnum.IMAGE_DATA.getDictCode().equals(orderFile.getFileCategory())) {
                imageDataFiles.add(item);
            } else if (FileBizTypeEnum.IMAGE_REPORT.getDictCode().equals(orderFile.getFileCategory())) {
                imageReportFiles.add(item);
            }
        }
        vo.setImageDataFiles(imageDataFiles);
        vo.setImageReportFiles(imageReportFiles);
    }

    /**
     * 填充设计阶段文件：数据包列表（含包内文件 + 最新版指令单/图纸）、可视化模型、设计报告
     *
     * @param vo      工单详情 VO
     * @param orderId 订单ID
     */
    private void fillDesignFiles(DesignWorkorderDetailVO vo, Long orderId) {
        // 数据包列表（含包内文件）
        List<DesignPackageVO> packages = designFileService.listPackages(orderId);

        if (!packages.isEmpty()) {
            // 批量获取最新版指令单和图纸，填入各包
            Set<Long> packageIds = packages.stream().map(DesignPackageVO::getId).collect(Collectors.toSet());
            Map<Long, DesignDocVersionVO> latestInstructions = designDocService.getLatestInstructionMap(packageIds);
            Map<Long, DesignDocVersionVO> latestDrawings = designDocService.getLatestDrawingMap(packageIds);
            for (DesignPackageVO pkg : packages) {
                pkg.setLatestInstruction(latestInstructions.get(pkg.getId()));
                pkg.setLatestDrawing(latestDrawings.get(pkg.getId()));
            }
        }
        vo.setPackageList(packages);

        // 可视化模型列表
        vo.setModelList(designFileService.listModels(orderId));

        // 设计报告
        vo.setReport(designFileService.getReport(orderId));
    }

    /**
     * OrderFileEntity + FileVO 转 OrderDetailVO.OrderFileVO
     */
    private OrderDetailVO.OrderFileVO toOrderFileVO(OrderFileEntity orderFile, FileVO fileVO) {
        OrderDetailVO.OrderFileVO item = new OrderDetailVO.OrderFileVO();
        item.setFileId(orderFile.getFileId());
        item.setFileName(fileVO.getFileName());
        item.setFileCategory(orderFile.getFileCategory());
        FileBizTypeEnum biz = FileBizTypeEnum.getByDictCode(orderFile.getFileCategory());
        item.setFileCategoryName(biz != null ? biz.getName() : null);
        item.setFileUrl(fileVO.getFileUrl());
        item.setThUrl(fileVO.getThUrl());
        item.setFileSize(fileVO.getFileSize());
        item.setFileSizeText(fileVO.getFileSizeText());
        item.setFileExt(fileVO.getFileExt());
        return item;
    }

    /**
     * 将订单主表实体转换为工单列表 VO（不含批量填充字段）
     */
    private DesignWorkorderListVO toWorkorderListVO(OrderMainEntity entity) {
        DesignWorkorderListVO vo = new DesignWorkorderListVO();
        vo.setId(entity.getId());
        vo.setIsUrgent(entity.getIsUrgent());
        vo.setOrderCode(entity.getOrderCode());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(designQueryHelper.getStatusName(entity.getStatus()));
        vo.setBusinessType(entity.getBusinessType());
        vo.setBusinessTypeName(designQueryHelper.getDictName(entity.getBusinessType()));
        vo.setOrderType(entity.getOrderType());
        vo.setOrderTypeName(designQueryHelper.getOrderTypeName(entity.getOrderType()));
        vo.setNeedsPhysicalDelivery(entity.getNeedsPhysicalDelivery());
        vo.setNeedsPhysicalDeliveryName(designQueryHelper.getNeedsPhysicalDeliveryName(entity.getNeedsPhysicalDelivery()));
        vo.setPatientName(entity.getPatientName());
        vo.setHospitalId(entity.getHospitalId());
        vo.setHospitalName(entity.getHospitalName());
        vo.setHospitalDeptName(entity.getHospitalDeptName());
        vo.setDoctorName(entity.getDoctorName());
        vo.setAreaName(entity.getAreaName());
        vo.setDesignerId(entity.getDesignerId());
        vo.setDesignerName(entity.getDesignerName());
        vo.setDesignStartTime(entity.getDesignStartTime());
        vo.setDesignSubmitTime(entity.getDesignSubmitTime());
        vo.setExpectedDeliveryDate(entity.getExpectedDeliveryDate());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 批量填充工单列表的重建项目摘要（避免 N+1 查询）
     * 格式：左髋骨导板, 右髋骨模型
     */
    private void fillRebuildProjectSummary(List<DesignWorkorderListVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        List<OrderItemEntity> allItems = orderItemService.listByOrderIds(orderIds);
        Map<Long, List<OrderItemEntity>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        for (DesignWorkorderListVO vo : voList) {
            List<OrderItemEntity> items = itemsByOrderId.get(vo.getId());
            if (items != null && !items.isEmpty()) {
                String summary = items.stream()
                        .map(item -> {
                            String bodyPart = StrUtil.isNotBlank(item.getBodyPartName()) ? item.getBodyPartName() : "";
                            String project = StrUtil.isNotBlank(item.getProjectName()) ? item.getProjectName() : "";
                            return bodyPart + project;
                        })
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining(", "));
                vo.setRebuildProjectSummary(summary);
            }
        }
    }

    /**
     * 批量填充工单列表的数据包数量（避免 N+1 查询）
     */
    private void fillPackageCount(List<DesignWorkorderListVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        List<DesignPackageEntity> allPackages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .in(DesignPackageEntity::getOrderId, orderIds)
                        .eq(DesignPackageEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Map<Long, Long> countByOrderId = allPackages.stream()
                .collect(Collectors.groupingBy(DesignPackageEntity::getOrderId, Collectors.counting()));

        for (DesignWorkorderListVO vo : voList) {
            Long count = countByOrderId.get(vo.getId());
            vo.setPackageCount(count != null ? count.intValue() : 0);
        }
    }

    /**
     * 批量填充工单列表的打印信息状态（避免 N+1 查询）
     * design_product 记录仅在用户主动保存打印信息时创建，因此只需判断记录是否存在
     *
     * @param voList 工单列表 VO
     */
    private void fillPrintInfoStatus(List<DesignWorkorderListVO> voList) {
        if (CollUtil.isEmpty(voList)) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        List<DesignProductEntity> allProducts = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .select(DesignProductEntity::getOrderId)
                        .in(DesignProductEntity::getOrderId, orderIds)
                        .eq(DesignProductEntity::getIsDeleted, StatusConstants.NOT_DELETED));

        Set<Long> orderIdsWithPrintInfo = allProducts.stream()
                .map(DesignProductEntity::getOrderId)
                .collect(Collectors.toSet());

        for (DesignWorkorderListVO vo : voList) {
            vo.setHasPrintInfo(orderIdsWithPrintInfo.contains(vo.getId()) ? StatusConstants.YES : StatusConstants.NO);
        }
    }

    /**
     * 批量填充工单列表的最近一次驳回原因（避免 N+1 查询）
     */
    private void fillRejectReason(List<DesignWorkorderListVO> voList) {
        if (voList.isEmpty()) {
            return;
        }
        List<Long> orderIds = voList.stream().map(DesignWorkorderListVO::getId).collect(Collectors.toList());
        // 查询所有相关驳回记录（reviewResult=0 为驳回），按创建时间倒序
        List<DesignReviewEntity> allReviews = designReviewMapper.selectList(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .in(DesignReviewEntity::getOrderId, orderIds)
                        .eq(DesignReviewEntity::getReviewResult, StatusConstants.NO)
                        .eq(DesignReviewEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByDesc(DesignReviewEntity::getCreateTime));
        // 按 orderId 取最近一条驳回记录（LinkedHashMap 保证插入有序，putIfAbsent 保证只取最新）
        Map<Long, String> rejectReasonByOrderId = new LinkedHashMap<>();
        for (DesignReviewEntity review : allReviews) {
            rejectReasonByOrderId.putIfAbsent(review.getOrderId(), review.getRejectReason());
        }
        for (DesignWorkorderListVO vo : voList) {
            vo.setRejectReason(rejectReasonByOrderId.get(vo.getId()));
        }
    }

    /**
     * 获取工单最近一次驳回原因
     */
    private String getLatestRejectReason(Long orderId) {
        List<DesignReviewEntity> reviews = designReviewMapper.selectList(
                new LambdaQueryWrapper<DesignReviewEntity>()
                        .eq(DesignReviewEntity::getOrderId, orderId)
                        .eq(DesignReviewEntity::getReviewResult, StatusConstants.NO)
                        .eq(DesignReviewEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                        .orderByDesc(DesignReviewEntity::getCreateTime)
                        .last("LIMIT 1"));
        return reviews.isEmpty() ? null : reviews.get(0).getRejectReason();
    }

    /**
     * 构建详情页重建项目列表
     */
    private List<DesignWorkorderDetailVO.RebuildProjectItemVO> buildRebuildProjectList(Long orderId) {
        List<OrderItemEntity> items = orderItemService.listByOrderId(orderId);
        return items.stream()
                .map(item -> {
                    DesignWorkorderDetailVO.RebuildProjectItemVO projectVO = new DesignWorkorderDetailVO.RebuildProjectItemVO();
                    projectVO.setProjectName(item.getProjectName());
                    projectVO.setBodyPartName(item.getBodyPartName());
                    projectVO.setCategoryCode(item.getCategoryCode());
                    projectVO.setCategoryName(item.getCategoryName());
                    projectVO.setCount(1);
                    projectVO.setProjectDesc(item.getProjectDesc());
                    projectVO.setFormingRequirement(item.getFormingRequirement());
                    projectVO.setOtherRequirement(item.getOtherRequirement());
                    return projectVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建提交校验状态
     * 依次检查：数据包 → 打印信息 → 指令单 → 图纸 → 可视化模型 → 设计报告 → 图纸确认 → 指令单确认
     * <p>
     * 确认可通过两种路径实现：手动调用 confirm 接口（在线），或上传修订版文件（自动确认）。
     * 后端不区分模式，统一检查 is_confirmed 字段。
     * </p>
     *
     * @param orderId    订单ID
     * @param designMode 设计模式（前端展示用，后端校验不依赖此值）
     */
    private SubmitCheckVO buildSubmitCheck(Long orderId, Integer designMode) {
        SubmitCheckVO check = new SubmitCheckVO();

        // 1. 查询所有未删除数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .eq(DesignPackageEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        check.setHasPackage(!packages.isEmpty());

        Set<Long> packageIds = packages.stream()
                .map(DesignPackageEntity::getId)
                .collect(Collectors.toSet());

        List<DesignInstructionEntity> instructions = Collections.emptyList();
        List<DesignDrawingEntity> drawings = Collections.emptyList();

        if (!packages.isEmpty()) {
            // 2. 打印信息：每个数据包都有至少一条 design_product 记录
            List<DesignProductEntity> products = designProductMapper.selectList(
                    new LambdaQueryWrapper<DesignProductEntity>()
                            .in(DesignProductEntity::getPackageId, packageIds)
                            .eq(DesignProductEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithProduct = products.stream()
                    .map(DesignProductEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasPrintInfo(pkgsWithProduct.containsAll(packageIds));

            // 3. 指令单：每个数据包都有 design_instruction 记录
            instructions = designInstructionMapper.selectList(
                    new LambdaQueryWrapper<DesignInstructionEntity>()
                            .in(DesignInstructionEntity::getPackageId, packageIds)
                            .eq(DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithInstruction = instructions.stream()
                    .map(DesignInstructionEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasInstruction(pkgsWithInstruction.containsAll(packageIds));

            // 4. 图纸：每个数据包都有 design_drawing 记录
            drawings = designDrawingMapper.selectList(
                    new LambdaQueryWrapper<DesignDrawingEntity>()
                            .in(DesignDrawingEntity::getPackageId, packageIds)
                            .eq(DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithDrawing = drawings.stream()
                    .map(DesignDrawingEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasDrawing(pkgsWithDrawing.containsAll(packageIds));
        } else {
            // 无数据包时，后续所有检查均为 false
            check.setHasPrintInfo(false);
            check.setHasInstruction(false);
            check.setHasDrawing(false);
        }

        // 5. 可视化模型
        long modelCount = designModelMapper.selectCount(
                new LambdaQueryWrapper<DesignModelEntity>()
                        .eq(DesignModelEntity::getOrderId, orderId)
                        .eq(DesignModelEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        check.setHasModel(modelCount > 0);

        // 6. 设计报告（objectType = '10.5'）
        long reportCount = fileService.listByBiz(FileBizTypeEnum.DESIGN_REPORT.getDictCode(), orderId).size();
        check.setHasReport(reportCount > 0);

        // 7. 修订版文件校验已废弃——提交校验直接检查 isConfirmed（见下方步骤8/9）

        // 8. 图纸确认状态：复用步骤4已查询的 drawings，按 versionSeq 倒序取各包最新版
        if (!packages.isEmpty()) {
            Map<Long, DesignDrawingEntity> latestDrawingByPkg = new java.util.LinkedHashMap<>();
            drawings.stream()
                    .sorted((a, b) -> Integer.compare(b.getVersionSeq(), a.getVersionSeq()))
                    .forEach(d -> latestDrawingByPkg.putIfAbsent(d.getPackageId(), d));
            boolean allDrawingConfirmed = packageIds.stream().allMatch(pkgId -> {
                DesignDrawingEntity drawing = latestDrawingByPkg.get(pkgId);
                return drawing != null && Objects.equals(StatusConstants.CONFIRMED, drawing.getIsConfirmed());
            });
            check.setHasDrawingConfirmed(allDrawingConfirmed);
        } else {
            check.setHasDrawingConfirmed(true);
        }

        // 9. 指令单确认状态：复用步骤3已查询的 instructions，按 versionSeq 倒序取各包最新版
        if (!packages.isEmpty()) {
            Map<Long, DesignInstructionEntity> latestInstructionByPkg = new java.util.LinkedHashMap<>();
            instructions.stream()
                    .sorted((a, b) -> Integer.compare(b.getVersionSeq(), a.getVersionSeq()))
                    .forEach(i -> latestInstructionByPkg.putIfAbsent(i.getPackageId(), i));
            boolean allInstructionConfirmed = packageIds.stream().allMatch(pkgId -> {
                DesignInstructionEntity inst = latestInstructionByPkg.get(pkgId);
                return inst != null && Objects.equals(StatusConstants.CONFIRMED, inst.getIsConfirmed());
            });
            check.setHasInstructionConfirmed(allInstructionConfirmed);
        } else {
            check.setHasInstructionConfirmed(true);
        }

        // 计算 canSubmit 和 blockReason（按优先级顺序）
        if (!check.getHasPackage()) {
            check.setCanSubmit(false);
            check.setBlockReason("请先上传打印文件数据包");
        } else if (!check.getHasPrintInfo()) {
            check.setCanSubmit(false);
            check.setBlockReason("请完善数据包的打印信息");
        } else if (!check.getHasInstruction()) {
            check.setCanSubmit(false);
            check.setBlockReason("请生成指令单");
        } else if (!check.getHasDrawing()) {
            check.setCanSubmit(false);
            check.setBlockReason("请生成图纸");
        } else if (!Boolean.TRUE.equals(check.getHasDrawingConfirmed())) {
            check.setCanSubmit(false);
            check.setBlockReason("请确认图纸");
        } else if (!Boolean.TRUE.equals(check.getHasInstructionConfirmed())) {
            check.setCanSubmit(false);
            check.setBlockReason("请确认指令单");
        } else {
            check.setCanSubmit(true);
            check.setBlockReason(null);
        }

        return check;
    }

    /**
     * 从系统配置中读取当前设计模式，供前端展示 viewer 入口等 UI 决策使用
     * 后端提交校验不依赖此值，返回 null 时前端降级为离线 UI
     */
    private Integer getDesignMode() {
        try {
            String modeStr = configService.getConfigValue(SystemConfigKeyEnum.DESIGN_MODE.getKey());
            return modeStr != null ? Integer.parseInt(modeStr) : null;
        } catch (Exception e) {
            log.warn("读取设计模式配置失败，默认使用线下模式", e);
            return null;
        }
    }

    /**
     * 查询订单设计师分配历史
     *
     * @param orderId 订单ID
     * @return 分配历史列表
     */
    @Override
    public List<com.yigongbao.module.design.vo.DesignerAssignmentHistoryVO> listAssignmentHistory(Long orderId) {
        // 查询分配历史记录，按分配时间倒序
        List<com.yigongbao.module.order.entity.OrderDesignerAssignmentLogEntity> logs = assignmentLogMapper.selectList(
                new LambdaQueryWrapper<com.yigongbao.module.order.entity.OrderDesignerAssignmentLogEntity>()
                        .eq(com.yigongbao.module.order.entity.OrderDesignerAssignmentLogEntity::getOrderId, orderId)
                        .orderByDesc(com.yigongbao.module.order.entity.OrderDesignerAssignmentLogEntity::getAssignTime));
        // 转换为 VO
        return logs.stream().map(log -> {
            com.yigongbao.module.design.vo.DesignerAssignmentHistoryVO vo = new com.yigongbao.module.design.vo.DesignerAssignmentHistoryVO();
            cn.hutool.core.bean.BeanUtil.copyProperties(log, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 完成设计
     * 根据 needsPhysicalDelivery 标志执行不同的校验路径：
     * - 需要实体交付(needsPhysicalDelivery=1)：校验数据包、打印信息、指令单、图纸及确认状态
     * - 不需要实体交付(needsPhysicalDelivery=0)：只校验 STL 重建模型
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeDesign(Long orderId) {
        // 根据ID查询订单实体
        OrderMainEntity order = orderMainService.getById(orderId);
        // 校验订单是否存在
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 检查是否存在待审核的取消申请
        if (cancelApplyService.hasPendingCancelApply(orderId)) {
            log.warn("订单存在待审核的取消申请，不允许完成设计: orderId={}", orderId);
            throw new BusinessException(ErrorCodeEnum.ORDER_CANCEL_APPLY_PENDING);
        }

        // 校验订单状态必须为设计中
        if (!FlowStatusEnum.DESIGN_IN_PROGRESS.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许完成设计: orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 根据是否需要实体交付执行不同校验
        log.info("开始完成设计校验: orderId={}, orderType={}, needsPhysicalDelivery={}, currentStatus={}",
            orderId, order.getOrderType(), order.getNeedsPhysicalDelivery(), order.getStatus());

        if (Integer.valueOf(StatusConstants.YES).equals(order.getNeedsPhysicalDelivery())) {
            // 校验实体交付所需的完整数据
            log.info("执行实体交付校验: orderId={}", orderId);
            validatePhysicalDelivery(order, orderId);
        }

        // 获取当前操作用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userService.getById(currentUserId);
        String currentUserName = currentUser != null ? currentUser.getRealName() : null;

        // 执行状态流转：设计中 → 设计完成
        TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.COMPLETE_DESIGN,
                FlowOperator.of(currentUserId, currentUserName));

        // 回写订单表：更新阶段和状态
        OrderMainEntity update = new OrderMainEntity();
        update.setId(orderId);
        update.setPhase(result.getTargetPhase());
        update.setStatus(result.getFinalStatus());
        update.setDesignSubmitTime(LocalDateTime.now());
        update.setCurrentHandlerId(currentUserId);
        update.setCurrentHandlerName(currentUserName);
        orderMainService.updateById(update);

        // 发布设计完成事件，触发生产流转卡创建
        eventPublisher.publishEvent(new com.yigongbao.common.event.DesignCompletedEvent(this, orderId));

        log.info("完成设计: orderId={}, needsPhysicalDelivery={}, {} -> {}, userId={}",
                orderId, order.getNeedsPhysicalDelivery(),
                FlowStatusEnum.DESIGN_IN_PROGRESS.getName(), FlowStatusEnum.DESIGN_COMPLETED.getName(), currentUserId);
    }

    /**
     * 校验实体交付订单：数据包 → 打印信息 → 指令单 → 图纸 → 图纸确认 → 指令单确认
     */
    private void validatePhysicalDelivery(OrderMainEntity order, Long orderId) {
        // 查询所有未删除数据包
        List<DesignPackageEntity> packages = designPackageMapper.selectList(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .eq(DesignPackageEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        if (packages.isEmpty()) {
            // 非医疗器械订单允许不上传打印文件数据包
            if (OrderTypeEnum.NON_MEDICAL_DEVICE.getValue().equals(order.getOrderType())) {
                log.info("非医疗器械订单无打印文件数据包，跳过完成设计校验: orderId={}", orderId);
                return;
            }
            // 医疗器械订单必须上传打印文件
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请先上传打印文件数据包");
        }

        Set<Long> packageIds = packages.stream().map(DesignPackageEntity::getId).collect(Collectors.toSet());

        // 打印信息：每个数据包都有至少一条 design_product 记录
        List<DesignProductEntity> products = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .in(DesignProductEntity::getPackageId, packageIds)
                        .eq(DesignProductEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Set<Long> pkgsWithProduct = products.stream().map(DesignProductEntity::getPackageId).collect(Collectors.toSet());
        if (!pkgsWithProduct.containsAll(packageIds)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请完善数据包的打印信息");
        }

        // 指令单：每个数据包都有 design_instruction 记录
        List<DesignInstructionEntity> instructions = designInstructionMapper.selectList(
                new LambdaQueryWrapper<DesignInstructionEntity>()
                        .in(DesignInstructionEntity::getPackageId, packageIds)
                        .eq(DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Set<Long> pkgsWithInstruction = instructions.stream().map(DesignInstructionEntity::getPackageId).collect(Collectors.toSet());
        if (!pkgsWithInstruction.containsAll(packageIds)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请生成指令单");
        }

        // 图纸：每个数据包都有 design_drawing 记录
        List<DesignDrawingEntity> drawings = designDrawingMapper.selectList(
                new LambdaQueryWrapper<DesignDrawingEntity>()
                        .in(DesignDrawingEntity::getPackageId, packageIds)
                        .eq(DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED));
        Set<Long> pkgsWithDrawing = drawings.stream().map(DesignDrawingEntity::getPackageId).collect(Collectors.toSet());
        if (!pkgsWithDrawing.containsAll(packageIds)) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请生成图纸");
        }

        // 图纸确认状态：按 versionSeq 倒序取各包最新版
        Map<Long, DesignDrawingEntity> latestDrawingByPkg = new java.util.LinkedHashMap<>();
        drawings.stream()
                .sorted((a, b) -> Integer.compare(b.getVersionSeq(), a.getVersionSeq()))
                .forEach(d -> latestDrawingByPkg.putIfAbsent(d.getPackageId(), d));
        boolean allDrawingConfirmed = packageIds.stream().allMatch(pkgId -> {
            DesignDrawingEntity drawing = latestDrawingByPkg.get(pkgId);
            return drawing != null && Objects.equals(StatusConstants.CONFIRMED, drawing.getIsConfirmed());
        });
        if (!allDrawingConfirmed) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请确认图纸");
        }

        // 指令单确认状态：按 versionSeq 倒序取各包最新版
        Map<Long, DesignInstructionEntity> latestInstructionByPkg = new java.util.LinkedHashMap<>();
        instructions.stream()
                .sorted((a, b) -> Integer.compare(b.getVersionSeq(), a.getVersionSeq()))
                .forEach(i -> latestInstructionByPkg.putIfAbsent(i.getPackageId(), i));
        boolean allInstructionConfirmed = packageIds.stream().allMatch(pkgId -> {
            DesignInstructionEntity inst = latestInstructionByPkg.get(pkgId);
            return inst != null && Objects.equals(StatusConstants.CONFIRMED, inst.getIsConfirmed());
        });
        if (!allInstructionConfirmed) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, "请确认指令单");
        }
    }
}
