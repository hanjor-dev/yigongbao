package com.yigongbao.module.design.vo;

import lombok.Data;

/**
 * 设计工单提交校验状态 VO
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Data
public class SubmitCheckVO {

    /** 是否已上传数据包 */
    private Boolean hasPackage;

    /** 是否已填写打印信息（所有数据包） */
    private Boolean hasPrintInfo;

    /** 是否已生成指令单（所有数据包） */
    private Boolean hasInstruction;

    /** 是否已生成图纸（所有数据包） */
    private Boolean hasDrawing;

    /** 是否已上传可视化模型 */
    private Boolean hasModel;

    /** 是否已上传设计报告 */
    private Boolean hasReport;

    /** 是否可以提交（全部为 true 时才为 true） */
    private Boolean canSubmit;

    /** 不可提交的原因（首个未满足项的说明） */
    private String blockReason;
}
