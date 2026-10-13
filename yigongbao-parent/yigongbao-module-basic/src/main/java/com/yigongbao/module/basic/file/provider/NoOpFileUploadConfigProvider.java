package com.yigongbao.module.basic.file.provider;

import org.springframework.stereotype.Component;

/**
 * 默认文件上传配置提供者（无限制实现）
 * 当 module-system 未加载时（如单元测试、独立部署），此实现生效，所有文件格式和大小均放行
 * module-system 中的 @Primary 实现会在完整应用启动时覆盖此 Bean
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Component
public class NoOpFileUploadConfigProvider implements FileUploadConfigProvider {

    @Override
    public String getAllowedExtensions(String configPrefix) {
        // 默认无限制，由 ConfigBasedFileUploadConfigProvider（module-system）覆盖
        return null;
    }

    @Override
    public Integer getMaxSizeMb(String configPrefix) {
        // 默认无限制
        return null;
    }
}
