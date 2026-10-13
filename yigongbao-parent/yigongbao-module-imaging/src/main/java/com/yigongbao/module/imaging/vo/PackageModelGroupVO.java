package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 订单维度——按数据包分组的模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "按数据包分组的模型文件列表")
public class PackageModelGroupVO {

    @Schema(description = "数据包ID")
    private Long packageId;

    @Schema(description = "数据包编号（如：ORD20260410001-1）")
    private String packageCode;

    @Schema(description = "包内模型文件列表")
    private List<PackageModelFileVO> files;
}
