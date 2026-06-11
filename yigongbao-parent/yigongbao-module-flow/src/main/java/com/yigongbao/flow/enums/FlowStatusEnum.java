package com.yigongbao.flow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流转状态枚举
 * 按阶段分段的完整状态码体系
 *
 * 【编码规则】
 * - Status value = phase.value × 100 + 序号×10 (10,20,30...)
 * - 例：ORDER(10) 的状态从 1010 开始：DRAFT=1010, PENDING_DATA_AUDIT=1020...
 * - 每阶段最多9个插槽（序号10,20,...,90）；序号间隔保证中间可插入新状态且值有序
 * - belongsTo(phase) 判断：statusValue / 100 == phase.getValue()（序号最大90，不会进位）
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum FlowStatusEnum {

    // ==================== 订单阶段（1010-1090）====================
    /**
     * 草稿（提交前）
     */
    DRAFT(1010, "草稿"),

    /**
     * 数据待审核
     */
    PENDING_DATA_AUDIT(1020, "数据待审核"),

    /**
     * 数据审核通过
     */
    DATA_AUDIT_PASSED(1030, "数据审核通过"),

    /**
     * 数据审核不通过
     */
    DATA_AUDIT_REJECTED(1040, "数据审核不通过"),

    // ==================== 设计阶段（2010-2090）====================
    /**
     * 待设计（审核通过后进入；已分配设计师或待分配）
     */
    PENDING_DESIGN(2010, "待设计"),

    /**
     * 设计中（设计师已开始设计）
     */
    DESIGN_IN_PROGRESS(2020, "设计中"),

    /**
     * 设计完成
     */
    DESIGN_COMPLETED(2030, "设计完成"),

    // ==================== 打印阶段（3010-3090）====================
    /**
     * 待打印
     */
    PENDING_PRINT(3010, "待打印"),

    /**
     * 打印中
     */
    PRINTING(3020, "打印中"),

    /**
     * 打印完成
     */
    PRINT_COMPLETED(3030, "打印完成"),

    /**
     * 打印失败
     */
    PRINT_FAILED(3040, "打印失败"),

    // ==================== 后处理阶段（4010-4090）====================
    /**
     * 后处理中
     */
    POST_PROCESSING(4010, "后处理中"),

    // ==================== 质检阶段（5010-5090）====================
    /**
     * 质检中
     */
    QC_IN_PROGRESS(5010, "质检中"),

    /**
     * 质检合格
     */
    QC_PASSED(5020, "质检合格"),

    /**
     * 质检不合格
     */
    QC_FAILED(5030, "质检不合格"),

    /**
     * 返工
     */
    REWORK(5040, "返工"),

    /**
     * 包装中
     */
    PACKING(5050, "包装中"),

    // ==================== 仓储阶段（6010-6090）====================
    /**
     * 待入库
     */
    PENDING_WAREHOUSE_IN(6010, "待入库"),

    /**
     * 已入库
     */
    WAREHOUSED(6020, "已入库"),

    /**
     * 已出库
     */
    WAREHOUSE_OUT(6030, "已出库"),

    // ==================== 已完成（8010）====================
    /**
     * 已完成
     */
    COMPLETED(8010, "已完成"),

    // ==================== 已取消（9010）====================
    /**
     * 已取消（订单废弃，全阶段可执行）
     */
    CANCELLED(9010, "已取消");

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
     * 例：QC_IN_PROGRESS(5010) / 100 = 50 == QC.getValue()
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
