package com.yigongbao.module.system.dept.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门 VO（视图对象）
 * 用于返回给前端的部门数据
 *
 * @author hanjor
 * @date 2026-03-17
 */
@Data
public class DeptVO {

    /**
     * 关联机构简要信息
     */
    @Data
    public static class OrgSimpleVO {
        private Long id;
        private String orgName;
        private String orgCode;
        private String orgType;
    }

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门类型（字典编码：6.1=企业部门，6.2=业务部门）
     */
    private String deptType;

    /**
     * 部门类型名称
     */
    private String deptTypeName;

    /**
     * 关联机构列表
     */
    private List<OrgSimpleVO> orgs;

    /**
     * 部门负责人姓名（自由输入）
     */
    private String leaderUser;

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
