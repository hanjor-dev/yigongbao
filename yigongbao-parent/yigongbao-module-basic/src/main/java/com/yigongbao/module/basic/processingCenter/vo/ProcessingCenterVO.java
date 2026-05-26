package com.yigongbao.module.basic.processingCenter.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 加工中心视图对象
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class ProcessingCenterVO {

    /** 加工中心ID */
    private Long id;

    /** 中心编码（唯一） */
    private String centerCode;

    /** 中心名称 */
    private String centerName;

    /** 联系人姓名 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 可用设备ID范围（JSON数组格式，如 [{"start":"P001","end":"P099"}]） */
    private String deviceIdRanges;

    /** 状态（0=禁用，1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
