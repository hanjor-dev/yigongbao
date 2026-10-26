package com.yigongbao.module.backup.security.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 机器 HMAC key 元数据；secretReference 指向外部密钥配置。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_machine_key")
public class MachineKeyEntity extends BaseEntity {

    @TableField("machine_id")
    private String machineId;

    @TableField("key_id")
    private String keyId;

    @TableField("secret_ref")
    private String secretReference;

    @TableField("key_status")
    private String keyStatus;

    @TableField("valid_from")
    private LocalDateTime validFrom;

    @TableField("valid_until")
    private LocalDateTime validUntil;

    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("allowed_scopes")
    private String allowedScopes;
}
