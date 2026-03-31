package com.yigongbao.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单动作枚举
 * 定义订单状态变更时可执行的动作
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderActionEnum {

    /**
     * 创建订单
     */
    CREATE("CREATE", "创建订单"),

    /**
     * 提交订单
     */
    SUBMIT("SUBMIT", "提交订单"),

    /**
     * 撤回订单
     */
    WITHDRAW("WITHDRAW", "撤回订单"),

    /**
     * 重新提交
     */
    RESUBMIT("RESUBMIT", "重新提交"),

    /**
     * 审核通过
     */
    AUDIT_PASS("AUDIT_PASS", "审核通过"),

    /**
     * 审核驳回
     */
    AUDIT_REJECT("AUDIT_REJECT", "审核驳回"),

    /**
     * 完成订单
     */
    COMPLETE("COMPLETE", "完成订单"),

    /**
     * 取消订单
     */
    CANCEL("CANCEL", "取消订单"),

    /**
     * 阶段流转
     */
    PHASE_TRANSFER("PHASE_TRANSFER", "阶段流转");

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
    public static OrderActionEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (OrderActionEnum enumItem : OrderActionEnum.values()) {
            if (enumItem.getCode().equals(code)) {
                return enumItem;
            }
        }
        return null;
    }
}
