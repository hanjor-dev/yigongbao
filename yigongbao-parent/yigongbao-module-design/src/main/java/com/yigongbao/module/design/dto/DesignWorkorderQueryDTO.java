package com.yigongbao.module.design.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设计工单列表查询参数
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class DesignWorkorderQueryDTO {

    /** 页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10，最大 100 */
    private Integer pageSize = 10;

    /** 订单编号（模糊匹配） */
    private String orderCode;

    /** 患者姓名（模糊匹配） */
    private String patientName;

    /** 状态（精确匹配，如 2010/2020/2030） */
    private Integer status;

    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;

    /** 医院ID（精确匹配） */
    private Long hospitalId;

    /** 业务类型（字典码，精确匹配） */
    private String businessType;

    /** 创建时间-开始 */
    private LocalDateTime createTimeStart;

    /** 创建时间-结束 */
    private LocalDateTime createTimeEnd;

    /** 排序字段，默认 createTime */
    private String sortField;

    /** 排序方向（ASC/DESC），默认 DESC */
    private String sortOrder;
}
