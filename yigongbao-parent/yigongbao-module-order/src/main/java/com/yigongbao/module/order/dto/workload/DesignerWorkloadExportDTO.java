package com.yigongbao.module.order.dto.workload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设计师工作量导出 DTO
 *
 * @author hanjor
 * @date 2026-06-10
 */
@Data
public class DesignerWorkloadExportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime createTimeStart;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime createTimeEnd;
}
