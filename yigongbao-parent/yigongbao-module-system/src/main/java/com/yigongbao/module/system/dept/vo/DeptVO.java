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
     * 部门类型（1=内部，2=外部）
     */
    private Integer deptType;

    /**
     * 关联机构ID列表
     */
    private List<Long> orgIds;

    /**
     * 关联机构名称列表
     */
    private List<String> orgNames;

    /**
     * 部门负责人用户ID
     */
    private Long leaderUserId;

    /**
     * 部门负责人姓名
     */
    private String leaderUserName;

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
