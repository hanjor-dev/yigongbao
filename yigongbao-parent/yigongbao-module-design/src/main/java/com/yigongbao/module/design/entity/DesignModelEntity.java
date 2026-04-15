package com.yigongbao.module.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yigongbao.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 可视化模型文件 Entity
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@TableName("design_model")
@EqualsAndHashCode(callSuper = false)
public class DesignModelEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

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
     * 文件扩展名（3dpdf/ply/stl）
     */
    private String fileExt;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}
