package com.yigongbao.module.design.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单当前图纸二维码图片信息。
 */
@Data
public class DesignQrImageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileHash;
    private LocalDateTime uploadTime;
}
