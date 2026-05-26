package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 更新加工中心请求 DTO（仅更新非空字段）
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class UpdateProcessingCenterDTO {

    /** 加工中心ID（必填） */
    @NotNull(message = "ID不能为空")
    private Long id;

    /** 中心名称 */
    private String centerName;

    /** 联系人姓名 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /**
     * 可用设备ID范围（JSON数组格式，如 [{"start":"P001","end":"P099"}]）
     * 同一范围不允许与其他加工中心的范围交叉或重叠
     */
    private String deviceIdRanges;

    /** 状态（0=禁用，1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
