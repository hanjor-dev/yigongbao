package com.yigongbao.module.order.dto.modify;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单修改申请 DTO
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class CreateModifyApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请修改的类型（多选，逗号分隔字典编码）
     * 取值：14.1（基础信息）/ 14.2（影像文件）/ 14.3（重建项目）
     * 示例："14.1,14.3"
     */
    @NotBlank(message = "申请类型不能为空")
    private String applyTypes;

    /**
     * 申请原因
     */
    @NotBlank(message = "申请原因不能为空")
    private String applyReason;
}
