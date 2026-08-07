package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.enums.RoleCodeEnum;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowPhaseEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.enums.DeviceTypeEnum;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.record.vo.*;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.AssignProductWeightDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.ProductLedgerExportDTO;
import com.yigongbao.module.production.record.dto.SaveProductionColumnConfigDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import com.yigongbao.common.event.ProductionCardClaimedEvent;
import com.yigongbao.module.production.device.service.IDeviceUsageCounterService;
import com.yigongbao.module.production.product.service.IProductNumberService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.service.UserService;
import com.yigongbao.module.system.user.service.UserHospitalService;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产流转卡服务实现
 * 负责流转卡的查询、状态流转、设备分配、批号管理，以及 Flow 聚合触发逻辑
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionRecordServiceImpl extends ServiceImpl<ProductionRecordMapper, ProductionRecordEntity>
        implements IProductionRecordService {

    private final CodeGeneratorService codeGeneratorService;
    private final DesignPackageMapper designPackageMapper;
    private final DesignInstructionMapper designInstructionMapper;
    private final DesignDrawingMapper designDrawingMapper;
    private final OrderMainMapper orderMainMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final FlowFacade flowFacade;
    private final FlowCardExcelBuilder flowCardExcelBuilder;
    private final com.yigongbao.module.production.helper.ProductLedgerExcelBuilder productLedgerExcelBuilder;
    private final com.yigongbao.module.basic.file.service.FileService fileService;
    private final ConfigService configService;
    private final UserService userService;
    private final UserHospitalService userHospitalService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IDeviceUsageCounterService deviceUsageCounterService;
    private final IProductNumberService productNumberService;

    private static final List<Integer> NORMAL_PRODUCTION_STATUSES = List.of(
            FlowStatusEnum.DESIGN_COMPLETED.getValue(),
            FlowStatusEnum.PENDING_PRINT.getValue(),
            FlowStatusEnum.PRINTING.getValue(),
            FlowStatusEnum.PRINT_COMPLETED.getValue(),
            FlowStatusEnum.POST_PROCESSING.getValue(),
            FlowStatusEnum.QC_IN_PROGRESS.getValue(),
            FlowStatusEnum.PACKING.getValue(),
            FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(),
            FlowStatusEnum.WAREHOUSED.getValue(),
            FlowStatusEnum.WAREHOUSE_OUT.getValue(),
            FlowStatusEnum.COMPLETED.getValue()
    );

    private static final Map<Integer, FlowActionEnum> ACTION_TO_REACH_STATUS = Map.of(
            FlowStatusEnum.PENDING_PRINT.getValue(), FlowActionEnum.DOWNLOAD_DATA_PACKAGE,
            FlowStatusEnum.PRINTING.getValue(), FlowActionEnum.START_PRINT,
            FlowStatusEnum.PRINT_COMPLETED.getValue(), FlowActionEnum.COMPLETE_PRINT,
            FlowStatusEnum.POST_PROCESSING.getValue(), FlowActionEnum.START_POST_PROCESSING,
            FlowStatusEnum.QC_IN_PROGRESS.getValue(), FlowActionEnum.COMPLETE_POST_PROCESSING,
            FlowStatusEnum.PACKING.getValue(), FlowActionEnum.QC_PASS,
            FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue(), FlowActionEnum.COMPLETE_PACKING,
            FlowStatusEnum.WAREHOUSED.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_IN,
            FlowStatusEnum.WAREHOUSE_OUT.getValue(), FlowActionEnum.COMPLETE_WAREHOUSE_OUT
    );

    /** 查询流转卡详情，包含产品列表、当前工序中文名和设计文件信息 */
    @Override
    public ProductionRecordVO getRecordDetail(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        ProductionRecordVO vo = new ProductionRecordVO();
        BeanUtil.copyProperties(record, vo);
        fillCurrentProcessName(vo);
        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, id));
        vo.setProducts(products.stream().map(this::toProductVO).collect(Collectors.toList()));
        fillDesignFiles(vo, record);
        fillFlowCardFile(vo, record);
        return vo;
    }

    private void fillDesignFiles(ProductionRecordVO vo, ProductionRecordEntity record) {
        Long designPackageId = record.getDesignPackageId();
        if (designPackageId == null) return;
        DesignPackageEntity pkg = designPackageMapper.selectById(designPackageId);
        if (pkg != null) {
            com.yigongbao.module.basic.file.vo.FileVO pkgFile = new com.yigongbao.module.basic.file.vo.FileVO();
            pkgFile.setFileName(pkg.getFileName());
            pkgFile.setFileUrl(pkg.getFileUrl());
            pkgFile.setFileSize(pkg.getFileSize());
            vo.setDataPackageFile(pkgFile);

            DesignInstructionEntity instruction = designInstructionMapper.selectOne(
                    new LambdaQueryWrapper<DesignInstructionEntity>()
                            .eq(DesignInstructionEntity::getPackageId, designPackageId)
                            .orderByDesc(DesignInstructionEntity::getVersionSeq)
                            .last("LIMIT 1"));
            if (instruction != null) {
                String url = cn.hutool.core.util.StrUtil.isNotBlank(instruction.getRevisedFileUrl())
                        ? instruction.getRevisedFileUrl() : instruction.getTemplateFileUrl();
                com.yigongbao.module.basic.file.vo.FileVO instrFile = new com.yigongbao.module.basic.file.vo.FileVO();
                instrFile.setFileUrl(url);
                vo.setInstructionFile(instrFile);
            }

            DesignDrawingEntity drawing = designDrawingMapper.selectOne(
                    new LambdaQueryWrapper<DesignDrawingEntity>()
                            .eq(DesignDrawingEntity::getPackageId, designPackageId)
                            .eq(DesignDrawingEntity::getProductCategory, record.getProductCategory())
                            .orderByDesc(DesignDrawingEntity::getVersionSeq)
                            .last("LIMIT 1"));
            if (drawing != null) {
                String url = cn.hutool.core.util.StrUtil.isNotBlank(drawing.getRevisedFileUrl())
                        ? drawing.getRevisedFileUrl() : drawing.getTemplateFileUrl();
                com.yigongbao.module.basic.file.vo.FileVO drawFile = new com.yigongbao.module.basic.file.vo.FileVO();
                drawFile.setFileUrl(url);
                vo.setDrawingFile(drawFile);
            }
        }
    }

    /** 填充流转卡Excel文件，使用缓存机制避免重复生成 */
    private void fillFlowCardFile(ProductionRecordVO vo, ProductionRecordEntity record) {
        boolean needRegenerate = record.getFlowCardFileUrl() == null
                || record.getFlowCardGenerateTime() == null
                || record.getContentUpdateTime() == null
                || record.getContentUpdateTime().isAfter(record.getFlowCardGenerateTime());

        if (needRegenerate) {
            com.yigongbao.module.basic.file.vo.FileVO fileVO = generateFlowCardExcel(record.getId());
            vo.setFlowCardFile(fileVO);
        } else {
            // 获取订单信息，添加患者姓名前缀
            OrderMainEntity order = orderMainMapper.selectById(record.getOrderId());
            String patientName = (order != null && order.getPatientName() != null) ? order.getPatientName() : "";
            com.yigongbao.module.basic.file.vo.FileVO fileVO = new com.yigongbao.module.basic.file.vo.FileVO();
            fileVO.setFileUrl(record.getFlowCardFileUrl());
            fileVO.setFileName(patientName + "流转卡.xlsx");
            vo.setFlowCardFile(fileVO);
        }
    }

    /** 通过流转卡编号查询详情（扫码入口） */
    @Override
    public ProductionRecordVO getByRecordNo(String recordNo) {
        ProductionRecordEntity record = getOne(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getRecordNo, recordNo));
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return getRecordDetail(record.getId());
    }

    /** 获取流转卡二维码内容 */
    @Override
    public String getQrCodeUrl(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return record.getQrCodeUrl();
    }

    /** 分页查询流转卡列表；支持关键字和时间范围过滤，批量关联查询避免 N+1 */
    @Override
    public IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(currentUserId);

        Page<ProductionRecordEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<ProductionRecordEntity>()
                .orderByDesc(ProductionRecordEntity::getCreateTime);
        if (dto.getStatuses() != null && !dto.getStatuses().isEmpty()) {
            wrapper.in(ProductionRecordEntity::getStatus, dto.getStatuses());
        } else if (dto.getStatus() != null) {
            // 根据 includeFollowingStatuses 决定查询方式
            if (Boolean.TRUE.equals(dto.getIncludeFollowingStatuses())) {
                // 质检接口：使用 >= 范围查询（包含后续阶段，排除已取消）
                wrapper.ge(ProductionRecordEntity::getStatus, dto.getStatus())
                       .lt(ProductionRecordEntity::getStatus, FlowStatusEnum.CANCELLED.getValue());
            } else {
                // 生产流转卡列表：精确匹配
                wrapper.eq(ProductionRecordEntity::getStatus, dto.getStatus());
            }
        }
        // 数据权限过滤
        if (dto.getProcessingCenterId() != null) {
            // 前端或管理员指定加工中心时精确过滤
            wrapper.eq(ProductionRecordEntity::getProcessingCenterId, dto.getProcessingCenterId());
        } else {
            // 使用统一数据权限机制
            DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);
            buildDataScopeCondition(wrapper, currentUser, scopeType);
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            String kw = dto.getKeyword();
            wrapper.and(w -> w
                    .like(ProductionRecordEntity::getOrderCode, kw)
                    .or().like(ProductionRecordEntity::getDesignPackageCode, kw)
                    .or().like(ProductionRecordEntity::getPatientName, kw));
        }
        // 按订单创建时间范围过滤（join order_main）
        if (dto.getOrderCreateTimeStart() != null || dto.getOrderCreateTimeEnd() != null) {
            List<Long> orderIds = orderMainMapper.selectList(
                    new LambdaQueryWrapper<OrderMainEntity>()
                            .ge(dto.getOrderCreateTimeStart() != null, OrderMainEntity::getCreateTime, dto.getOrderCreateTimeStart())
                            .le(dto.getOrderCreateTimeEnd() != null, OrderMainEntity::getCreateTime, dto.getOrderCreateTimeEnd())
                            .select(OrderMainEntity::getId))
                    .stream().map(OrderMainEntity::getId).collect(Collectors.toList());
            if (orderIds.isEmpty()) {
                return page.convert(e -> new ProductionRecordVO());
            }
            wrapper.in(ProductionRecordEntity::getOrderId, orderIds);
        }
        Page<ProductionRecordEntity> result = page(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return result.convert(e -> new ProductionRecordVO());
        }
        // 批量查询关联数据，避免 N+1
        List<Long> recordIds = result.getRecords().stream()
                .map(ProductionRecordEntity::getId).collect(Collectors.toList());
        List<ProductionProductEntity> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .in(ProductionProductEntity::getProductionRecordId, recordIds));
        java.util.Map<Long, List<ProductionProductVO>> productMap = allProducts.stream()
                .collect(Collectors.groupingBy(
                        ProductionProductEntity::getProductionRecordId,
                        Collectors.mapping(p -> toProductVO(p), Collectors.toList())));

        // 批量查询订单信息
        List<Long> orderIdsForBatch = result.getRecords().stream()
                .map(ProductionRecordEntity::getOrderId).distinct().collect(Collectors.toList());
        java.util.Map<Long, OrderMainEntity> orderMap = orderMainMapper.selectList(
                new LambdaQueryWrapper<OrderMainEntity>().in(OrderMainEntity::getId, orderIdsForBatch))
                .stream().collect(Collectors.toMap(OrderMainEntity::getId, o -> o, (a, b) -> a));

        // 批量查询生产员姓名（producerId → realName）
        java.util.Set<Long> producerIds = orderMap.values().stream()
                .filter(o -> o.getProducerId() != null)
                .map(OrderMainEntity::getProducerId).collect(Collectors.toSet());
        java.util.Map<Long, String> producerNameMap = producerIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<UserEntity>().in(UserEntity::getId, producerIds))
                        .stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));

        return result.convert(e -> {
            ProductionRecordVO vo = new ProductionRecordVO();
            BeanUtil.copyProperties(e, vo);
            fillCurrentProcessName(vo);
            vo.setProducts(productMap.getOrDefault(e.getId(), java.util.Collections.emptyList()));
            OrderMainEntity order = orderMap.get(e.getOrderId());
            if (order != null) {
                vo.setOrderStatus(order.getStatus());
                vo.setOrderPhase(order.getPhase());
                vo.setOrgName(order.getOrgName());
                vo.setOperatorName(order.getOperatorName());
                vo.setOperatorPhone(order.getOperatorPhone());
                vo.setAreaName(order.getAreaName());
                vo.setFullAreaName(order.getFullAreaName());
                vo.setOperatorDeptName(order.getOperatorDeptName());
                vo.setPatientAge(order.getPatientAge());
                vo.setPatientGender(order.getPatientGender());
                vo.setPostalAddress(order.getPostalAddress());
                vo.setDesignerName(order.getDesignerName());
                vo.setEstimatedCost(order.getEstimatedCost());
                vo.setActualCompleteTime(order.getActualCompleteTime());
                if (order.getProducerId() != null) {
                    vo.setProducerName(producerNameMap.get(order.getProducerId()));
                }
            }
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public String downloadDataPackage(Long recordId) {
        // 查询本条流转卡（用于发布事件和后续状态更新）- 使用悲观锁防止并发认领
        ProductionRecordEntity record = baseMapper.selectOne(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getId, recordId)
                .last("FOR UPDATE"));

        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        Long designPackageId = record.getDesignPackageId();
        DesignPackageEntity designPackage = designPackageMapper.selectById(designPackageId);
        if (designPackage == null) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        OrderMainEntity order = orderMainMapper.selectById(designPackage.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }

        // 回写订单操作人和加工中心信息（当前生产员）
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(userId);
        String realName = currentUser != null ? currentUser.getRealName() : null;

        // 更新本条流转卡状态和生产员信息：DESIGN_COMPLETED → PENDING_PRINT（幂等）
        int updated = baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getId, recordId)
                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.DESIGN_COMPLETED.getValue())
                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PENDING_PRINT.getValue())
                .set(ProductionRecordEntity::getProducerId, userId)
                .set(ProductionRecordEntity::getProducerName, realName));

        // 检查更新结果，如果影响行数为0，说明流转卡已被其他用户认领
        if (updated == 0) {
            log.warn("流转卡已被其他用户认领: recordId={}, userId={}", recordId, userId);
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_ALREADY_CLAIMED);
        }

        // 订单归属加工中心：首次下载时确定（使用条件更新保证幂等）
        if (currentUser != null && currentUser.getCenterId() != null) {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrderMainEntity> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            updateWrapper.eq(OrderMainEntity::getId, order.getId())
                .isNull(OrderMainEntity::getCenterId)  // 仅在未分配时更新
                .set(OrderMainEntity::getCenterId, currentUser.getCenterId())
                .set(OrderMainEntity::getCenterName, currentUser.getCenterName())
                .set(OrderMainEntity::getCurrentHandlerId, userId)
                .set(OrderMainEntity::getCurrentHandlerName, realName)
                .set(OrderMainEntity::getProducerId, userId);

            int orderUpdated = orderMainMapper.update(null, updateWrapper);
            if (orderUpdated > 0) {
                log.info("订单归属加工中心: orderId={}, centerId={}, centerName={}, producerId={}",
                    order.getId(), currentUser.getCenterId(), currentUser.getCenterName(), userId);
            } else {
                // 已被其他事务分配，仅更新操作人信息
                OrderMainEntity orderUpdate = new OrderMainEntity();
                orderUpdate.setId(order.getId());
                orderUpdate.setCurrentHandlerId(userId);
                orderUpdate.setCurrentHandlerName(realName);
                orderUpdate.setProducerId(userId);
                orderMainMapper.updateById(orderUpdate);
                log.info("订单已归属其他加工中心，仅更新操作人: orderId={}, producerId={}", order.getId(), userId);
            }
        } else {
            // 用户未绑定加工中心，仅更新操作人信息
            OrderMainEntity orderUpdate = new OrderMainEntity();
            orderUpdate.setId(order.getId());
            orderUpdate.setCurrentHandlerId(userId);
            orderUpdate.setCurrentHandlerName(realName);
            orderUpdate.setProducerId(userId);
            orderMainMapper.updateById(orderUpdate);
            log.warn("生产员未绑定加工中心，跳过订单加工中心分配: orderId={}, userId={}", order.getId(), userId);
        }

        log.info("下载设计数据包，流转卡推进到待打印: recordId={}, orderId={}, designPackageId={}, producerId={}",
            recordId, order.getId(), designPackageId, userId);

        // 发布流转卡认领事件
        if (record != null) {
            eventPublisher.publishEvent(new ProductionCardClaimedEvent(this, record.getId(), userId, realName));
        }

        // 聚合逻辑：检查是否所有流转卡都已下载，如果是则通过状态机推进订单状态
        if (order.getStatus().equals(FlowStatusEnum.DESIGN_COMPLETED.getValue())) {
            triggerFlowIfAllReach(order.getId(), FlowStatusEnum.PENDING_PRINT.getValue(),
                    FlowActionEnum.DOWNLOAD_DATA_PACKAGE);
        }

        return designPackage.getFileUrl();
    }

    @Override
    public void triggerFlowIfAllReach(Long orderId, Integer requiredStatus, FlowActionEnum action) {
        long totalActive = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .notIn(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.PRINT_FAILED.getValue(),
                        FlowStatusEnum.REWORK.getValue(),
                        FlowStatusEnum.CANCELLED.getValue()));
        if (totalActive == 0) {
            return;
        }
        List<Integer> reachedStatuses = getReachedOrBeyondStatuses(requiredStatus);
        long reachedCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .in(ProductionRecordEntity::getStatus, reachedStatuses));
        if (totalActive == reachedCount) {
            try {
                triggerFlowAndSync(orderId, action);
                log.info("聚合条件满足，触发Flow: orderId={}, requiredStatus={}, action={}", orderId, requiredStatus, action);
            } catch (BusinessException e) {
                log.info("Flow状态流转被拒绝（可能已被并发触发）: orderId={}, action={}, reason={}", orderId, action, e.getMessage(), e);
            }
        } else {
            log.info("聚合条件未满足，暂不触发Flow: orderId={}, requiredStatus={}, active={}, reached={}",
                    orderId, requiredStatus, totalActive, reachedCount);
        }
    }

    @Override
    public void triggerFlowIfAllExact(Long orderId, Integer exactStatus, FlowActionEnum action) {
        long totalActive = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .notIn(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.PRINT_FAILED.getValue(),
                        FlowStatusEnum.REWORK.getValue(),
                        FlowStatusEnum.CANCELLED.getValue()));
        if (totalActive == 0) {
            return;
        }
        long matchCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .eq(ProductionRecordEntity::getStatus, exactStatus));
        if (totalActive == matchCount) {
            try {
                triggerFlowAndSync(orderId, action);
                log.info("聚合条件满足（精确匹配），触发Flow: orderId={}, exactStatus={}, action={}", orderId, exactStatus, action);
            } catch (BusinessException e) {
                log.info("Flow状态流转被拒绝（可能已被并发触发）: orderId={}, action={}, reason={}", orderId, action, e.getMessage(), e);
            }
        } else {
            log.info("聚合条件未满足，暂不触发Flow: orderId={}, exactStatus={}, active={}, matched={}",
                    orderId, exactStatus, totalActive, matchCount);
        }
    }

    @Override
    public void reconcileOrderProductionStatus(Long orderId) {
        if (orderId == null) {
            return;
        }
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null || order.getStatus() == null) {
            return;
        }

        List<ProductionRecordEntity> records = list(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId));
        Integer targetStatus = records.stream()
                .map(ProductionRecordEntity::getStatus)
                .filter(this::isActiveNormalProductionStatus)
                .min(Comparator.comparingInt(NORMAL_PRODUCTION_STATUSES::indexOf))
                .orElse(null);
        if (targetStatus == null) {
            log.info("订单无有效生产流转卡，跳过状态补偿: orderId={}", orderId);
            return;
        }

        int currentIndex = NORMAL_PRODUCTION_STATUSES.indexOf(order.getStatus());
        int targetIndex = NORMAL_PRODUCTION_STATUSES.indexOf(targetStatus);
        if (currentIndex < 0 || targetIndex < 0 || currentIndex >= targetIndex) {
            return;
        }

        for (int i = currentIndex + 1; i <= targetIndex; i++) {
            Integer nextStatus = NORMAL_PRODUCTION_STATUSES.get(i);
            FlowActionEnum action = ACTION_TO_REACH_STATUS.get(nextStatus);
            if (action == null) {
                log.warn("订单状态补偿缺少Flow动作，停止补偿: orderId={}, currentStatus={}, targetStatus={}, nextStatus={}",
                        orderId, order.getStatus(), targetStatus, nextStatus);
                return;
            }
            if (!triggerFlowAndSyncSafely(orderId, action)) {
                log.info("订单状态补偿中止: orderId={}, action={}, nextStatus={}, aggregateTargetStatus={}",
                        orderId, action, nextStatus, targetStatus);
                return;
            }
            log.info("订单状态补偿触发Flow成功: orderId={}, action={}, nextStatus={}, aggregateTargetStatus={}",
                    orderId, action, nextStatus, targetStatus);
        }
    }

    /** 返回已达到或超过指定状态的所有状态码（状态机单向推进） */
    private List<Integer> getReachedOrBeyondStatuses(Integer requiredStatus) {
        int idx = NORMAL_PRODUCTION_STATUSES.indexOf(requiredStatus);
        if (idx < 0) {
            return List.of(requiredStatus);
        }
        return NORMAL_PRODUCTION_STATUSES.subList(idx, NORMAL_PRODUCTION_STATUSES.size());
    }

    private boolean isActiveNormalProductionStatus(Integer status) {
        if (status == null) {
            return false;
        }
        if (FlowStatusEnum.PRINT_FAILED.getValue().equals(status)
                || FlowStatusEnum.REWORK.getValue().equals(status)
                || FlowStatusEnum.CANCELLED.getValue().equals(status)) {
            return false;
        }
        return NORMAL_PRODUCTION_STATUSES.contains(status);
    }

    @Override
    public String generateBatchNo(Long recordId) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        // 预览生成，不写库
        return codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO);
    }

    /** 提交生产批号和原材料批号，写入流转卡 */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public void submitBatchNo(Long recordId, SubmitBatchNoDTO dto) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        record.setProductionBatchNo(dto.getProductionBatchNo());
        record.setMaterialBatchNo(dto.getMaterialBatchNo());
        record.setContentUpdateTime(java.time.LocalDateTime.now());
        updateById(record);
        log.info("提交生产批号: recordId={}, batchNo={}", recordId, dto.getProductionBatchNo());
    }

    /** 获取流转卡的设备配置信息（已分配的打印机编号、名称等） */
    @Override
    public DeviceConfigVO getDeviceConfig(Long recordId) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        DeviceConfigVO vo = new DeviceConfigVO();
        BeanUtil.copyProperties(record, vo);
        ProductionProcessEntity printProcess = processMapper.selectOne(
                new LambdaQueryWrapper<ProductionProcessEntity>()
                        .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                        .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode()));
        if (printProcess != null) {
            vo.setPrintParams(printProcess.getProcessParams());
        }
        return vo;
    }

    /** 按加工中心分组返回打印机列表；生产员只能看到自己绑定加工中心的设备 */
    @Override
    public List<ProcessingCenterPrintersVO> listPrinters() {
        // 1. 获取当前登录用户
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(userId);

        // 2. 查询设备列表（根据角色权限过滤）
        List<DeviceEntity> devices;
        if (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(currentUser.getRoleCode())) {
            // 生产员：只查询自己绑定的加工中心下的设备
            if (currentUser.getCenterId() == null) {
                log.warn("生产员未绑定加工中心: userId={}", userId);
                return Collections.emptyList();
            }
            devices = deviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceType, DeviceTypeEnum.PRINTER_SLA.getCode())
                .eq(DeviceEntity::getCenterId, currentUser.getCenterId())
                .eq(DeviceEntity::getIsDeleted, StatusConstants.NO));
        } else {
            // 其他角色：查询所有打印机
            devices = deviceMapper.selectList(
                new LambdaQueryWrapper<DeviceEntity>()
                    .eq(DeviceEntity::getDeviceType, DeviceTypeEnum.PRINTER_SLA.getCode())
                    .eq(DeviceEntity::getIsDeleted, StatusConstants.NO));
        }

        // 3. 转换为 PrinterVO 并按加工中心分组
        Map<Long, List<PrinterVO>> centerPrintersMap = devices.stream()
            .map(d -> {
                PrinterVO vo = new PrinterVO();
                vo.setId(d.getId());
                vo.setDeviceNo(d.getDeviceId());
                vo.setDeviceName(d.getDeviceName());
                int statusCode = resolveDeviceStatus(d);
                vo.setStatus(statusCode);
                vo.setStatusName(statusCode == 0 ? "空闲" : "占用");
                return new Object[]{d.getCenterId(), d.getCenterName(), vo};
            })
            .collect(Collectors.groupingBy(
                arr -> (Long) arr[0],
                Collectors.mapping(arr -> (PrinterVO) arr[2], Collectors.toList())
            ));

        // 4. 构建返回结果（保留加工中心名称）
        Map<Long, String> centerNameMap = devices.stream()
            .collect(Collectors.toMap(
                DeviceEntity::getCenterId,
                DeviceEntity::getCenterName,
                (v1, v2) -> v1
            ));

        return centerPrintersMap.entrySet().stream()
            .map(entry -> {
                ProcessingCenterPrintersVO vo = new ProcessingCenterPrintersVO();
                vo.setCenterId(entry.getKey());
                vo.setCenterName(centerNameMap.get(entry.getKey()));
                vo.setPrinters(entry.getValue());
                return vo;
            })
            .sorted(Comparator.comparing(ProcessingCenterPrintersVO::getCenterId))
            .collect(Collectors.toList());
    }

    /** 为流转卡分配打印机；校验流转卡状态为待打印、设备在线且空闲，同步更新打印工序记录 */
    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public void assignDevice(Long recordId, AssignDeviceDTO dto) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        DeviceEntity device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND);
        }
        // 校验流转卡状态：只有 PENDING_PRINT 才能分配打印机
        if (!FlowStatusEnum.PENDING_PRINT.getValue().equals(record.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_ASSIGN_DEVICE);
        }
        // 校验设备在线且未被占用
        if (resolveDeviceStatus(device) != 0) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }

        // 检查是否有其他流转卡正在使用该设备（悲观锁防止并发分配）
        ProductionRecordEntity conflictRecord = baseMapper.selectOne(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getPrintDeviceId, device.getId())
                .in(ProductionRecordEntity::getStatus, List.of(
                        FlowStatusEnum.PENDING_PRINT.getValue(),
                        FlowStatusEnum.PRINTING.getValue()))
                .ne(ProductionRecordEntity::getId, recordId)
                .select(ProductionRecordEntity::getId)
                .last("LIMIT 1 FOR UPDATE"));
        if (conflictRecord != null) {
            log.warn("设备已被其他流转卡占用: deviceId={}, conflictRecordId={}, currentRecordId={}",
                device.getId(), conflictRecord.getId(), recordId);
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }

        saveProductWeights(recordId, dto.getProductWeights());

        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        if (dto.getMaterial() != null) {
            record.setMaterial(dto.getMaterial());
        }
        record.setContentUpdateTime(java.time.LocalDateTime.now());
        updateById(record);
        Long userId = StpUtil.getLoginIdAsLong();
        com.yigongbao.module.system.user.entity.UserEntity currentUser = userMapper.selectById(userId);
        String realName = currentUser != null ? currentUser.getRealName() : null;
        processMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .set(ProductionProcessEntity::getDeviceId, device.getId())
                .set(ProductionProcessEntity::getDeviceNo, device.getDeviceId())
                .set(ProductionProcessEntity::getDeviceName, device.getDeviceName())
                .set(ProductionProcessEntity::getOperatorId, userId)
                .set(ProductionProcessEntity::getOperatorName, realName)
                .set(dto.getPrintParams() != null, ProductionProcessEntity::getProcessParams, dto.getPrintParams()));

        // 累加设备当日上机次数并生成正式产品编号
        Integer usageCount = deviceUsageCounterService.incrementAndGet(device.getId());
        productNumberService.generateFormalNumbers(recordId, device.getId(), usageCount);

        log.info("分配打印设备并生成产品编号: recordId={}, deviceId={}, deviceNo={}, usageCount={}",
            recordId, device.getId(), device.getDeviceId(), usageCount);
    }

    /** 校验并保存当前流转卡下所有生产产品的重量，单位：克 */
    private void saveProductWeights(Long recordId, List<AssignProductWeightDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品重量列表不能为空");
        }

        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId));
        if (products == null || products.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "流转卡关联产品不能为空");
        }

        Map<Long, ProductionProductEntity> productMap = products.stream()
                .collect(Collectors.toMap(ProductionProductEntity::getId, product -> product));
        Set<Long> submittedProductIds = new HashSet<>();
        for (AssignProductWeightDTO request : requests) {
            if (request == null || request.getProductId() == null) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品ID不能为空");
            }
            if (!submittedProductIds.add(request.getProductId())) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品重量不能重复提交");
            }

            ProductionProductEntity product = productMap.get(request.getProductId());
            if (product == null) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品不属于当前流转卡");
            }

            BigDecimal weight = request.getWeight();
            if (weight != null) {
                if (weight.signum() < 0) {
                    throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品重量不能小于0克");
                }
                if (weight.scale() > 2 || weight.compareTo(new BigDecimal("99999999.99")) > 0) {
                    throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品重量最多支持8位整数和2位小数");
                }
            }
            product.setWeight(weight);
        }

        if (submittedProductIds.size() != productMap.size()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "必须提交当前流转卡全部产品的重量");
        }

        for (ProductionProductEntity product : products) {
            if (productMapper.updateById(product) <= 0) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "产品重量保存失败");
            }
        }
    }

    /** 将产品 Entity 转为 VO，并填充状态中文名 */
    private ProductionProductVO toProductVO(ProductionProductEntity p) {
        ProductionProductVO vo = new ProductionProductVO();
        BeanUtil.copyProperties(p, vo);
        ProductStatusEnum statusEnum = java.util.Arrays.stream(ProductStatusEnum.values())
                .filter(e -> e.getCode().equals(p.getStatus()))
                .findFirst().orElse(null);
        vo.setStatusName(statusEnum != null ? statusEnum.getDesc() : p.getStatus());
        return vo;
    }

    /** 根据 currentProcess 枚举码填充流转卡 VO 的当前工序中文名 */
    private void fillCurrentProcessName(ProductionRecordVO vo) {
        if (vo.getCurrentProcess() == null) {
            return;
        }
        java.util.Arrays.stream(ProcessTypeEnum.values())
                .filter(e -> e.getCode().equals(vo.getCurrentProcess()))
                .findFirst()
                .ifPresent(e -> vo.setCurrentProcessName(e.getDesc()));
    }

    /** 返回设备可用状态：0=空闲可用，1=占用不可用（state=0空闲，非0占用） */
    private int resolveDeviceStatus(DeviceEntity device) {
        if (device.getConnectionStatus() == null || device.getConnectionStatus() == 0) {
            return 1;  // 离线视为占用
        }
        // state=0 空闲，非0 占用
        return (device.getState() != null && device.getState() == 0) ? 0 : 1;
    }

    @Override
    public CancelPreviewVO getCancelPreview(Long recordId) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        OrderMainEntity order = orderMainMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        long totalRecordCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, record.getOrderId())
                .notIn(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.CANCELLED.getValue(),
                        FlowStatusEnum.PRINT_FAILED.getValue()));
        CancelPreviewVO vo = new CancelPreviewVO();
        vo.setOrderId(order.getId());
        vo.setOrderCode(order.getOrderCode());
        vo.setTotalRecordCount((int) totalRecordCount);
        vo.setMessage(String.format("订单 %s 共有 %d 张流转卡，取消后将全部作废", order.getOrderCode(), totalRecordCount));
        return vo;
    }

    /**
     * 执行 Flow 状态流转并回写 order_main
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void triggerFlowAndSync(Long orderId, FlowActionEnum action) {
        triggerFlowAndSyncSafely(orderId, action);
    }

    private boolean triggerFlowAndSyncSafely(Long orderId, FlowActionEnum action) {
        FlowOperator operator;
        try {
            Long operatorId = StpUtil.getLoginIdAsLong();
            UserEntity user = userMapper.selectById(operatorId);
            operator = FlowOperator.of(operatorId, user != null ? user.getRealName() : "system");
        } catch (Exception e) {
            operator = FlowOperator.of(0L, "system");
        }
        TransitionResult result;
        try {
            result = flowFacade.executeFlow(orderId, action, operator);
        } catch (com.yigongbao.common.exception.BusinessException e) {
            log.info("Flow状态流转被拒绝（可能已被并发触发）: orderId={}, action={}, reason={}", orderId, action, e.getMessage());
            return false;
        }
        if (result == null) {
            log.warn("Flow状态流转返回空结果，跳过订单状态回写: orderId={}, action={}", orderId, action);
            return false;
        }
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setPhase(result.getTargetPhase());
        order.setStatus(result.getFinalStatus());
        // 订单完成或出库时，设置实际完成时间
        if (FlowStatusEnum.WAREHOUSE_OUT.getValue().equals(result.getFinalStatus())
                || FlowStatusEnum.COMPLETED.getValue().equals(result.getFinalStatus())) {
            order.setActualCompleteTime(java.time.LocalDateTime.now());
        }
        orderMainMapper.updateById(order);
        log.info("Flow状态流转完成: orderId={}, action={}, targetPhase={}, targetStatus={}",
                orderId, action, result.getTargetPhase(), result.getFinalStatus());
        return true;
    }

    @Override
    public com.yigongbao.module.basic.file.vo.FileVO getOrGenerateFlowCardExcel(Long recordId) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        boolean needRegenerate = record.getFlowCardFileUrl() == null
                || record.getFlowCardGenerateTime() == null
                || record.getContentUpdateTime() == null
                || record.getContentUpdateTime().isAfter(record.getFlowCardGenerateTime());

        if (needRegenerate) {
            return generateFlowCardExcel(recordId);
        } else {
            // 获取订单信息，添加患者姓名前缀
            OrderMainEntity order = orderMainMapper.selectById(record.getOrderId());
            String patientName = (order != null && order.getPatientName() != null) ? order.getPatientName() : "";
            com.yigongbao.module.basic.file.vo.FileVO fileVO = new com.yigongbao.module.basic.file.vo.FileVO();
            fileVO.setFileUrl(record.getFlowCardFileUrl());
            fileVO.setFileName(patientName + "流转卡.xlsx");
            return fileVO;
        }
    }

    @Override
    public com.yigongbao.module.basic.file.vo.FileVO generateFlowCardExcel(Long recordId) {
        log.info("生成流转卡Excel: recordId={}", recordId);

        // 直接查询数据库，避免调用 getRecordDetail 导致无限递归
        // 并行执行三个独立查询以提升性能
        java.util.concurrent.CompletableFuture<ProductionRecordEntity> recordFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(() -> getById(recordId));

        java.util.concurrent.CompletableFuture<List<ProductionProductEntity>> productsFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(() ->
                productMapper.selectList(new LambdaQueryWrapper<ProductionProductEntity>()
                    .eq(ProductionProductEntity::getProductionRecordId, recordId)));

        java.util.concurrent.CompletableFuture<List<ProductionProcessEntity>> processesFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(() ->
                processMapper.selectList(new LambdaQueryWrapper<ProductionProcessEntity>()
                    .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                    .orderByAsc(ProductionProcessEntity::getProcessOrder)));

        ProductionRecordEntity record = recordFuture.join();
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        List<ProductionProductEntity> products = productsFuture.join();
        List<ProductionProcessEntity> processes = processesFuture.join();
        String designerAssetNo = queryDesignerAssetNo(record.getOrderId());

        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setRecordNo(record.getRecordNo());
        context.setVersionNo("A/0");
        context.setDesignPackageCode(record.getDesignPackageCode());
        context.setTotalProductCount(record.getTotalProductCount());
        context.setProductionBatchNo(record.getProductionBatchNo());
        context.setMaterial(record.getMaterial());
        context.setMaterialBatchNo(record.getMaterialBatchNo());
        context.setPrintStartTime(record.getPrintStartTime());
        context.setPrintFinishTime(record.getPrintFinishTime());
        context.setDesignerAssetNo(designerAssetNo);
        context.setPackMaterial(record.getPackMaterial());

        List<FlowCardExcelBuilder.ProcessInfo> processInfos = processes.stream()
            .map(p -> {
                FlowCardExcelBuilder.ProcessInfo info = new FlowCardExcelBuilder.ProcessInfo();
                info.setProcessType(p.getProcessType());
                info.setDeviceNo(p.getDeviceNo());
                info.setSecondaryDeviceNo(p.getSecondaryDeviceNo());
                info.setProcessParams(p.getProcessParams());
                info.setStartTime(p.getStartTime());
                info.setEndTime(p.getEndTime());
                return info;
            })
            .collect(Collectors.toList());
        context.setProcesses(processInfos);

        List<FlowCardExcelBuilder.ProductInfo> productInfos = products.stream()
            .map(p -> {
                FlowCardExcelBuilder.ProductInfo info = new FlowCardExcelBuilder.ProductInfo();
                info.setProductNo(p.getProductNo());
                info.setProductName(p.getProductName());
                info.setSpecName(p.getSpecName());
                info.setMaterialName(p.getMaterialName());
                info.setColorName(p.getColorName());
                return info;
            })
            .collect(Collectors.toList());
        context.setProducts(productInfos);

        try {
            byte[] excelBytes = flowCardExcelBuilder.build(context);
            // 获取订单信息，添加患者姓名前缀
            OrderMainEntity order = orderMainMapper.selectById(record.getOrderId());
            String patientName = (order != null && order.getPatientName() != null) ? order.getPatientName() : "";
            String filename = patientName + "流转卡.xlsx";
            com.yigongbao.module.basic.file.vo.FileVO fileVO = fileService.uploadBytes(
                excelBytes, filename, com.yigongbao.common.enums.FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());

            // 保存生成时间和URL到数据库
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getId, recordId)
                    .set(ProductionRecordEntity::getFlowCardFileUrl, fileVO.getFileUrl())
                    .set(ProductionRecordEntity::getFlowCardGenerateTime, now);
            // contentUpdateTime 可能为 null 的情况：
            // 1. 新创建的流转卡尚未执行任何内容修改操作（submitBatchNo、assignDevice等）
            // 2. 数据库迁移前的旧记录（已通过 SQL UPDATE 初始化，但可能存在遗漏）
            // 初始化为当前时间，避免后续每次查询都重新生成 Excel
            if (record.getContentUpdateTime() == null) {
                updateWrapper.set(ProductionRecordEntity::getContentUpdateTime, now);
            }
            update(updateWrapper);

            log.info("流转卡Excel生成并上传成功: recordId={}, recordNo={}, fileUrl={}",
                recordId, record.getRecordNo(), fileVO.getFileUrl());
            return fileVO;
        } catch (Exception e) {
            log.error("流转卡Excel生成失败: recordId={}", recordId, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private String queryDesignerAssetNo(Long orderId) {
        if (orderId == null) {
            return null;
        }
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null || order.getDesignerId() == null) {
            return null;
        }
        UserEntity designer = userMapper.selectById(order.getDesignerId());
        return designer != null ? designer.getAssetNumber() : null;
    }

    @Override
    public ProductionColumnConfigVO getColumnConfig() {
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            UserEntity user = userService.getById(currentUserId);
            if (user != null && StrUtil.isNotBlank(user.getProductionColumnSettings())) {
                try {
                    return objectMapper.readValue(user.getProductionColumnSettings(), ProductionColumnConfigVO.class);
                } catch (JsonProcessingException e) {
                    log.warn("解析用户生产列配置失败，降级为系统默认，userId={}", currentUserId, e);
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败，使用系统默认配置", e);
        }

        String configJson = configService.getConfigValue(SystemConfigKeyEnum.PRODUCTION_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("系统默认生产列配置为空");
            return new ProductionColumnConfigVO();
        }
        try {
            return objectMapper.readValue(configJson, ProductionColumnConfigVO.class);
        } catch (JsonProcessingException e) {
            log.error("解析系统生产列配置失败", e);
            return new ProductionColumnConfigVO();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumnConfig(SaveProductionColumnConfigDTO dto) {
        Long currentUserId;
        try {
            currentUserId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.error("获取当前登录用户失败", e);
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        UserEntity user = userService.getById(currentUserId);
        if (user == null) {
            log.error("用户不存在: userId={}", currentUserId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        ProductionColumnConfigVO configVO = new ProductionColumnConfigVO();
        if (dto.getColumns() != null) {
            List<ProductionColumnConfigVO.ColumnItemVO> columnItems = dto.getColumns().stream()
                    .map(item -> {
                        ProductionColumnConfigVO.ColumnItemVO colVO = new ProductionColumnConfigVO.ColumnItemVO();
                        colVO.setField(item.getField());
                        colVO.setLabel(item.getLabel());
                        colVO.setVisible(item.getVisible());
                        colVO.setSort(item.getSort());
                        colVO.setWidth(item.getWidth());
                        colVO.setFixed(item.getFixed());
                        return colVO;
                    }).collect(Collectors.toList());
            configVO.setColumns(columnItems);
        }

        try {
            String configJson = objectMapper.writeValueAsString(configVO);
            user.setProductionColumnSettings(configJson);
            userService.updateById(user);
            log.info("保存生产列配置成功: userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化生产列配置失败: userId={}", currentUserId, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 根据数据范围类型向查询条件注入数据权限过滤
     * CENTER：未分配流转卡全员可见（用于接单），已分配的按订单 center_id 过滤
     * ALL：不做限制
     */
    private void buildDataScopeCondition(LambdaQueryWrapper<ProductionRecordEntity> wrapper,
                                         UserEntity currentUser,
                                         DataScopeTypeEnum scopeType) {
        if (currentUser == null) {
            log.warn("当前用户信息为空，数据权限过滤返回空列表");
            wrapper.apply("1 = 0");
            return;
        }

        switch (scopeType) {
            case CENTER:
                Long centerId = currentUser.getCenterId();
                if (centerId != null) {
                    // 先查出本加工中心的订单ID列表
                    List<Long> centerOrderIds = orderMainMapper.selectList(
                        new LambdaQueryWrapper<OrderMainEntity>()
                            .eq(OrderMainEntity::getCenterId, centerId)
                            .select(OrderMainEntity::getId))
                        .stream().map(OrderMainEntity::getId).collect(Collectors.toList());

                    // 未分配的全员可见 OR 订单属于本加工中心
                    wrapper.and(w -> {
                        w.isNull(ProductionRecordEntity::getProcessingCenterId);
                        if (!centerOrderIds.isEmpty()) {
                            w.or().in(ProductionRecordEntity::getOrderId, centerOrderIds);
                        }
                    });
                } else {
                    // 未绑定加工中心，只能看未分配的
                    wrapper.isNull(ProductionRecordEntity::getProcessingCenterId);
                }
                break;
            case ALL:
                break;
            default:
                log.warn("生产模块不支持的数据权限类型，降级为 CENTER: scopeType={}", scopeType);
                buildDataScopeCondition(wrapper, currentUser, DataScopeTypeEnum.CENTER);
                break;
        }
    }

    /**
     * 导出生产产品台账Excel
     * <p>
     * 核心流程：
     * 1. 应用数据权限过滤（医院/加工中心/全部）
     * 2. 参数校验（至少一个查询条件）
     * 3. 查询总数（判断是否为空、是否超1万条）
     * 4. 查询数据（最多1万条）
     * 5. 生成Excel（超1万条时顶部红色警告）
     * </p>
     */
    @Override
    public byte[] exportProductLedger(ProductLedgerExportDTO dto) {
        // 应用数据权限：根据用户角色自动注入 hospitalIds 或 centerIds
        applyDataScopeForExport(dto);

        // 时间范围合理性校验
        if (dto.getStartTime() != null && dto.getEndTime() != null
            && dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "开始时间不能晚于结束时间");
        }

        // 查询总数
        Long totalCount = productMapper.countProductLedgerData(dto);
        if (totalCount == 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND.getCode(), "未查询到符合条件的数据");
        }

        // 查询数据（最多1万条）
        List<Map<String, Object>> dataList = productMapper.listProductLedgerData(dto);

        log.info("导出生产产品台账: 总数={}, 实际导出={}, dto={}", totalCount, dataList.size(), dto);

        // 生成Excel
        try {
            return productLedgerExcelBuilder.build(dataList, totalCount);
        } catch (Exception e) {
            log.error("导出生产产品台账失败: dto={}", dto, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "Excel生成失败：" + e.getMessage());
        }
    }

    /**
     * 应用数据权限过滤（仅用于导出功能）
     * <p>
     * 权限策略：
     * - 医院角色：只能导出本医院订单的生产数据，通过 dto.hospitalIds 过滤
     * - 加工中心角色：只能导出分配给自己中心的数据，通过 dto.centerIds 过滤
     * - 全部权限：可导出全部数据，不添加过滤条件
     * - 其他角色：拒绝导出
     * </p>
     *
     * @param dto 查询条件DTO，方法内会自动填充 hospitalIds 或 centerIds
     * @throws BusinessException 无权限或未绑定医院/加工中心时抛出
     */
    private void applyDataScopeForExport(ProductLedgerExportDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(userId);

        switch (scopeType) {
            case HOSPITALS:
                // 医院权限：获取用户绑定的所有医院ID
                List<Long> hospitalIds = userHospitalService.getHospitalIdsByUserId(userId);
                if (hospitalIds.isEmpty()) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "您未绑定任何医院，无权导出数据");
                }
                dto.setHospitalIds(hospitalIds);
                log.info("医院数据权限: userId={}, hospitalIds={}", userId, hospitalIds);
                break;

            case CENTER:
                // 加工中心权限：获取用户绑定的加工中心ID
                UserEntity currentUser = userMapper.selectById(userId);
                if (currentUser == null || currentUser.getCenterId() == null) {
                    throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "您未绑定加工中心，无权导出数据");
                }
                dto.setCenterIds(List.of(currentUser.getCenterId()));
                log.info("加工中心数据权限: userId={}, centerId={}", userId, currentUser.getCenterId());
                break;

            case ALL:
                // 全部权限：不添加过滤条件
                log.info("全部数据权限: userId={}", userId);
                break;

            default:
                log.warn("不支持的数据权限类型: userId={}, scopeType={}", userId, scopeType);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR.getCode(), "您没有导出权限");
        }
    }
}
