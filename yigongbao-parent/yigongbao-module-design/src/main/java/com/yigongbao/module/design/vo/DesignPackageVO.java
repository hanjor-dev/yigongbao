package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印文件数据包 VO
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
public class DesignPackageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据包ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 数据包编号
     */
    private String packageCode;

    /**
     * 序号
     */
    private Integer packageSeq;

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
     * 包内文件数量
     */
    private Integer fileCount;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 包内文件列表
     */
    private List<DesignPackageFileVO> files;
}
