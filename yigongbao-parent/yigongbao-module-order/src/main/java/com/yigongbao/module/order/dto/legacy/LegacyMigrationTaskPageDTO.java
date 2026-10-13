package com.yigongbao.module.order.dto.legacy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/** 历史订单迁移任务分页查询参数。 */
@Data
public class LegacyMigrationTaskPageDTO implements Serializable {
    @Min(1)
    private long pageNum = 1;
    @Min(1)
    @Max(100)
    private long pageSize = 20;
    /** 可选：PENDING、RUNNING、SUCCESS、PARTIAL_SUCCESS、FAILED。 */
    private String status;
}
