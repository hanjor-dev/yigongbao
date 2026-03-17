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
     * 机构类型（关联字典编码=1，子节点dict_code=1.1/1.2/1.3/1.4）
     * 1.1=生产企业，1.2=经销商，1.3=医疗机构，1.4=其他
     */
    private Integer orgType;

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
     * 营业执照（存储路径/URL）
     */
    private String businessLicense;

    /**
     * 代理区域（经销商）
     */
    private String agentArea;

    /**
     * 代理产品线（多个用逗号分隔，关联字典编码=5）
     */
    private String agentProductLine;

    /**
     * 医院等级（医疗机构，关联字典编码=3）
     */
    private Integer hospitalLevel;

    /**
     * 医院类型（医疗机构，关联字典编码=4）
     */
    private Integer hospitalType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
