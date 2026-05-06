package com.yigongbao.module.system.org.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建机构 DTO
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class CreateOrgDTO {

    /**
     * 机构名称
     */
    @NotBlank(message = "机构名称不能为空")
    @Size(max = 128, message = "机构名称长度不能超过128个字符")
    private String orgName;

    /**
     * 机构类型（字典编码，如：1.1=生产企业，1.2=经销商，1.3=医疗机构，1.4=其他）
     */
    @NotBlank(message = "机构类型不能为空")
    private String orgType;

    /**
     * 所属地区ID
     */
    @NotNull(message = "地区不能为空")
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
    private String creditCode;

    /**
     * 资质文件路径
     */
    @Size(max = 512, message = "资质文件路径长度不能超过512个字符")
    private String qualificationFile;

    /**
     * 资质类型（1=医疗器械，2=非医疗器械）
     */
    private Integer qualificationType;

    /**
     * 关联医院机构ID列表（经销商）
     */
    private List<Long> hospitalOrgIds;

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

    /**
     * 账号前缀（英文字母和数字，2-16位，用于自动生成用户名）
     * 仅对经销商（1.2）和生产企业（1.1）有效
     */
    @Pattern(regexp = "^[a-zA-Z0-9]{2,16}$", message = "账号前缀只允许英文字母和数字，长度2-16位")
    private String usernamePrefix;
}
