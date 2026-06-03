package com.yigongbao.module.order.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yigongbao.common.constants.StatusConstants;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.common.mapper.OrderMainMapper;
import com.yigongbao.module.order.convert.ClassicCaseConvert;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.service.IClassicCaseFileService;
import com.yigongbao.module.order.service.IOrderClassicCaseService;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单经典案例服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderClassicCaseServiceImpl implements IOrderClassicCaseService {

    private final OrderMainMapper orderMainMapper;
    private final IClassicCaseFileService classicCaseFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsClassicCase(MarkClassicCaseDTO dto) {
        OrderMainEntity order = orderMainMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }

        if (!Integer.valueOf(80).equals(order.getPhase())) {
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ORDER_NOT_COMPLETED);
        }

        if (StatusConstants.YES.equals(order.getIsClassicCase())) {
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_ALREADY_MARKED);
        }

        order.setIsClassicCase(StatusConstants.YES);
        order.setClassicCaseTime(LocalDateTime.now());
        order.setClassicCaseBy(StpUtil.getLoginIdAsLong());
        order.setClassicCaseRemark(dto.getRemark());
        orderMainMapper.updateById(order);

        try {
            classicCaseFileService.migrateFilesToClassicCase(order.getId(), order.getOrderCode());
        } catch (Exception e) {
            log.error("文件迁移失败: orderId={}, orderCode={}", order.getId(), order.getOrderCode(), e);
            throw new BusinessException(ErrorCodeEnum.CLASSIC_CASE_FILE_MIGRATE_FAILED);
        }

        log.info("标记订单为经典案例: orderId={}, orderCode={}, operator={}, remark={}",
                order.getId(), order.getOrderCode(), order.getClassicCaseBy(), dto.getRemark());
    }

    @Override
    public IPage<ClassicCaseVO> listClassicCases(ClassicCaseQueryDTO dto) {
        Page<OrderMainEntity> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderMainEntity::getIsClassicCase, StatusConstants.YES);

        if (StrUtil.isNotBlank(dto.getOrderCode())) {
            wrapper.like(OrderMainEntity::getOrderCode, dto.getOrderCode());
        }
        if (StrUtil.isNotBlank(dto.getPatientName())) {
            wrapper.like(OrderMainEntity::getPatientName, dto.getPatientName());
        }
        if (dto.getHospitalId() != null) {
            wrapper.eq(OrderMainEntity::getHospitalId, dto.getHospitalId());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(OrderMainEntity::getClassicCaseTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(OrderMainEntity::getClassicCaseTime, dto.getEndTime());
        }

        wrapper.orderByDesc(OrderMainEntity::getClassicCaseTime);

        IPage<OrderMainEntity> entityPage = orderMainMapper.selectPage(page, wrapper);
        return entityPage.convert(ClassicCaseConvert::toVO);
    }

    @Override
    public ClassicCaseVO getClassicCaseDetail(Long orderId) {
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        if (order == null || !StatusConstants.YES.equals(order.getIsClassicCase())) {
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND);
        }
        return ClassicCaseConvert.toVO(order);
    }

    @Override
    public boolean isClassicCase(Long orderId) {
        OrderMainEntity order = orderMainMapper.selectById(orderId);
        return order != null && StatusConstants.YES.equals(order.getIsClassicCase());
    }
}
