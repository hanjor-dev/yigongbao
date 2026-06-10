package com.yigongbao.module.order.vo.workload;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设计师工作量统计 VO
 *
 * @author hanjor
 * @date 2026-06-10
 */
@Data
public class DesignerWorkloadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设计师ID
     */
    private Long designerId;

    /**
     * 设计师姓名
     */
    private String designerName;

    /**
     * 案例数（重建项目数量）
     */
    private Integer caseCount;

    /**
     * 案例数占比
     */
    private BigDecimal caseCountRate;

    /**
     * 分值总数
     */
    private Integer totalPoints;

    /**
     * 分值占比
     */
    private BigDecimal totalPointsRate;
}
