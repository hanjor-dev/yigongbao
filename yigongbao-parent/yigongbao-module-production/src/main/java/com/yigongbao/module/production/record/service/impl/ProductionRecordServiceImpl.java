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
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
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
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.PrinterVO;
import com.yigongbao.module.production.record.vo.ProcessingCenterPrintersVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
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
    private final OrderMainMapper orderMainMapper;
    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final FlowFacade flowFacade;

    @Override
    public ProductionRecordVO getRecordDetail(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        ProductionRecordVO vo = new ProductionRecordVO();
        BeanUtil.copyProperties(record, vo);
        List<ProductionProductEntity> products = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, id));
        vo.setProducts(BeanUtil.copyToList(products, ProductionProductVO.class));
        return vo;
    }

    @Override
    public ProductionRecordVO getByRecordNo(String recordNo) {
        ProductionRecordEntity record = getOne(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getRecordNo, recordNo));
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return getRecordDetail(record.getId());
    }

    @Override
    public String getQrCodeUrl(Long id) {
        ProductionRecordEntity record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        return record.getQrCodeUrl();
    }

    @Override
    public IPage<ProductionRecordVO> pageRecords(ProductionRecordPageDTO dto) {
        Page<ProductionRecordEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<>();
        if (dto.getStatus() != null) {
            wrapper.eq(ProductionRecordEntity::getStatus, dto.getStatus());
        }
        if (dto.getRecordNo() != null) {
            wrapper.like(ProductionRecordEntity::getRecordNo, dto.getRecordNo());
        }
        if (dto.getProcessingCenterId() != null) {
            wrapper.eq(ProductionRecordEntity::getProcessingCenterId, dto.getProcessingCenterId());
        }
        Page<ProductionRecordEntity> result = page(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return result.convert(e -> new ProductionRecordVO());
        }
        // 批量查询产品，避免 N+1
        List<Long> recordIds = result.getRecords().stream()
                .map(ProductionRecordEntity::getId).collect(Collectors.toList());
        List<ProductionProductEntity> allProducts = productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .in(ProductionProductEntity::getProductionRecordId, recordIds));
        java.util.Map<Long, List<ProductionProductVO>> productMap = allProducts.stream()
                .collect(Collectors.groupingBy(
                        ProductionProductEntity::getProductionRecordId,
                        Collectors.mapping(p -> BeanUtil.copyProperties(p, ProductionProductVO.class), Collectors.toList())));
        return result.convert(e -> {
            ProductionRecordVO vo = new ProductionRecordVO();
            BeanUtil.copyProperties(e, vo);
            vo.setProducts(productMap.getOrDefault(e.getId(), java.util.Collections.emptyList()));
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void downloadDataPackage(Long designPackageId) {
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
            return;
        }
        triggerFlowAndSync(order.getId(), FlowActionEnum.START_PRINT);

        // 回写订单操作人信息（当前生产员）
        Long userId = StpUtil.getLoginIdAsLong();
        String userName = (String) StpUtil.getSession().get("username");
        OrderMainEntity orderUpdate = new OrderMainEntity();
        orderUpdate.setId(order.getId());
        orderUpdate.setCurrentHandlerId(userId);
        orderUpdate.setCurrentHandlerName(userName);
        orderUpdate.setProducerId(userId);
        orderMainMapper.updateById(orderUpdate);

        log.info("下载设计数据包，触发待打印状态流转: orderId={}, designPackageId={}", order.getId(), designPackageId);
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

    /** 返回已达到或超过指定状态的所有状态码（状态机单向推进） */
    private List<Integer> getReachedOrBeyondStatuses(Integer requiredStatus) {
        List<Integer> ordered = List.of(
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBatchNo(Long recordId, SubmitBatchNoDTO dto) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        record.setProductionBatchNo(dto.getProductionBatchNo());
        record.setMaterialBatchNo(dto.getMaterialBatchNo());
        updateById(record);
        log.info("提交生产批号: recordId={}, batchNo={}", recordId, dto.getProductionBatchNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitToQc(Long recordId) {
        ProductionRecordEntity record = getById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        // 校验流转卡状态：必须是后处理中才能提交终检
        if (!FlowStatusEnum.POST_PROCESSING.getValue().equals(record.getStatus())) {
            throw new BusinessException(400, "流转卡未完成后处理，无法提交终检");
        }
        // 校验所有后处理工序已完成
        long incompleteCount = processMapper.selectCount(new LambdaQueryWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .ne(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .ne(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PACK.getCode())
                .ne(ProductionProcessEntity::getStatus, ProcessStatusEnum.COMPLETED.getCode()));
        if (incompleteCount > 0) {
            throw new BusinessException(400, "存在未完成的后处理工序，无法提交终检");
        }
        // 通过 Flow 驱动状态流转（POST_PROCESSING → QC_IN_PROGRESS），同步回写 order_main
        triggerFlowAndSync(record.getOrderId(), FlowActionEnum.COMPLETE_POST_PROCESSING);
        log.info("提交质检管理: recordId={}, recordNo={}", recordId, record.getRecordNo());
    }

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
                vo.setStatusName(statusCode == 0 ? "离线" : statusCode == 2 ? "繁忙" : "空闲");
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
        // 校验设备在线且未被占用
        if (resolveDeviceStatus(device) == 0) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
        if (resolveDeviceStatus(device) == 2) {
            throw new BusinessException(ErrorCodeEnum.DEVICE_NOT_AVAILABLE);
        }
        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
        updateById(record);
        processMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, ProcessTypeEnum.PRINT.getCode())
                .set(ProductionProcessEntity::getDeviceId, device.getId())
                .set(ProductionProcessEntity::getDeviceNo, device.getDeviceId())
                .set(ProductionProcessEntity::getStatus, ProcessStatusEnum.IN_PROGRESS.getCode())
                .set(ProductionProcessEntity::getStartTime, java.time.LocalDateTime.now()));
        log.info("分配打印机: recordId={}, deviceId={}, deviceNo={}", recordId, device.getId(), device.getDeviceId());
    }

    /**
     * 解析设备状态：0=离线，1=空闲，2=繁忙
     */
    private int resolveDeviceStatus(DeviceEntity device) {
        if (device.getConnectionStatus() == null || device.getConnectionStatus() == 0) {
            return 0;
        }
        if (Integer.valueOf(1).equals(device.getState())) {
            return 2;
        }
        return 1;
    }

    /**
     * 执行 Flow 状态流转并回写 order_main
     */
    private void triggerFlowAndSync(Long orderId, FlowActionEnum action) {
        Long operatorId = StpUtil.getLoginIdAsLong();
        String operatorName = (String) StpUtil.getSession().get("username");
        FlowOperator operator = FlowOperator.of(operatorId, operatorName != null ? operatorName : "system");
        TransitionResult result = flowFacade.executeFlow(orderId, action, operator);
        OrderMainEntity order = new OrderMainEntity();
        order.setId(orderId);
        order.setPhase(result.getTargetPhase());
        order.setStatus(result.getFinalStatus());
        orderMainMapper.updateById(order);
        log.info("Flow状态流转完成: orderId={}, action={}, targetPhase={}, targetStatus={}",
                orderId, action, result.getTargetPhase(), result.getFinalStatus());
    }
}
