package com.yigongbao.module.order.vo.modify;

import com.yigongbao.module.order.enums.ModifyApplyTypeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 判断订单是否可申请修改的结果 VO
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class CanApplyModifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否可以发起申请
     */
    private boolean canApply;

    /**
     * 允许的申请类型字典编码列表（如 ["14.1", "14.2", "14.3"]）
     */
    private List<String> allowedTypes;

    /**
     * 允许的申请类型中文名（如 "基础信息、影像文件、重建项目"）
     */
    private String allowedTypesText;

    /**
     * 不可申请时的原因代码（可申请时为 null）
     * PHASE_NOT_ALLOWED / PENDING_EXISTS
     */
    private String reason;

    public static CanApplyModifyResult yes(List<String> allowedTypes) {
        CanApplyModifyResult r = new CanApplyModifyResult();
        r.setCanApply(true);
        r.setAllowedTypes(allowedTypes);
        r.setAllowedTypesText(ModifyApplyTypeEnum.toNamesText(String.join(",", allowedTypes)));
        return r;
    }

    public static CanApplyModifyResult no(String reason) {
        CanApplyModifyResult r = new CanApplyModifyResult();
        r.setCanApply(false);
        r.setAllowedTypes(Collections.emptyList());
        r.setAllowedTypesText("");
        r.setReason(reason);
        return r;
    }
}
