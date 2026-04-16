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
     * 最大文件大小（字节），默认 2GB
     * 全局上限，各业务类型的实际限制由业务层自行校验
     */
    private long maxFileSize = 2147483648L;
}
