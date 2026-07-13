package com.yigongbao.module.production.device.service;

/**
 * 设备每日使用次数计数器服务接口
 * <p>
 * 提供设备每日使用次数的统计和查询功能，支持并发安全的计数器累加。
 * 用于生成产品编号时获取设备当日使用次数。
 * </p>
 *
 * @author hanjor
 * @date 2026-07-13
 */
public interface IDeviceUsageCounterService {

    /**
     * 累加设备当日使用次数并返回新值
     * <p>
     * 使用乐观锁机制保证并发安全，最多重试3次。
     * 首次调用时自动创建当日计数记录（初始值为1）。
     * </p>
     *
     * @param deviceId 设备ID
     * @return 累加后的使用次数
     * @throws com.yigongbao.common.exception.BusinessException 当重试3次后仍然更新失败时抛出
     */
    Integer incrementAndGet(Long deviceId);

    /**
     * 查询设备当日使用次数（不累加）
     * <p>
     * 仅查询当前计数值，不进行累加操作。
     * 若当日尚未使用该设备，返回0。
     * </p>
     *
     * @param deviceId 设备ID
     * @return 当日使用次数，不存在时返回0
     */
    Integer getTodayCount(Long deviceId);
}
