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
     * 手机号（不传则不修改；传空字符串无效，需传 null）
     */
    @Size(min = 11, max = 11, message = "手机号长度必须为11位")
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
    private String sex;

    /**
     * 头像路径
     */
    @Size(max = 512, message = "头像路径长度不能超过512个字符")
    private String avatar;

    /**
     * 账户分类（字典编码：6.1=企业账户，6.2=业务账户）
     * 必填，必须与所选角色的账户分类一致
     */
    @NotBlank(message = "账户分类不能为空")
    @Pattern(regexp = "^(6\\.1|6\\.2)$", message = "账户分类参数不合法")
    private String accountType;

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
     * 所属加工中心ID（生产员角色专用）
     */
    private Long centerId;

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
     * 资产编码（企业账户选填）
     */
    @Size(max = 64, message = "资产编码长度不能超过64个字符")
    private String assetNumber;

    /**
     * 专业方向字典编码列表（设计师/设计师管理员必填，如 ["7.1", "7.2"]）
     */
    private List<String> specialtyList;

    /**
     * 资质证书信息
     */
    @Size(max = 256, message = "资质证书信息长度不能超过256个字符")
    private String qualification;

    /**
     * 结算类型（来自字典表）
     */
    @Min(value = 0, message = "结算类型值不合法")
    private Integer settlementType;

    /**
     * 收费模板ID（业务类型账户）
     */
    private Long chargingTemplateId;

    /**
     * 状态（0=禁用，1=正常）
     */
    @Min(value = 0, message = "状态值不合法，仅支持0（禁用）或1（正常）")
    @Max(value = 1, message = "状态值不合法，仅支持0（禁用）或1（正常）")
    private Integer status;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;
}
