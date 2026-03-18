package com.yigongbao.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户 Entity
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
@TableName("sys_user")
@EqualsAndHashCode(callSuper = false)
public class UserEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 登录密码（BCrypt加密）
     */
    private String password;

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
     * 头像路径
     */
    private String avatar;

    /**
     * 账户分类（1=内部用户，2=外部用户）
     */
    private Integer accountType;

    /**
     * 所属机构ID
     */
    private Long orgId;

    /**
     * 所属部门ID
     */
    private Long deptId;

    /**
     * 关联角色ID
     */
    private Long roleId;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 专业方向
     */
    private String specialty;

    /**
     * 资质证书信息
     */
    private String qualification;

    /**
     * 结算类型
     */
    private Integer settlementType;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;
}
