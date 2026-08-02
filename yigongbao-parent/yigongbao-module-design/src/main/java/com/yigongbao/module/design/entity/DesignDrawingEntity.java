package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图纸 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_drawing")
@EqualsAndHashCode(callSuper = false)
public class DesignDrawingEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 数据包ID
     */
    private Long packageId;

    /** 产品分类字典码，同一数据包内按分类分别生成图纸 */
    private String productCategory;

    /**
     * 总页数
     */
    private Integer pageCount;

    /**
     * 版本号（与指令单同步）
     */
    private String version;

    /**
     * 版本序号
     */
    private Integer versionSeq;

    /**
     * 版本来源：AUTO=自动生成，MANUAL=手动上传
     */
    private String sourceType;

    /**
     * 模板文件ID（系统生成）
     */
    private String templateFileId;

    /**
     * 模板文件URL
     */
    private String templateFileUrl;

    /**
     * 生成该图纸版本时实际使用的二维码文件ID（文件业务类型10.21）
     */
    private String qrFileId;

    /**
     * 修订版文件ID（设计师上传）
     */
    private String revisedFileId;

    /**
     * 修订版文件URL
     */
    private String revisedFileUrl;

    /**
     * 生成时间
     */
    private LocalDateTime generateTime;

    /**
     * 修订版上传时间
     */
    private LocalDateTime revisedUploadTime;

    /**
     * 设计人ID
     */
    private Long designerId;

    /**
     * 设计人姓名（冗余）
     */
    private String designerName;

    /**
     * 设计日期
     */
    private LocalDate designDate;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核人姓名（冗余）
     */
    private String auditorName;

    /**
     * 审核日期
     */
    private LocalDate auditDate;

    /**
     * 图纸是否已确认（0=未确认，1=已确认）
     * 在线模式：生成/重新生成时重置为0，设计师手动确认后置1
     * 离线模式：上传修订版时自动置1
     */
    private Integer isConfirmed;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;
}
