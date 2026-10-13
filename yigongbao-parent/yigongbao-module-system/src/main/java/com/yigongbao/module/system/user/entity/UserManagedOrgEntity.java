package com.yigongbao.module.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 区域管理员账户的额外管理机构。主机构始终以 sys_user.org_id 为准。 */
@Data
@TableName("sys_user_managed_org")
public class UserManagedOrgEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long orgId;
    private LocalDateTime createTime;
    private Long createBy;
}
