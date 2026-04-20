package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指令单 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_instruction")
@EqualsAndHashCode(callSuper = false)
public class DesignInstructionEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 数据包ID
     */
    private Long packageId;

    /**
     * 指令单编号（ZL-XXXX）
     */
    private String instructionCode;

    /**
     * 版本号（A/1, A/2...）
     */
    private String version;

    /**
     * 版本序号（1, 2, 3...）
     */
    private Integer versionSeq;

    /**
     * 模板文件ID（系统生成）
     */
    private String templateFileId;

    /**
     * 模板文件URL
     */
    private String templateFileUrl;

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
     * 指令单是否已确认（0=未确认，1=已确认；在线模式下生成/重新生成时重置为0，手动确认后置1；离线模式下上传修订版时自动置1）
     */
    private Integer isConfirmed;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 指令人ID
     */
    private Long issuerId;

    /**
     * 指令人姓名（冗余）
     */
    private String issuerName;

    /**
     * 指令日期
     */
    private LocalDate issueDate;

    /**
     * 复核人ID
     */
    private Long checkerId;

    /**
     * 复核人姓名（冗余）
     */
    private String checkerName;

    /**
     * 复核日期
     */
    private LocalDate checkDate;
}
