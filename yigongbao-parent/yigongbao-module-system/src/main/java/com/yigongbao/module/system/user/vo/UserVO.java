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
     * 性别
     */
    private Integer sex;

    /**
     * 性别名称
     */
    private String sexName;

    /**
     * 头像路径
     */
    private String avatar;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

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
     * 是否启用医院范围权限（0=否，1=是）
     * 用于前端判断是否显示医院选择区域
     */
    private Integer hospitalScopeEnabled;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 专业方向
     */
    private String specialty;

    /**
     * 专业方向名称
     */
    private String specialtyName;

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
     * 已分配的医院ID列表
     * 用于前端展示用户已分配的医院
     */
    private List<Long> hospitalIds;
}
