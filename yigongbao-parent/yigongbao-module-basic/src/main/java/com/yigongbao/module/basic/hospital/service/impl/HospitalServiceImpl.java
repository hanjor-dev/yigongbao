package com.yigongbao.module.basic.hospital.service.impl;

/**
 * 医院 Service 实现类
 * 处理医院相关的业务逻辑，包括医院CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-19
 */

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.area.entity.AreaEntity;
import com.yigongbao.module.basic.area.service.AreaService;
import com.yigongbao.module.basic.hospital.convert.HospitalConvert;
import com.yigongbao.module.basic.hospital.dto.CreateHospitalDTO;
import com.yigongbao.module.basic.hospital.dto.UpdateHospitalDTO;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.service.HospitalService;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
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
 * 医院 Service 实现类
 * 处理医院相关的业务逻辑，包括医院CRUD、状态管理等
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalServiceImpl extends ServiceImpl<HospitalMapper, HospitalEntity> implements HospitalService {

    private final AreaService areaService;

    /**
     * 分页查询医院列表
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param hospitalName 医院名称（模糊查询）
     * @param areaId 地区ID
     * @param hospitalLevel 医院等级
     * @param hospitalType 医院类型
     * @param status 状态
     * @return 分页后的医院列表
     */
    @Override
    public IPage<HospitalVO> listHospital(Integer pageNum, Integer pageSize, String hospitalName,
                                            Long areaId, Integer hospitalLevel, Integer hospitalType, Integer status) {
        log.info("分页查询医院列表，pageNum={}, pageSize={}, hospitalName={}, areaId={}, hospitalLevel={}, hospitalType={}, status={}",
                pageNum, pageSize, hospitalName, areaId, hospitalLevel, hospitalType, status);
        try {
            Page<HospitalEntity> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<HospitalEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(StrUtil.isNotBlank(hospitalName), HospitalEntity::getHospitalName, hospitalName)
                    .eq(Objects.nonNull(areaId), HospitalEntity::getAreaId, areaId)
                    .eq(Objects.nonNull(hospitalLevel), HospitalEntity::getHospitalLevel, hospitalLevel)
                    .eq(Objects.nonNull(hospitalType), HospitalEntity::getHospitalType, hospitalType)
                    .eq(Objects.nonNull(status), HospitalEntity::getStatus, status)
                    .orderByDesc(HospitalEntity::getCreateTime);
            IPage<HospitalEntity> pageResult = page(page, wrapper);
            IPage<HospitalVO> voPage = pageResult.convert(this::toVO);
            log.info("分页查询医院列表成功，总数={}", pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("分页查询医院列表异常", e);
            throw e;
        }
    }

    /**
     * 根据ID查询医院详情
     *
     * @param id 医院ID
     * @return 医院详情
     * @throws BusinessException 医院不存在
     */
    @Override
    public HospitalVO getHospitalById(Long id) {
        log.info("根据ID查询医院详情，id={}", id);
        try {
            HospitalEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            HospitalVO vo = toVO(entity);
            log.info("查询医院详情成功，id={}", id);
            return vo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询医院详情异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 创建医院
     *
     * @param dto 创建参数
     * @throws BusinessException 医院名称已存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createHospital(CreateHospitalDTO dto) {
        log.info("创建医院，hospitalName={}", dto.getHospitalName());
        try {
            if (isHospitalNameExists(dto.getHospitalName())) {
                log.warn("医院名称已存在，hospitalName={}", dto.getHospitalName());
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_EXISTS);
            }
            HospitalEntity entity = HospitalConvert.toEntity(dto);
            entity.setHospitalCode(generateHospitalCode());
            entity.setStatus(StatusConstants.NORMAL);
            fillAreaInfo(entity);
            save(entity);
            log.info("创建医院成功，id={}, hospitalCode={}", entity.getId(), entity.getHospitalCode());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建医院异常，hospitalName={}", dto.getHospitalName(), e);
            throw e;
        }
    }

    /**
     * 更新医院
     *
     * @param id 医院ID
     * @param dto 更新参数
     * @throws BusinessException 医院不存在或名称已存在
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHospital(Long id, UpdateHospitalDTO dto) {
        log.info("更新医院，id={}", id);
        try {
            HospitalEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            if (StrUtil.isNotBlank(dto.getHospitalName()) && !dto.getHospitalName().equals(entity.getHospitalName())) {
                if (isHospitalNameExistsExcludingId(dto.getHospitalName(), id)) {
                    log.warn("医院名称已存在，hospitalName={}", dto.getHospitalName());
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_EXISTS);
                }
            }
            BeanUtils.copyProperties(dto, entity, "id", "hospitalCode", "createTime", "updateTime", "createBy", "updateBy");
            if (dto.getAreaId() != null && !dto.getAreaId().equals(entity.getAreaId())) {
                fillAreaInfo(entity);
            }
            updateById(entity);
            log.info("更新医院成功，id={}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新医院异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 修改医院状态
     *
     * @param id 医院ID
     * @param status 状态（0=禁用，1=正常）
     * @throws BusinessException 医院不存在或状态值不合法
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        log.info("修改医院状态，id={}, status={}", id, status);
        try {
            if (status == null || (status != StatusConstants.DISABLED && status != StatusConstants.NORMAL)) {
                log.warn("状态值不合法，status={}", status);
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
            }
            HospitalEntity entity = getById(id);
            if (entity == null) {
                log.warn("医院不存在，id={}", id);
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            entity.setStatus(status);
            updateById(entity);
            log.info("修改医院状态成功，id={}, status={}", id, status);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("修改医院状态异常，id={}", id, e);
            throw e;
        }
    }

    /**
     * 获取医院下拉选项
     *
     * @param status 状态筛选（可选）
     * @return 医院下拉列表
     */
    @Override
    public List<HospitalVO> listOptions(Integer status) {
        log.info("获取医院下拉选项，status={}", status);
        try {
            LambdaQueryWrapper<HospitalEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Objects.nonNull(status), HospitalEntity::getStatus, status)
                    .orderByAsc(HospitalEntity::getHospitalName);
            List<HospitalEntity> list = list(wrapper);
            List<HospitalVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
            log.info("获取医院下拉选项成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("获取医院下拉选项异常", e);
            throw e;
        }
    }

    /**
     * 获取当前用户可操作的医院下拉选项（根据用户权限过滤）
     * 用于业务员创建订单等业务场景时的医院选择
     *
     * @param userId 用户ID
     * @return 当前用户可操作的医院列表
     */
    @Override
    public List<HospitalVO> listMyOptions(Long userId) {
        log.info("获取当前用户可操作的医院下拉选项，userId={}", userId);
        try {
            // 此方法由 HospitalScopeController 调用，实际逻辑在 Controller 层根据用户权限判断
            // 这里默认返回所有正常状态的医院，Controller 层会根据角色的 hospitalScopeEnabled 进行过滤
            LambdaQueryWrapper<HospitalEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HospitalEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(HospitalEntity::getHospitalName);
            List<HospitalEntity> list = list(wrapper);
            List<HospitalVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
            log.info("获取当前用户可操作的医院下拉选项成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("获取当前用户可操作的医院下拉选项异常，userId={}", userId, e);
            throw e;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 转换为VO
     *
     * @param entity 医院实体
     * @return 医院VO
     */
    private HospitalVO toVO(HospitalEntity entity) {
        if (entity == null) {
            return null;
        }
        HospitalVO vo = HospitalConvert.toVO(entity);
        if (vo.getStatus() != null) {
            vo.setStatusName(StatusConstants.getStatusName(vo.getStatus()));
        }
        return vo;
    }

    /**
     * 填充地区信息
     *
     * @param entity 医院实体
     */
    private void fillAreaInfo(HospitalEntity entity) {
        if (entity.getAreaId() == null) {
            return;
        }
        AreaEntity area = areaService.getById(entity.getAreaId());
        if (area != null) {
            entity.setAreaName(area.getName());
            entity.setFullAreaName(area.getMergerName());
        } else {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "地区信息无效，请检查地区ID");
        }
    }

    /**
     * 校验医院名称是否存在
     *
     * @param hospitalName 医院名称
     * @return true-存在，false-不存在
     */
    private boolean isHospitalNameExists(String hospitalName) {
        return count(new LambdaQueryWrapper<HospitalEntity>()
                .eq(HospitalEntity::getHospitalName, hospitalName)) > 0;
    }

    /**
     * 校验医院名称是否存在（排除指定ID）
     *
     * @param hospitalName 医院名称
     * @param excludeId 排除的医院ID
     * @return true-存在，false-不存在
     */
    private boolean isHospitalNameExistsExcludingId(String hospitalName, Long excludeId) {
        return count(new LambdaQueryWrapper<HospitalEntity>()
                .eq(HospitalEntity::getHospitalName, hospitalName)
                .ne(HospitalEntity::getId, excludeId)) > 0;
    }

    /**
     * 生成医院编码
     *
     * @return 医院编码（格式：HOS-XXX）
     */
    private String generateHospitalCode() {
        LambdaQueryWrapper<HospitalEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(HospitalEntity::getHospitalCode, "HOS-")
                .orderByDesc(HospitalEntity::getHospitalCode)
                .last("LIMIT 1");
        HospitalEntity lastEntity = getOne(wrapper);
        int nextSeq = 1;
        if (lastEntity != null && StrUtil.isNotBlank(lastEntity.getHospitalCode())) {
            String lastCode = lastEntity.getHospitalCode();
            String seqStr = lastCode.substring("HOS-".length());
            try {
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }
        return String.format("HOS-%03d", nextSeq);
    }
}
