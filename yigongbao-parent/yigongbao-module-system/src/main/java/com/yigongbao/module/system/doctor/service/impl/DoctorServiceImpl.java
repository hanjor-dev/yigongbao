package com.yigongbao.module.system.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.doctor.convert.DoctorConvert;
import com.yigongbao.module.system.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.system.doctor.dto.DoctorListDTO;
import com.yigongbao.module.system.doctor.dto.DoctorPageDTO;
import com.yigongbao.module.system.doctor.dto.DoctorSuggestDTO;
import com.yigongbao.module.system.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.system.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.system.doctor.entity.DoctorEntity;
import com.yigongbao.module.system.doctor.mapper.DoctorMapper;
import com.yigongbao.module.system.doctor.service.DoctorService;
import com.yigongbao.module.system.doctor.vo.DoctorVO;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 医生 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, DoctorEntity> implements DoctorService {

    private final DoctorMapper doctorMapper;
    private final OrgService orgService;

    /**
     * 分页查询医生列表
     */
    @Override
    public IPage<DoctorVO> listDoctors(DoctorPageDTO dto) {
        try {
            int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
            int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
            Page<DoctorEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(dto.getDoctorName()), DoctorEntity::getDoctorName, dto.getDoctorName())
                    .eq(Objects.nonNull(dto.getHospitalId()), DoctorEntity::getHospitalId, dto.getHospitalId())
                    .eq(Objects.nonNull(dto.getStatus()), DoctorEntity::getStatus, dto.getStatus())
                    .orderByDesc(DoctorEntity::getCreateTime);

            IPage<DoctorEntity> pageResult = page(page, wrapper);

            // 批量填充医院名称，避免 N+1 查询
            List<DoctorEntity> records = pageResult.getRecords();
            List<DoctorVO> voList = records.stream().map(DoctorConvert::toVO).collect(Collectors.toList());
            fillExtraFieldsBatch(voList, records);
            Page<DoctorVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            voPage.setRecords(voList);

            return voPage;
        } catch (Exception e) {
            log.error("分页查询医生列表异常", e);
            throw e;
        }
    }

    /**
     * 查询所有医生列表
     */
    @Override
    public List<DoctorVO> listAll(DoctorListDTO dto) {
        try {
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(dto.getDoctorName()), DoctorEntity::getDoctorName, dto.getDoctorName())
                    .eq(Objects.nonNull(dto.getHospitalId()), DoctorEntity::getHospitalId, dto.getHospitalId())
                    .eq(Objects.nonNull(dto.getStatus()), DoctorEntity::getStatus, dto.getStatus())
                    .orderByDesc(DoctorEntity::getCreateTime);

            List<DoctorEntity> list = list(wrapper);
            List<DoctorVO> voList = list.stream().map(DoctorConvert::toVO).collect(Collectors.toList());
            // 批量填充医院名称，避免 N+1 查询
            fillExtraFieldsBatch(voList, list);

            return voList;
        } catch (Exception e) {
            log.error("查询所有医生列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询医生
     */
    @Override
    public DoctorVO getById(Long id) {
        try {
            DoctorEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("医生不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            DoctorVO vo = DoctorConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        } catch (Exception e) {
            log.error("查询医生异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateDoctorDTO dto) {
        log.info("创建医生，doctorName={}, hospitalId={}", dto.getDoctorName(), dto.getHospitalId());
        try {
            // 校验医疗机构是否存在且类型正确
            if (dto.getHospitalId() != null) {
                OrgEntity org = orgService.getById(dto.getHospitalId());
                if (org == null || !DictCodeConstants.ORG_TYPE_HOSPITAL.equals(org.getOrgType())) {
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
                }
            }

            DoctorEntity entity = DoctorConvert.toEntity(dto);
            // 从 Sa-Token 会话获取当前登录用户ID
            Long creatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
            entity.setCreatorId(creatorId);
            if (entity.getStatus() == null) {
                entity.setStatus(StatusConstants.NORMAL);
            }
            if (entity.getOrderCount() == null) {
                entity.setOrderCount(0);
            }

            save(entity);
            log.info("创建医生: id={}, doctorName={}", entity.getId(), dto.getDoctorName());
        } catch (Exception e) {
            log.error("创建医生异常，doctorName={}", dto.getDoctorName(), e);
            throw e;
        }
    }

    /**
     * 更新医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateDoctorDTO dto) {
        log.info("更新医生，id={}", id);
        try {
            DoctorEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("医生不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }

            BeanUtils.copyProperties(dto, entity, "id", "hospitalId", "creatorId", "orderCount",
                    "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新医生: id={}", id);
        } catch (Exception e) {
            log.error("更新医生异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        log.info("删除医生，id={}", id);
        try {
            DoctorEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("医生不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            removeById(id);
            log.info("删除医生: id={}", id);
        } catch (Exception e) {
            log.error("删除医生异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 查询业务员在医院下的历史医生列表
     */
    @Override
    public List<DoctorVO> listByCreatorAndHospital(DoctorSuggestDTO dto) {
        try {
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(dto.getCreatorId() != null, DoctorEntity::getCreatorId, dto.getCreatorId());
            wrapper.eq(dto.getHospitalId() != null, DoctorEntity::getHospitalId, dto.getHospitalId());
            wrapper.like(StrUtil.isNotBlank(dto.getKeyword()), DoctorEntity::getDoctorName, dto.getKeyword());
            wrapper.orderByDesc(DoctorEntity::getCreateTime);
            List<DoctorEntity> list = baseMapper.selectList(wrapper);
            List<DoctorVO> voList = list.stream().map(DoctorConvert::toVO).collect(Collectors.toList());
            // 批量填充医院名称，避免 N+1 查询
            fillExtraFieldsBatch(voList, list);
            return voList;
        } catch (Exception e) {
            log.error("查询历史医生列表异常，dto={}", dto, e);
            throw e;
        }
    }

    /**
     * 快速添加医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorVO quickAdd(QuickAddDoctorDTO dto) {
        log.info("快速添加医生，doctorName={}, hospitalId={}", dto.getDoctorName(), dto.getHospitalId());
        try {
            // 校验医疗机构是否存在且类型正确
            if (dto.getHospitalId() != null) {
                OrgEntity org = orgService.getById(dto.getHospitalId());
                if (org == null || !DictCodeConstants.ORG_TYPE_HOSPITAL.equals(org.getOrgType())) {
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
                }
            }

            // 从 Sa-Token 会话获取当前登录用户ID
            Long creatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
            // 查询是否已存在同名医生（在同一医院内，未删除）
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DoctorEntity::getDoctorName, dto.getDoctorName())
                    .eq(DoctorEntity::getHospitalId, dto.getHospitalId());
            DoctorEntity existing = getOne(wrapper);
            if (existing != null) {
                log.info("医生已存在，返回现有医生，id={}", existing.getId());
                DoctorVO vo = DoctorConvert.toVO(existing);
                fillExtraFields(vo, existing);
                return vo;
            }

            // 检查是否存在已删除的同名医生记录，如有则物理删除，避免历史垃圾数据残留
            DoctorEntity deletedDoctor = doctorMapper.selectDeletedByHospitalAndName(
                    dto.getHospitalId(), dto.getDoctorName());
            if (deletedDoctor != null) {
                log.info("发现已删除的医生记录，物理删除后重新创建，deletedId={}", deletedDoctor.getId());
                doctorMapper.physicallyDeleteDeletedById(deletedDoctor.getId());
            }

            // 创建新医生
            DoctorEntity entity = new DoctorEntity();
            entity.setDoctorName(dto.getDoctorName());
            entity.setDoctorPhone(dto.getDoctorPhone());
            entity.setHospitalId(dto.getHospitalId());
            entity.setCreatorId(creatorId);
            entity.setStatus(StatusConstants.NORMAL);
            entity.setOrderCount(0);

            save(entity);
            log.info("快速添加医生: id={}", entity.getId());

            DoctorVO vo = DoctorConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        } catch (Exception e) {
            log.error("快速添加医生异常，doctorName={}", dto.getDoctorName(), e);
            throw e;
        }
    }

    /**
     * 修改状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改医生状态，id={}, status={}", id, status);
        try {
            DoctorEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("医生不存在: id={}", id);
                throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改医生状态: id={}, status={}", id, status);
        } catch (Exception e) {
            log.error("修改医生状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 填充额外字段（单条场景）
     */
    private void fillExtraFields(DoctorVO vo, DoctorEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        }
        if (entity.getHospitalId() != null) {
            try {
                OrgEntity org = orgService.getById(entity.getHospitalId());
                if (org != null) {
                    vo.setHospitalName(org.getOrgName());
                }
            } catch (Exception e) {
                log.debug("获取医疗机构信息失败，hospitalId={}", entity.getHospitalId());
            }
        }
    }

    /**
     * 批量填充额外字段（列表场景，避免 N+1 查询）
     */
    private void fillExtraFieldsBatch(List<DoctorVO> voList, List<DoctorEntity> entities) {
        if (voList == null || voList.isEmpty()) {
            return;
        }
        // 收集所有需要查询的医疗机构ID
        Set<Long> hospitalIds = entities.stream()
                .map(DoctorEntity::getHospitalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询医疗机构（1次 IN 查询）
        Map<Long, String> orgNameMap = hospitalIds.isEmpty() ? Collections.emptyMap() :
                orgService.listByIds(hospitalIds).stream()
                        .collect(Collectors.toMap(OrgEntity::getId, OrgEntity::getOrgName));

        // 批量填充 VO
        for (int i = 0; i < voList.size(); i++) {
            DoctorVO vo = voList.get(i);
            DoctorEntity entity = entities.get(i);
            if (entity.getStatus() != null) {
                vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
            }
            if (entity.getHospitalId() != null) {
                vo.setHospitalName(orgNameMap.get(entity.getHospitalId()));
            }
        }
    }

    @Override
    public List<DoctorVO> listByHospitalId(Long hospitalId) {
        List<DoctorEntity> entities = lambdaQuery()
                .eq(DoctorEntity::getHospitalId, hospitalId)
                .list();
        List<DoctorVO> voList = entities.stream().map(DoctorConvert::toVO).collect(Collectors.toList());
        fillExtraFieldsBatch(voList, entities);
        return voList;
    }
}
