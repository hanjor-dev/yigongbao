package com.yigongbao.module.system.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.entity.ConfigEntity;
import com.yigongbao.module.system.config.vo.ConfigVO;

/**
 * 配置 Service
 *
 * @author hanjor
 * @date 2026-03-18
 */
public interface ConfigService extends IService<ConfigEntity> {

    /**
     * 分页查询配置列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param configKey 配置键（模糊查询）
     * @param configName 配置名称（模糊查询）
     * @param configGroup 配置分组（精确查询）
     * @param configType 配置类型（精确查询）
     * @param status 状态（精确查询）
     * @return 分页后的配置列表
     */
    IPage<ConfigVO> pageConfig(Integer pageNum, Integer pageSize, String configKey, String configName,
                                String configGroup, String configType, Integer status);

    /**
     * 根据ID查询配置
     *
     * @param id 配置ID
     * @return 配置详情
     */
    ConfigVO getConfigById(Long id);

    /**
     * 根据键名查询配置
     *
     * @param configKey 配置键
     * @return 配置详情
     */
    ConfigVO getConfigByKey(String configKey);

    /**
     * 创建配置
     *
     * @param dto 创建参数
     */
    void createConfig(CreateConfigDTO dto);

    /**
     * 更新配置
     *
     * @param id 配置ID
     * @param dto 更新参数
     */
    void updateConfig(Long id, UpdateConfigDTO dto);

    /**
     * 删除配置
     *
     * @param id 配置ID
     */
    void deleteConfig(Long id);

    /**
     * 获取所有配置（公开+非公开）
     *
     * @return 配置列表
     */
    java.util.List<ConfigVO> listAllConfig();

    /**
     * 获取所有公开配置
     *
     * @return 公开配置列表
     */
    java.util.List<ConfigVO> listPublicConfig();

    /**
     * 获取配置分组列表
     *
     * @return 分组列表（label=分组名称，value=分组编码）
     */
    java.util.List<com.yigongbao.module.system.basedata.vo.SelectTreeVO> listConfigGroups();

    /**
     * 根据键名获取配置值
     * 如果配置不存在或已禁用，返回 null
     *
     * @param configKey 配置键
     * @return 配置值，如果不存在或已禁用则返回 null
     */
    String getConfigValue(String configKey);
}
