package com.yigongbao.module.system.dict.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新字典 DTO
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class UpdateDictDTO {

    /**
     * 字典名称
     */
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /**
     * 字典值（叶子节点使用）
     */
    private String dictValue;

    /**
     * 排序（同级内排序）
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
