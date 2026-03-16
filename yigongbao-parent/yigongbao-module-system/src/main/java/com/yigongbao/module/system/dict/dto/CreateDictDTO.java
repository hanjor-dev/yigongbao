package com.yigongbao.module.system.dict.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建字典 DTO
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class CreateDictDTO {

    /**
     * 父级ID（0表示根节点/字典类型）
     */
    @NotNull(message = "父级ID不能为空")
    private Long parentId;

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
