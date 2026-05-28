package com.yigongbao.module.production.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产产品服务实现
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionProductServiceImpl extends ServiceImpl<ProductionProductMapper, ProductionProductEntity>
        implements IProductionProductService {

    private final ProductionRecordMapper recordMapper;

    @Override
    public List<ProductionProductEntity> listByRecordId(Long recordId) {
        return list(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getId));
    }

    @Override
    public ProductionProductEntity getByProductNo(String productNo) {
        ProductionProductEntity product = getOne(new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductNo, productNo));
        if (product == null) {
            log.warn("产品不存在: productNo={}", productNo);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long productId, String status) {
        ProductionProductEntity product = getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NOT_FOUND);
        }
        String oldStatus = product.getStatus();
        product.setStatus(status);
        updateById(product);
        log.info("更新产品状态: productId={}, productNo={}, {} -> {}",
                productId, product.getProductNo(), oldStatus, status);
    }

    /**
     * 分页查询产品明细列表
     * <p>
     * keyword 模糊匹配：订单号、数据包编号、流转卡编号、产品名称、患者姓名
     * 先按 keyword 过滤流转卡，再分页查产品，最后批量回填流转卡信息
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<ProductionProductDetailVO> pageProductDetails(ProductionProductPageDTO dto) {
        // 1. 分页查产品，keyword 同时匹配产品名 OR 流转卡关联字段（订单号、数据包编号、流转卡编号、患者姓名）
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<ProductionProductEntity>()
                .orderByDesc(ProductionProductEntity::getId);
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            String kw = dto.getKeyword();
            // 先查流转卡匹配的 recordId 集合
            List<Long> recordIds = recordMapper.selectList(
                    new LambdaQueryWrapper<ProductionRecordEntity>()
                            .and(w -> w
                                    .like(ProductionRecordEntity::getOrderCode, kw)
                                    .or().like(ProductionRecordEntity::getDesignPackageCode, kw)
                                    .or().like(ProductionRecordEntity::getRecordNo, kw)
                                    .or().like(ProductionRecordEntity::getPatientName, kw))
                            .select(ProductionRecordEntity::getId))
                    .stream().map(ProductionRecordEntity::getId).collect(Collectors.toList());
            // 产品名 OR 流转卡匹配（两者均无结果才返回空）
            wrapper.and(w -> {
                w.like(ProductionProductEntity::getProductName, kw);
                if (!recordIds.isEmpty()) {
                    w.or().in(ProductionProductEntity::getProductionRecordId, recordIds);
                }
            });
        }

        Page<ProductionProductEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ProductionProductEntity> result = page(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return result.convert(e -> new ProductionProductDetailVO());
        }

        // 3. 批量查流转卡，避免 N+1
        Set<Long> relatedRecordIds = result.getRecords().stream()
                .map(ProductionProductEntity::getProductionRecordId).collect(Collectors.toSet());
        Map<Long, ProductionRecordEntity> recordMap = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .in(ProductionRecordEntity::getId, relatedRecordIds))
                .stream().collect(Collectors.toMap(ProductionRecordEntity::getId, r -> r, (a, b) -> a));

        // 4. 组装 VO
        return result.convert(p -> {
            ProductionProductDetailVO vo = new ProductionProductDetailVO();
            BeanUtil.copyProperties(p, vo);
            ProductionRecordEntity record = recordMap.get(p.getProductionRecordId());
            if (record != null) {
                vo.setRecordNo(record.getRecordNo());
                vo.setProductionBatchNo(record.getProductionBatchNo());
                vo.setRecordStatus(record.getStatus());
                vo.setOrderId(record.getOrderId());
                vo.setOrderCode(record.getOrderCode());
                vo.setOrderType(record.getOrderType());
                vo.setDesignPackageCode(record.getDesignPackageCode());
                vo.setHospitalName(record.getHospitalName());
                vo.setHospitalDeptName(record.getHospitalDeptName());
                vo.setDoctorName(record.getDoctorName());
                vo.setPatientName(record.getPatientName());
                vo.setIsUrgent(record.getIsUrgent());
                vo.setIsPostal(record.getIsPostal());
                vo.setExpectedDeliveryDate(record.getExpectedDeliveryDate());
            }
            return vo;
        });
    }
}
