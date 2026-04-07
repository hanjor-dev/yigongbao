package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单分页查询 DTO
 * 用于 POST /api/order/page 接口，替代原来的 GET /api/order/list
 *
 * @author hanjor
 * @date 2026-04-03
 */
@Data
public class OrderPageDTO implements Serializable {

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
     * 订单编号（可选，模糊查询）
     */
    private String orderCode;

    /**
     * 医院ID（可选，必须在用户权限范围内，否则返回空列表）
     */
    private Long hospitalId;

    /**
     * 地区ID（可选，精确匹配 area_id）
     */
    private Long areaId;

    /**
     * 医生姓名（可选，模糊查询）
     */
    private String doctorName;

    /**
     * 患者姓名（可选，模糊查询）
     */
    private String patientName;

    /**
     * 业务类型（可选，精确匹配，如 "11.1"）
     */
    private String businessType;

    /**
     * 创建时间起始（可选，闭区间）
     */
    private LocalDateTime createTimeStart;

    /**
     * 创建时间结束（可选，闭区间）
     */
    private LocalDateTime createTimeEnd;

    /**
     * 身体部位ID列表（可选，通过 order_item 子查询过滤）
     */
    private List<Long> bodyPartIds;

    /**
     * 重建项目ID列表（可选，通过 order_item 子查询过滤）
     */
    private List<Long> projectIds;

    /**
     * 操作员ID（可选，精确匹配）
     */
    private Long operatorId;

    /**
     * 订单状态（可选）
     */
    private Integer status;

    /**
     * 排序字段（可选，默认 createTime）
     * 允许值：createTime, updateTime, orderCode, patientName, doctorName,
     *         hospitalName, areaName, businessType, estimatedCost,
     *         expectedDeliveryDate, status, isUrgent
     */
    private String sortField;

    /**
     * 排序方向（可选，默认 DESC）
     * 允许值：ASC / DESC（不区分大小写）
     */
    private String sortOrder;
}
