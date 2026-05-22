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
import com.yigongbao.module.basic.hospitalDept.dto.HospitalDeptListDTO;
import com.yigongbao.module.basic.hospitalDept.dto.HospitalDeptPageDTO;
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
    public IPage<HospitalDeptVO> listDepts(HospitalDeptPageDTO dto) {
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        Page<HospitalDeptEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HospitalDeptEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Objects.nonNull(dto.getHospitalDeptName()) && !dto.getHospitalDeptName().isEmpty(),
                        HospitalDeptEntity::getHospitalDeptName, dto.getHospitalDeptName())
                .eq(Objects.nonNull(dto.getStatus()), HospitalDeptEntity::getStatus, dto.getStatus())
                .orderByAsc(HospitalDeptEntity::getSort)
                .orderByDesc(HospitalDeptEntity::getCreateTime);

        IPage<HospitalDeptEntity> pageResult = page(page, wrapper);

        return pageResult.convert(entity -> {
            HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        });
    }

    /**
     * 查询所有科室列表
     */
    @Override
    public List<HospitalDeptVO> listAll(HospitalDeptListDTO dto) {
        LambdaQueryWrapper<HospitalDeptEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Objects.nonNull(dto.getHospitalDeptName()) && !dto.getHospitalDeptName().isEmpty(),
                        HospitalDeptEntity::getHospitalDeptName, dto.getHospitalDeptName())
                .eq(Objects.nonNull(dto.getStatus()), HospitalDeptEntity::getStatus, dto.getStatus())
                .orderByAsc(HospitalDeptEntity::getSort);

        List<HospitalDeptEntity> list = list(wrapper);
        return list.stream().map(entity -> {
            HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
            fillExtraFields(vo, entity);
            return vo;
        }).toList();
    }

    /**
     * 根据ID查询科室
     */
    @Override
    public HospitalDeptVO getById(Long id) {
        HospitalDeptEntity entity = super.getById(id);
        if (entity == null) {
            log.warn("科室不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
        }
        HospitalDeptVO vo = HospitalDeptConvert.toVO(entity);
        fillExtraFields(vo, entity);
        return vo;
    }

    /**
     * 创建科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateHospitalDeptDTO dto) {
        // 校验科室名称是否已存在
        if (isNameExists(dto.getHospitalDeptName(), null)) {
            log.warn("科室名称已存在: hospitalDeptName={}", dto.getHospitalDeptName());
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
        log.info("创建科室: id={}, hospitalDeptCode={}, hospitalDeptName={}", entity.getId(), deptCode, dto.getHospitalDeptName());
    }

    /**
     * 更新科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UpdateHospitalDeptDTO dto) {
        HospitalDeptEntity entity = super.getById(id);
        if (entity == null) {
            log.warn("科室不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
        }

        // 校验科室名称是否与其他科室重复
        if (dto.getHospitalDeptName() != null && !dto.getHospitalDeptName().isEmpty()
                && !dto.getHospitalDeptName().equals(entity.getHospitalDeptName())) {
            if (isNameExists(dto.getHospitalDeptName(), id)) {
                log.warn("科室名称已存在: hospitalDeptName={}", dto.getHospitalDeptName());
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_EXISTS);
            }
        }

        BeanUtils.copyProperties(dto, entity, "id", "hospitalDeptCode", "createTime", "updateTime", "createBy", "updateBy");
        updateById(entity);
        log.info("更新科室: id={}", id);
    }

    /**
     * 删除科室
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        HospitalDeptEntity entity = super.getById(id);
        if (entity == null) {
            log.warn("科室不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
        }
        removeById(id);
        log.info("删除科室: id={}", id);
    }

    /**
     * 修改状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        HospitalDeptEntity entity = super.getById(id);
        if (entity == null) {
            log.warn("科室不存在: id={}", id);
            throw new BusinessException(ErrorCodeEnum.HOSPITAL_DEPT_NOT_FOUND);
        }
        entity.setStatus(status);
        updateById(entity);
        log.info("修改科室状态: id={}, status={}", id, status);
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
