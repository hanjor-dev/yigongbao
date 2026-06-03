package com.yigongbao.module.order.vo;

import com.yigongbao.module.order.vo.order.OrderListVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 经典案例视图对象VO
 * <p>
 * 继承订单列表VO的所有字段，并增加经典案例特有字段。
 * 经典案例包含订单的完整信息，以及标记相关的元数据。
 * </p>
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "经典案例详情")
public class ClassicCaseVO extends OrderListVO {

    private static final long serialVersionUID = 1L;

    // ==================== 经典案例特有字段 ====================

    /**
     * 标记为经典案例的时间
     */
    @Schema(description = "标记时间")
    private LocalDateTime classicCaseTime;

    /**
     * 标记人ID
     */
    @Schema(description = "标记人ID")
    private Long classicCaseBy;

    /**
     * 标记人姓名
     */
    @Schema(description = "标记人姓名")
    private String classicCaseByName;

    /**
     * 标记备注
     */
    @Schema(description = "标记备注")
    private String classicCaseRemark;
}
