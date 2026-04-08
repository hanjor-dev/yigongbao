package com.yigongbao.module.order.dto.modify;

import lombok.Data;

import java.io.Serializable;

/**
 * 执行修改时的重建项目明细 DTO
 * 独立于 OrderItemDraftItemDTO，明确 orderItemId 语义
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ExecuteModificationItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 要更新的订单明细ID（传 null 表示新增；传值时必须属于当前订单，否则报错）
     */
    private Long orderItemId;

    /**
     * 部位ID
     */
    private Long bodyPartId;

    /**
     * 重建项目ID
     */
    private Long projectId;

    /**
     * 项目说明
     */
    private String projectDesc;

    /**
     * 成形需求
     */
    private String formingRequirement;

    /**
     * 其他要求
     */
    private String otherRequirement;

    /**
     * 排序序号
     */
    private Integer sortOrder;
}
