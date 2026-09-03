package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 强制完成打印请求。 */
@Data
public class ForceCompletePrintDTO {

    @NotBlank(message = "强制完成原因不能为空")
    @Size(min = 5, max = 500, message = "强制完成原因长度必须为5到500个字符")
    private String reason;
}
