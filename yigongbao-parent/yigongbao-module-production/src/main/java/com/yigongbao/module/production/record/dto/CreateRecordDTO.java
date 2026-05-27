package com.yigongbao.module.production.record.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 创建生产流转卡 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class CreateRecordDTO {
    @NotNull(message = "设计数据包ID不能为空")
    private Long designPackageId;

    @NotNull(message = "打印机ID不能为空")
    private Long printDeviceId;

    private String productionBatchNo;
    private String material;
}
