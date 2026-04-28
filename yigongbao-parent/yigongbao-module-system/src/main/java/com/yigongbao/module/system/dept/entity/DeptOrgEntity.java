package com.yigongbao.module.system.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 部门-机构关联 Entity
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Data
@TableName("sys_dept_org")
public class DeptOrgEntity {
    private Long id;
    private Long deptId;
    private Long orgId;
    private LocalDateTime createTime;
}
