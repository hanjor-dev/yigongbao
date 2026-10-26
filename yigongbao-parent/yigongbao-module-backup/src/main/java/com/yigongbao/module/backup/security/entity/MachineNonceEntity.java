package com.yigongbao.module.backup.security.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/** 已消费 nonce；数据库应对 machine_id、key_id、nonce_hash 建立唯一索引。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_hmac_nonce")
public class MachineNonceEntity extends BaseEntity {

    @TableField("machine_id")
    private String machineId;

    @TableField("key_id")
    private String keyId;

    @TableField("nonce_hash")
    private String nonceHash;

    @TableField("request_timestamp")
    private Instant requestTimestamp;

    @TableField("expires_at")
    private Instant expiresAt;
}
