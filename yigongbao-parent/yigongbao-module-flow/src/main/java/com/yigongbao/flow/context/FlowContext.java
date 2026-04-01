package com.yigongbao.flow.context;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import lombok.Data;

/**
 * 状态机上下文
 * 记录订单流转过程中的关键状态，用于检测异常循环和提供业务决策依据
 *
 * 使用场景：
 * - 记录审核驳回次数，防止无限循环提交-驳回
 * - 记录返工次数，防止质检反复不合格导致无限返工
 * - 记录设计审核驳回次数，防止反复驳回设计
 *
 * 上下文从订单历史中重建，在 executeTransition 执行时校验
 *
 * @author hanjor
 * @date 2026-04-01
 */
@Data
public class FlowContext {

    /**
     * 审核驳回次数
     */
    private int auditRejectCount;

    /**
     * 返工次数
     */
    private int reworkCount;

    /**
     * 设计审核驳回次数
     */
    private int designRejectCount;

    /**
     * 最大允许的审核驳回次数
     */
    private static final int MAX_AUDIT_REJECT = 10;

    /**
     * 最大允许的返工次数
     */
    private static final int MAX_REWORK = 5;

    /**
     * 最大允许的设计审核驳回次数
     */
    private static final int MAX_DESIGN_REJECT = 5;

    /**
     * 递增审核驳回计数
     * 当订单被审核驳回时调用
     */
    public void incrementAuditReject() {
        this.auditRejectCount++;
    }

    /**
     * 递增返工计数
     * 当订单被质检判定不合格并返工时调用
     */
    public void incrementRework() {
        this.reworkCount++;
    }

    /**
     * 递增设计审核驳回计数
     * 当设计审核被驳回时调用
     */
    public void incrementDesignReject() {
        this.designRejectCount++;
    }

    /**
     * 校验是否超出循环次数上限
     *
     * @throws BusinessException 超出上限时抛出
     */
    public void validateNoExcessiveLoops() {
        if (auditRejectCount > MAX_AUDIT_REJECT) {
            throw new BusinessException(
                    ErrorCodeEnum.ORDER_EXCESSIVE_AUDIT_REJECT,
                    String.valueOf(MAX_AUDIT_REJECT));
        }
        if (reworkCount > MAX_REWORK) {
            throw new BusinessException(
                    ErrorCodeEnum.ORDER_EXCESSIVE_REWORK,
                    String.valueOf(MAX_REWORK));
        }
        if (designRejectCount > MAX_DESIGN_REJECT) {
            throw new BusinessException(
                    ErrorCodeEnum.ORDER_EXCESSIVE_DESIGN_REJECT,
                    String.valueOf(MAX_DESIGN_REJECT));
        }
    }

    /**
     * 从订单历史记录中构建上下文
     * 扫描历史记录，统计各类循环次数
     *
     * @param historyActions 按时间顺序排列的动作列表
     * @return 构建后的上下文
     */
    public static FlowContext buildFromHistory(java.util.List<String> historyActions) {
        FlowContext ctx = new FlowContext();
        if (historyActions == null || historyActions.isEmpty()) {
            return ctx;
        }
        for (String action : historyActions) {
            switch (action) {
                case "DATA_AUDIT_REJECT" -> ctx.incrementAuditReject();
                case "REWORK" -> ctx.incrementRework();
                case "DESIGN_REVIEW_REJECT" -> ctx.incrementDesignReject();
                default -> { }
            }
        }
        return ctx;
    }
}
