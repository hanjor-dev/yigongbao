package com.yigongbao.module.basic.hospital.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 创建医院 DTO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class CreateHospitalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医院名称
     */
    @NotBlank(message = "医院名称不能为空")
    @Size(max = 128, message = "医院名称不能超过128字符")
    private String hospitalName;

    /**
     * 地区ID
     */
    @NotNull(message = "地区ID不能为空")
    private Long areaId;

    /**
     * 联系人
     */
    @NotBlank(message = "联系人不能为空")
    private String contact;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 电子邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 医院等级
     */
    private Integer hospitalLevel;

    /**
     * 医院类型
     */
    private Integer hospitalType;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 信用代码
     */
    private String creditCode;

    /**
     * 营业执照路径
     */
    private String businessLicense;

    /**
     * 备注
     */
    private String remark;
}
