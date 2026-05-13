package com.yigongbao.module.system.dept.dto;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 更新部门 DTO
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class UpdateDeptDTO {

    /**
     * 部门名称
     */
    @Size(max = 128, message = "部门名称长度不能超过128个字符")
    private String deptName;

    /**
     * 部门类型（1=内部，2=外部）
     */
    private Integer deptType;

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
