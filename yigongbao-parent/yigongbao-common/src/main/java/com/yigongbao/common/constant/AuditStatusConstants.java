package com.yigongbao.common.constant;

/**
 * 审核状态常量
 *
 * @author Kiro AI
 * @date 2026-06-04
 */
public class AuditStatusConstants {

    /**
     * 待审核
     */
    public static final int PENDING = 0;

    /**
     * 已通过
     */
    public static final int PASSED = 1;

    /**
     * 已驳回
     */
    public static final int REJECTED = 2;

    private AuditStatusConstants() {
    }
}
