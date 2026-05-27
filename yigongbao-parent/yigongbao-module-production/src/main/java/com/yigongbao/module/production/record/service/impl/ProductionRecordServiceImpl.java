package com.yigongbao.module.production.record.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.facade.FlowFacade;
import com.yigongbao.flow.operator.FlowOperator;
import com.yigongbao.flow.result.TransitionResult;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.RecordStatusEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.record.dto.AssignDeviceDTO;
import com.yigongbao.module.production.record.dto.CreateRecordDTO;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.dto.SubmitBatchNoDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.DeviceConfigVO;
import com.yigongbao.module.production.record.vo.PrinterVO;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import com.yigongbao.module.production.util.QrCodeUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
    private final DesignPackageFileMapper designPackageFileMapper;
    private final OrderMainMapper orderMainMapper;
    private final DeviceMapper deviceMapper;
    private final ProductionProductMapper productMapper;
    private final ProductionProcessMapper processMapper;
    private final FlowFacade flowFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(CreateRecordDTO dto) {
        // 校验数据包
        DesignPackageEntity designPackage = designPackageMapper.selectById(dto.getDesignPackageId());
        if (designPackage == null) {
            throw new BusinessException(ErrorCodeEnum.DESIGN_PACKAGE_NOT_FOUND);
        }
        // 校验订单
        OrderMainEntity order = orderMainMapper.selectById(designPackage.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 校验打印设备
        DeviceEntity device = deviceMapper.selectById(dto.getPrintDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCodeEnum.PRINT_DEVICE_NOT_FOUND);
        }

        String recordNo = codeGeneratorService.generate(ProductionConstants.PRODUCTION_RECORD_NO);
        String batchNo = dto.getProductionBatchNo() != null
                ? dto.getProductionBatchNo()
                : codeGeneratorService.generate(ProductionConstants.PRODUCTION_BATCH_NO);

        ProductionRecordEntity record = new ProductionRecordEntity();
        record.setRecordNo(recordNo);
        record.setOrderId(order.getId());
        record.setOrderCode(order.getOrderCode());
        record.setOrderType(order.getOrderType());
        record.setDesignPackageId(designPackage.getId());
        record.setDesignPackageCode(designPackage.getPackageCode());
        record.setProductionBatchNo(batchNo);
        record.setMaterial(dto.getMaterial());
        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        record.setProcessingCenterId(device.getCenterId());
        record.setProcessingCenterName(device.getCenterName());
        record.setStatus(RecordStatusEnum.PENDING_PRINT.getCode());
        save(record);

        // 按打印文件列表生成产品记录
        int totalCount = createProductRecords(record, designPackage);
        record.setTotalProductCount(totalCount);
        // 按订单类型生成工序记录
        createProcessRecords(record.getId(), order.getOrderType());

        // 生成二维码
        String qrContent = String.format("RECORD:%s|BATCH:%s", recordNo, batchNo);
        record.setQrCodeUrl("data:image/png;base64," + QrCodeUtil.generateQrCodeBase64(qrContent));
        updateById(record);

        log.info("创建生产流转卡: recordId={}, recordNo={}, orderId={}, orderType={}, productCount={}",
                record.getId(), recordNo, order.getId(), order.getOrderType(), totalCount);
        return record.getId();
    }

    /**
     * 根据数据包内文件列表创建产品记录
     */
    private int createProductRecords(ProductionRecordEntity record, DesignPackageEntity designPackage) {
        List<DesignPackageFileEntity> printFiles = designPackageFileMapper.selectList(
                new LambdaQueryWrapper<DesignPackageFileEntity>()
                        .eq(DesignPackageFileEntity::getPackageId, designPackage.getId()));
        if (printFiles.isEmpty()) {
            return 0;
        }
        List<ProductionProductEntity> products = new ArrayList<>();
        for (DesignPackageFileEntity file : printFiles) {
            ProductionProductEntity product = new ProductionProductEntity();
            product.setProductionRecordId(record.getId());
            product.setPrintFileId(file.getId());
            product.setProductNo(codeGeneratorService.generate(ProductionConstants.PRODUCT_NO));
            product.setProductName(file.getFileName());
            product.setFileName(file.getFileName());
            product.setStatus(RecordStatusEnum.PENDING_PRINT.getCode());
            products.add(product);
        }
        products.forEach(productMapper::insert);
        return products.size();
    }

    /**
     * 按订单类型创建工序记录
     * 医疗器械订单：打印→酒精初洗→UV固化→超声清洗+干燥→包装
     * 非医疗器械订单：打印→包装
     */
    private void createProcessRecords(Long recordId, Integer orderType) {
        List<ProcessTypeEnum> processTypes = new ArrayList<>();
        processTypes.add(ProcessTypeEnum.PRINT);
        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(orderType)) {
            processTypes.add(ProcessTypeEnum.WASH);
            processTypes.add(ProcessTypeEnum.CURE);
            processTypes.add(ProcessTypeEnum.CLEAN_DRY);
        }
        processTypes.add(ProcessTypeEnum.PACK);
        for (int i = 0; i < processTypes.size(); i++) {
            ProcessTypeEnum pt = processTypes.get(i);
            ProductionProcessEntity process = new ProductionProcessEntity();
            process.setProductionRecordId(recordId);
            process.setProcessType(pt.getCode());
            process.setProcessName(pt.getDesc());
            process.setProcessOrder(i + 1);
            process.setStatus(ProcessStatusEnum.PENDING.getCode());
            processMapper.insert(process);
        }
        log.info("自动创建工序记录: recordId={}, orderType={}, processCount={}", recordId, orderType, processTypes.size());
    }

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
        log.info("下载设计数据包，触发待打印状态流转: orderId={}, designPackageId={}", order.getId(), designPackageId);
    }

    @Override
    public void triggerFlowIfAllReach(Long orderId, String requiredStatus, FlowActionEnum action) {
        // 活跃流转卡（排除失败和废弃）
        long totalActive = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .notIn(ProductionRecordEntity::getStatus,
                        RecordStatusEnum.PRINT_FAILED.getCode(),
                        RecordStatusEnum.ABANDONED.getCode()));
        long reachedCount = count(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, orderId)
                .eq(ProductionRecordEntity::getStatus, requiredStatus));
        if (totalActive > 0 && totalActive == reachedCount) {
            triggerFlowAndSync(orderId, action);
            log.info("聚合条件满足，触发Flow: orderId={}, requiredStatus={}, action={}", orderId, requiredStatus, action);
        } else {
            log.info("聚合条件未满足，暂不触发Flow: orderId={}, requiredStatus={}, active={}, reached={}",
                    orderId, requiredStatus, totalActive, reachedCount);
        }
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
        record.setStatus(RecordStatusEnum.QC_IN_PROGRESS.getCode());
        updateById(record);
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
    public List<PrinterVO> listPrinters() {
        List<DeviceEntity> devices = deviceMapper.selectList(
                new LambdaQueryWrapper<DeviceEntity>()
                        .eq(DeviceEntity::getDeviceType, "PRINTER"));
        return devices.stream().map(d -> {
            PrinterVO vo = new PrinterVO();
            vo.setId(d.getId());
            vo.setDeviceNo(d.getDeviceId());
            vo.setDeviceName(d.getDeviceName());
            if (d.getConnectionStatus() == null || d.getConnectionStatus() == 0) {
                vo.setStatus(0);
                vo.setStatusName("离线");
            } else if (Integer.valueOf(1).equals(d.getState())) {
                vo.setStatus(2);
                vo.setStatusName("使用中");
            } else {
                vo.setStatus(1);
                vo.setStatusName("可使用");
            }
            return vo;
        }).collect(Collectors.toList());
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
        record.setPrintDeviceId(device.getId());
        record.setPrintDeviceCode(device.getDeviceId());
        record.setPrintDeviceName(device.getDeviceName());
        record.setStatus(RecordStatusEnum.PRINTING.getCode());
        updateById(record);
        processMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionProcessEntity>()
                .eq(ProductionProcessEntity::getProductionRecordId, recordId)
                .eq(ProductionProcessEntity::getProcessType, "print")
                .set(ProductionProcessEntity::getDeviceId, device.getId())
                .set(ProductionProcessEntity::getDeviceNo, device.getDeviceId())
                .set(ProductionProcessEntity::getStatus, ProcessStatusEnum.IN_PROGRESS.getCode())
                .set(ProductionProcessEntity::getStartTime, java.time.LocalDateTime.now()));
        log.info("分配打印机: recordId={}, deviceId={}, deviceNo={}", recordId, device.getId(), device.getDeviceId());
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
