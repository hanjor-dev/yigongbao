package com.yigongbao.module.design.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
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

    /**
     * 分页查询设计工单列表
     * 按当前用户的数据权限范围过滤，固定查询设计阶段（phase=20）的工单
     *
     * @param queryDTO 查询参数
     * @return 分页工单列表
     */
    @Override
    public IPage<DesignWorkorderListVO> listWorkorders(DesignWorkorderQueryDTO queryDTO) {
        log.info("查询设计工单列表，queryDTO={}", queryDTO);

        // 获取当前用户信息和数据权限类型
        Long currentUserId = designQueryHelper.getCurrentUserId();
        UserEntity currentUser = designQueryHelper.getCurrentUser();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
        log.info("当前用户数据权限类型，userId={}，scopeType={}", currentUserId, scopeType);

        // 构建查询条件
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

        // 固定过滤：仅查询设计阶段订单（phase=20）
        wrapper.eq(OrderMainEntity::getPhase, 20);

        // 注入数据权限过滤（按 designer_id）
        designQueryHelper.buildDataScopeCondition(wrapper, currentUser, scopeType);

        // 动态筛选条件
        wrapper.like(StrUtil.isNotBlank(queryDTO.getOrderCode()), OrderMainEntity::getOrderCode, queryDTO.getOrderCode());
        wrapper.like(StrUtil.isNotBlank(queryDTO.getPatientName()), OrderMainEntity::getPatientName, queryDTO.getPatientName());
        wrapper.eq(queryDTO.getStatus() != null, OrderMainEntity::getStatus, queryDTO.getStatus());
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
        log.info("查询到工单数量，total={}", entityPage.getTotal());

        // 转换为列表 VO
        List<OrderMainEntity> entities = entityPage.getRecords();
        List<DesignWorkorderListVO> voList = entities.stream()
                .map(this::toWorkorderListVO)
                .collect(Collectors.toList());

        // 批量填充重建项目摘要（避免 N+1 问题）
        fillRebuildProjectSummary(voList);

        // 批量填充数据包数量
        fillPackageCount(voList);

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
        log.info("查询设计工单详情，orderId={}", orderId);

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
        log.info("获取用户设计列配置");
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
        log.info("保存用户设计列配置，userId={}", currentUserId);

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
            log.info("用户设计列配置保存成功，userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化列配置失败，userId={}", currentUserId, e);
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
        log.info("设计师开始设计，orderId={}", orderId);

        // 1. 校验订单存在
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
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

            log.info("开始设计成功，orderId={}, phase={}, status={}", orderId, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("开始设计异常，orderId={}", orderId, e);
            throw e;
        }
    }

    /**
     * 驳回后继续修改
     * 校验：订单状态必须为 DESIGN_REVIEW_REJECTED(2060)，且当前用户是分配设计师
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void continueDesign(Long orderId) {
        log.info("设计师继续修改，orderId={}", orderId);

        // 1. 校验订单存在
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 2. 校验订单状态（必须是设计审核不通过）
        if (!FlowStatusEnum.DESIGN_REVIEW_REJECTED.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许继续修改，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 3. 校验当前登录用户是该订单的分配设计师
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            log.warn("非分配设计师，无权继续修改，orderId={}, designerId={}, currentUserId={}",
                    orderId, order.getDesignerId(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
        }

        // 4. 查询当前用户姓名
        UserEntity currentUser = userService.getById(currentUserId);
        String currentUserName = currentUser != null ? currentUser.getRealName() : null;

        // 5. 执行状态流转：DESIGN_REVIEW_REJECTED → DESIGN_IN_PROGRESS
        try {
            TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.CONTINUE_DESIGN,
                    FlowOperator.of(currentUserId, currentUserName));

            // 6. 回写订单表
            OrderMainEntity update = new OrderMainEntity();
            update.setId(orderId);
            update.setPhase(result.getTargetPhase());
            update.setStatus(result.getFinalStatus());
            update.setCurrentHandlerId(currentUserId);
            update.setCurrentHandlerName(currentUserName);
            orderMainService.updateById(update);

            log.info("继续修改成功，orderId={}, phase={}, status={}",
                    orderId, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("继续修改异常，orderId={}", orderId, e);
            throw e;
        }
    }

    /**
     * 提交设计审核
     * 校验：订单状态必须为 DESIGN_IN_PROGRESS(2020)，且当前用户是分配设计师
     * 提交前执行完整的 7 项校验（线下模式下包括修订版文件校验）
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDesign(Long orderId) {
        log.info("设计师提交设计审核，orderId={}", orderId);

        // 1. 校验订单存在
        OrderMainEntity order = orderMainService.getById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 2. 校验订单状态（必须是设计中）
        if (!FlowStatusEnum.DESIGN_IN_PROGRESS.getValue().equals(order.getStatus())) {
            log.warn("订单状态不允许提交设计，orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(ErrorCodeEnum.ORDER_STATUS_ERROR);
        }

        // 3. 校验当前登录用户是该订单的分配设计师
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!currentUserId.equals(order.getDesignerId())) {
            log.warn("非分配设计师，无权提交设计，orderId={}, designerId={}, currentUserId={}",
                    orderId, order.getDesignerId(), currentUserId);
            throw new BusinessException(ErrorCodeEnum.ORDER_DESIGNER_MISMATCH);
        }

        // 4. 执行提交前完整校验（含修订版文件检查）
        Integer designMode = getDesignMode();
        SubmitCheckVO check = buildSubmitCheck(orderId, designMode);
        if (!Boolean.TRUE.equals(check.getCanSubmit())) {
            log.warn("提交设计校验未通过，orderId={}, blockReason={}", orderId, check.getBlockReason());
            throw new BusinessException(ErrorCodeEnum.DESIGN_SUBMIT_CHECK_FAILED, check.getBlockReason());
        }

        // 5. 查询当前用户姓名
        UserEntity currentUser = userService.getById(currentUserId);
        String currentUserName = currentUser != null ? currentUser.getRealName() : null;

        // 6. 执行状态流转：DESIGN_IN_PROGRESS → DESIGN_REVIEWING(2040)
        try {
            TransitionResult result = flowFacade.executeFlow(orderId, FlowActionEnum.SUBMIT_DESIGN,
                    FlowOperator.of(currentUserId, currentUserName));

            // 7. 回写订单表（含设计提交时间）
            OrderMainEntity update = new OrderMainEntity();
            update.setId(orderId);
            update.setPhase(result.getTargetPhase());
            update.setStatus(result.getFinalStatus());
            update.setDesignSubmitTime(LocalDateTime.now());
            update.setCurrentHandlerId(currentUserId);
            update.setCurrentHandlerName(currentUserName);
            orderMainService.updateById(update);

            log.info("提交设计审核成功，orderId={}, phase={}, status={}",
                    orderId, result.getTargetPhase(), result.getFinalStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交设计审核异常，orderId={}", orderId, e);
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
            List<DesignInstructionEntity> instructions = designInstructionMapper.selectList(
                    new LambdaQueryWrapper<DesignInstructionEntity>()
                            .in(DesignInstructionEntity::getPackageId, packageIds)
                            .eq(DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED));
            Set<Long> pkgsWithInstruction = instructions.stream()
                    .map(DesignInstructionEntity::getPackageId)
                    .collect(Collectors.toSet());
            check.setHasInstruction(pkgsWithInstruction.containsAll(packageIds));

            // 4. 图纸：每个数据包都有 design_drawing 记录
            List<DesignDrawingEntity> drawings = designDrawingMapper.selectList(
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

        // 8. 图纸确认状态：所有数据包的最新版图纸都必须已确认（is_confirmed=1）
        if (!packages.isEmpty()) {
            List<DesignDrawingEntity> allDrawingsForConfirm = designDrawingMapper.selectList(
                    new LambdaQueryWrapper<DesignDrawingEntity>()
                            .in(DesignDrawingEntity::getPackageId, packageIds)
                            .eq(DesignDrawingEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                            .orderByDesc(DesignDrawingEntity::getVersionSeq));
            Map<Long, DesignDrawingEntity> latestDrawingByPkg = new java.util.LinkedHashMap<>();
            for (DesignDrawingEntity drawing : allDrawingsForConfirm) {
                latestDrawingByPkg.putIfAbsent(drawing.getPackageId(), drawing);
            }
            boolean allDrawingConfirmed = packageIds.stream().allMatch(pkgId -> {
                DesignDrawingEntity drawing = latestDrawingByPkg.get(pkgId);
                return drawing != null && Integer.valueOf(1).equals(drawing.getIsConfirmed());
            });
            check.setHasDrawingConfirmed(allDrawingConfirmed);
        } else {
            check.setHasDrawingConfirmed(true);
        }

        // 9. 指令单确认状态：所有数据包的最新版指令单都必须已确认（is_confirmed=1）
        if (!packages.isEmpty()) {
            List<DesignInstructionEntity> allInstructionsForConfirm = designInstructionMapper.selectList(
                    new LambdaQueryWrapper<DesignInstructionEntity>()
                            .in(DesignInstructionEntity::getPackageId, packageIds)
                            .eq(DesignInstructionEntity::getIsDeleted, StatusConstants.NOT_DELETED)
                            .orderByDesc(DesignInstructionEntity::getVersionSeq));
            Map<Long, DesignInstructionEntity> latestInstructionByPkg = new java.util.LinkedHashMap<>();
            for (DesignInstructionEntity inst : allInstructionsForConfirm) {
                latestInstructionByPkg.putIfAbsent(inst.getPackageId(), inst);
            }
            boolean allInstructionConfirmed = packageIds.stream().allMatch(pkgId -> {
                DesignInstructionEntity inst = latestInstructionByPkg.get(pkgId);
                return inst != null && Integer.valueOf(1).equals(inst.getIsConfirmed());
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
        } else if (!check.getHasModel()) {
            check.setCanSubmit(false);
            check.setBlockReason("请上传可视化模型文件");
        } else if (!check.getHasReport()) {
            check.setCanSubmit(false);
            check.setBlockReason("请上传设计报告");
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
}
