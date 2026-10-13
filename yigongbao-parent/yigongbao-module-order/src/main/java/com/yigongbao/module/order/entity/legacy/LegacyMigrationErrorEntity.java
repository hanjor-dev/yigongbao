package com.yigongbao.module.order.entity.legacy;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("legacy_migration_error")
public class LegacyMigrationErrorEntity extends BaseEntity {
    private Long taskId;
    private Long sourceOrderId;
    private String sourceOrderCode;
    private String errorType;
    private String errorMessage;
    private String rawSnapshot;
}
