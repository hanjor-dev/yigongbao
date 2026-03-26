package com.yigongbao.module.system.org.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * 更新机构 DTO
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class UpdateOrgDTO {

    /**
     * 机构名称
     */
    @Size(max = 128, message = "机构名称长度不能超过128个字符")
    private String orgName;

    /**
     * 机构类型（字典编码）
     */
    private String orgType;

    /**
     * 所属地区ID
     */
    private Long areaId;

    /**
     * 所属地区名称
     */
    private String areaName;

    /**
     * 详细地址
     */
    @Size(max = 256, message = "详细地址长度不能超过256个字符")
    private String address;

    /**
     * 联系人
     */
    @Size(max = 32, message = "联系人长度不能超过32个字符")
    private String contact;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String phone;

    /**
     * 联系邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 统一社会信用代码
     */
    @Pattern(regexp = "^[0-9A-Z]{18}$", message = "统一社会信用代码格式不正确")
    private String creditCode;

    /**
     * 营业执照（存储路径/URL）
     */
    @Size(max = 512, message = "营业执照路径长度不能超过512个字符")
    private String businessLicense;

    /**
     * 代理区域（经销商）
     */
    @Size(max = 64, message = "代理区域长度不能超过64个字符")
    private String agentArea;

    /**
     * 代理产品线（多个用逗号分隔）
     */
    @Size(max = 256, message = "代理产品线长度不能超过256个字符")
    private String agentProductLine;

    /**
     * 医院等级（医疗机构，关联字典编码=3，值如 3.1/3.2/3.3/3.4/3.5）
     */
    private String hospitalLevel;

    /**
     * 医院类型（医疗机构，关联字典编码=4，值如 4.1/4.2）
     */
    private String hospitalType;

    /**
     * 备注说明
     */
    @Size(max = 512, message = "备注说明长度不能超过512个字符")
    private String remark;
}
