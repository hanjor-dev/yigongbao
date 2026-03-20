package com.yigongbao.module.basic.hospital.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 医院 Entity
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
@TableName("hospital")
@EqualsAndHashCode(callSuper = false)
public class HospitalEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 医院编码（系统唯一，自动生成）
     */
    private String hospitalCode;

    /**
     * 所属地区ID
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
     * 医院等级（字典：dict_code=3）
     */
    private Integer hospitalLevel;

    /**
     * 医院类型（字典：dict_code=4）
     */
    private Integer hospitalType;

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
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 营业执照路径
     */
    private String businessLicense;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
