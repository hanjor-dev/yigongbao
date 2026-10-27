package com.yigongbao.module.backup.security.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 线上机器身份元数据；不保存任何密钥正文。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_machine_identity")
public class MachineIdentityEntity extends BaseEntity {

    @TableField("machine_id")
    private String machineId;

    @TableField("machine_status")
    private String machineStatus;

    /** 逗号分隔的 MachineScope 编码，由管理服务维护。 */
    @TableField("allowed_scopes")
    private String allowedScopes;
}
