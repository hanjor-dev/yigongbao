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
    /** 所属流转卡ID */
    private Long productionRecordId;
    /** 打印文件ID（关联 design_package_file） */
    private Long printFileId;
    /** 产品编号（系统生成，唯一） */
    private String productNo;
    /** 产品名称（冗余自设计产品） */
    private String productName;
    /** 型号规格名称（冗余自设计产品） */
    private String specName;
    /** 注册证号（冗余自设计产品） */
    private String certNo;
    /** 材质名称（冗余自设计产品） */
    private String materialName;
    /** 颜色名称（冗余自设计产品） */
    private String colorName;
    /** 打印文件名 */
    private String fileName;
    /** UDI码（医疗器械质检合格时生成） */
    private String udiCode;
    /** UDI-DI 设备标识符 */
    private String udiDi;
    /** UDI-PI 生产标识符 */
    private String udiPi;
    /** UDI生成时间 */
    private LocalDateTime udiGenerateTime;
    /** 产品状态（in_process/fail/pass/completed/cancelled） */
    private String status;
    /** 当前所在工序类型 */
    private String currentProcessType;
    /** 质检结果（pass/fail） */
    private String qcResult;
    /** 质检不合格原因 */
    private String qcRemark;
    /** 质检时间 */
    private LocalDateTime qcTime;
    /** 质检员ID */
    private Long qcUserId;
}
