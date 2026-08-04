package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 分配打印机 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class AssignDeviceDTO {
    @NotNull(message = "打印机ID不能为空")
    private Long deviceId;
    /** 每个生产产品的重量，单位：克；列表须覆盖当前流转卡全部产品 */
    @Valid
    @NotEmpty(message = "产品重量列表不能为空")
    private List<AssignProductWeightDTO> productWeights;
    /** 打印材质 */
    private String material;
    /** 打印参数（JSON格式，如层厚、支撑密度等） */
    private String printParams;
}
