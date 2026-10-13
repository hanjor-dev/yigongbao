package com.yigongbao.module.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 经典案例查询请求DTO
 * <p>
 * 支持关键词模糊搜索和时间范围筛选。
 * 关键词会同时匹配订单编号、患者姓名、医院名称、机构名称、业务员姓名等字段。
 * </p>
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@Schema(description = "经典案例查询条件")
public class ClassicCaseQueryDTO {

    /**
     * 关键词（模糊匹配：订单编号、患者姓名、医院名称、机构名称、业务员姓名）
     */
    @Schema(description = "关键词，支持订单编号、患者姓名、医院名称等模糊搜索")
    private String keyword;

    /**
     * 医院ID（精确匹配）
     */
    @Schema(description = "医院ID")
    private Long hospitalId;

    /**
     * 开始时间（订单创建时间范围）
     */
    @Schema(description = "创建开始时间")
    private LocalDateTime startTime;

    /**
     * 结束时间（订单创建时间范围）
     */
    @Schema(description = "创建结束时间")
    private LocalDateTime endTime;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
