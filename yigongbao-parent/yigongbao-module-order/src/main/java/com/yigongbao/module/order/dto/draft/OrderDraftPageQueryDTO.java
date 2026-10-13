package com.yigongbao.module.order.dto.draft;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 草稿列表分页查询 DTO（仅分页参数，按创建时间倒序）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class OrderDraftPageQueryDTO {

    /**
     * 页码（默认1）
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;
}
