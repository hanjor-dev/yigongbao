package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据包内模型文件 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "数据包内模型文件")
public class PackageModelFileVO {

    @Schema(description = "包文件ID（design_package_file.id）")
    private Long packageFileId;

    @Schema(description = "文件名（含扩展名）")
    private String fileName;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "包内相对路径")
    private String filePath;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "颜色RGB值（如：170,255,0），匹配不到时为 null")
    private String colorCode;

    @Schema(description = "透明度（0.00~1.00），匹配不到时为 null")
    private BigDecimal opacity;
}
