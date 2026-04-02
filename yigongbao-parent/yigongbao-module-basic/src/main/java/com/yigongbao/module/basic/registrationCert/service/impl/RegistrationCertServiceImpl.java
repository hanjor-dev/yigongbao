package com.yigongbao.module.basic.registrationCert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.registrationCert.convert.RegistrationCertConvert;
import com.yigongbao.module.basic.registrationCert.dto.CreateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.dto.RegistrationCertPageDTO;
import com.yigongbao.module.basic.registrationCert.dto.UpdateRegistrationCertDTO;
import com.yigongbao.module.basic.registrationCert.entity.RegistrationCertEntity;
import com.yigongbao.module.basic.registrationCert.mapper.RegistrationCertMapper;
import com.yigongbao.module.basic.registrationCert.service.RegistrationCertService;
import com.yigongbao.module.basic.registrationCert.vo.RegistrationCertVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

    /**
     * 注册证 Service 实现类
     * 处理注册证相关的业务逻辑，包括注册证CRUD、有效期管理等
     *
     * @author hanjor
     * @date 2026-03-24
     */
    @Service
    @Slf4j
    public class RegistrationCertServiceImpl extends ServiceImpl<RegistrationCertMapper, RegistrationCertEntity>
            implements RegistrationCertService {

    /**
     * 分页查询注册证列表
     *
     * @param dto 分页查询参数
     * @return 分页后的注册证列表
     */
    @Override
    public IPage<RegistrationCertVO> listCerts(RegistrationCertPageDTO dto) {
        log.info("分页查询注册证，dto={}", dto);
        try {
            int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
            int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
            Page<RegistrationCertEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<RegistrationCertEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StringUtils.hasText(dto.getCertCode()), RegistrationCertEntity::getCertCode, dto.getCertCode())
                    .like(StringUtils.hasText(dto.getCertName()), RegistrationCertEntity::getCertName, dto.getCertName())
                    .eq(Objects.nonNull(dto.getStatus()), RegistrationCertEntity::getStatus, dto.getStatus())
                    .orderByDesc(RegistrationCertEntity::getCreateTime);
            IPage<RegistrationCertVO> result = page(page, wrapper).convert(entity -> {
                RegistrationCertVO vo = RegistrationCertConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            });
            log.info("分页查询注册证成功，共{}条", result.getRecords().size());
            return result;
        } catch (Exception e) {
            log.error("分页查询注册证异常，dto={}", dto, e);
            throw e;
        }
    }

    /**
     * 查询有效注册证列表
     *
     * @return 有效注册证列表
     */
    @Override
    public List<RegistrationCertVO> listValidCerts() {
        log.info("查询有效注册证列表");
        try {
            LambdaQueryWrapper<RegistrationCertEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RegistrationCertEntity::getStatus, StatusConstants.NORMAL)
                    .orderByDesc(RegistrationCertEntity::getCreateTime);
            List<RegistrationCertVO> result = list(wrapper).stream().map(entity -> {
                RegistrationCertVO vo = RegistrationCertConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            }).toList();
            log.info("查询有效注册证列表成功，共{}条", result.size());
            return result;
        } catch (Exception e) {
            log.error("查询有效注册证列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询注册证详情
     *
     * @param id 注册证ID
     * @return 注册证详情
     * @throws BusinessException 注册证不存在
     */
    @Override
    public RegistrationCertVO getById(Long id) {
        log.info("根据ID查询注册证详情，id={}", id);
        try {
            RegistrationCertEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("注册证不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND);
            }
            RegistrationCertVO vo = RegistrationCertConvert.toVO(entity);
            fillExtraFields(vo, entity);
            log.info("查询注册证详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询注册证详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建注册证
     *
     * @param dto 创建参数
     * @throws BusinessException 注册证号已存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateRegistrationCertDTO dto) {
        log.info("创建注册证，certCode={}", dto.getCertCode());
        try {
            if (isCertCodeExists(dto.getCertCode(), null)) {
                log.warn("注册证号已存在，certCode={}", dto.getCertCode());
                throw new BusinessException(ErrorCodeEnum.CERT_EXISTS);
            }
            RegistrationCertEntity entity = RegistrationCertConvert.toEntity(dto);
            if (entity.getStatus() == null) {
                entity.setStatus(isExpired(entity.getValidTo()) ? StatusConstants.DISABLED : StatusConstants.NORMAL);
                log.info("注册证有效期已过，创建时自动设为禁用状态，certCode={}", dto.getCertCode());
            }
            save(entity);
            log.info("创建注册证成功，id={}, certCode={}", entity.getId(), dto.getCertCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建注册证异常，certCode={}", dto.getCertCode(), e);
            throw e;
        }
    }

    /**
     * 更新注册证信息
     *
     * @param id 注册证ID
     * @param dto 更新参数
     * @throws BusinessException 注册证不存在或注册证号已存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateRegistrationCertDTO dto) {
        log.info("更新注册证，id={}", id);
        try {
            RegistrationCertEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("注册证不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND);
            }
            if (StringUtils.hasText(dto.getCertCode()) && !dto.getCertCode().equals(entity.getCertCode())
                    && isCertCodeExists(dto.getCertCode(), id)) {
                log.warn("注册证号已存在，certCode={}", dto.getCertCode());
                throw new BusinessException(ErrorCodeEnum.CERT_EXISTS);
            }
            BeanUtils.copyProperties(dto, entity, "id", "createTime", "updateTime", "createBy", "updateBy");
            if (entity.getStatus() == null) {
                entity.setStatus(isExpired(entity.getValidTo()) ? StatusConstants.DISABLED : StatusConstants.NORMAL);
                log.info("注册证有效期已过，更新时自动设为禁用状态，id={}", id);
            }
            updateById(entity);
            log.info("更新注册证成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新注册证异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除注册证
     *
     * @param id 注册证ID
     * @throws BusinessException 注册证不存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        log.info("删除注册证，id={}", id);
        try {
            RegistrationCertEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("注册证不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.CERT_NOT_FOUND);
            }
            removeById(id);
            log.info("删除注册证成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除注册证异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 刷新过期注册证状态（定时任务调用）
     * 将所有已过期的注册证自动设为禁用状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshExpiredStatus() {
        log.info("刷新过期注册证状态任务开始");
        try {
            List<RegistrationCertEntity> allCerts = baseMapper.selectList(Wrappers.query());
            if (allCerts.isEmpty()) {
                log.info("刷新过期注册证状态任务完成，无注册证记录");
                return;
            }
            int updatedCount = 0;
            for (RegistrationCertEntity entity : allCerts) {
                Integer targetStatus = isExpired(entity.getValidTo()) ? StatusConstants.DISABLED : StatusConstants.NORMAL;
                if (!Objects.equals(entity.getStatus(), targetStatus)) {
                    entity.setStatus(targetStatus);
                    updateById(entity);
                    updatedCount++;
                }
            }
            log.info("刷新过期注册证状态任务完成，共更新{}条（总记录数{}）", updatedCount, allCerts.size());
        } catch (Exception e) {
            log.error("刷新过期注册证状态任务异常", e);
            throw e;
        }
    }

    /**
     * 补充注册证额外字段
     *
     * @param vo 注册证VO
     * @param entity 注册证实体
     */
    private void fillExtraFields(RegistrationCertVO vo, RegistrationCertEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(entity.getStatus().equals(StatusConstants.NORMAL) ? "有效" : "过期");
        }
    }

    private boolean isCertCodeExists(String certCode, Long excludeId) {
        LambdaQueryWrapper<RegistrationCertEntity> wrapper = new LambdaQueryWrapper<RegistrationCertEntity>()
                .eq(RegistrationCertEntity::getCertCode, certCode);
        if (excludeId != null) {
            wrapper.ne(RegistrationCertEntity::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private boolean isExpired(LocalDate validTo) {
        return validTo != null && validTo.isBefore(LocalDate.now());
    }

    @Override
    public List<RegistrationCertVO> listVOByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return super.listByIds(ids).stream().map(entity -> {
            RegistrationCertVO vo = RegistrationCertConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        }).toList();
    }
}
