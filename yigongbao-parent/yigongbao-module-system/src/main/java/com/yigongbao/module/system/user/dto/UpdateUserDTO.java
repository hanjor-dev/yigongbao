package com.yigongbao.module.system.user.dto;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

/**
 * 更新用户 DTO
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class UpdateUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 真实姓名
     */
    @Size(max = 32, message = "真实姓名长度不能超过32个字符")
    private String realName;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过64个字符")
    private String email;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 头像路径
     */
    @Size(max = 512, message = "头像路径长度不能超过512个字符")
    private String avatar;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

    /**
     * 所属机构ID
     */
    private Long orgId;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 关联角色ID
     */
    private Long roleId;

    /**
     * 医院ID列表（当角色 dataScopeType=hospitals 时，用于分配医院范围权限）
     * 管理员可先选择模板（预览模板医院列表），再微调后提交
     */
    private List<Long> hospitalIds;

    /**
     * 工号
     */
    @Size(max = 32, message = "工号长度不能超过32个字符")
    private String employeeNo;

    /**
     * 专业方向
     */
    @Size(max = 64, message = "专业方向长度不能超过64个字符")
    private String specialty;

    /**
     * 资质证书信息
     */
    @Size(max = 256, message = "资质证书信息长度不能超过256个字符")
    private String qualification;

    /**
     * 结算类型
     */
    private Integer settlementType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;
}
