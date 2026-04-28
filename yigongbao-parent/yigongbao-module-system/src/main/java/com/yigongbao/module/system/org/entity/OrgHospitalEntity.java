package com.yigongbao.module.system.org.entity;

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
    private Long id;
    private Long distributorOrgId;
    private Long hospitalOrgId;
    private LocalDateTime createTime;
}
