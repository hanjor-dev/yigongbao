package com.yigongbao.module.production.product.service;

/**
 * 产品编号服务接口
 * 负责产品编号的生成和管理
 *
 * @author hanjor
 * @date 2026-07-13
 */
public interface IProductNumberService {

    /**
     * 为流转卡下的所有产品生成正式编号
     *
     * @param recordId 流转卡ID
     * @param deviceId 设备ID
     * @param usageCount 设备当日上机次数
     */
    void generateFormalNumbers(Long recordId, Long deviceId, Integer usageCount);

    /**
     * 生成单个产品的正式编号
     *
     * @param batchNo 生产批号（YYMMDD）
     * @param productName 产品名称
     * @param deviceNo 设备编号
     * @param usageCount 上机次数
     * @param sequenceNo 产品流水号
     * @return 正式产品编号
     */
    String generateSingleNumber(String batchNo, String productName,
                                String deviceNo, Integer usageCount,
                                Integer sequenceNo);

    /**
     * 根据产品名称获取产品代码
     *
     * @param productName 产品名称
     * @return 产品代码（A/B/C/D/X）
     */
    String getProductTypeCode(String productName);

    /**
     * 校验产品编号唯一性
     *
     * @param productNo 产品编号
     * @return true=唯一，false=重复
     */
    boolean checkUniqueness(String productNo);
}
