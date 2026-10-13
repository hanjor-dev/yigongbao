package com.yigongbao.module.production.product.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.production.product.dto.ProductionProductPageDTO;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductionProductService;
import com.yigongbao.module.production.product.vo.ProductionProductDetailVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.flow.service.FlowStatusColorResolver;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.service.OrderMainService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
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
    private final OrderMainService orderMainService;
    private final UserMapper userMapper;
    private final UserHospitalService userHospitalService;
    private final FlowStatusColorResolver flowStatusColorResolver;

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
     * 【数据权限】：CENTER 类型只能看本加工中心的产品，ALL 类型可看全部
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<ProductionProductDetailVO> pageProductDetails(ProductionProductPageDTO dto) {
        // 数据权限过滤：先查询有权限的流转卡ID列表
        Long currentUserId = StpUtil.getLoginIdAsLong();
        UserEntity currentUser = userMapper.selectById(currentUserId);
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

        List<Long> allowedRecordIds = getAccessibleRecordIds(currentUser, scopeType, dto.getKeyword());
        if (allowedRecordIds.isEmpty()) {
            return new Page<>(dto.getPageNum(), dto.getPageSize());
        }

        // 分页查产品，限定在有权限的流转卡范围内
        LambdaQueryWrapper<ProductionProductEntity> wrapper = new LambdaQueryWrapper<ProductionProductEntity>()
                .in(ProductionProductEntity::getProductionRecordId, allowedRecordIds)
                .orderByDesc(ProductionProductEntity::getId);
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(ProductionProductEntity::getProductName, dto.getKeyword());
        }

        Page<ProductionProductEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ProductionProductEntity> result = page(page, wrapper);
        if (result.getRecords().isEmpty()) {
            return result.convert(e -> new ProductionProductDetailVO());
        }

        // 批量查流转卡，避免 N+1
        Set<Long> relatedRecordIds = result.getRecords().stream()
                .map(ProductionProductEntity::getProductionRecordId).collect(Collectors.toSet());
        Map<Long, ProductionRecordEntity> recordMap = recordMapper.selectList(
                new LambdaQueryWrapper<ProductionRecordEntity>()
                        .in(ProductionRecordEntity::getId, relatedRecordIds))
                .stream().collect(Collectors.toMap(ProductionRecordEntity::getId, r -> r, (a, b) -> a));
        List<Long> relatedOrderIds = recordMap.values().stream()
                        .map(ProductionRecordEntity::getOrderId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
        Map<Long, OrderMainEntity> orderMap = relatedOrderIds.isEmpty()
                ? Collections.emptyMap()
                : orderMainService.listByIds(relatedOrderIds).stream()
                        .collect(Collectors.toMap(OrderMainEntity::getId, o -> o, (a, b) -> a));

        // 组装 VO
        return result.convert(p -> {
            ProductionProductDetailVO vo = new ProductionProductDetailVO();
            BeanUtil.copyProperties(p, vo);
            ProductionRecordEntity record = recordMap.get(p.getProductionRecordId());
            if (record != null) {
                vo.setPrintDeviceCode(record.getPrintDeviceCode());
                vo.setRecordNo(record.getRecordNo());
                vo.setProductionBatchNo(record.getProductionBatchNo());
                vo.setRecordStatus(record.getStatus());
                vo.setRecordStatusColor(flowStatusColorResolver.getColor(record.getStatus()));
                vo.setOrderId(record.getOrderId());
                vo.setOrderCode(record.getOrderCode());
                OrderMainEntity order = orderMap.get(record.getOrderId());
                vo.setPublicOrderCode(order == null ? null : order.getPublicOrderCode());
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

    /**
     * 获取当前用户有权访问的流转卡ID列表
     * CENTER：未分配的全员可见（用于接单），已分配的按订单 center_id 过滤
     */
    private List<Long> getAccessibleRecordIds(UserEntity currentUser, DataScopeTypeEnum scopeType, String keyword) {
        LambdaQueryWrapper<ProductionRecordEntity> wrapper = new LambdaQueryWrapper<>();

        // 数据权限过滤
        switch (scopeType) {
            case CENTER:
                Long centerId = currentUser.getCenterId();
                if (centerId != null) {
                    // 先查本加工中心的订单ID列表
                    List<Long> centerOrderIds = recordMapper.selectList(
                        new LambdaQueryWrapper<ProductionRecordEntity>()
                            .apply("order_id IN (SELECT id FROM order_main WHERE center_id = {0})", centerId)
                            .select(ProductionRecordEntity::getOrderId))
                        .stream().map(ProductionRecordEntity::getOrderId).distinct().collect(Collectors.toList());

                    // 未分配 OR 订单属于本加工中心
                    wrapper.and(w -> {
                        w.isNull(ProductionRecordEntity::getProcessingCenterId);
                        if (!centerOrderIds.isEmpty()) {
                            w.or().in(ProductionRecordEntity::getOrderId, centerOrderIds);
                        }
                    });
                } else {
                    wrapper.isNull(ProductionRecordEntity::getProcessingCenterId);
                }
                break;
            case ALL:
                break;
            default:
                log.warn("产品明细查询不支持的数据权限类型，降级为 CENTER: scopeType={}", scopeType);
                if (currentUser.getCenterId() != null) {
                    List<Long> centerOrderIds = recordMapper.selectList(
                        new LambdaQueryWrapper<ProductionRecordEntity>()
                            .apply("order_id IN (SELECT id FROM order_main WHERE center_id = {0})", currentUser.getCenterId())
                            .select(ProductionRecordEntity::getOrderId))
                        .stream().map(ProductionRecordEntity::getOrderId).distinct().collect(Collectors.toList());

                    wrapper.and(w -> {
                        w.isNull(ProductionRecordEntity::getProcessingCenterId);
                        if (!centerOrderIds.isEmpty()) {
                            w.or().in(ProductionRecordEntity::getOrderId, centerOrderIds);
                        }
                    });
                } else {
                    wrapper.isNull(ProductionRecordEntity::getProcessingCenterId);
                }
                break;
        }

        List<Long> publicCodeOrderIds = Collections.emptyList();
        if (StrUtil.isNotBlank(keyword)) {
            publicCodeOrderIds = orderMainService.list(
                            new LambdaQueryWrapper<OrderMainEntity>()
                                    .like(OrderMainEntity::getPublicOrderCode, keyword)
                                    .select(OrderMainEntity::getId))
                    .stream().map(OrderMainEntity::getId).collect(Collectors.toList());
        }

        // keyword 过滤
        if (StrUtil.isNotBlank(keyword)) {
            final List<Long> matchedPublicCodeOrderIds = publicCodeOrderIds;
            wrapper.and(w -> w
                    .like(ProductionRecordEntity::getOrderCode, keyword)
                    .or().like(ProductionRecordEntity::getDesignPackageCode, keyword)
                    .or().like(ProductionRecordEntity::getRecordNo, keyword)
                    .or().like(ProductionRecordEntity::getPatientName, keyword)
                    .or(!matchedPublicCodeOrderIds.isEmpty(), publicIdWrapper ->
                            publicIdWrapper.in(ProductionRecordEntity::getOrderId, matchedPublicCodeOrderIds)));
        }

        List<ProductionRecordEntity> records = recordMapper.selectList(wrapper.select(ProductionRecordEntity::getId));
        return records.isEmpty() ? Collections.emptyList() :
               records.stream().map(ProductionRecordEntity::getId).collect(Collectors.toList());
    }
}
