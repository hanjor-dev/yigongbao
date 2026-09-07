package com.yigongbao.module.design.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 保存设计师备注 DTO。 */
@Data
public class SaveDesignerRemarkDTO {

    /** 设计师备注。 */
    @NotBlank(message = "设计师备注不能为空")
    @Size(max = 2000, message = "设计师备注长度不能超过2000个字符")
    private String designerRemark;
}
