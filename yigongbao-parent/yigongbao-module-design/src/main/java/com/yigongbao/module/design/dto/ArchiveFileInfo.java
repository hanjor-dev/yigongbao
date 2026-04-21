package com.yigongbao.module.design.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 压缩包内文件信息
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveFileInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件名（不含路径）
     */
    private String fileName;

    /**
     * 包内完整路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 扩展名（小写，含点号，如 .stl）
     */
    private String extension;

    /**
     * 解压后的文件内容（字节数组），用于后续独立上传到 OSS
     */
    private byte[] fileContent;
}
