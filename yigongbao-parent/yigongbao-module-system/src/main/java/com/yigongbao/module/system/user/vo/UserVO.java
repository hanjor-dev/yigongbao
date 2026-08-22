package com.yigongbao.module.system.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 VO（视图对象）
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别（字典编码，如 2.1=男，2.2=女）
     */
    private String sex;

    /**
     * 性别名称
     */
    private String sexName;

    /**
     * 头像路径
     */
    private String avatar;

    /**
     * 账户分类（6.1=企业账户，6.2=业务账户）
     */
    private String accountType;

    /**
     * 账户分类名称
     */
    private String accountTypeName;

    /**
     * 所属机构ID
     */
    private Long orgId;

    /**
     * 所属机构名称
     */
    private String orgName;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 所属部门名称
     */
    private String deptName;

    /**
     * 关联角色ID
     */
    private Long roleId;

    /**
     * 关联角色名称
     */
    private String roleName;

    /**
     * 关联角色编码
     */
    private String roleCode;

    /**
     * 所属加工中心ID（生产员角色专用）
     */
    private Long centerId;

    /**
     * 所属加工中心名称（生产员角色专用）
     */
    private String centerName;

    /**
     * 数据权限范围（来自关联角色），前端通过 dataScopeType == 'hospitals' 判断是否显示医院选择区域
     */
    private String dataScopeType;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 资产编码（企业账户选填）
     */
    private String assetNumber;

    /**
     * 专业方向
     */
    private String specialty;

    /**
     * 专业方向名称
     */
    private String specialtyName;

    /**
     * 专业方向字典编码列表（多选，供前端展示）
     */
    private List<String> specialtyList;

    /**
     * 专业方向名称列表（多选，供前端展示）
     */
    private List<String> specialtyNameList;

    /**
     * 资质证书信息
     */
    private String qualification;

    /**
     * 结算类型
     */
    private Integer settlementType;

    /**
     * 结算类型名称
     */
    private String settlementTypeName;

    /**
     * 收费模板ID（业务类型账户）
     */
    private Long chargingTemplateId;

    /**
     * 收费模板名称
     */
    private String chargingTemplateName;

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
     * 订单列配置（JSON，用户个人自定义列显示设置）
     */
    private String orderColumnSettings;

    /**
     * 设计工单列配置（JSON，用户个人自定义列显示设置）
     */
    private String designColumnSettings;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 已分配的医院ID列表
     * 用于前端展示用户已分配的医院
     */
    private List<Long> hospitalIds;

    /**
     * 已分配的医院名称列表
     * 用于前端展示用户已分配的医院名称
     */
    private List<String> hospitalNames;

    /** 区域管理员额外管理机构ID列表（不含主机构） */
    private List<Long> managedOrgIds;

    /** 区域管理员实际生效机构ID列表（主机构 + 额外管理机构） */
    private List<Long> effectiveOrgIds;
}
