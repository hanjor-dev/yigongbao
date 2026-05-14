package com.yigongbao.module.system.dept.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

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
     * 部门类型（字典编码：6.1=企业部门，6.2=业务部门）
     * 必填
     */
    @NotBlank(message = "部门类型不能为空")
    @Pattern(regexp = "^(6\\.1|6\\.2)$", message = "部门类型值不合法，仅支持6.1（企业部门）或6.2（业务部门）")
    private String deptType;

    /**
     * 关联机构ID列表
     */
    private List<Long> orgIds;

    /**
     * 部门负责人姓名（自由输入）
     */
    @Size(max = 64, message = "负责人姓名长度不能超过64个字符")
    private String leaderUser;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注说明长度不能超过512个字符")
    private String remark;
}
