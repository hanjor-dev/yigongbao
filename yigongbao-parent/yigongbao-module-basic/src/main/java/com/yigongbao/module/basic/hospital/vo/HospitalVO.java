package com.yigongbao.module.basic.hospital.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院 VO（视图对象）
 * 用于返回给前端的医院数据
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医院ID
     */
    private Long id;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 医院编码
     */
    private String hospitalCode;

    /**
     * 地区ID
     */
    private Long areaId;

    /**
     * 地区名称（冗余，精确到区）
     */
    private String areaName;

    /**
     * 完整地区路径（省-市-区）
     */
    private String fullAreaName;

    /**
     * 医院等级（字典：dict_code=3，值如 3.1/3.2/3.3/3.4/3.5）
     */
    private String hospitalLevel;

    /**
     * 医院等级名称
     */
    private String hospitalLevelName;

    /**
     * 医院类型（字典：dict_code=4，值如 4.1/4.2）
     */
    private String hospitalType;

    /**
     * 医院类型名称
     */
    private String hospitalTypeName;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

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
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
