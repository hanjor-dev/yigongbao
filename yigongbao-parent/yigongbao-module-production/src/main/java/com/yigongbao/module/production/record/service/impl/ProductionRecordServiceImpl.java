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
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.helper.FlowCardExcelBuilder;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final com.yigongbao.module.basic.file.service.FileService fileService;

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
        fillDesignFiles(vo, record.getDesignPackageId());
        return vo;
    }

    private void fillDesignFiles(ProductionRecordVO vo, Long designPackageId) {
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

    /** 分页查询流转卡列表；生产员自动限定到自己绑定的加工中心，支持关键字和时间范围过滤，批量关联查询避免 N+1 */
    @Override
    public IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto) {
        UserEntity currentUser = userMapper.selectById(StpUtil.getLoginIdAsLong());

        Page<ProductionRecordEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<ProductionRecordEntity>()
                .orderByDesc(ProductionRecordEntity::getCreateTime);
        if (dto.getStatuses() != null && !dto.getStatuses().isEmpty()) {
            wrapper.in(ProductionRecordEntity::getStatus, dto.getStatuses());
        } else if (dto.getStatus() != null) {
            wrapper.eq(ProductionRecordEntity::getStatus, dto.getStatus());
        }
        if (dto.getProcessingCenterId() != null) {
            // 前端或管理员指定加工中心时精确过滤
            wrapper.eq(ProductionRecordEntity::getProcessingCenterId, dto.getProcessingCenterId());
        } else if (RoleCodeEnum.PRODUCTION_WORKER.getCode().equals(currentUser.getRoleCode())) {
            // 生产员：已分配加工中心的数据只看自己的，未分配（设计审核通过状态）的数据所有人可见
            Long centerId = currentUser.getCenterId();
            if (centerId != null) {
                wrapper.and(w -> w
                        .eq(ProductionRecordEntity::getProcessingCenterId, centerId)
                        .or().isNull(ProductionRecordEntity::getProcessingCenterId));
            } else {
                // 生产员未绑定加工中心，只能看未分配的数据
                wrapper.isNull(ProductionRecordEntity::getProcessingCenterId);
            }
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
    @Transactional(rollbackFor = Exception.class)
    public String downloadDataPackage(Long designPackageId) {
        DesignPackageEntity designPackage = designPackageMapper.selectById(designPackageId);
        if (designPackage == null) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        OrderMainEntity order = orderMainMapper.selectById(designPackage.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 幂等：仅当 START_PRINT 在可用动作列表中时才触发
        List<String> availableActions = flowFacade.getAvailableActions(order.getId());
        if (!availableActions.contains(FlowActionEnum.START_PRINT.name())) {
            log.info("下载数据包幂等跳过，订单已推进: orderId={}, designPackageId={}", order.getId(), designPackageId);
            // 即使订单已推进，仍需确保本条流转卡状态正确
            baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getDesignPackageId, designPackageId)
                    .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.DESIGN_REVIEW_PASSED.getValue())
                    .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PENDING_PRINT.getValue()));
            return designPackage.getFileUrl();
        }

        // 聚合判断：只有订单下所有流转卡都已下载（即全部从 DESIGN_REVIEW_PASSED 推进）才触发 Flow
        // 先更新本条流转卡状态
        baseMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getDesignPackageId, designPackageId)
                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.DESIGN_REVIEW_PASSED.getValue())
                .set(ProductionRecordEntity::getStatus, FlowStatusEnum.PENDING_PRINT.getValue()));

        // 回写订单操作人信息（当前生产员）
        Long userId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(userId);
        String realName = currentUser != null ? currentUser.getRealName() : null;
        OrderMainEntity orderUpdate = new OrderMainEntity();
        orderUpdate.setId(order.getId());
        orderUpdate.setCurrentHandlerId(userId);
        orderUpdate.setCurrentHandlerName(realName);
        orderUpdate.setProducerId(userId);
        orderMainMapper.updateById(orderUpdate);

        // 聚合触发：所有流转卡都精确到达 PENDING_PRINT 状态时才触发 Flow
        triggerFlowIfAllExact(order.getId(), FlowStatusEnum.PENDING_PRINT.getValue(), FlowActionEnum.START_PRINT);

        log.info("下载设计数据包，触发待打印状态流转: orderId={}, designPackageId={}", order.getId(), designPackageId);
        return designPackage.getFileUrl();
    }

    @Override
    public void triggerFlowIfAllReach(Long orderId, Integer requiredStatus, FlowActionEnum action) {
        long totalActive = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .notIn(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.PRINT_FAILED.getValue(),
                        FlowStatusEnum.CANCELLED.getValue()));
        if (totalActive == 0) {
            return;
        }
        // 已达到或超过 requiredStatus 的状态集合（状态机单向推进）
        List<Integer> reachedStatuses = getReachedOrBeyondStatuses(requiredStatus);
        long reachedCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .in(ProductionRecordEntity::getStatus, reachedStatuses));
        if (totalActive == reachedCount) {
            // 幂等保护：再次确认 action 仍可执行（防止并发重复触发）
            List<String> availableActions = flowFacade.getAvailableActions(orderId);
            if (!availableActions.contains(action.name())) {
                log.info("聚合条件满足但Flow已推进，跳过: orderId={}, action={}", orderId, action);
                return;
            }
            triggerFlowAndSync(orderId, action);
            log.info("聚合条件满足，触发Flow: orderId={}, requiredStatus={}, action={}", orderId, requiredStatus, action);
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
                        FlowStatusEnum.CANCELLED.getValue()));
        if (totalActive == 0) {
            return;
        }
        long matchCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .eq(ProductionRecordEntity::getStatus, exactStatus));
        if (totalActive == matchCount) {
            List<String> availableActions = flowFacade.getAvailableActions(orderId);
            if (!availableActions.contains(action.name())) {
                log.info("聚合条件满足但Flow已推进，跳过: orderId={}, action={}", orderId, action);
                return;
            }
            triggerFlowAndSync(orderId, action);
            log.info("聚合条件满足（精确匹配），触发Flow: orderId={}, exactStatus={}, action={}", orderId, exactStatus, action);
        } else {
            log.info("聚合条件未满足，暂不触发Flow: orderId={}, exactStatus={}, active={}, matched={}",
                    orderId, exactStatus, totalActive, matchCount);
        }
    }

    /** 返回已达到或超过指定状态的所有状态码（状态机单向推进） */
    private List<Integer> getReachedOrBeyondStatuses(Integer requiredStatus) {
        List<Integer> ordered = List.of(
                FlowStatusEnum.PENDING_PRINT.getValue(),
                FlowStatusEnum.PRINTING.getValue(),
                FlowStatusEnum.PRINT_COMPLETED.getValue(),
                FlowStatusEnum.POST_PROCESSING.getValue(),
                FlowStatusEnum.QC_IN_PROGRESS.getValue(),
                FlowStatusEnum.PACKING.getValue(),
                FlowStatusEnum.WAREHOUSE_IN.getValue(),
                FlowStatusEnum.COMPLETED.getValue()
        );
        int idx = ordered.indexOf(requiredStatus);
        if (idx < 0) {
            return List.of(requiredStatus);
        }
        return ordered.subList(idx, ordered.size());
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
    @Transactional(rollbackFor = Exception.class)
    public void submitBatchNo(Long recordId, SubmitBatchNoDTO dto) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        // 批号唯一性校验
        if (cn.hutool.core.util.StrUtil.isNotBlank(dto.getProductionBatchNo())) {
            long existCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getProductionBatchNo, dto.getProductionBatchNo())
                .ne(ProductionRecordEntity::getId, recordId));
            if (existCount > 0) {
                throw new BusinessException(ErrorCodeEnum.PRODUCTION_BATCH_NO_EXISTS);
            }
        }
        record.setProductionBatchNo(dto.getProductionBatchNo());
        record.setMaterialBatchNo(dto.getMaterialBatchNo());
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
    @Transactional(rollbackFor = Exception.class)
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
        if (resolveDeviceStatus(device) != 1) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        if (dto.getMaterial() != null) {
            record.setMaterial(dto.getMaterial());
        }
        updateById(record);
        Long userId = StpUtil.getLoginIdAsLong();
        com.yigongbao.module.system.user.entity.UserEntity currentUser = userMapper.selectById(userId);
        String realName = currentUser != null ? currentUser.getRealName() : null;
        java.time.LocalDateTime printStartTime = java.time.LocalDateTime.now();
        record.setPrintStartTime(printStartTime);
        updateById(record);
        processMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .set(ProductionProcessEntity::getDeviceId, device.getId())
                .set(ProductionProcessEntity::getDeviceNo, device.getDeviceId())
                .set(ProductionProcessEntity::getDeviceName, device.getDeviceName())
                .set(ProductionProcessEntity::getOperatorId, userId)
                .set(ProductionProcessEntity::getOperatorName, realName)
                .set(ProductionProcessEntity::getStatus, ProcessStatusEnum.IN_PROGRESS.getCode())
                .set(ProductionProcessEntity::getStartTime, printStartTime)
                .set(dto.getPrintParams() != null, ProductionProcessEntity::getProcessParams, dto.getPrintParams()));
        log.info("分配打印机: recordId={}, deviceId={}, deviceNo={}", recordId, device.getId(), device.getDeviceId());
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

    /**
     * 解析设备状态：0=空闲，1=占用（包括离线、繁忙、不可用）
     *
     * 打印设备（PRINTER_SLA）：state=0表示空闲，state=1表示繁忙
     * 其他设备：state=0表示可用，state=1表示不可用
     */
    private int resolveDeviceStatus(DeviceEntity device) {
        // 离线视为占用
        if (device.getConnectionStatus() == null || device.getConnectionStatus() == 0) {
            return 1;
        }

        // 根据设备类型区分state字段含义
        if (DeviceTypeEnum.PRINTER_SLA.getCode().equals(device.getDeviceType())) {
            // 打印设备：state=0空闲，state=1繁忙
            return Integer.valueOf(1).equals(device.getState()) ? 1 : 0;
        } else {
            // 其他设备：state=0可用，state=1不可用
            return Integer.valueOf(1).equals(device.getState()) ? 1 : 0;
        }
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
    public void triggerFlowAndSync(Long orderId, FlowActionEnum action) {
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
            return;
        }
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setPhase(result.getTargetPhase());
        order.setStatus(result.getFinalStatus());
        if (FlowActionEnum.COMPLETE_WAREHOUSE_IN.equals(action)) {
            order.setActualCompleteTime(java.time.LocalDateTime.now());
        }
        orderMainMapper.updateById(order);
        log.info("Flow状态流转完成: orderId={}, action={}, targetPhase={}, targetStatus={}",
                orderId, action, result.getTargetPhase(), result.getFinalStatus());
    }

    @Override
    public com.yigongbao.module.basic.file.vo.FileVO generateFlowCardExcel(Long recordId) {
        log.info("生成流转卡Excel: recordId={}", recordId);

        ProductionRecordVO recordVO = getRecordDetail(recordId);
        if (recordVO == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        List<ProductionProcessEntity> processes = processMapper.selectList(
            new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProcessEntity::getProcessOrder)
        );

        String designerAssetNo = queryDesignerAssetNo(recordVO.getOrderId());

        FlowCardExcelBuilder.BuildContext context = new FlowCardExcelBuilder.BuildContext();
        context.setRecordNo(recordVO.getRecordNo());
        context.setVersionNo("A/0");
        context.setDesignPackageCode(recordVO.getDesignPackageCode());
        context.setTotalProductCount(recordVO.getTotalProductCount());
        context.setProductionBatchNo(recordVO.getProductionBatchNo());
        context.setMaterial(recordVO.getMaterial());
        context.setMaterialBatchNo(recordVO.getMaterialBatchNo());
        context.setPrintStartTime(recordVO.getPrintStartTime());
        context.setPrintFinishTime(recordVO.getPrintFinishTime());
        context.setDesignerAssetNo(designerAssetNo);

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

        List<FlowCardExcelBuilder.ProductInfo> productInfos = Collections.emptyList();
        if (recordVO.getProducts() != null) {
            productInfos = recordVO.getProducts().stream()
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
        }
        context.setProducts(productInfos);

        try {
            byte[] excelBytes = flowCardExcelBuilder.build(context);
            String filename = "流转卡_" + recordVO.getRecordNo() + ".xlsx";
            com.yigongbao.module.basic.file.vo.FileVO fileVO = fileService.uploadBytes(
                excelBytes, filename, com.yigongbao.common.enums.FileBizTypeEnum.INSTRUCTION_FILE.getDictCode());
            log.info("流转卡Excel生成并上传成功: recordId={}, recordNo={}, fileUrl={}",
                recordId, recordVO.getRecordNo(), fileVO.getFileUrl());
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
}
