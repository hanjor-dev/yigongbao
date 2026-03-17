package com.yigongbao.module.system.dept.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建部门 DTO
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class CreateDeptDTO {

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 128, message = "部门名称长度不能超过128个字符")
    private String deptName;

    /**
     * 所属机构ID
     */
    @NotNull(message = "所属机构不能为空")
    private Long orgId;

    /**
     * 部门负责人用户ID
     */
    private Long leaderUserId;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注说明长度不能超过512个字符")
    private String remark;
}
