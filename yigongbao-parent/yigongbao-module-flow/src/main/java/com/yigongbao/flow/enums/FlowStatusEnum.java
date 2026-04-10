package com.yigongbao.flow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流转状态枚举
 * 按阶段分段的完整状态码体系
 *
 * 【编码规则】
 * - Status value = phase.value × 100 + 序号(1-9)
 * - 例：ORDER(10) 的状态从 1001 开始：DRAFT=1001, PENDING_DATA_AUDIT=1002...
 * - 每阶段最多9个状态（序号1-9）；每阶段间有间隔，支持插入新阶段
 * - belongsTo(phase) 判断：statusValue / 100 == phase.getValue()
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum FlowStatusEnum {

    // ==================== 订单阶段（1001-1009）====================
    /**
     * 草稿（提交前）
     */
    DRAFT(1001, "草稿"),

    /**
     * 数据待审核
     */
    PENDING_DATA_AUDIT(1002, "数据待审核"),

    /**
     * 数据审核通过
     */
    DATA_AUDIT_PASSED(1003, "数据审核通过"),

    /**
     * 数据审核不通过
     */
    DATA_AUDIT_REJECTED(1004, "数据审核不通过"),

    // ==================== 设计阶段（2001-2009）====================
    /**
     * 待设计（审核通过后进入；已分配设计师或待分配）
     */
    PENDING_DESIGN(2001, "待设计"),

    /**
     * 设计中（设计师已开始设计）
     */
    DESIGN_IN_PROGRESS(2002, "设计中"),

    /**
     * 设计完成
     */
    DESIGN_COMPLETED(2003, "设计完成"),

    /**
     * 设计审核中
     */
    DESIGN_REVIEWING(2004, "设计审核中"),

    /**
     * 设计审核通过（不可见状态，系统自动推进到下一阶段）
     */
    DESIGN_REVIEW_PASSED(2005, "设计审核通过"),

    /**
     * 设计审核不通过
     */
    DESIGN_REVIEW_REJECTED(2006, "设计审核不通过"),

    // ==================== 打印阶段（3001-3009）====================
    /**
     * 待打印
     */
    PENDING_PRINT(3001, "待打印"),

    /**
     * 打印中
     */
    PRINTING(3002, "打印中"),

    /**
     * 打印完成
     */
    PRINT_COMPLETED(3003, "打印完成"),

    // ==================== 后处理阶段（4001-4009）====================
    /**
     * 后处理中
     */
    POST_PROCESSING(4001, "后处理中"),

    // ==================== 质检阶段（5001-5009）====================
    /**
     * 质检中
     */
    QC_IN_PROGRESS(5001, "质检中"),

    /**
     * 质检合格
     */
    QC_PASSED(5002, "质检合格"),

    /**
     * 质检不合格
     */
    QC_FAILED(5003, "质检不合格"),

    /**
     * 返工
     */
    REWORK(5004, "返工"),

    // ==================== 仓储阶段（6001-6009）====================
    /**
     * 入库中
     */
    WAREHOUSE_IN(6001, "入库中"),

    /**
     * 已入库
     */
    WAREHOUSED(6002, "已入库"),

    // ==================== 确认阶段（7001-7009）====================
    /**
     * 待客户确认
     */
    AWAITING_CONFIRM(7001, "待客户确认"),

    // ==================== 已完成（8001）====================
    /**
     * 已完成
     */
    COMPLETED(8001, "已完成");

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
    public static FlowStatusEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (FlowStatusEnum enumItem : FlowStatusEnum.values()) {
            if (enumItem.getValue().equals(value)) {
                return enumItem;
            }
        }
        return null;
    }

    /**
     * 判断当前状态是否属于指定阶段
     * 判断规则：statusValue / 100 == phase.getValue()
     * 例：QC_IN_PROGRESS(5001) / 100 = 50 == QC.getValue()
     *
     * @param phase 阶段枚举
     * @return true-属于，false-不属于
     */
    public boolean belongsTo(FlowPhaseEnum phase) {
        if (phase == null) {
            return false;
        }
        return this.value / 100 == phase.getValue();
    }
}
