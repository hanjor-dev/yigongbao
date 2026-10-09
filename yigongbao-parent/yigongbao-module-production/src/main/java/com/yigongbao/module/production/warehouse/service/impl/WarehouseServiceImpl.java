package com.yigongbao.module.production.warehouse.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.constant.ColumnConfigConstants;
import com.yigongbao.common.util.ColumnConfigMergeUtil;
import com.yigongbao.common.enums.SystemConfigKeyEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.flow.enums.FlowActionEnum;
import com.yigongbao.flow.enums.FlowStatusEnum;
import com.yigongbao.flow.service.FlowStatusColorResolver;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.vo.ProductionProductVO;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import com.yigongbao.module.production.record.service.IProductionRecordService;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseStatisticsQueryDTO;
import com.yigongbao.module.production.warehouse.dto.ListWarehouseProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseInProductDTO;
import com.yigongbao.module.production.warehouse.dto.WarehouseOutProductDTO;
import com.yigongbao.module.production.warehouse.dto.SaveWarehouseColumnConfigDTO;
import com.yigongbao.module.production.warehouse.service.IWarehouseService;
import com.yigongbao.module.production.warehouse.vo.WarehouseDetailVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseProductVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseRecordVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseStatisticsVO;
import com.yigongbao.module.production.warehouse.vo.WarehouseColumnConfigVO;
import com.yigongbao.module.production.util.ColumnConfigValidator;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 仓储管理服务实现
 *
 * @author hanjor
 * @date 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements IWarehouseService {

    private final ProductionProductMapper productMapper;
    private final ProductionRecordMapper recordMapper;
    private final IProductionRecordService recordService;
    private final UserService userService;
    private final ConfigService configService;
    private final ObjectMapper objectMapper;
    private final FlowStatusColorResolver flowStatusColorResolver;
    private final OrderMainMapper orderMainMapper;

    @Override
    public WarehouseStatisticsVO getStatistics(WarehouseStatisticsQueryDTO dto) {
        if (dto == null) {
            dto = new WarehouseStatisticsQueryDTO();
        }
        dto.setWarehouseInTimeEnd(toExclusiveEndTime(dto.getWarehouseInTimeEnd()));
        dto.setWarehouseOutTimeEnd(toExclusiveEndTime(dto.getWarehouseOutTimeEnd()));
        WarehouseStatisticsVO result = recordMapper.selectWarehouseStatistics(dto);
        return result == null ? new WarehouseStatisticsVO() : result;
    }

    @Override
    public IPage<WarehouseRecordVO> listWarehouse(ListWarehouseDTO dto) {
        dto.setWarehouseInTimeEnd(toExclusiveEndTime(dto.getWarehouseInTimeEnd()));
        dto.setWarehouseOutTimeEnd(toExclusiveEndTime(dto.getWarehouseOutTimeEnd()));
        Page<WarehouseRecordVO> page = new Page<>(dto.getPage(), dto.getSize());
        return recordMapper.listWarehouse(page, dto).convert(vo -> {
            vo.setStatusColor(flowStatusColorResolver.getColor(vo.getStatus()));
            return vo;
        });
    }

    private LocalDateTime toExclusiveEndTime(LocalDateTime endTime) {
        return endTime == null ? null : endTime.toLocalDate().plusDays(1).atStartOfDay();
    }

    @Override
    public WarehouseColumnConfigVO getColumnConfig() {
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            UserEntity user = userService.getById(currentUserId);
            if (user != null && StrUtil.isNotBlank(user.getWarehouseColumnSettings())) {
                try {
                    WarehouseColumnConfigVO config = objectMapper.readValue(user.getWarehouseColumnSettings(), WarehouseColumnConfigVO.class);
                    if (config != null && Integer.valueOf(ColumnConfigConstants.CURRENT_VERSION).equals(config.getVersion())) return config;
                    WarehouseColumnConfigVO merged = mergeWithDefault(config, getSystemDefaultColumnConfig());
                    persistMigratedConfig(user.getId(), user.getWarehouseColumnSettings(), merged);
                    return merged;
                } catch (JsonProcessingException e) {
                    log.warn("解析用户仓储列配置失败，降级为系统默认，userId={}", currentUserId, e);
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户仓储列配置失败，使用系统默认配置", e);
        }

        String configJson = configService.getConfigValue(SystemConfigKeyEnum.WAREHOUSE_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) {
            log.warn("系统默认仓储列配置为空");
            return new WarehouseColumnConfigVO();
        }
        try {
            WarehouseColumnConfigVO config = objectMapper.readValue(configJson, WarehouseColumnConfigVO.class);
            if (config != null) config.setVersion(ColumnConfigConstants.CURRENT_VERSION);
            return config;
        } catch (JsonProcessingException e) {
            log.error("解析系统仓储列配置失败", e);
            return new WarehouseColumnConfigVO();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumnConfig(SaveWarehouseColumnConfigDTO dto) {
        ColumnConfigValidator.validate("warehouse", dto == null ? null : dto.getColumns());
        Long currentUserId;
        try {
            currentUserId = StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.error("获取当前登录用户失败", e);
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }

        UserEntity user = userService.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        WarehouseColumnConfigVO configVO = new WarehouseColumnConfigVO();
        if (dto.getColumns() != null) {
            List<WarehouseColumnConfigVO.ColumnItemVO> columnItems = dto.getColumns().stream()
                    .map(item -> {
                        WarehouseColumnConfigVO.ColumnItemVO column = new WarehouseColumnConfigVO.ColumnItemVO();
                        column.setField(item.getField());
                        column.setLabel(item.getLabel());
                        column.setVisible(item.getVisible());
                        column.setSort(item.getSort());
                        column.setWidth(item.getWidth());
                        column.setFixed(item.getFixed());
                        return column;
                    }).collect(Collectors.toList());
            configVO.setColumns(columnItems);
        }
        configVO = mergeWithDefault(configVO, getSystemDefaultColumnConfig());
        configVO.setVersion(ColumnConfigConstants.CURRENT_VERSION);

        try {
            user.setWarehouseColumnSettings(objectMapper.writeValueAsString(configVO));
            userService.updateById(user);
            log.info("保存仓储列配置成功: userId={}", currentUserId);
        } catch (JsonProcessingException e) {
            log.error("序列化仓储列配置失败: userId={}", currentUserId, e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    public WarehouseDetailVO getWarehouseDetail(Long recordId) {
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeEnum.PRODUCTION_RECORD_NOT_FOUND);
        }

        WarehouseDetailVO vo = BeanUtil.copyProperties(record, WarehouseDetailVO.class);
        vo.setStatusColor(flowStatusColorResolver.getColor(record.getStatus()));
        vo.setRecordId(record.getId());
        vo.setOrderCode(record.getOrderCode());
        OrderMainEntity order = orderMainMapper.selectById(record.getOrderId());
        if (order != null) {
            vo.setPublicOrderCode(order.getPublicOrderCode());
        }

        List<ProductionProductEntity> products = productMapper.selectList(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .orderByAsc(ProductionProductEntity::getId)
        );

        vo.setTotalCount(products.size());
        vo.setPendingWarehouseInCount((int) products.stream()
            .filter(p -> ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode().equals(p.getStatus())).count());
        vo.setWarehousedCount((int) products.stream()
            .filter(p -> ProductStatusEnum.WAREHOUSED.getCode().equals(p.getStatus())).count());
        vo.setWarehouseOutCount((int) products.stream()
            .filter(p -> ProductStatusEnum.WAREHOUSE_OUT.getCode().equals(p.getStatus())).count());
        vo.setCancelledCount((int) products.stream()
            .filter(p -> ProductStatusEnum.CANCELLED.getCode().equals(p.getStatus())).count());

        vo.setProducts(products.stream()
            .map(p -> BeanUtil.copyProperties(p, ProductionProductVO.class))
            .collect(Collectors.toList()));

        return vo;
    }

    @Override
    public IPage<WarehouseProductVO> listWarehouseProducts(ListWarehouseProductDTO dto) {
        dto.setWarehouseInTimeEnd(toExclusiveEndTime(dto.getWarehouseInTimeEnd()));
        dto.setWarehouseOutTimeEnd(toExclusiveEndTime(dto.getWarehouseOutTimeEnd()));
        Page<WarehouseProductVO> page = new Page<>(dto.getPage(), dto.getSize());
        return recordMapper.listWarehouseProducts(page, dto);
    }

    private WarehouseColumnConfigVO getSystemDefaultColumnConfig() {
        String configJson = configService.getConfigValue(SystemConfigKeyEnum.WAREHOUSE_COLUMN_CONFIG.getKey());
        if (StrUtil.isBlank(configJson)) return new WarehouseColumnConfigVO();
        try { WarehouseColumnConfigVO config = objectMapper.readValue(configJson, WarehouseColumnConfigVO.class);
            return config;
        } catch (JsonProcessingException e) { log.error("解析系统仓储列配置失败", e); return new WarehouseColumnConfigVO(); }
    }

    private WarehouseColumnConfigVO mergeWithDefault(WarehouseColumnConfigVO userConfig, WarehouseColumnConfigVO defaultConfig) {
        if (defaultConfig == null) return userConfig;
        if (userConfig == null) return defaultConfig;
        userConfig.setColumns(ColumnConfigMergeUtil.mergeMissingColumns(userConfig.getColumns(), defaultConfig.getColumns(),
                WarehouseColumnConfigVO.ColumnItemVO::getField,
                column -> { WarehouseColumnConfigVO.ColumnItemVO copy = new WarehouseColumnConfigVO.ColumnItemVO();
                    copy.setField(column.getField()); copy.setLabel(column.getLabel()); copy.setVisible(column.getVisible());
                    copy.setSort(column.getSort()); copy.setWidth(column.getWidth()); copy.setFixed(column.getFixed()); return copy; },
                WarehouseColumnConfigVO.ColumnItemVO::getSort,
                (column, sort) -> { column.setSort(sort); return column; }));
        userConfig.setVersion(ColumnConfigConstants.CURRENT_VERSION); return userConfig;
    }

    private void persistMigratedConfig(Long userId, String originalJson, WarehouseColumnConfigVO config) {
        if (userId == null || config == null) return;
        try {
            String migratedJson = objectMapper.writeValueAsString(config);
            userService.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserEntity>()
                    .eq(UserEntity::getId, userId)
                    .eq(UserEntity::getWarehouseColumnSettings, originalJson)
                    .set(UserEntity::getWarehouseColumnSettings, migratedJson));
        } catch (Exception e) {
            log.warn("回写升级后的仓储列配置失败，userId={}", userId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void warehouseInProduct(Long productId, WarehouseInProductDTO dto) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (!ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许入库: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_IN);
        }

        product.setStatus(ProductStatusEnum.WAREHOUSED.getCode());
        product.setWarehouseInTime(LocalDateTime.now());
        product.setWarehouseInUserId(StpUtil.getLoginIdAsLong());
        product.setWarehouseInRemark(dto.getRemark());
        productMapper.updateById(product);

        log.info("产品入库: productId={}, productNo={}", productId, product.getProductNo());

        checkAndTransferRecordToWarehoused(product.getProductionRecordId());
    }

    private void checkAndTransferRecordToWarehoused(Long recordId) {
        long pendingCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.PENDING_WAREHOUSE_IN.getCode())
        );

        if (pendingCount > 0) {
            log.info("流转卡下仍有待入库产品: recordId={}, pendingCount={}", recordId, pendingCount);
            return;
        }

        long warehousedCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSED.getCode())
        );

        long cancelledCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode())
        );

        if (warehousedCount == 0) {
            log.warn("流转卡下没有已入库产品，跳过聚合: recordId={}, cancelledCount={}", recordId, cancelledCount);
            return;
        }

        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null || !FlowStatusEnum.PENDING_WAREHOUSE_IN.getValue().equals(record.getStatus())) {
            return;
        }

        record.setStatus(FlowStatusEnum.WAREHOUSED.getValue());
        recordMapper.updateById(record);

        log.info("流转卡全部产品已入库: recordId={}, recordNo={}, warehousedCount={}, cancelledCount={}",
            recordId, record.getRecordNo(), warehousedCount, cancelledCount);

        // 触发订单聚合：检查所有流转卡是否都已入库
        recordService.triggerFlowIfAllReach(
            record.getOrderId(),
            FlowStatusEnum.WAREHOUSED.getValue(),
            FlowActionEnum.COMPLETE_WAREHOUSE_IN
        );
        recordService.reconcileOrderProductionStatus(record.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void warehouseOutProduct(Long productId, WarehouseOutProductDTO dto) {
        ProductionProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (!ProductStatusEnum.WAREHOUSED.getCode().equals(product.getStatus())) {
            log.warn("产品状态不允许出库: productId={}, currentStatus={}", productId, product.getStatus());
            throw new BusinessException(ErrorCodeEnum.PRODUCT_STATUS_NOT_ALLOW_WAREHOUSE_OUT);
        }

        product.setStatus(ProductStatusEnum.WAREHOUSE_OUT.getCode());
        product.setWarehouseOutTime(LocalDateTime.now());
        product.setWarehouseOutUserId(StpUtil.getLoginIdAsLong());
        product.setWarehouseOutRemark(dto.getRemark());
        productMapper.updateById(product);

        log.info("产品出库: productId={}, productNo={}", productId, product.getProductNo());

        checkAndTransferRecordToWarehouseOut(product.getProductionRecordId());
    }

    private void checkAndTransferRecordToWarehouseOut(Long recordId) {
        long warehousedCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSED.getCode())
        );

        if (warehousedCount > 0) {
            log.info("流转卡下仍有已入库产品: recordId={}, warehousedCount={}", recordId, warehousedCount);
            return;
        }

        long warehouseOutCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.WAREHOUSE_OUT.getCode())
        );

        long cancelledCount = productMapper.selectCount(
            new LambdaQueryWrapper<ProductionProductEntity>()
                .eq(ProductionProductEntity::getProductionRecordId, recordId)
                .eq(ProductionProductEntity::getStatus, ProductStatusEnum.CANCELLED.getCode())
        );

        if (warehouseOutCount == 0) {
            log.warn("流转卡下没有已出库产品，跳过聚合: recordId={}, cancelledCount={}", recordId, cancelledCount);
            return;
        }

        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null || !FlowStatusEnum.WAREHOUSED.getValue().equals(record.getStatus())) {
            return;
        }

        record.setStatus(FlowStatusEnum.WAREHOUSE_OUT.getValue());
        recordMapper.updateById(record);

        log.info("流转卡全部产品已出库: recordId={}, recordNo={}, warehouseOutCount={}, cancelledCount={}",
            recordId, record.getRecordNo(), warehouseOutCount, cancelledCount);

        recordService.triggerFlowIfAllReach(
            record.getOrderId(),
            FlowStatusEnum.WAREHOUSE_OUT.getValue(),
            FlowActionEnum.COMPLETE_WAREHOUSE_OUT
        );
        recordService.reconcileOrderProductionStatus(record.getOrderId());
    }
}
