package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.mapper.OrgMapper;
import com.yigongbao.module.system.org.vo.OrgVO;
import com.yigongbao.module.system.role.entity.RoleEntity;
import com.yigongbao.module.system.role.service.RoleService;
import com.yigongbao.module.system.user.entity.UserEntity;
import com.yigongbao.module.system.user.entity.UserHospitalEntity;
import com.yigongbao.module.system.user.mapper.UserHospitalMapper;
import com.yigongbao.module.system.user.mapper.UserMapper;
import com.yigongbao.module.system.user.service.UserHospitalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户-医院关联 Service 实现类
 * hospital_id 语义已变更为 sys_org.id（医疗机构类型，orgType=1.3）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserHospitalServiceImpl implements UserHospitalService {

    private final UserHospitalMapper userHospitalMapper;
    private final OrgMapper orgMapper;
    private final UserMapper userMapper;
    private final RoleService roleService;

    @Override
    public List<Long> getHospitalIdsByUserId(Long userId) {
        List<Long> ids = userHospitalMapper.selectHospitalIdsByUserId(userId);
        return ids != null ? ids : new ArrayList<>();
    }

    @Override
    public Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        List<UserHospitalEntity> list = userHospitalMapper.selectList(
                new LambdaQueryWrapper<UserHospitalEntity>().in(UserHospitalEntity::getUserId, userIds));
        return list.stream().collect(Collectors.groupingBy(
                UserHospitalEntity::getUserId,
                Collectors.mapping(UserHospitalEntity::getHospitalId, Collectors.toList())));
    }

    @Override
    public List<OrgVO> getHospitalsByUserId(Long userId) {
        List<Long> ids = getHospitalIdsByUserId(userId);
        if (ids.isEmpty()) return new ArrayList<>();
        return orgMapper.selectBatchIds(ids).stream()
                .filter(Objects::nonNull)
                .map(this::toOrgVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignHospitals(Long userId, List<Long> hospitalIds) {
        log.info("分配用户医疗机构范围，userId={}, 数量={}", userId, hospitalIds != null ? hospitalIds.size() : 0);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);

        // 校验 hospitalIds 均为有效医疗机构（orgType=1.3）
        if (hospitalIds != null && !hospitalIds.isEmpty()) {
            List<OrgEntity> orgs = orgMapper.selectBatchIds(hospitalIds);
            List<OrgEntity> valid = orgs.stream().filter(Objects::nonNull)
                    .filter(o -> "1.3".equals(o.getOrgType())).collect(Collectors.toList());
            if (valid.size() != hospitalIds.size()) {
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            for (OrgEntity org : valid) {
                if (Integer.valueOf(StatusConstants.DISABLED).equals(org.getStatus())) {
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_DISABLED.getCode(),
                            "医疗机构【" + org.getOrgName() + "】已停用");
                }
            }
        }

        userHospitalMapper.deleteByUserId(userId);
        if (hospitalIds != null && !hospitalIds.isEmpty()) {
            for (Long hospitalId : hospitalIds) {
                UserHospitalEntity entity = new UserHospitalEntity();
                entity.setUserId(userId);
                entity.setHospitalId(hospitalId);
                userHospitalMapper.insert(entity);
            }
        }
        log.info("分配用户医疗机构范围成功，userId={}", userId);
    }

    @Override
    public List<OrgVO> getMyHospitalOptions(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        boolean isHospitalsScope = false;
        if (user.getRoleId() != null) {
            RoleEntity role = roleService.getById(user.getRoleId());
            if (role != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(role.getDataScopeType())) {
                isHospitalsScope = true;
            }
        }
        return isHospitalsScope ? getHospitalsByUserId(userId) : new ArrayList<>();
    }

    @Override
    public List<OrgVO> getHospitalOptionsByUserId(Long userId) {
        List<OrgEntity> list = orgMapper.selectList(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgType, "1.3")
                .eq(OrgEntity::getStatus, StatusConstants.NORMAL)
                .orderByAsc(OrgEntity::getOrgName));
        return list.stream().map(this::toOrgVO).collect(Collectors.toList());
    }

    @Override
    public DataScopeTypeEnum getDataScopeType(Long userId) {
        if (userId == null) return DataScopeTypeEnum.ORG;
        UserEntity user = userMapper.selectById(userId);
        if (user == null) return DataScopeTypeEnum.ORG;
        if (user.getRoleId() != null) {
            RoleEntity role = roleService.getById(user.getRoleId());
            if (role != null && role.getDataScopeType() != null) {
                return DataScopeTypeEnum.getByCodeOrDefault(role.getDataScopeType());
            }
        }
        return DataScopeTypeEnum.ORG;
    }

    @Override
    public boolean hasPermissionOnHospital(Long userId, Long hospitalId) {
        if (userId == null || hospitalId == null) return false;
        DataScopeTypeEnum scopeType = getDataScopeType(userId);
        if (scopeType == DataScopeTypeEnum.ALL || scopeType == DataScopeTypeEnum.ORG) return true;
        if (scopeType == DataScopeTypeEnum.HOSPITALS) {
            return getHospitalIdsByUserId(userId).contains(hospitalId);
        }
        return false;
    }

    private OrgVO toOrgVO(OrgEntity entity) {
        OrgVO vo = new OrgVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
