package com.yigongbao.module.production.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.device.entity.DeviceEntity;
import com.yigongbao.module.basic.device.mapper.DeviceMapper;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.production.product.entity.ProductionProductEntity;
import com.yigongbao.module.production.product.mapper.ProductionProductMapper;
import com.yigongbao.module.production.product.service.IProductNumberService;
import com.yigongbao.module.production.record.entity.ProductionRecordEntity;
import com.yigongbao.module.production.record.mapper.ProductionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品编号服务实现类
 * 负责产品编号的生成和管理
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductNumberServiceImpl extends ServiceImpl<ProductionProductMapper, ProductionProductEntity>
        implements IProductNumberService {

    // 产品类型代码映射
    private static final String PRODUCT_NAME_SURGICAL_GUIDE = "医用个性化手术导板";
    private static final String PRODUCT_NAME_BONE_MODEL = "定制式3D打印骨模型";
    private static final String PRODUCT_NAME_NEURO_GUIDE = "定制式神经外科手术导板";
    private static final String PRODUCT_NAME_RADIOACTIVE_GUIDE = "定制式放射粒子手术导板";

    private static final String PRODUCT_CODE_SURGICAL_GUIDE = "A";
    private static final String PRODUCT_CODE_BONE_MODEL = "B";
    private static final String PRODUCT_CODE_NEURO_GUIDE = "C";
    private static final String PRODUCT_CODE_RADIOACTIVE_GUIDE = "D";
    private static final String PRODUCT_CODE_OTHER = "X";

    // 编号格式
    private static final String DEVICE_NO_FORMAT = "%03d";
    private static final String USAGE_COUNT_FORMAT = "%02d";
    private static final String SEQUENCE_NO_FORMAT = "%02d";

    private final ProductionRecordMapper recordMapper;
    private final ProductionProductMapper productMapper;
    private final DesignProductMapper designProductMapper;
    private final DeviceMapper deviceMapper;

    /**
     * 为流转卡下的所有产品生成正式编号
     * 编号格式：生产批号(6位) + 产品代码(1位) + 设备编号(3位) + 上机次数(2位) + 产品流水号(2位)
     *
     * @param recordId 流转卡ID
     * @param deviceId 设备ID
     * @param usageCount 设备当日上机次数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateFormalNumbers(Long recordId, Long deviceId, Integer usageCount) {
        // 1. 查询流转卡信息
        ProductionRecordEntity record = recordMapper.selectById(recordId);
        if (record == null) {
            log.error("流转卡不存在: recordId={}", recordId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 2. 查询设备信息
        DeviceEntity device = deviceMapper.selectById(deviceId);
        if (device == null) {
            log.error("设备不存在: deviceId={}", deviceId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 3. 获取生产批号和设备编号
        String batchNo = record.getProductionBatchNo();

        // 提取设备编号的数字部分（格式如"SLA-001"，取"-"后的"001"）
        String deviceIdStr = device.getDeviceId();
        int deviceIdNum;
        try {
            // 如果包含"-"，提取后半部分；否则直接解析整个字符串
            String numericPart = deviceIdStr.contains("-")
                ? deviceIdStr.substring(deviceIdStr.lastIndexOf("-") + 1)
                : deviceIdStr;

            deviceIdNum = Integer.parseInt(numericPart);
            if (deviceIdNum < 1 || deviceIdNum > 999) {
                log.error("设备编号超出范围: deviceId={}, numericPart={}, value={}",
                    deviceId, numericPart, deviceIdNum);
                throw new BusinessException(ErrorCodeEnum.DEVICE_ID_OUT_OF_RANGE);
            }
        } catch (NumberFormatException e) {
            log.error("设备编号格式无效: deviceId={}, deviceIdStr={}", deviceId, deviceIdStr, e);
            throw new BusinessException(ErrorCodeEnum.DEVICE_ID_INVALID_FORMAT);
        }
        String deviceNo = String.format(DEVICE_NO_FORMAT, deviceIdNum);

        // 4. 查询流转卡下的所有产品
        List<ProductionProductEntity> products = getProductsInOrder(recordId, record.getDesignPackageId());
        if (products.isEmpty()) {
            log.warn("流转卡无产品: recordId={}", recordId);
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 5. 批量生成产品编号（原子递增的usageCount保证唯一性）
        int sequenceNo = 1;
        for (ProductionProductEntity product : products) {
            String productNo = generateSingleNumber(
                    batchNo,
                    product.getProductName(),
                    deviceNo,
                    usageCount,
                    sequenceNo
            );
            if (!checkUniqueness(productNo)) {
                log.error("产品编号重复: recordId={}, productNo={}", recordId, productNo);
                throw new BusinessException(ErrorCodeEnum.PRODUCT_NUMBER_DUPLICATE, productNo);
            }
            product.setProductNo(productNo);
            sequenceNo++;
        }

        // 6. 批量更新（减少数据库交互，数据库唯一索引保证最终唯一性）
        try {
            updateBatchById(products);
        } catch (Exception e) {
            // 捕获唯一约束冲突或其他数据库异常
            log.error("批量更新产品编号失败: recordId={}, deviceId={}, error={}", recordId, deviceId, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.PRODUCT_NUMBER_DUPLICATE,
                products.isEmpty() ? "unknown" : products.get(0).getProductNo());
        }

        log.info("生成正式产品编号: recordId={}, deviceId={}, usageCount={}, productCount={}, numbers={}",
                recordId, deviceId, usageCount, products.size(),
                products.stream().map(ProductionProductEntity::getProductNo).collect(Collectors.joining(",")));
    }

    /**
     * 生成单个产品的正式编号
     * 编号格式：生产批号(6位) + 产品代码(1位) + 设备编号(3位) + 上机次数(2位) + 产品流水号(2位)
     *
     * @param batchNo 生产批号（YYMMDD）
     * @param productName 产品名称
     * @param deviceNo 设备编号（已补齐3位）
     * @param usageCount 上机次数
     * @param sequenceNo 产品流水号
     * @return 正式产品编号（14位）
     */
    @Override
    public String generateSingleNumber(String batchNo, String productName,
                                       String deviceNo, Integer usageCount,
                                       Integer sequenceNo) {
        String productCode = getProductTypeCode(productName);
        if (usageCount == null || usageCount < 0 || usageCount > 99) {
            throw new BusinessException("设备当日上机次数超出产品编号两位长度限制: " + usageCount);
        }
        String usageCountStr = String.format(USAGE_COUNT_FORMAT, usageCount);
        String sequenceNoStr = String.format(SEQUENCE_NO_FORMAT, sequenceNo);
        return batchNo + productCode + deviceNo + usageCountStr + sequenceNoStr;
    }

    /**
     * 根据产品名称获取产品代码
     * 采用精准匹配（equals）产品名称
     *
     * @param productName 产品名称
     * @return 产品代码（A/B/C/D/X）
     */
    @Override
    public String getProductTypeCode(String productName) {
        if (productName == null) {
            return PRODUCT_CODE_OTHER;
        }

        return switch (productName) {
            case PRODUCT_NAME_SURGICAL_GUIDE -> PRODUCT_CODE_SURGICAL_GUIDE;
            case PRODUCT_NAME_BONE_MODEL -> PRODUCT_CODE_BONE_MODEL;
            case PRODUCT_NAME_NEURO_GUIDE -> PRODUCT_CODE_NEURO_GUIDE;
            case PRODUCT_NAME_RADIOACTIVE_GUIDE -> PRODUCT_CODE_RADIOACTIVE_GUIDE;
            default -> PRODUCT_CODE_OTHER;
        };
    }

    /**
     * 校验产品编号唯一性
     *
     * @param productNo 产品编号
     * @return true=唯一，false=重复
     */
    @Override
    public boolean checkUniqueness(String productNo) {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductNo, productNo)
        );
        return count == 0;
    }

    /**
     * 按排序规则查询流转卡下的产品列表
     * 当前按创建时间排序
     * TODO: 后续可通过print_file_id关联design_product获取sort_order进行排序
     *
     * @param recordId 流转卡ID
     * @param designPackageId 设计包ID（预留参数，后续实现sort_order排序时使用）
     * @return 产品列表
     */
    private List<ProductionProductEntity> getProductsInOrder(Long recordId, Long designPackageId) {
        return productMapper.selectList(
                new LambdaQueryWrapper<ProductionProductEntity>()
                        .eq(ProductionProductEntity::getProductionRecordId, recordId)
                        .orderByAsc(ProductionProductEntity::getCreateTime)
        );
    }
}
