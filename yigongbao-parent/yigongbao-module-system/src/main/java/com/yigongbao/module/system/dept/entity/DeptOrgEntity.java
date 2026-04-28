package com.yigongbao.module.system.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 部门-机构关联实体，记录部门与机构的多对多关联关系
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Data
@TableName("sys_dept_org")
public class DeptOrgEntity {

    /** 主键ID */
    private Long id;

    /** 部门ID */
    private Long deptId;

    /** 机构ID */
    private Long orgId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
