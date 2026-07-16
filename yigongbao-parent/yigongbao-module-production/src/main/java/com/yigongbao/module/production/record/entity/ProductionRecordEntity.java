package com.yigongbao.module.production.record.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产流转卡实体
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("production_record")
public class ProductionRecordEntity extends BaseEntity {
    /** 流转卡编号（系统生成，唯一） */
    private String recordNo;
    /** 关联订单ID */
    private Long orderId;
    /** 订单编号（冗余） */
    private String orderCode;
    /** 订单类型（1=医疗器械，2=非医疗器械） */
    private Integer orderType;
    /** 关联设计数据包ID */
    private Long designPackageId;
    /** 设计数据包编号（冗余） */
    private String designPackageCode;
    /** 产品ID */
    private Long productId;
    /** 产品名称（冗余） */
    private String productName;
    /** 产品大类代码（如17.1，冗余自product.category） */
    private String productCategory;
    /** 产品大类名称（如"模型"、"导板"，冗余自product.category_name） */
    private String productCategoryName;
    /** 生产批号 */
    private String productionBatchNo;
    /** 版本号 */
    private String versionNo;
    /** 打印材质 */
    private String material;
    /** 加工中心ID */
    private Long processingCenterId;
    /** 加工中心名称 */
    private String processingCenterName;
    /** 生产员ID */
    private Long producerId;
    /** 生产员姓名 */
    private String producerName;
    /** 质检员ID */
    private Long qcId;
    /** 质检员姓名 */
    private String qcName;
    /** 打印机ID（assignDevice 时写入） */
    private Long printDeviceId;
    /** 打印机编号 */
    private String printDeviceCode;
    /** 打印机名称 */
    private String printDeviceName;
    /** 产品总数（创建时写入，始终有值） */
    private Integer totalProductCount;
    /** 质检合格数量（每次 markProductPass 时原子自增） */
    private Integer qualifiedCount;
    /** 质检不合格数量（每次 markProductFail 时原子自增，累计值） */
    private Integer unqualifiedCount;
    /** 流转卡状态（对应 FlowStatusEnum 值） */
    private Integer status;
    /** 当前所在工序类型（后处理阶段有值，其他阶段为 null） */
    private String currentProcess;
    /** 流转卡二维码内容 */
    private String qrCodeUrl;
    /** 包装设备ID */
    private Long packDeviceId;
    /** 包装设备编号 */
    private String packDeviceNo;
    /** 热封温度（℃） */
    private BigDecimal packSealTemperature;
    /** 热封时间（秒） */
    private Integer packSealTime;
    /** 包装材质（如：纸封袋、PE符合食品包装袋） */
    private String packMaterial;
    /** 灭菌方式 */
    private String packSterilizationMethod;
    /** 灭菌批号 */
    private String packSterilizationBatchNo;
    /** 包装操作员ID */
    private Long packOperatorId;
    /** 包装操作员姓名 */
    private String packOperatorName;
    /** 包装操作时间 */
    private LocalDateTime packTime;
    /** 原材料批号 */
    private String materialBatchNo;
    /** 打印开始时间（由设备 WebSocket 状态推送写入） */
    private LocalDateTime printStartTime;
    /** 打印完成时间（由设备 WebSocket 状态推送写入） */
    private LocalDateTime printFinishTime;
    /** 后处理结束时间 */
    private LocalDateTime postProcessingEndTime;
    // ===== 订单基础信息冗余字段（创建时从 order_main 复制，避免跨模块查询）=====
    private String hospitalName;
    private String hospitalDeptName;
    private String doctorName;
    private String patientName;
    /** 是否加急（0=否，1=是） */
    private Integer isUrgent;
    /** 是否邮寄（0=否，1=是） */
    private Integer isPostal;
    /** 期望交付时间 */
    private LocalDateTime expectedDeliveryDate;
    /** 流转卡Excel文件URL */
    private String flowCardFileUrl;
    /** 流转卡Excel生成时间 */
    private LocalDateTime flowCardGenerateTime;
    /** 流转卡内容最后更新时间 */
    private LocalDateTime contentUpdateTime;
}
