package com.yigongbao.module.system.org.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构 VO（视图对象）
 * 用于返回给前端的机构数据
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Data
public class OrgVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 机构名称
     */
    private String orgName;

    /**
     * 机构编码
     */
    private String orgCode;

    /**
     * 机构类型（字典编码）
     */
    private String orgType;

    /**
     * 机构类型名称
     */
    private String orgTypeName;

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
     * 营业执照
     */
    private String businessLicense;

    /**
     * 代理区域（经销商）
     */
    private String agentArea;

    /**
     * 代理产品线（经销商）
     */
    private String agentProductLine;

    /**
     * 代理产品线名称（逗号分隔）
     */
    private String agentProductLineNames;

    /**
     * 医院等级（医疗机构，关联字典编码=3，值如 3.1/3.2/3.3/3.4/3.5）
     */
    private String hospitalLevel;

    /**
     * 医院等级名称
     */
    private String hospitalLevelName;

    /**
     * 医院类型（医疗机构，关联字典编码=4，值如 4.1/4.2）
     */
    private String hospitalType;

    /**
     * 医院类型名称
     */
    private String hospitalTypeName;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注说明
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
