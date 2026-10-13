package com.yigongbao.module.system.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 机构 Entity
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
@TableName("sys_org")
@EqualsAndHashCode(callSuper = false)
public class OrgEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 机构编码（系统唯一）
     */
    private String orgCode;

    /**
     * 机构类型（字典编码，如：1.1=生产企业，1.2=经销商，1.3=医疗机构，1.4=其他）
     */
    private String orgType;

    /**
     * 所属地区ID
     */
    private Long areaId;

    /**
     * 所属地区名称（冗余存储）
     */
    private String areaName;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 联系邮箱
     */
    private String email;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 资质文件路径
     */
    private String qualificationFile;

    /**
     * 资质类型（1=医疗器械，2=非医疗器械）
     */
    private Integer qualificationType;

    /**
     * 医院等级（医疗机构，关联字典编码=3，值如 3.1/3.2/3.3/3.4/3.5）
     */
    private String hospitalLevel;

    /**
     * 医院类型（医疗机构，关联字典编码=4，值如 4.1/4.2）
     */
    private String hospitalType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 账号前缀（英文字母和数字，2-16位，用于自动生成用户名）
     * 仅对 orgType=1.1（生产企业）和 orgType=1.2（经销商）有效
     */
    private String usernamePrefix;
}
