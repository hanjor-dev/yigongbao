package com.yigongbao.module.basic.file.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件信息 VO（视图对象）
 * 用于返回给前端的文件数据
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Data
public class FileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件ID（由框架雪花算法生成）
     */
    private String id;

    /**
     * 业务类型（如：registration_cert、doctor_cert）
     */
    private String bizType;

    /**
     * 业务ID（关联的业务主键）
     */
    private Long bizId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 存储路径（不含文件名）
     */
    private String filePath;

    /**
     * 访问URL
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 格式化文件大小（如 2.35 MB）
     */
    private String fileSizeText;

    /**
     * 文件MIME类型
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 存储平台标识（如：local、aliyun-oss-1）
     */
    private String platform;

    /**
     * 缩略图访问URL
     */
    private String thUrl;

    /**
     * 缩略图大小（字节）
     */
    private Long thSize;

    /**
     * 文件哈希（MD5，从 hashInfo 中提取）
     */
    private String fileHash;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
