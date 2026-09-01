package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新影像数据评估意见 DTO
 */
@Data
public class UpdateEvaluationOpinionDTO {

    /**
     * 影像数据评估意见
     */
    @NotBlank(message = "评估意见不能为空")
    @Size(max = 2000, message = "评估意见长度不能超过2000个字符")
    private String dataEvaluationOpinion;
}
