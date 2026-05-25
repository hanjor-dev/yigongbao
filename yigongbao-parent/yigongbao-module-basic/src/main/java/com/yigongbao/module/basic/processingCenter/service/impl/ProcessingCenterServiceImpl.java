package com.yigongbao.module.basic.processingCenter.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.module.basic.processingCenter.convert.ProcessingCenterConvert;
import com.yigongbao.module.basic.processingCenter.dto.CreateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.dto.ProcessingCenterPageDTO;
import com.yigongbao.module.basic.processingCenter.dto.UpdateProcessingCenterDTO;
import com.yigongbao.module.basic.processingCenter.entity.ProcessingCenterEntity;
import com.yigongbao.module.basic.processingCenter.mapper.ProcessingCenterMapper;
import com.yigongbao.module.basic.processingCenter.service.IProcessingCenterService;
import com.yigongbao.module.basic.processingCenter.vo.ProcessingCenterVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingCenterServiceImpl extends ServiceImpl<ProcessingCenterMapper, ProcessingCenterEntity>
        implements IProcessingCenterService {

    /**
     * 分页查询加工中心列表
     */
    @Override
    public IPage<ProcessingCenterVO> listProcessingCenters(ProcessingCenterPageDTO dto) {
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getCenterName()), ProcessingCenterEntity::getCenterName, dto.getCenterName())
               .eq(dto.getStatus() != null, ProcessingCenterEntity::getStatus, dto.getStatus())
               .orderByDesc(ProcessingCenterEntity::getCreateTime);

        IPage<ProcessingCenterEntity> page = page(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return page.convert(ProcessingCenterConvert::toVO);
    }

    /**
     * 根据ID查询加工中心
     */
    @Override
    public ProcessingCenterVO getProcessingCenterById(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return ProcessingCenterConvert.toVO(entity);
    }

    /**
     * 创建加工中心
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProcessingCenter(CreateProcessingCenterDTO dto) {
        // 检查编码是否重复
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getCenterCode, dto.getCenterCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCodeEnum.DATA_EXISTS);
        }

        ProcessingCenterEntity entity = ProcessingCenterConvert.toEntity(dto);
        entity.setStatus(StatusConstants.NORMAL);
        save(entity);

        log.info("创建加工中心: id={}, centerCode={}, centerName={}",
            entity.getId(), entity.getCenterCode(), entity.getCenterName());

        return entity.getId();
    }

    /**
     * 更新加工中心
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessingCenter(UpdateProcessingCenterDTO dto) {
        ProcessingCenterEntity entity = getById(dto.getId());
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        // 更新非空字段
        if (StrUtil.isNotBlank(dto.getCenterName())) {
            entity.setCenterName(dto.getCenterName());
        }
        if (StrUtil.isNotBlank(dto.getContactPerson())) {
            entity.setContactPerson(dto.getContactPerson());
        }
        if (StrUtil.isNotBlank(dto.getContactPhone())) {
            entity.setContactPhone(dto.getContactPhone());
        }
        if (StrUtil.isNotBlank(dto.getAddress())) {
            entity.setAddress(dto.getAddress());
        }
        if (StrUtil.isNotBlank(dto.getDeviceIdRanges())) {
            entity.setDeviceIdRanges(dto.getDeviceIdRanges());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getRemark())) {
            entity.setRemark(dto.getRemark());
        }

        updateById(entity);
        log.info("更新加工中心: id={}, centerCode={}", entity.getId(), entity.getCenterCode());
    }

    /**
     * 删除加工中心
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessingCenter(Long id) {
        ProcessingCenterEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        removeById(id);
        log.info("删除加工中心: id={}, centerCode={}", id, entity.getCenterCode());
    }

    /**
     * 查询所有启用的加工中心
     */
    @Override
    public List<ProcessingCenterVO> listAllEnabled() {
        LambdaQueryWrapper<ProcessingCenterEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessingCenterEntity::getStatus, StatusConstants.NORMAL)
               .orderByAsc(ProcessingCenterEntity::getCenterCode);

        return list(wrapper).stream()
                .map(ProcessingCenterConvert::toVO)
                .collect(Collectors.toList());
    }
}
