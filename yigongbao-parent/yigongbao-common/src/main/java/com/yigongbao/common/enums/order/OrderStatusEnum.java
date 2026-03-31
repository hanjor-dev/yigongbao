package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 * 按阶段分段的完整状态码体系
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    // ==================== 订单阶段（10-19）====================
    /**
     * 草稿（提交前）
     */
    DRAFT(10, "草稿"),

    /**
     * 数据待审核
     */
    PENDING_DATA_AUDIT(11, "数据待审核"),

    /**
     * 数据审核通过
     */
    DATA_AUDIT_PASSED(12, "数据审核通过"),

    /**
     * 数据审核不通过
     */
    DATA_AUDIT_REJECTED(13, "数据审核不通过"),

    // ==================== 设计阶段（20-29）====================
    /**
     * 设计中
     */
    DESIGNING(21, "设计中"),

    /**
     * 设计完成
     */
    DESIGN_COMPLETED(22, "设计完成"),

    /**
     * 设计审核中
     */
    DESIGN_REVIEWING(23, "设计审核中"),

    /**
     * 设计审核通过
     */
    DESIGN_REVIEW_PASSED(24, "设计审核通过"),

    /**
     * 设计审核不通过
     */
    DESIGN_REVIEW_REJECTED(25, "设计审核不通过"),

    // ==================== 打印阶段（30-39）====================
    /**
     * 待打印
     */
    PENDING_PRINT(31, "待打印"),

    /**
     * 打印中
     */
    PRINTING(32, "打印中"),

    /**
     * 打印完成
     */
    PRINT_COMPLETED(33, "打印完成"),

    // ==================== 后处理阶段（40-49）====================
    /**
     * 后处理中
     */
    POST_PROCESSING(41, "后处理中"),

    // ==================== 质检阶段（50-59）====================
    /**
     * 质检中
     */
    QC_IN_PROGRESS(51, "质检中"),

    /**
     * 质检合格
     */
    QC_PASSED(52, "质检合格"),

    /**
     * 质检不合格
     */
    QC_FAILED(53, "质检不合格"),

    /**
     * 返工
     */
    REWORK(54, "返工"),

    // ==================== 仓储阶段（60-69）====================
    /**
     * 入库中
     */
    WAREHOUSE_IN(61, "入库中"),

    /**
     * 已入库
     */
    WAREHOUSED(62, "已入库"),

    // ==================== 确认阶段（70-79）====================
    /**
     * 待客户确认
     */
    AWAITING_CONFIRM(71, "待客户确认"),

    // ==================== 已完成（80）====================
    /**
     * 已完成
     */
    COMPLETED(80, "已完成");

    /**
     * 状态值
     */
    private final Integer value;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据值获取枚举
     *
     * @param value 状态值
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static OrderStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (OrderStatusEnum enumItem : OrderStatusEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 判断当前状态是否属于指定阶段
     *
     * @param phase 阶段枚举
     * @return true-属于，false-不属于
     */
    public boolean belongsTo(OrderPhaseEnum phase) {
        if (phase == null) {
            return false;
        }
        int phaseValue = phase.getValue();
        int statusValue = this.value;
        return statusValue >= phaseValue && statusValue < phaseValue + 10;
    }
}
