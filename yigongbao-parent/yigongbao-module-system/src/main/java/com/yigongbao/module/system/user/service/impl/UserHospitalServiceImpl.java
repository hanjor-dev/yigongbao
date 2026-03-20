package com.yigongbao.module.system.user.service.impl;

import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import com.yigongbao.module.system.user.mapper.UserHospitalMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户-医院关联 Service 实现类
 * 处理用户与医院之间的关联关系，用于数据范围权限控制
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserHospitalServiceImpl implements UserHospitalService {

    private final UserHospitalMapper userHospitalMapper;
    private final HospitalMapper hospitalMapper;

    /**
     * 查询用户的医院列表
     *
     * @param userId 用户ID
     * @return 医院列表
     */
    @Override
    public List<HospitalVO> getHospitalsByUserId(Long userId) {
        log.info("查询用户的医院列表，userId={}", userId);
        try {
            List<Long> hospitalIds = userHospitalMapper.selectHospitalIdsByUserId(userId);
            if (hospitalIds == null || hospitalIds.isEmpty()) {
                return new ArrayList<>();
            }
            List<HospitalEntity> hospitals = hospitalMapper.selectBatchIds(hospitalIds);
            List<HospitalVO> voList = hospitals.stream().map(this::toVO).collect(Collectors.toList());
            log.info("查询用户的医院列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询用户的医院列表异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 分配用户医院范围（覆盖式）
     *
     * @param userId 用户ID
     * @param hospitalIds 医院ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignHospitals(Long userId, List<Long> hospitalIds) {
        log.info("分配用户医院范围，userId={}, 医院数量={}", userId, hospitalIds != null ? hospitalIds.size() : 0);
        try {
            userHospitalMapper.deleteByUserId(userId);
            if (hospitalIds != null && !hospitalIds.isEmpty()) {
                for (Long hospitalId : hospitalIds) {
                    UserHospitalEntity entity = new UserHospitalEntity();
                    entity.setUserId(userId);
                    entity.setHospitalId(hospitalId);
                    userHospitalMapper.insert(entity);
                }
            }
            log.info("分配用户医院范围成功，userId={}", userId);
        } catch (Exception e) {
            log.error("分配用户医院范围异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取当前用户可操作医院（下拉选项）
     *
     * @param userId 用户ID
     * @return 医院列表
     */
    @Override
    public List<HospitalVO> getHospitalOptionsByUserId(Long userId) {
        return getHospitalsByUserId(userId);
    }

    /**
     * 实体转换为VO
     *
     * @param entity 医院实体
     * @return 医院VO
     */
    private HospitalVO toVO(HospitalEntity entity) {
        if (entity == null) {
            return null;
        }
        HospitalVO vo = new HospitalVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
