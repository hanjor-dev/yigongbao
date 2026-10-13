package com.yigongbao.module.order.dto.workload;

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
     * 开始时间，不传则不限制
     */
    private LocalDateTime createTimeStart;

    /**
     * 结束时间，不传则不限制
     */
    private LocalDateTime createTimeEnd;
}
