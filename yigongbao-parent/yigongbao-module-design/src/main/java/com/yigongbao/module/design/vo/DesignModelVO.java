package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可视化模型 VO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 文件ID
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
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}
