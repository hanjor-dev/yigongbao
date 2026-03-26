package com.yigongbao.module.system.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import com.yigongbao.module.system.dict.service.DictService;
import com.yigongbao.module.system.dict.vo.DictVO;
import com.yigongbao.module.system.org.convert.OrgConvert;
import com.yigongbao.module.system.org.dto.CreateOrgDTO;
import com.yigongbao.module.system.org.dto.UpdateOrgDTO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.service.OrgService;
import com.yigongbao.module.system.org.vo.OrgVO;
import com.yigongbao.module.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 机构 Service 实现类
 * 处理机构相关的业务逻辑，包括机构CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrgServiceImpl extends ServiceImpl<OrgMapper, OrgEntity> implements OrgService {

    private final DictService dictService;
    private final UserMapper userMapper;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * 分页查询机构列表
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param orgName  机构名称（模糊查询）
     * @param orgType  机构类型
     * @param areaId   地区ID
     * @param status   状态
     * @return 分页后的机构列表
     */
    @Override
    public IPage<OrgVO> listOrg(Integer pageNum, Integer pageSize, String orgName, String orgType, Long areaId, Integer status) {
        log.info("分页查询机构列表，pageNum={}, pageSize={}, orgName={}, orgType={}, areaId={}, status={}",
                pageNum, pageSize, orgName, orgType, areaId, status);
        try {
            // 构建分页对象
            Page<OrgEntity> page = new Page<>(pageNum, pageSize);
            // 构建查询条件
            LambdaQueryWrapper<OrgEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(orgName), OrgEntity::getOrgName, orgName)
                    .eq(StrUtil.isNotBlank(orgType), OrgEntity::getOrgType, orgType)
                    .eq(Objects.nonNull(areaId), OrgEntity::getAreaId, areaId)
                    .eq(Objects.nonNull(status), OrgEntity::getStatus, status)
                    .orderByDesc(OrgEntity::getCreateTime);
            // 执行分页查询
            IPage<OrgEntity> pageResult = page(page, wrapper);
            // 转换为VO并填充字典名称
            IPage<OrgVO> voPage = pageResult.convert(this::toVOWithDictNames);
            log.info("分页查询机构列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询机构列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @Override
    public OrgVO getOrgById(Long id) {
        log.info("根据ID查询机构详情，id={}", id);
        try {
            OrgEntity entity = getById(id);
            if (entity == null) {
                log.warn("机构不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            }
            OrgVO vo = toVOWithDictNames(entity);
            log.info("查询机构详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询机构详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建机构
     *
     * @param dto 创建参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrg(CreateOrgDTO dto) {
        log.info("创建机构，orgName={}", dto.getOrgName());
        try {
            // 校验机构名称是否已存在
            if (isOrgNameExists(dto.getOrgName())) {
                log.warn("机构名称已存在，orgName={}", dto.getOrgName());
                throw new BusinessException(ErrorCodeEnum.ORG_EXISTS);
            }
            // 校验机构类型是否存在
            if (!isOrgTypeValid(dto.getOrgType())) {
                log.warn("机构类型不存在，orgType={}", dto.getOrgType());
                throw new BusinessException(ErrorCodeEnum.ORG_TYPE_NOT_FOUND);
            }
            // 生成机构编码
            String prefix = getOrgPrefixByType(dto.getOrgType());
            String orgCode = codeGeneratorService.generateWithCustomPrefix(CodeRuleConstants.ORG_NO, prefix);
            // DTO转换为实体对象
            OrgEntity entity = OrgConvert.toEntity(dto);
            entity.setOrgCode(orgCode);
            entity.setStatus(StatusConstants.NORMAL);
            // 插入数据库
            save(entity);
            log.info("创建机构成功，id={}, orgCode={}", entity.getId(), orgCode);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建机构异常，orgName={}", dto.getOrgName(), e);
            throw e;
        }
    }

    /**
     * 更新机构
     *
     * @param id  机构ID
     * @param dto 更新参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrg(Long id, UpdateOrgDTO dto) {
        log.info("更新机构，id={}", id);
        try {
            // 校验机构是否存在
            OrgEntity entity = getById(id);
            if (entity == null) {
                log.warn("机构不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            }
            // 校验机构名称是否与其他机构重复
            if (StrUtil.isNotBlank(dto.getOrgName()) && !dto.getOrgName().equals(entity.getOrgName())) {
                if (isOrgNameExistsExcludingId(dto.getOrgName(), id)) {
                    log.warn("机构名称已存在，orgName={}", dto.getOrgName());
                    throw new BusinessException(ErrorCodeEnum.ORG_EXISTS);
                }
            }
            // 更新机构信息
            BeanUtils.copyProperties(dto, entity, "id", "orgCode", "createTime", "updateTime", "createBy", "updateBy");
            // 更新数据库
            updateById(entity);
            log.info("更新机构成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新机构异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除机构
     *
     * @param id 机构ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrg(Long id) {
        log.info("删除机构，id={}", id);
        try {
            // 校验机构是否存在
            OrgEntity entity = getById(id);
            if (entity == null) {
                log.warn("机构不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            }
            // 校验该机构下是否有用户
            if (hasUsers(id)) {
                log.warn("该机构下存在用户，无法删除，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORG_HAS_USERS);
            }
            // 逻辑删除
            removeById(id);
            log.info("删除机构成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除机构异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改机构状态
     *
     * @param id     机构ID
     * @param status 状态（0=禁用，1=正常）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改机构状态，id={}, status={}", id, status);
        try {
            // 校验机构是否存在
            OrgEntity entity = getById(id);
            if (entity == null) {
                log.warn("机构不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.ORG_NOT_FOUND);
            }
            // 更新状态
            entity.setStatus(status);
            updateById(entity);
            log.info("修改机构状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改机构状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为VO并填充字典名称
     *
     * @param entity 机构实体
     * @return 机构VO
     */
    private OrgVO toVOWithDictNames(OrgEntity entity) {
        OrgVO vo = OrgConvert.toVO(entity);
        if (vo == null) {
            return null;
        }
        // 填充机构类型名称（基于 dictCode 关联）
        if (vo.getOrgType() != null) {
            DictVO dict = dictService.getByDictCode(vo.getOrgType());
            vo.setOrgTypeName(dict != null ? dict.getDictName() : null);
        }
        // 填充状态名称
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        // 填充医院等级名称
        if (vo.getHospitalLevel() != null) {
            DictVO dict = dictService.getByDictCode(vo.getHospitalLevel());
            vo.setHospitalLevelName(dict != null ? dict.getDictName() : null);
        }
        // 填充医院类型名称
        if (vo.getHospitalType() != null) {
            DictVO dict = dictService.getByDictCode(vo.getHospitalType());
            vo.setHospitalTypeName(dict != null ? dict.getDictName() : null);
        }
        // 填充代理产品线名称
        if (StrUtil.isNotBlank(vo.getAgentProductLine())) {
            vo.setAgentProductLineNames(getDictNamesByDictCodes(vo.getAgentProductLine()));
        }
        return vo;
    }

    /**
     * 根据字典编码列表获取字典名称列表
     *
     * @param dictCodes 字典编码（逗号分隔）
     * @return 字典名称（逗号分隔）
     */
    private String getDictNamesByDictCodes(String dictCodes) {
        if (StrUtil.isBlank(dictCodes)) {
            return null;
        }
        return Arrays.stream(dictCodes.split(","))
                .map(code -> {
                    DictVO dict = dictService.getByDictCode(code.trim());
                    return dict != null ? dict.getDictName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

    /**
     * 校验机构名称是否存在
     *
     * @param orgName 机构名称
     * @return true-存在，false-不存在
     */
    private boolean isOrgNameExists(String orgName) {
        return count(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgName, orgName)) > 0;
    }

    /**
     * 校验机构名称是否存在（排除指定ID）
     *
     * @param orgName 机构名称
     * @param excludeId 排除的机构ID
     * @return true-存在，false-不存在
     */
    private boolean isOrgNameExistsExcludingId(String orgName, Long excludeId) {
        return count(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgName, orgName)
                .ne(OrgEntity::getId, excludeId)) > 0;
    }

    /**
     * 校验机构类型是否有效
     *
     * @param orgType 机构类型（字典编码）
     * @return true-有效，false-无效
     */
    private boolean isOrgTypeValid(String orgType) {
        if (orgType == null) {
            return false;
        }
        return dictService.getByDictCode(orgType) != null;
    }

    /**
     * 根据机构类型获取编码前缀
     *
     * @param orgType 机构类型（字典编码）
     * @return 编码前缀
     */
    private String getOrgPrefixByType(String orgType) {
        return switch (orgType) {
            case "1.1" -> "ORG-P-";  // 生产企业
            case "1.2" -> "ORG-D-";  // 经销商
            case "1.3" -> "ORG-H-";  // 医疗机构
            default -> "ORG-O-";       // 其他
        };
    }

    /**
     * 校验该机构下是否有用户
     *
     * @param orgId 机构ID
     * @return true-有用户，false-无用户
     */
    private boolean hasUsers(Long orgId) {
        return userMapper.countByOrgId(orgId) > 0;
    }
}
