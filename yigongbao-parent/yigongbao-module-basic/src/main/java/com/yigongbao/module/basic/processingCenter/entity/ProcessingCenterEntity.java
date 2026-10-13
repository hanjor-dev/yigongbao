package com.yigongbao.module.basic.processingCenter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 加工中心实体类
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("processing_center")
public class ProcessingCenterEntity extends BaseEntity {

    /** 中心编码（唯一） */
    private String centerCode;

    /** 中心名称 */
    private String centerName;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 可用设备ID范围（JSON格式） */
    private String deviceIdRanges;

    /** 状态（0=禁用，1=启用） */
    private Integer status;

    /** 连接状态（0=离线，1=在线） */
    private Integer connectionStatus;

    /** 最后心跳时间 */
    private java.time.LocalDateTime lastHeartbeat;

    /** 备注 */
    private String remark;
}
