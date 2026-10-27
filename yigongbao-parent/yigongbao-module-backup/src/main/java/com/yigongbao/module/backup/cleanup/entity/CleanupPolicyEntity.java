package com.yigongbao.module.backup.cleanup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 持久化 COS 清理策略版本。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cos_cleanup_policy")
public class CleanupPolicyEntity extends BaseEntity {
    private String policyCode;
    private Integer policyVersion;
    private String policyJson;
    private Integer enabled;
    private Integer version;
}
