package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可视化模型 VO
 * 文件详情通过 fileId 从 FileService 获取后填充
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignModelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID（design_model.id）
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 文件ID（file_detail.id）
     */
    private String fileId;

    /**
     * 原始文件名（来自 FileVO）
     */
    private String fileName;

    /**
     * 文件访问地址（来自 FileVO）
     */
    private String fileUrl;
    private String downloadUrl;

    /**
     * 文件大小（字节，来自 FileVO）
     */
    private Long fileSize;

    /**
     * 文件扩展名（来自 FileVO）
     */
    private String fileExt;

    /**
     * 创建时间（来自 design_model.create_time）
     */
    private LocalDateTime createTime;
}
