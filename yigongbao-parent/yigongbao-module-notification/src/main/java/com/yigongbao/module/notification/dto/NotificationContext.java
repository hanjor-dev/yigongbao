package com.yigongbao.module.notification.dto;

import lombok.Data;

/**
 * 通知上下文：描述通知的接收范围（医院 / 机构 / 部门 / 中心 / 全部）
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
public class NotificationContext {

    private Long hospitalId;
    private Long orgId;
    private Long deptId;
    private Long centerId;

    public static NotificationContext ofHospital(Long hospitalId) {
        NotificationContext ctx = new NotificationContext();
        ctx.hospitalId = hospitalId;
        return ctx;
    }

    public static NotificationContext ofOrg(Long orgId) {
        NotificationContext ctx = new NotificationContext();
        ctx.orgId = orgId;
        return ctx;
    }

    public static NotificationContext ofDept(Long deptId) {
        NotificationContext ctx = new NotificationContext();
        ctx.deptId = deptId;
        return ctx;
    }

    public static NotificationContext ofCenter(Long centerId) {
        NotificationContext ctx = new NotificationContext();
        ctx.centerId = centerId;
        return ctx;
    }

    /**
     * 不限范围，查询该角色的全部用户
     */
    public static NotificationContext all() {
        return new NotificationContext();
    }
}
