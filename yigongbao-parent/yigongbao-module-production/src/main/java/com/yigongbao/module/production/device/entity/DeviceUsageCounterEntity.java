package com.yigongbao.module.production.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 设备每日使用次数计数器实体
 * <p>
 * 用于记录每台设备每天的使用次数，支持乐观锁并发控制。
 * </p>
 *
 * @author hanjor
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_daily_usage_counter")
public class DeviceUsageCounterEntity extends BaseEntity {

    /** 设备ID */
    private Long deviceId;

    /** 使用日期 */
    private LocalDate usageDate;

    /** 使用次数 */
    private Integer usageCount;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
