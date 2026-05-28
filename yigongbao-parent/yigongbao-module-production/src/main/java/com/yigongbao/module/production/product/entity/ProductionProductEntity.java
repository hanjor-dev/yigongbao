package com.yigongbao.module.production.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 生产产品记录实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_product")
public class ProductionProductEntity extends BaseEntity {
    private Long productionRecordId;
    private Long printFileId;
    private String productNo;
    private String productName;
    private String specName;
    private String certNo;
    private String materialName;
    private String colorName;
    private String fileName;
    private String udiCode;
    private String udiDi;
    private String udiPi;
    private LocalDateTime udiGenerateTime;
    private String status;
    private String currentProcessType;
    private String qcResult;
    private String qcRemark;
    private LocalDateTime qcTime;
    private Long qcUserId;
    private String redoProcessType;
}
