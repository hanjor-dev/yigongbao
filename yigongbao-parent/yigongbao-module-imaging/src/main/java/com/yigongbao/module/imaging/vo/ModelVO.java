package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 可视化模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "可视化模型文件")
public class ModelVO {

    @Schema(description = "模型ID（design_model.id）")
    private Long modelId;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名（含扩展名）")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;
    private String downloadUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "颜色RGB值（如：255,0,0），匹配不到时为 null")
    private String colorCode;

    @Schema(description = "透明度（0.00~1.00），匹配不到时为 null")
    private BigDecimal opacity;
}
