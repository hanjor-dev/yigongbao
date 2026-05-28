package com.yigongbao.module.production.qc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProcessStatusEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.QcHandleTypeEnum;
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.process.entity.ProductionProcessEntity;
import com.yigongbao.module.production.process.mapper.ProductionProcessMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
import com.yigongbao.module.production.qc.dto.ProductionRedoPageDTO;
import com.yigongbao.module.production.qc.service.IProductionQcService;
import com.yigongbao.module.production.record.dto.ProductionRecordPageDTO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.module.production.record.vo.ProductionRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 质检服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionQcServiceImpl implements IProductionQcService {

    private final ProductionProductMapper productMapper;
    private final ProductionRecordMapper recordMapper;
    private final ProductionProcessMapper processMapper;
    private final CodeGeneratorService codeGeneratorService;
    private final IProductionRecordService recordService;

    /**
     * 标记产品质检合格；医疗器械同步生成 UDI 码；回写流转卡合格计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductPass(Long productId) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        // 幂等校验：只有 in_process 状态的产品才能质检
        if (!ProductStatusEnum.IN_PROCESS.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许质检: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(400, "产品当前状态不允许质检");
        }
        ProductionRecordEntity record = recordMapper.selectById(product.getProductionRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        product.setStatus(ProductStatusEnum.PASS.getCode());
        product.setQcResult(QcResultEnum.PASS.getCode());
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());
        // 医疗器械订单同步生成 UDI 码
        if (ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
            String udiCode = codeGeneratorService.generate(ProductionConstants.UDI_CODE);
            product.setUdiCode(udiCode);
            product.setUdiGenerateTime(LocalDateTime.now());
            log.info("生成UDI码: productId={}, productNo={}, udiCode={}", productId, product.getProductNo(), udiCode);
        }
        productMapper.updateById(product);
        // 回写流转卡合格计数（原子自增，避免并发覆盖）
        recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getId, record.getId())
                .setSql("qualified_count = qualified_count + 1"));
        log.info("标记产品质检合格: productId={}, productNo={}, orderType={}, hasUDI={}",
                productId, product.getProductNo(), record.getOrderType(), product.getUdiCode() != null);
    }

    /**
     * 标记产品质检不合格（redo），同步更新流转卡 has_redo_product 标志和不合格计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductRedo(Long productId, String reason, String handleType) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        boolean validHandleType = java.util.Arrays.stream(QcHandleTypeEnum.values())
                .anyMatch(e -> e.getCode().equals(handleType));
        if (!validHandleType) {
            throw new BusinessException(400, "无效的处理方式: " + handleType);
        }
        if (!ProductStatusEnum.IN_PROCESS.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许标记不合格: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(400, "产品当前状态不允许标记不合格");
        }
        product.setStatus(ProductStatusEnum.REDO.getCode());
        product.setQcResult(QcResultEnum.REDO.getCode());
        product.setQcRemark(reason);
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());
        productMapper.updateById(product);
        // 回写流转卡不合格计数和 redo 标志
        ProductionRecordEntity record = recordMapper.selectById(product.getProductionRecordId());
        if (record != null) {
            recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getId, record.getId())
                    .setSql("unqualified_count = unqualified_count + 1")
                    .set(ProductionRecordEntity::getHasRedoProduct, 1));
        }
        log.info("标记产品质检不合格: productId={}, productNo={}, reason={}, handleType={}", productId, product.getProductNo(), reason, handleType);

        // REWORK_TO_PRINT：将该产品所在流转卡的所有工序重置为 PENDING，流转卡回退到待打印
        if (QcHandleTypeEnum.REWORK_TO_PRINT.getCode().equals(handleType)) {
            List<ProductionProcessEntity> processes = processMapper.selectList(
                    new LambdaQueryWrapper<ProductionProcessEntity>()
                            .eq(ProductionProcessEntity::getProductionRecordId, product.getProductionRecordId()));
            processes.forEach(p -> {
                p.setStatus(ProcessStatusEnum.PENDING.getCode());
                p.setDeviceId(null);
                p.setDeviceNo(null);
                p.setDeviceName(null);
                p.setOperatorId(null);
                p.setOperatorName(null);
                p.setStartTime(null);
                p.setEndTime(null);
                p.setProcessParams(null);
                processMapper.updateById(p);
            });
            if (record != null) {
                record.setStatus(FlowStatusEnum.PENDING_PRINT.getValue());
                record.setCurrentProcess(null);
                record.setPrintStartTime(null);
                record.setPrintFinishTime(null);
                record.setPrintDeviceId(null);
                record.setPrintDeviceCode(null);
                record.setPrintDeviceName(null);
                recordMapper.updateById(record);
                // 聚合判断：所有活跃流转卡都精确等于 PENDING_PRINT 时才触发 Flow 回退订单
                recordService.triggerFlowIfAllExact(record.getOrderId(),
                        FlowStatusEnum.PENDING_PRINT.getValue(), FlowActionEnum.REWORK_TO_PRINT);
            }
            log.info("REWORK_TO_PRINT: productId={}, recordId={}, 重置所有工序为PENDING", productId, product.getProductionRecordId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRedoProcess(Long productId, String processType) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        product.setRedoProcessType(processType);
        productMapper.updateById(product);
        log.info("指定产品重做工序: productId={}, processType={}", productId, processType);
    }

    /**
     * 质检完成，流转到包装
     * 校验本张流转卡所有产品均已 pass → 更新状态为 packing → 聚合触发 QC_PASS
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToPacking(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        // 校验所有产品均已合格（废弃产品不参与校验）
        long notPassCount = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .ne(ProductionProductEntity::getStatus, ProductStatusEnum.PASS.getCode())
                .ne(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode()));
        if (notPassCount > 0) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_ALL_PASS);
        }
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        recordMapper.updateById(record);
        // 聚合判断：所有流转卡均到达 packing 时触发 QC_PASS
        recordService.triggerFlowIfAllReach(record.getOrderId(),
                FlowStatusEnum.PACKING.getValue(), FlowActionEnum.QC_PASS);
        log.info("质检完成，流转到包装: recordId={}, recordNo={}, orderId={}",
                recordId, record.getRecordNo(), record.getOrderId());
    }

    @Override
    public List<ProductionProductVO> listProductsByRecordId(Long recordId) {
        return productMapper.selectList(new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId)
                        .orderByAsc(ProductionProductEntity::getId))
                .stream()
                .map(p -> BeanUtil.copyProperties(p, ProductionProductVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto) {
        ProductionRecordPageDTO pageDTO = new ProductionRecordPageDTO();
        pageDTO.setPageNum(dto.getPageNum());
        pageDTO.setPageSize(dto.getPageSize());
        pageDTO.setKeyword(dto.getKeyword());
        // 不传 status 时默认查质检中
        pageDTO.setStatus(dto.getStatus() != null ? dto.getStatus() : FlowStatusEnum.QC_IN_PROGRESS.getValue());
        return recordService.pageRecords(pageDTO);
    }

    @Override
    public IPage<ProductionProductVO> listRedoProducts(ProductionRedoPageDTO dto) {
        Page<ProductionProductEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.REDO.getCode());
        if (dto.getRecordId() != null) {
            wrapper.eq(ProductionProductEntity::getProductionRecordId, dto.getRecordId());
        }
        return productMapper.selectPage(page, wrapper)
                .convert(p -> BeanUtil.copyProperties(p, ProductionProductVO.class));
    }
}
