package com.yigongbao.module.basic.hospitalDept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.CodeRuleConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.hospitalDept.convert.HospitalDeptConvert;
import com.yigongbao.module.basic.hospitalDept.dto.CreateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.dto.UpdateHospitalDeptDTO;
import com.yigongbao.module.basic.hospitalDept.entity.HospitalDeptEntity;
import com.yigongbao.module.basic.hospitalDept.mapper.HospitalDeptMapper;
import com.yigongbao.module.basic.hospitalDept.service.HospitalDeptService;
import com.yigongbao.module.basic.hospitalDept.vo.HospitalDeptVO;
import com.yigongbao.module.basic.code.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 医院科室 Service 实现类
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalDeptServiceImpl extends ServiceImpl<HospitalDeptMapper, HospitalDeptEntity> implements HospitalDeptService {

    private final CodeGeneratorService codeGeneratorService;

    /**
     * 分页查询科室列表
     */
    @Override
    public IPage<HospitalDeptVO> listDepts(Integer pageNum, Integer pageSize, String hospitalDeptName, Integer status) {
        log.info("分页查询科室列表，pageNum={}, pageSize={}, hospitalDeptName={}, status={}",
                pageNum, pageSize, hospitalDeptName, status);
        try {
            Page<HospitalDeptEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<HospitalDeptEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Objects.nonNull(hospitalDeptName) && !hospitalDeptName.isEmpty(),
                            HospitalDeptEntity::getHospitalDeptName, hospitalDeptName)
                    .eq(Objects.nonNull(status), HospitalDeptEntity::getStatus, status)
                    .orderByAsc(HospitalDeptEntity::getSort)
                    .orderByDesc(HospitalDeptEntity::getCreateTime);

            IPage<HospitalDeptEntity> pageResult = page(page, wrapper);

            // 转换为 VO
            IPage<HospitalDeptVO> voPage = pageResult.convert(entity -> {
                HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            });

            log.info("分页查询科室列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询科室列表异常", e);
            throw e;
        }
    }

    /**
     * 查询所有科室列表
     */
    @Override
    public List<HospitalDeptVO> listAll(String hospitalDeptName, Integer status) {
        log.info("查询所有科室列表，hospitalDeptName={}, status={}", hospitalDeptName, status);
        try {
            LambdaQueryWrapper<HospitalDeptEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Objects.nonNull(hospitalDeptName) && !hospitalDeptName.isEmpty(),
                            HospitalDeptEntity::getHospitalDeptName, hospitalDeptName)
                    .eq(Objects.nonNull(status), HospitalDeptEntity::getStatus, status)
                    .orderByAsc(HospitalDeptEntity::getSort);

            List<HospitalDeptEntity> list = list(wrapper);
            List<HospitalDeptVO> voList = list.stream().map(entity -> {
                HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
                fillExtraFields(vo, entity);
                return vo;
            }).toList();

            log.info("查询所有科室列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询所有科室列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询科室
     */
    @Override
    public HospitalDeptVO getById(Long id) {
        log.info("根据ID查询科室，id={}", id);
        try {
            HospitalDeptEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("科室不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
            }
            HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
            fillExtraFields(vo, entity);
            log.info("查询科室成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询科室异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateHospitalDeptDTO dto) {
        log.info("创建科室，hospitalDeptName={}", dto.getHospitalDeptName());
        try {
            // 校验科室名称是否已存在
            if (isNameExists(dto.getHospitalDeptName(), null)) {
                log.warn("科室名称已存在，hospitalDeptName={}", dto.getHospitalDeptName());
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_EXISTS);
            }

            HospitalDeptEntity entity = HospitalDeptConvert.toEntity(dto);
            // 生成科室编码
            String deptCode = codeGeneratorService.generate(CodeRuleConstants.HDEPT_NO);
            entity.setHospitalDeptCode(deptCode);
            // 设置默认值
            if (entity.getSort() == null) {
                entity.setSort(0);
            }
            if (entity.getStatus() == null) {
                entity.setStatus(StatusConstants.NORMAL);
            }

            save(entity);
            log.info("创建科室成功，id={}, hospitalDeptCode={}", entity.getId(), deptCode);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建科室异常，hospitalDeptName={}", dto.getHospitalDeptName(), e);
            throw e;
        }
    }

    /**
     * 更新科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateHospitalDeptDTO dto) {
        log.info("更新科室，id={}", id);
        try {
            HospitalDeptEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("科室不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
            }

            // 校验科室名称是否与其他科室重复
            if (dto.getHospitalDeptName() != null && !dto.getHospitalDeptName().isEmpty()
                    && !dto.getHospitalDeptName().equals(entity.getHospitalDeptName())) {
                if (isNameExists(dto.getHospitalDeptName(), id)) {
                    log.warn("科室名称已存在，hospitalDeptName={}", dto.getHospitalDeptName());
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_EXISTS);
                }
            }

            BeanUtils.copyProperties(dto, entity, "id", "hospitalDeptCode", "createTime", "updateTime", "createBy", "updateBy");
            updateById(entity);
            log.info("更新科室成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新科室异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 删除科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        log.info("删除科室，id={}", id);
        try {
            HospitalDeptEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("科室不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
            }
            removeById(id);
            log.info("删除科室成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除科室异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改科室状态，id={}, status={}", id, status);
        try {
            HospitalDeptEntity entity = super.getById(id);
            if (entity == null) {
                log.warn("科室不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改科室状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改科室状态异常，id={}, status={}", id, status, e);
            throw e;
        }
    }

    /**
     * 填充额外字段
     */
    private void fillExtraFields(HospitalDeptVO vo, HospitalDeptEntity entity) {
        if (entity.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(entity.getStatus()));
        }
    }

    /**
     * 校验科室名称是否存在
     */
    private boolean isNameExists(String hospitalDeptName, Long excludeId) {
        LambdaQueryWrapper<HospitalDeptEntity> wrapper = new LambdaQueryWrapper<HospitalDeptEntity>()
                .eq(HospitalDeptEntity::getHospitalDeptName, hospitalDeptName);
        if (excludeId != null) {
            wrapper.ne(HospitalDeptEntity::getId, excludeId);
        }
        return count(wrapper) > 0;
    }
}
