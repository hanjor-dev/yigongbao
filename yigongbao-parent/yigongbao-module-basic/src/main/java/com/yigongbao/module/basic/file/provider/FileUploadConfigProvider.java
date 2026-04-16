package com.yigongbao.module.basic.file.provider;

/**
 * 文件上传配置提供者接口
 * 根据 sys_config 配置前缀（configPrefix）动态提供文件上传时的格式和大小限制
 * <p>
 * 模块边界说明：
 * - 接口定义在 module-basic（FileService 所在层），避免 module-basic 直接依赖 module-system
 * - 默认实现 {@link NoOpFileUploadConfigProvider} 返回 null（表示无限制）
 * - module-system 提供 @Primary 实现，通过 ConfigService 读取 sys_config 动态返回配置
 * <p>
 * configPrefix 约定（与 FileBizTypeEnum.configPrefix 和 sys_dict.dict_value 保持一致）：
 * <pre>
 *   allowedExtensions key = configPrefix + ".allowed_extensions"  （逗号分隔扩展名）
 *   maxSizeMb         key = configPrefix + ".max_size_mb"         （MB 整数）
 * </pre>
 *
 * @author hanjor
 * @date 2026-04-16
 */
public interface FileUploadConfigProvider {

    /**
     * 获取指定业务类型允许上传的文件扩展名配置字符串
     *
     * @param configPrefix sys_config 配置前缀（来自 FileBizTypeEnum.configPrefix）
     * @return 逗号分隔的扩展名字符串（如 ".pdf,.doc"），null 表示无限制
     */
    String getAllowedExtensions(String configPrefix);

    /**
     * 获取指定业务类型允许上传的最大文件大小（MB）
     *
     * @param configPrefix sys_config 配置前缀
     * @return 最大大小（MB），null 表示无限制
     */
    Integer getMaxSizeMb(String configPrefix);
}
