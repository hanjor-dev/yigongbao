package com.yigongbao.module.basic.bodyPart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建部位 DTO
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class CreateBodyPartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位名称
     */
    @NotBlank(message = "部位名称不能为空")
    @Size(max = 100, message = "部位名称长度不能超过100")
    private String name;

    /**
     * 排序
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
