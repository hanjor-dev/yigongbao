package com.yigongbao.module.system.config.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.common.vo.SelectTreeVO;
import com.yigongbao.module.system.config.dto.ConfigPageDTO;
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
     * @param dto 分页查询参数
     * @return 分页后的配置列表
     */
    IPage<ConfigVO> pageConfig(ConfigPageDTO dto);

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
    java.util.List<SelectTreeVO> listConfigGroups();

    /**
     * 根据键名获取配置值
     * 优先从数据库配置获取，如果不存在或已禁用则使用配置文件中的默认值兜底
     *
     * @param configKey 配置键
     * @return 配置值，兜底值也不可能为 null（除非环境严重异常）
     */
    String getConfigValue(String configKey);
}
