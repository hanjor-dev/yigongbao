package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 打印文件数据包 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_package")
@EqualsAndHashCode(callSuper = false)
public class DesignPackageEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号（冗余）
     */
    private String orderCode;

    /**
     * 数据包编号（规则：订单编号-序号）
     */
    private String packageCode;

    /**
     * 序号（订单内递增）
     */
    private Integer packageSeq;

    /**
     * 文件ID（关联 file_detail）
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 包内文件数量
     */
    private Integer fileCount;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}
