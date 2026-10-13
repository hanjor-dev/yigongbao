package com.yigongbao.module.system.config.provider;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.module.basic.file.provider.FileUploadConfigProvider;
import com.yigongbao.module.system.config.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 基于 sys_config 动态配置的文件上传配置提供者
 * 覆盖 module-basic 中的默认无限制实现，通过 ConfigService 读取数据库配置
 * <p>
 * 配置键约定：
 * <pre>
 *   allowedExtensions = configPrefix + ".allowed_extensions"
 *   maxSizeMb         = configPrefix + ".max_size_mb"
 * </pre>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ConfigBasedFileUploadConfigProvider implements FileUploadConfigProvider {

    private final ConfigService configService;

    @Override
    public String getAllowedExtensions(String configPrefix) {
        if (StrUtil.isBlank(configPrefix)) {
            return null;
        }
        String key = configPrefix + ".allowed_extensions";
        String value = configService.getConfigValue(key);
        return StrUtil.isNotBlank(value) ? value : null;
    }

    @Override
    public Integer getMaxSizeMb(String configPrefix) {
        if (StrUtil.isBlank(configPrefix)) {
            return null;
        }
        String key = configPrefix + ".max_size_mb";
        String value = configService.getConfigValue(key);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("文件大小配置值格式错误，configKey={}, value={}", key, value);
            return null;
        }
    }
}
