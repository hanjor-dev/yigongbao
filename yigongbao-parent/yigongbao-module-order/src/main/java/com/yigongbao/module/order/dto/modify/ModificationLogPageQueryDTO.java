package com.yigongbao.module.order.dto.modify;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改留痕分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ModificationLogPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码（默认1）
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 100, message = "每页条数最大值为100")
    private Integer pageSize = 10;

    /**
     * 修改开始日期（可选，格式：yyyy-MM-dd）
     */
    private String startDate;

    /**
     * 修改结束日期（可选，格式：yyyy-MM-dd）
     */
    private String endDate;

    /**
     * 筛选字段名（精确匹配，可选）
     */
    private String fieldName;
}
