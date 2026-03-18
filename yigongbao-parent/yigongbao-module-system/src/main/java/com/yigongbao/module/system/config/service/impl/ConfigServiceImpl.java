package com.yigongbao.module.system.config.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.module.system.config.convert.ConfigConvert;
import com.yigongbao.module.system.config.dto.CreateConfigDTO;
import com.yigongbao.module.system.config.dto.UpdateConfigDTO;
import com.yigongbao.module.system.config.entity.ConfigEntity;
import com.yigongbao.module.system.config.mapper.ConfigMapper;
import com.yigongbao.module.system.config.service.ConfigService;
import com.yigongbao.module.system.config.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置 Service 实现类
 * 处理配置相关的业务逻辑，包括配置CRUD、配置键唯一性校验、系统内置配置保护等
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, ConfigEntity> implements ConfigService {

    private final ConfigConvert configConvert;

    /**
     * 分页查询配置列表
     *
     * @param pageNum 分页参数-页码
     * @param pageSize 分页参数-每页大小
     * @param configKey 配置键（模糊查询）
     * @param configName 配置名称（模糊查询）
     * @param configGroup 配置分组（精确查询）
     * @param configType 配置类型（精确查询）
     * @param status 状态（精确查询）
     * @return 分页后的配置列表
     */
    @Override
    public IPage<ConfigVO> pageConfig(Integer pageNum, Integer pageSize, String configKey, String configName,
                                       String configGroup, String configType, Integer status) {
        // 构建分页对象
        Page<ConfigEntity> page = new Page<>(pageNum, pageSize);
        // 构建查询条件
        LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
        // 配置键模糊查询（非空时生效）
        wrapper.like(StrUtil.isNotEmpty(configKey), ConfigEntity::getConfigKey, configKey)
                // 配置名称模糊查询（非空时生效）
                .like(StrUtil.isNotEmpty(configName), ConfigEntity::getConfigName, configName)
                // 配置分组精确查询（非空时生效）
                .eq(StrUtil.isNotEmpty(configGroup), ConfigEntity::getConfigGroup, configGroup)
                // 配置类型精确查询（非空时生效）
                .eq(StrUtil.isNotEmpty(configType), ConfigEntity::getConfigType, configType)
                // 状态精确查询（非空时生效）
                .eq(status != null, ConfigEntity::getStatus, status)
                // 按排序字段正序，创建时间倒序
                .orderByAsc(ConfigEntity::getSort)
                .orderByDesc(ConfigEntity::getCreateTime);
        // 执行分页查询
        IPage<ConfigEntity> pageResult = this.page(page, wrapper);
        // 转换为VO返回
        return pageResult.convert(configConvert::toVO);
    }

    /**
     * 根据ID查询配置
     *
     * @param id 配置ID
     * @return 配置详情
     * @throws BusinessException 配置不存在
     */
    @Override
    public ConfigVO getConfigById(Long id) {
        // 记录查询入参
        log.info("根据ID查询配置，id={}", id);
        try {
            // 执行数据库查询
            ConfigEntity entity = this.getById(id);
            // 校验配置是否存在
            if (entity == null) {
                log.warn("配置不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_FOUND);
            }
            // 转换为VO对象
            ConfigVO vo = configConvert.toVO(entity);
            // 记录查询成功
            log.info("查询配置成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 记录系统异常
            log.error("查询配置异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 根据键名查询配置
     *
     * @param configKey 配置键
     * @return 配置详情
     * @throws BusinessException 配置不存在
     */
    @Override
    public ConfigVO getConfigByKey(String configKey) {
        // 记录查询入参
        log.info("根据键名查询配置，configKey={}", configKey);
        try {
            // 构建查询条件
            LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ConfigEntity::getConfigKey, configKey);
            // 执行查询
            ConfigEntity entity = baseMapper.selectOne(wrapper);
            // 校验配置是否存在
            if (entity == null) {
                log.warn("配置不存在，configKey={}", configKey);
                throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_FOUND);
            }
            // 转换为VO对象
            ConfigVO vo = configConvert.toVO(entity);
            // 记录查询成功
            log.info("查询配置成功，configKey={}", configKey);
            return vo;
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 记录系统异常
            log.error("查询配置异常，configKey={}", configKey, e);
            throw e;
        }
    }

    /**
     * 创建配置
     *
     * @param dto 创建参数
     * @throws BusinessException 配置键已存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(CreateConfigDTO dto) {
        // 记录创建入参
        log.info("创建配置，configKey={}, configName={}", dto.getConfigKey(), dto.getConfigName());
        try {
            // 检查 configKey 是否已存在
            if (isConfigKeyExists(dto.getConfigKey(), null)) {
                log.warn("配置键已存在，configKey={}", dto.getConfigKey());
                throw new BusinessException(ErrorCodeEnum.CONFIG_KEY_EXISTS);
            }
            // DTO转换为实体对象
            ConfigEntity entity = configConvert.toEntity(dto);
            // 插入数据库
            this.save(entity);
            // 记录创建成功
            log.info("创建配置成功，id={}, configKey={}", entity.getId(), dto.getConfigKey());
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 记录系统异常
            log.error("创建配置异常，configKey={}", dto.getConfigKey(), e);
            throw e;
        }
    }

    /**
     * 更新配置
     *
     * @param id 配置ID
     * @param dto 更新参数
     * @throws BusinessException 配置不存在、系统内置配置不可修改
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, UpdateConfigDTO dto) {
        // 记录更新入参
        log.info("更新配置，id={}", id);
        try {
            // 查询配置是否存在
            ConfigEntity entity = this.getById(id);
            // 校验配置是否存在
            if (entity == null) {
                log.warn("配置不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_FOUND);
            }
            // 检查是否为系统内置配置，系统内置配置不可修改
            if (entity.getIsSystem() != null && entity.getIsSystem() == 1) {
                log.warn("系统内置配置不可修改，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CONFIG_SYSTEM_NOT_ALLOW_UPDATE);
            }
            // 更新配置实体
            configConvert.updateEntity(dto, entity);
            // 更新数据库
            this.updateById(entity);
            // 记录更新成功
            log.info("更新配置成功，id={}", id);
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 记录系统异常
            log.error("更新配置异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @throws BusinessException 配置不存在、系统内置配置不可删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        // 记录删除入参
        log.info("删除配置，id={}", id);
        try {
            // 查询配置是否存在
            ConfigEntity entity = this.getById(id);
            // 校验配置是否存在
            if (entity == null) {
                log.warn("配置不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_FOUND);
            }
            // 检查是否为系统内置配置，系统内置配置不可删除
            if (entity.getIsSystem() != null && entity.getIsSystem() == 1) {
                log.warn("系统内置配置不可删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CONFIG_SYSTEM_NOT_ALLOW_DELETE);
            }
            // 执行删除
            this.removeById(id);
            // 记录删除成功
            log.info("删除配置成功，id={}", id);
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 记录系统异常
            log.error("删除配置异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 获取所有配置列表
     *
     * @return 配置列表
     */
    @Override
    public List<ConfigVO> listAllConfig() {
        // 记录查询日志
        log.info("获取所有配置列表");
        // 构建查询条件
        LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
        // 按排序字段正序
        wrapper.orderByAsc(ConfigEntity::getSort);
        // 执行查询
        List<ConfigEntity> list = this.list(wrapper);
        // 转换为VO列表返回
        return list.stream().map(configConvert::toVO).collect(Collectors.toList());
    }

    /**
     * 获取所有公开配置
     * 公开配置指 isPublic=1 且 status=1 的配置，用于无需登录即可获取的配置信息
     *
     * @return 公开配置列表
     */
    @Override
    public List<ConfigVO> listPublicConfig() {
        // 记录查询日志
        log.info("获取公开配置列表");
        // 构建查询条件
        LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
        // 公开的配置
        wrapper.eq(ConfigEntity::getIsPublic, 1)
                // 状态正常
                .eq(ConfigEntity::getStatus, 1)
                // 按排序字段正序
                .orderByAsc(ConfigEntity::getSort);
        // 执行查询
        List<ConfigEntity> list = this.list(wrapper);
        // 转换为VO列表返回
        return list.stream().map(configConvert::toVO).collect(Collectors.toList());
    }

    /**
     * 检查 configKey 是否已存在
     *
     * @param configKey 配置键
     * @param excludeId 排除的ID（用于更新时检查）
     * @return true-存在，false-不存在
     */
    private boolean isConfigKeyExists(String configKey, Long excludeId) {
        // 构建查询条件
        LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigEntity::getConfigKey, configKey);
        // 排除指定ID（更新时使用）
        if (excludeId != null) {
            wrapper.ne(ConfigEntity::getId, excludeId);
        }
        // 返回计数结果
        return this.count(wrapper) > 0;
    }

    /**
     * 获取配置分组列表
     * 预设分组：系统配置、安全配置、其他配置
     *
     * @return 分组列表（label=分组名称，value=分组编码）
     */
    @Override
    public List<com.yigongbao.module.system.basedata.vo.SelectTreeVO> listConfigGroups() {
        // 记录查询日志
        log.info("获取配置分组列表");
        // 预设分组：系统配置、安全配置、其他配置
        List<com.yigongbao.module.system.basedata.vo.SelectTreeVO> groups = new java.util.ArrayList<>();

        // 系统配置分组
        com.yigongbao.module.system.basedata.vo.SelectTreeVO system = new com.yigongbao.module.system.basedata.vo.SelectTreeVO();
        system.setValue("system");
        system.setName("系统配置");
        groups.add(system);

        // 安全配置分组
        com.yigongbao.module.system.basedata.vo.SelectTreeVO security = new com.yigongbao.module.system.basedata.vo.SelectTreeVO();
        security.setValue("security");
        security.setName("安全配置");
        groups.add(security);

        // 其他配置分组
        com.yigongbao.module.system.basedata.vo.SelectTreeVO other = new com.yigongbao.module.system.basedata.vo.SelectTreeVO();
        other.setValue("other");
        other.setName("其他配置");
        groups.add(other);

        // 返回分组列表
        return groups;
    }

    /**
     * 根据键名获取配置值
     * 只返回状态为启用（status=1）的配置值
     * 如果配置不存在或已禁用，返回 null
     *
     * @param configKey 配置键
     * @return 配置值，如果不存在或已禁用则返回 null
     */
    @Override
    public String getConfigValue(String configKey) {
        // 记录查询日志
        log.info("根据键名获取配置值，configKey={}", configKey);
        // 构建查询条件
        LambdaQueryWrapper<ConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigEntity::getConfigKey, configKey)
                // 只查询启用的配置
                .eq(ConfigEntity::getStatus, StatusConstants.STATUS_ENABLED);
        // 执行查询
        ConfigEntity entity = baseMapper.selectOne(wrapper);
        // 返回配置值（可能为 null）
        return entity != null ? entity.getConfigValue() : null;
    }
}
