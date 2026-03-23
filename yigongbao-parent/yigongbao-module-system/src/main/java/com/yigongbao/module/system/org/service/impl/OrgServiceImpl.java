package com.yigongbao.module.system.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
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
    public IPage<OrgVO> listOrg(Integer pageNum, Integer pageSize, String orgName, Integer orgType, Long areaId, Integer status) {
        log.info("分页查询机构列表，pageNum={}, pageSize={}, orgName={}, orgType={}, areaId={}, status={}",
                pageNum, pageSize, orgName, orgType, areaId, status);
        try {
            // 构建分页对象
            Page<OrgEntity> page = new Page<>(pageNum, pageSize);
            // 构建查询条件
            LambdaQueryWrapper<OrgEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(orgName), OrgEntity::getOrgName, orgName)
                    .eq(Objects.nonNull(orgType), OrgEntity::getOrgType, orgType)
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
            String orgCode = generateOrgCode(dto.getOrgType());
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
        // 填充机构类型名称
        if (vo.getOrgType() != null) {
            vo.setOrgTypeName(getDictNameByTypeAndValue(DictCodeConstants.ORG_TYPE, vo.getOrgType()));
        }
        // 填充状态名称
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        // 填充医院等级名称
        if (vo.getHospitalLevel() != null) {
            vo.setHospitalLevelName(getDictNameByTypeAndValue(DictCodeConstants.HOSPITAL_LEVEL, vo.getHospitalLevel()));
        }
        // 填充医院类型名称
        if (vo.getHospitalType() != null) {
            vo.setHospitalTypeName(getDictNameByTypeAndValue(DictCodeConstants.HOSPITAL_TYPE, vo.getHospitalType()));
        }
        // 填充代理产品线名称
        if (StrUtil.isNotBlank(vo.getAgentProductLine())) {
            vo.setAgentProductLineNames(getDictNamesByTypeAndValues(DictCodeConstants.AGENT_PRODUCT_LINE, vo.getAgentProductLine()));
        }
        return vo;
    }

    /**
     * 根据字典类型和值获取字典名称
     *
     * @param dictCode  字典类型编码
     * @param dictValue 字典值
     * @return 字典名称
     */
    private String getDictNameByTypeAndValue(String dictCode, Integer dictValue) {
        List<DictVO> dictList = dictService.listByTypeCode(dictCode);
        return dictList.stream()
                .filter(d -> Objects.equals(d.getDictValue(), String.valueOf(dictValue)))
                .map(DictVO::getDictName)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据字典类型和值列表获取字典名称列表
     *
     * @param dictCode   字典类型编码
     * @param dictValues 字典值（逗号分隔）
     * @return 字典名称（逗号分隔）
     */
    private String getDictNamesByTypeAndValues(String dictCode, String dictValues) {
        List<DictVO> dictList = dictService.listByTypeCode(dictCode);
        List<String> valueList = List.of(dictValues.split(","));
        return dictList.stream()
                .filter(d -> valueList.contains(d.getDictValue()))
                .map(DictVO::getDictName)
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
     * @param orgType 机构类型
     * @return true-有效，false-无效
     */
    private boolean isOrgTypeValid(Integer orgType) {
        if (orgType == null) {
            return false;
        }
        List<DictVO> dictList = dictService.listByTypeCode(DictCodeConstants.ORG_TYPE);
        return dictList.stream()
                .anyMatch(d -> Objects.equals(d.getDictValue(), String.valueOf(orgType)));
    }

    /**
     * 生成机构编码
     * 编码规则：前缀 + 序号（3位）
     * - 生产企业 -> ORG-P-001
     * - 经销商 -> ORG-D-001
     * - 医疗机构 -> ORG-H-001
     * - 其他 -> ORG-O-001
     *
     * @param orgType 机构类型
     * @return 机构编码
     */
    private String generateOrgCode(Integer orgType) {
        // 获取机构类型对应的字典值
        String typeValue = String.valueOf(orgType);
        // 根据类型值确定前缀
        String prefix = switch (typeValue) {
            case "1" -> "ORG-P-";  // 生产企业
            case "2" -> "ORG-D-";  // 经销商
            case "3" -> "ORG-H-";  // 医疗机构
            default -> "ORG-O-";   // 其他
        };
        // 查询当前前缀下的最大序号
        LambdaQueryWrapper<OrgEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(OrgEntity::getOrgCode, prefix)
                .orderByDesc(OrgEntity::getOrgCode)
                .last("LIMIT 1");
        OrgEntity lastOrg = getOne(wrapper);
        int maxSeq = 0;
        if (lastOrg != null && StrUtil.isNotBlank(lastOrg.getOrgCode())) {
            String code = lastOrg.getOrgCode();
            String seqStr = code.replace(prefix, "");
            try {
                maxSeq = Integer.parseInt(seqStr);
            } catch (NumberFormatException e) {
                maxSeq = 0;
            }
        }
        // 生成新编码
        return prefix + String.format("%03d", maxSeq + 1);
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
