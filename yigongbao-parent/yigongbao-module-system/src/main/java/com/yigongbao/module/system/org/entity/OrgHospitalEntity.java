package com.yigongbao.module.system.org.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 经销商-医院关联 Entity
 *
 * @author hanjor
 * @date 2026-04-28
 */
@Data
@TableName("sys_org_hospital")
public class OrgHospitalEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 经销商机构ID */
    private Long distributorOrgId;

    /** 医院机构ID */
    private Long hospitalOrgId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
