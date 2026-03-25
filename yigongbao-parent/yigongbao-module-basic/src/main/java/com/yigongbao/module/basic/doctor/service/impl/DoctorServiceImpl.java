package com.yigongbao.module.basic.doctor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.doctor.convert.DoctorConvert;
import com.yigongbao.module.basic.doctor.dto.CreateDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.QuickAddDoctorDTO;
import com.yigongbao.module.basic.doctor.dto.UpdateDoctorDTO;
import com.yigongbao.module.basic.doctor.entity.DoctorEntity;
import com.yigongbao.module.basic.doctor.mapper.DoctorMapper;
import com.yigongbao.module.basic.doctor.service.DoctorService;
import com.yigongbao.module.basic.doctor.vo.DoctorVO;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Objects;

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

    private final HospitalService hospitalService;
    private final HospitalDeptService hospitalDeptService;

    /**
     * 分页查询医生列表
     */
    @Override
    public IPage<DoctorVO> listDoctors(Integer pageNum, Integer pageSize, String doctorName,
            Long hospitalId, Long hospitalDeptId, Integer status) {
        log.info("分页查询医生列表，pageNum={}, pageSize={}, doctorName={}, hospitalId={}, status={}",
                pageNum, pageSize, doctorName, hospitalId, status);
        try {
            Page<DoctorEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(doctorName), DoctorEntity::getDoctorName, doctorName)
                    .eq(Objects.nonNull(hospitalId), DoctorEntity::getHospitalId, hospitalId)
                    .eq(Objects.nonNull(hospitalDeptId), DoctorEntity::getHospitalDeptId, hospitalDeptId)
                    .eq(Objects.nonNull(status), DoctorEntity::getStatus, status)
                    .orderByDesc(DoctorEntity::getCreateTime);

            IPage<DoctorEntity> pageResult = page(page, wrapper);

            // 转换为 VO
            IPage<DoctorVO> voPage = pageResult.convert(entity -> {
                DoctorVO vo = DoctorConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            });

            log.info("分页查询医生列表成功，总数={}", pageResult.getTotal());
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
    public List<DoctorVO> listAll(String doctorName, Long hospitalId, Integer status) {
        log.info("查询所有医生列表，doctorName={}, hospitalId={}, status={}", doctorName, hospitalId, status);
        try {
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(doctorName), DoctorEntity::getDoctorName, doctorName)
                    .eq(Objects.nonNull(hospitalId), DoctorEntity::getHospitalId, hospitalId)
                    .eq(Objects.nonNull(status), DoctorEntity::getStatus, status)
                    .orderByDesc(DoctorEntity::getCreateTime);

            List<DoctorEntity> list = list(wrapper);
            List<DoctorVO> voList = list.stream().map(entity -> {
                DoctorVO vo = DoctorConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            }).toList();

            log.info("查询所有医生列表成功，数量={}", voList.size());
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
        log.info("根据ID查询医生，id={}", id);
        try {
            DoctorEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("医生不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND);
            }
            DoctorVO vo = DoctorConvert.toVO(entity);
            fillExtraFields(vo, entity);
            log.info("查询医生成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
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
    public void create(CreateDoctorDTO dto, Long creatorId) {
        log.info("创建医生，doctorName={}, hospitalId={}", dto.getDoctorName(), dto.getHospitalId());
        try {
            // 校验医院是否存在
            if (dto.getHospitalId() != null) {
                hospitalService.getById(dto.getHospitalId());
            }
            // 校验科室是否存在
            if (dto.getHospitalDeptId() != null) {
                hospitalDeptService.getById(dto.getHospitalDeptId());
            }

            DoctorEntity entity = DoctorConvert.toEntity(dto);
            entity.setCreatorId(creatorId);
            if (entity.getStatus() == null) {
                entity.setStatus(StatusConstants.NORMAL);
            }
            if (entity.getOrderCount() == null) {
                entity.setOrderCount(0);
            }

            save(entity);
            log.info("创建医生成功，id={}, doctorName={}", entity.getId(), dto.getDoctorName());
        } catch (BusinessException e) {
            throw e;
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
                log.warn("医生不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND);
            }

            // 校验科室是否存在
            if (dto.getHospitalDeptId() != null && !dto.getHospitalDeptId().equals(entity.getHospitalDeptId())) {
                hospitalDeptService.getById(dto.getHospitalDeptId());
            }

            BeanUtils.copyProperties(dto, entity, "id", "hospitalId", "creatorId", "orderCount",
                    "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新医生成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
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
                log.warn("医生不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND);
            }
            removeById(id);
            log.info("删除医生成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除医生异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 查询业务员在医院下的历史医生列表
     */
    @Override
    public List<DoctorVO> listByCreatorAndHospital(Long creatorId, Long hospitalId, String keyword) {
        log.info("查询业务员在医院下的历史医生列表，creatorId={}, hospitalId={}, keyword={}",
                creatorId, hospitalId, keyword);
        try {
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(creatorId != null, DoctorEntity::getCreatorId, creatorId);
            wrapper.eq(hospitalId != null, DoctorEntity::getHospitalId, hospitalId);
            wrapper.like(StrUtil.isNotBlank(keyword), DoctorEntity::getDoctorName, keyword);
            wrapper.orderByDesc(DoctorEntity::getCreateTime);
            List<DoctorEntity> list = baseMapper.selectList(wrapper);
            List<DoctorVO> voList = list.stream().map(entity -> {
                DoctorVO vo = DoctorConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            }).toList();
            log.info("查询历史医生列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询历史医生列表异常，creatorId={}, hospitalId={}", creatorId, hospitalId, e);
            throw e;
        }
    }

    /**
     * 快速添加医生
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DoctorVO quickAdd(QuickAddDoctorDTO dto, Long creatorId) {
        log.info("快速添加医生，doctorName={}, hospitalId={}", dto.getDoctorName(), dto.getHospitalId());
        try {
            // 校验医院是否存在
            if (dto.getHospitalId() != null) {
                hospitalService.getById(dto.getHospitalId());
            }

            // 查询是否已存在同名医生
            LambdaQueryWrapper<DoctorEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DoctorEntity::getDoctorName, dto.getDoctorName())
                    .eq(DoctorEntity::getHospitalId, dto.getHospitalId())
                    .eq(DoctorEntity::getCreatorId, creatorId);
            DoctorEntity existing = getOne(wrapper);
            if (existing != null) {
                log.info("医生已存在，返回现有医生，id={}", existing.getId());
                DoctorVO vo = DoctorConvert.toVO(existing);
                fillExtraFields(vo, existing);
                return vo;
            }

            // 创建新医生
            DoctorEntity entity = new DoctorEntity();
            entity.setDoctorName(dto.getDoctorName());
            entity.setDoctorPhone(dto.getDoctorPhone());
            entity.setHospitalId(dto.getHospitalId());
            entity.setHospitalDeptId(dto.getHospitalDeptId());
            entity.setCreatorId(creatorId);
            entity.setStatus(StatusConstants.NORMAL);
            entity.setOrderCount(0);

            save(entity);
            log.info("快速添加医生成功，id={}", entity.getId());

            DoctorVO vo = DoctorConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        } catch (BusinessException e) {
            throw e;
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
                log.warn("医生不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.DOCTOR_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改医生状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改医生状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 填充额外字段
     */
    private void fillExtraFields(DoctorVO vo, DoctorEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        }
        if (entity.getHospitalId() != null) {
            try {
                var hospital = hospitalService.getById(entity.getHospitalId());
                if (hospital != null) {
                    vo.setHospitalName(hospital.getHospitalName());
                }
            } catch (Exception e) {
                log.debug("获取医院信息失败，hospitalId={}", entity.getHospitalId());
            }
        }
        if (entity.getHospitalDeptId() != null) {
            try {
                var dept = hospitalDeptService.getById(entity.getHospitalDeptId());
                if (dept != null) {
                    vo.setHospitalDeptName(dept.getHospitalDeptName());
                }
            } catch (Exception e) {
                log.debug("获取科室信息失败，hospitalDeptId={}", entity.getHospitalDeptId());
            }
        }
    }
}
