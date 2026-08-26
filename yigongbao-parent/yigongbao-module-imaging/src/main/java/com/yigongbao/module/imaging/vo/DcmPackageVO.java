package com.yigongbao.module.imaging.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DCM影像数据包 VO
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
@Schema(description = "DCM影像数据包")
public class DcmPackageVO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件访问地址")
    private String fileUrl;
    private String downloadUrl;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "数据包编号")
    private String packageNo;

    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
}
