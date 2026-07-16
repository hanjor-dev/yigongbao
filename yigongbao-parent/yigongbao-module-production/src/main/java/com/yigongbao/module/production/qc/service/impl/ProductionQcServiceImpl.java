package com.yigongbao.module.production.qc.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.production.constants.ProductionConstants;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.enums.QcResultEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.qc.dto.BatchUpdateUdiDTO;
import com.yigongbao.module.production.qc.dto.ProductionQcPageDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final CodeGeneratorService codeGeneratorService;
    private final IProductionRecordService recordService;
    private final com.yigongbao.module.order.mapper.OrderMainMapper orderMainMapper;
    private final com.yigongbao.module.system.user.mapper.UserMapper userMapper;
    private final com.yigongbao.module.production.product.service.IProductionProductService productService;

    /**
     * 标记产品质检合格，回写流转卡合格计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductPass(Long productId) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatusEnum.IN_PROCESS.getCode().equals(product.getStatus())
                && !ProductStatusEnum.FAIL.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许质检: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_QC);
        }
        ProductionRecordEntity record = recordMapper.selectById(product.getProductionRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        // 记录之前的状态，用于判断是否需要减少不合格计数
        boolean wasFail = ProductStatusEnum.FAIL.getCode().equals(product.getStatus());

        product.setStatus(ProductStatusEnum.PASS.getCode());
        product.setQcResult(QcResultEnum.PASS.getCode());
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());
        productMapper.updateById(product);

        // 如果之前是不合格状态，需要同时减少不合格计数
        if (wasFail) {
            recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getId, record.getId())
                    .setSql("qualified_count = qualified_count + 1, unqualified_count = unqualified_count - 1"));
            log.info("标记产品质检合格（从不合格转为合格）: productId={}, productNo={}", productId, product.getProductNo());
        } else {
            recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                    .eq(ProductionRecordEntity::getId, record.getId())
                    .setSql("qualified_count = qualified_count + 1"));
            log.info("标记产品质检合格: productId={}, productNo={}", productId, product.getProductNo());
        }
    }

    /**
     * 标记产品质检不合格，记录原因，不触发任何回退
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProductFail(Long productId, String reason) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatusEnum.IN_PROCESS.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许标记不合格: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_MARK_FAIL);
        }
        product.setStatus(ProductStatusEnum.FAIL.getCode());
        product.setQcResult(QcResultEnum.FAIL.getCode());
        product.setQcRemark(reason);
        product.setQcTime(LocalDateTime.now());
        product.setQcUserId(StpUtil.getLoginIdAsLong());
        productMapper.updateById(product);
        recordMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getId, product.getProductionRecordId())
                .setSql("unqualified_count = unqualified_count + 1"));
        log.info("标记产品质检不合格: productId={}, productNo={}, reason={}", productId, product.getProductNo(), reason);
    }

    /**
     * 质检完成，流转到包装
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferToPacking(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (!FlowStatusEnum.QC_IN_PROGRESS.getValue().equals(record.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_TRANSFER_TO_PACK);
        }
        // 校验产品状态：必须全部合格或已取消
        long inProcessCount = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.IN_PROCESS.getCode()));
        if (inProcessCount > 0) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_HAS_NOT_QC);
        }
        long failCount = productMapper.selectCount(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.FAIL.getCode()));
        if (failCount > 0) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_HAS_FAIL);
        }

        // 记录质检员信息
        Long userId = StpUtil.getLoginIdAsLong();
        com.yigongbao.module.system.user.entity.UserEntity currentUser = userMapper.selectById(userId);
        String realName = currentUser != null ? currentUser.getRealName() : null;

        record.setQcId(userId);
        record.setQcName(realName);
        record.setStatus(FlowStatusEnum.PACKING.getValue());
        recordMapper.updateById(record);

        // 检查所有流转卡是否都进入包装状态，是则同步订单状态
        long totalActive = recordMapper.selectCount(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, record.getOrderId())
                .notIn(ProductionRecordEntity::getStatus,
                        FlowStatusEnum.PRINT_FAILED.getValue(),
                        FlowStatusEnum.REWORK.getValue(),
                        FlowStatusEnum.CANCELLED.getValue()));
        long packingCount = recordMapper.selectCount(new LambdaQueryWrapper<ProductionRecordEntity>()
                .eq(ProductionRecordEntity::getOrderId, record.getOrderId())
                .eq(ProductionRecordEntity::getStatus, FlowStatusEnum.PACKING.getValue()));
        if (totalActive == packingCount) {
            orderMainMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.yigongbao.common.entity.OrderMainEntity>()
                    .eq(com.yigongbao.common.entity.OrderMainEntity::getId, record.getOrderId())
                    .set(com.yigongbao.common.entity.OrderMainEntity::getStatus, FlowStatusEnum.PACKING.getValue()));
            log.info("所有流转卡进入包装，同步订单状态: orderId={}", record.getOrderId());
        }
        log.info("质检完成，流转到包装: recordId={}, recordNo={}, qcId={}, qcName={}, orderId={}",
                recordId, record.getRecordNo(), userId, realName, record.getOrderId());
    }

    /** 查询流转卡下所有产品列表，按 ID 升序排列 */
    @Override
    public List<ProductionProductVO> listProductsByRecordId(Long recordId) {
        return productMapper.selectList(new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId)
                        .orderByAsc(ProductionProductEntity::getId))
                .stream()
                .map(p -> BeanUtil.copyProperties(p, ProductionProductVO.class))
                .collect(Collectors.toList());
    }

    /** 分页查询质检流转卡列表；未指定状态时默认查询质检中的流转卡 */
    @Override
    public IPage<ProductionRecordVO> listQcRecords(ProductionQcPageDTO dto) {
        ProductionRecordPageDTO pageDTO = new ProductionRecordPageDTO();
        pageDTO.setPageNum(dto.getPageNum());
        pageDTO.setPageSize(dto.getPageSize());
        pageDTO.setKeyword(dto.getKeyword());
        if (dto.getStatus() != null) {
            // 指定状态：精确匹配
            pageDTO.setStatus(dto.getStatus());
            pageDTO.setIncludeFollowingStatuses(false);
            log.info("质检列表查询-指定状态: status={}, includeFollowingStatuses=false", dto.getStatus());
        } else {
            // 未指定状态：查询质检中及所有后续状态
            pageDTO.setStatus(FlowStatusEnum.QC_IN_PROGRESS.getValue());
            pageDTO.setIncludeFollowingStatuses(true);
            log.info("质检列表查询-默认: status={}, includeFollowingStatuses=true", FlowStatusEnum.QC_IN_PROGRESS.getValue());
        }
        return recordService.pageRecords(pageDTO);
    }

    /**
     * 批量更新产品UDI码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateUdi(BatchUpdateUdiDTO dto) {
        // 1. 校验流转卡状态
        ProductionRecordEntity record = recordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }
        if (record.getStatus() < FlowStatusEnum.PRINTING.getValue()) {
            throw new BusinessException(ErrorCodeEnum.RECORD_STATUS_NOT_ALLOW_UPDATE_UDI);
        }

        // 2. 校验订单类型（仅医疗器械）
        if (!ProductionConstants.ORDER_TYPE_MEDICAL.equals(record.getOrderType())) {
            throw new BusinessException(ErrorCodeEnum.NON_MEDICAL_NOT_ALLOW_UDI);
        }

        // 3. 批量校验UDI唯一性（性能优化：批量查询）
        List<String> udiCodes = dto.getProducts().stream()
            .map(BatchUpdateUdiDTO.ProductUdiItem::getUdiCode)
            .collect(Collectors.toList());
        List<Long> productIds = dto.getProducts().stream()
            .map(BatchUpdateUdiDTO.ProductUdiItem::getProductId)
            .collect(Collectors.toList());

        // 检查请求内部是否有重复UDI
        Set<String> uniqueUdis = new HashSet<>(udiCodes);
        if (uniqueUdis.size() < udiCodes.size()) {
            throw new BusinessException(ErrorCodeEnum.UDI_CODE_EXISTS);
        }

        long duplicateCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .in(ProductionProductEntity::getUdiCode, udiCodes)
                .notIn(ProductionProductEntity::getId, productIds));
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCodeEnum.UDI_CODE_EXISTS);
        }

        // 空值校验
        for (BatchUpdateUdiDTO.ProductUdiItem item : dto.getProducts()) {
            if (StrUtil.isBlank(item.getUdiCode())) {
                throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "UDI码不能为空");
            }
        }

        // 4. 批量更新产品UDI码（使用lambdaUpdate确保字段正确更新）
        LocalDateTime now = LocalDateTime.now();
        for (BatchUpdateUdiDTO.ProductUdiItem item : dto.getProducts()) {
            productService.lambdaUpdate()
                .set(ProductionProductEntity::getUdiCode, item.getUdiCode())
                .set(ProductionProductEntity::getUdiGenerateTime, now)
                .eq(ProductionProductEntity::getId, item.getProductId())
                .update();
        }

        // 记录详细日志
        String details = dto.getProducts().stream()
            .map(item -> "productId=" + item.getProductId() + ",udi=" + item.getUdiCode())
            .collect(Collectors.joining("; "));
        log.info("批量更新产品UDI码: recordId={}, productCount={}, details=[{}]",
            dto.getRecordId(), dto.getProducts().size(), details);
    }
}
