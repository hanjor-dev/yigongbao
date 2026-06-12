package com.yigongbao.flow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流转动作枚举
 * 定义状态变更时可执行的动作
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum FlowActionEnum {

    /**
     * 创建订单
     */
    CREATE("CREATE", "创建订单"),

    /**
     * 提交订单
     */
    SUBMIT_ORDER("SUBMIT_ORDER", "提交订单"),

    /**
     * 撤回订单
     */
    WITHDRAW("WITHDRAW", "撤回订单"),

    /**
     * 重新提交
     */
    RESUBMIT("RESUBMIT", "重新提交"),

    /**
     * 数据审核通过
     */
    DATA_AUDIT_PASS("DATA_AUDIT_PASS", "数据审核通过"),

    /**
     * 数据审核驳回
     */
    DATA_AUDIT_REJECT("DATA_AUDIT_REJECT", "数据审核驳回"),

    /**
     * 完成订单
     */
    COMPLETE("COMPLETE", "完成订单"),

    /**
     * 取消订单
     */
    CANCEL("CANCEL", "取消订单"),

    // ==================== 设计阶段动作 ====================
    /**
     * 开始设计
     */
    START_DESIGN("START_DESIGN", "开始设计"),

    /**
     * 完成设计
     */
    COMPLETE_DESIGN("COMPLETE_DESIGN", "完成设计"),

    /**
     * 下载数据包（设计完成后，生产员下载数据包触发推进到打印阶段）
     */
    DOWNLOAD_DATA_PACKAGE("DOWNLOAD_DATA_PACKAGE", "下载数据包"),

    // ==================== 打印阶段动作 ====================
    /**
     * 开始打印
     */
    START_PRINT("START_PRINT", "开始打印"),

    /**
     * 完成打印
     */
    COMPLETE_PRINT("COMPLETE_PRINT", "完成打印"),

    // ==================== 后处理动作 ====================
    /**
     * 开始后处理
     */
    START_POST_PROCESSING("START_POST_PROCESSING", "开始后处理"),

    /**
     * 完成后处理
     */
    COMPLETE_POST_PROCESSING("COMPLETE_POST_PROCESSING", "完成后处理"),

    // ==================== 质检阶段动作 ====================
    /**
     * 质检合格
     */
    QC_PASS("QC_PASS", "质检合格"),

    /**
     * 质检不合格
     */
    QC_FAIL("QC_FAIL", "质检不合格"),

    /**
     * 返工
     */
    REWORK("REWORK", "返工"),

    /**
     * 返工完成（返工结束后重新进入质检）
     */
    REWORK_COMPLETE("REWORK_COMPLETE", "返工完成"),

    /**
     * 完成包装
     */
    COMPLETE_PACKING("COMPLETE_PACKING", "完成包装"),

    /**
     * 质检不合格回退到打印
     */
    REWORK_TO_PRINT("REWORK_TO_PRINT", "回退到打印"),

    // ==================== 仓储阶段动作 ====================
    /**
     * 包装完成，流转到待入库
     */
    TRANSFER_TO_WAREHOUSE("TRANSFER_TO_WAREHOUSE", "流转到待入库"),

    /**
     * 完成入库
     */
    COMPLETE_WAREHOUSE_IN("COMPLETE_WAREHOUSE_IN", "完成入库"),

    /**
     * 完成出库
     */
    COMPLETE_WAREHOUSE_OUT("COMPLETE_WAREHOUSE_OUT", "完成出库");

    /**
     * 动作编码
     */
    private final String code;

    /**
     * 动作名称
     */
    private final String name;

    /**
     * 根据编码获取枚举
     *
     * @param code 动作编码
     * @return 对应的枚举实例，如果未找到则返回 null
     */
    public static FlowActionEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (FlowActionEnum enumItem : FlowActionEnum.values()) {
            if (enumItem.getCode().equals(code)) {
                return enumItem;
            }
        }
        return null;
    }
}
