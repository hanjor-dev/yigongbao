package com.yigongbao.module.system.test.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建测试 DTO
 *
 * @author hanjor
 * @date 2026-03-14 18:30:00
 */
@Data
public class CreateTestDTO {

    /**
     * 键
     */
    @NotBlank(message = "键不能为空")
    @Size(max = 100, message = "键长度不能超过100个字符")
    private String key;

    /**
     * 值
     */
    @NotBlank(message = "值不能为空")
    @Size(max = 500, message = "值长度不能超过500个字符")
    private String value;
}
