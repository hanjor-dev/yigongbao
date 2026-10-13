package com.yigongbao.module.order.vo;

import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 经典案例视图对象VO
 * <p>
 * 继承订单详情VO的所有字段（包含订单明细、文件列表等完整信息），
 * 并增加经典案例特有字段（标记时间、标记人、备注）。
 * </p>
 *
 * @author hanjor
 * @date 2026-06-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "经典案例详情")
public class ClassicCaseVO extends OrderDetailVO {

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

    /**
     * 重建项目明细列表
     */
    @Schema(description = "重建项目列表")
    private List<OrderListVO.RebuildProjectItemVO> rebuildProjectList;
}
