package com.yigongbao.module.order.vo.modify;

import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 获取订单可申请修改类型结果 VO
 * <p>
 * - allowedTypes 为空列表时，表示当前阶段不允许申请或已有待审核申请
 * - pendingApplyId 不为 null 时，表示已有待审核申请，前端可直接跳转查看
 *
 * @author hanjor
 * @date 2026-04-09
 */
@Data
public class ApplicableModifyTypesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 可申请的修改类型字典编码列表（如 ["14.1", "14.2", "14.3"]）
     * 空列表表示当前不可申请
     */
    private List<String> allowedTypes;

    /**
     * 可申请类型中文名（如 "基础信息、影像文件、重建项目"）
     */
    private String allowedTypesText;

    /**
     * 当前已有待审核申请的ID（null 表示无待审核申请）
     * 当 reason=PENDING_EXISTS 时有值，前端可据此跳转到申请详情页
     */
    private Long pendingApplyId;

    /** 当前阶段不支持发起修改申请 */
    public static final String REASON_PHASE_NOT_ALLOWED = "PHASE_NOT_ALLOWED";

    /** 当前订单已有待审核的修改申请 */
    public static final String REASON_PENDING_EXISTS = "PENDING_EXISTS";

    /**
     * 不可申请时的原因代码（可申请时为 null）
     * {@link #REASON_PHASE_NOT_ALLOWED} — 当前订单阶段不支持发起修改申请
     * {@link #REASON_PENDING_EXISTS}    — 当前订单已有待审核的修改申请
     */
    private String reason;

    /**
     * 当前阶段允许发起申请
     *
     * @param types 允许的类型编码列表
     * @return VO
     */
    public static ApplicableModifyTypesVO forAllowed(List<String> types) {
        ApplicableModifyTypesVO vo = new ApplicableModifyTypesVO();
        vo.setAllowedTypes(types);
        vo.setAllowedTypesText(ModifyApplyTypeEnum.toNamesText(String.join(",", types)));
        return vo;
    }

    /**
     * 当前阶段不允许发起申请
     *
     * @return VO（allowedTypes 为空列表）
     */
    public static ApplicableModifyTypesVO forPhaseNotAllowed() {
        ApplicableModifyTypesVO vo = new ApplicableModifyTypesVO();
        vo.setAllowedTypes(Collections.emptyList());
        vo.setAllowedTypesText("");
        vo.setReason(REASON_PHASE_NOT_ALLOWED);
        return vo;
    }

    /**
     * 当前订单已有待审核申请
     *
     * @param pendingApplyId 待审核申请ID
     * @return VO（allowedTypes 为空列表，pendingApplyId 有值）
     */
    public static ApplicableModifyTypesVO forPendingExists(Long pendingApplyId) {
        ApplicableModifyTypesVO vo = new ApplicableModifyTypesVO();
        vo.setAllowedTypes(Collections.emptyList());
        vo.setAllowedTypesText("");
        vo.setPendingApplyId(pendingApplyId);
        vo.setReason(REASON_PENDING_EXISTS);
        return vo;
    }
}
