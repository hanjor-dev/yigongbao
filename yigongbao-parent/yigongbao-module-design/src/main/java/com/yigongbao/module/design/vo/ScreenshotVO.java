package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件截图 VO
 * 用于截图上传/查询接口的返回数据
 *
 * @author hanjor
 * @date 2026-04-20
 */
@Data
public class ScreenshotVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 截图文件ID
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 公开访问URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
}
