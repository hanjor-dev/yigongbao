package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建加工中心请求 DTO
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class CreateProcessingCenterDTO {

    /** 中心编码（唯一，不可重复） */
    @NotBlank(message = "中心编码不能为空")
    private String centerCode;

    /** 中心名称 */
    @NotBlank(message = "中心名称不能为空")
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

    /** 备注 */
    private String remark;
}
