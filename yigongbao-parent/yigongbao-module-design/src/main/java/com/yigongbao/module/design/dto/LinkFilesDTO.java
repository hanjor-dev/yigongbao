package com.yigongbao.module.design.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 关联文件请求 DTO（支持批量）
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@Schema(description = "关联文件请求（支持批量）")
public class LinkFilesDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "文件ID列表（已通过 FileController 上传）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件ID列表不能为空")
    private List<String> fileIds;
}
