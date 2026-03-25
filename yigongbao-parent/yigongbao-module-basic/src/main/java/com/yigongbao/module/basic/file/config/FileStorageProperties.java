package com.yigongbao.module.basic.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置属性类
 * 读取 yigongbao.file-storage.* 配置项
 *
 * @author hanjor
 * @date 2026-03-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "yigongbao.file-storage")
public class FileStorageProperties {

    /**
     * 最大文件大小（字节），默认 500MB
     */
    private long maxFileSize = 524288000L;

    /**
     * 允许的文件扩展名
     */
    private String[] allowedExtensions = {
            "jpg", "jpeg", "png", "gif", "bmp", "pdf",
            "doc", "docx", "xls", "xlsx", "zip", "rar",
            "stl", "obj", "3ds", "ply", "step", "dcm"
    };
}
