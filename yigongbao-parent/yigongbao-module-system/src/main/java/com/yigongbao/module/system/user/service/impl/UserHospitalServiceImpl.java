package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.basic.hospital.entity.HospitalEntity;
import com.yigongbao.module.basic.hospital.mapper.HospitalMapper;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;
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
    private final UserMapper userMapper;
    private final RoleService roleService;

    /**
     * 查询用户的医院ID列表
     *
     * @param userId 用户ID
     * @return 医院ID列表
     */
    @Override
    public List<Long> getHospitalIdsByUserId(Long userId) {
        log.info("查询用户的医院ID列表，userId={}", userId);
        try {
            List<Long> hospitalIds = userHospitalMapper.selectHospitalIdsByUserId(userId);
            log.info("查询用户的医院ID列表成功，数量={}", hospitalIds != null ? hospitalIds.size() : 0);
            return hospitalIds != null ? hospitalIds : new ArrayList<>();
        } catch (Exception e) {
            log.error("查询用户的医院ID列表异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 批量查询用户的医院ID列表
     *
     * @param userIds 用户ID列表
     * @return Map<用户ID, 医院ID列表>
     */
    @Override
    public Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        log.info("批量查询用户的医院ID列表，userIds数量={}", userIds.size());
        try {
            // 批量查询所有用户-医院关联
            LambdaQueryWrapper<UserHospitalEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(UserHospitalEntity::getUserId, userIds);
            List<UserHospitalEntity> userHospitalList = userHospitalMapper.selectList(wrapper);

            // 按用户ID分组
            Map<Long, List<Long>> result = userHospitalList.stream()
                    .collect(Collectors.groupingBy(
                            UserHospitalEntity::getUserId,
                            Collectors.mapping(UserHospitalEntity::getHospitalId, Collectors.toList())
                    ));

            log.info("批量查询用户的医院ID列表成功，userIds数量={}", result.size());
            return result;
        } catch (Exception e) {
            log.error("批量查询用户的医院ID列表异常，userIds数量={}", userIds.size(), e);
            throw e;
        }
    }

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
            List<HospitalVO> voList = hospitals.stream()
                    .filter(h -> h != null)
                    .map(this::toVO)
                    .collect(Collectors.toList());
            log.info("查询用户的医院列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("查询用户的医院列表异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 分配用户医院范围（覆盖式）
     * 仅当用户角色的 hospitalScopeEnabled=1 时才调用此方法
     *
     * @param userId      用户ID
     * @param hospitalIds 医院ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignHospitals(Long userId, List<Long> hospitalIds) {
        log.info("分配用户医院范围，userId={}, 医院数量={}", userId, hospitalIds != null ? hospitalIds.size() : 0);
        try {
            // 1. 校验用户是否存在
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，userId={}", userId);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }

            // 2. 校验医院ID是否有效（是否存在、是否启用）
            if (hospitalIds != null && !hospitalIds.isEmpty()) {
                List<HospitalEntity> rawHospitals = hospitalMapper.selectBatchIds(hospitalIds);
                // 过滤掉不存在的医院（仅保留非 null 的记录）
                List<HospitalEntity> validHospitals = rawHospitals.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 检查是否有无效的医院ID（数据库中不存在的ID）
                if (validHospitals.size() != hospitalIds.size()) {
                    List<Long> invalidIds = hospitalIds.stream()
                            .filter(id -> validHospitals.stream().noneMatch(h -> h.getId().equals(id)))
                            .collect(Collectors.toList());
                    log.warn("部分医院不存在，userId={}, 无效ID={}", userId, invalidIds);
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND.getCode(),
                            "医院不存在，id=" + invalidIds);
                }

                // 检查是否有禁用的医院
                for (HospitalEntity hospital : validHospitals) {
                    if (hospital.getStatus() != null && hospital.getStatus() == StatusConstants.DISABLED) {
                        log.warn("医院已停用，hospitalId={}, hospitalName={}",
                                hospital.getId(), hospital.getHospitalName());
                        throw new BusinessException(ErrorCodeEnum.HOSPITAL_DISABLED.getCode(),
                                "医院【" + hospital.getHospitalName() + "】已停用，请先启用");
                    }
                }
            }

            // 3. 删除旧关联（覆盖式）
            userHospitalMapper.deleteByUserId(userId);

            // 4. 插入新关联
            if (hospitalIds != null && !hospitalIds.isEmpty()) {
                for (Long hospitalId : hospitalIds) {
                    UserHospitalEntity entity = new UserHospitalEntity();
                    entity.setUserId(userId);
                    entity.setHospitalId(hospitalId);
                    userHospitalMapper.insert(entity);
                }
            }
            log.info("分配用户医院范围成功，userId={}, 医院数量={}", userId,
                    hospitalIds != null ? hospitalIds.size() : 0);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分配用户医院范围异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取当前用户可操作医院（下拉选项）
     * 根据用户角色的 hospitalScopeEnabled 配置决定返回范围：
     * - hospitalScopeEnabled == 1：返回用户关联的医院列表
     * - hospitalScopeEnabled == 0 或无角色：返回空列表
     *
     * @param userId 用户ID
     * @return 医院列表
     * @throws BusinessException 用户不存在
     */
    @Override
    public List<HospitalVO> getMyHospitalOptions(Long userId) {
        log.info("获取当前用户可操作医院列表，userId={}", userId);
        try {
            // 1. 校验用户是否存在
            UserEntity user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，userId={}", userId);
                throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
            }

            // 2. 检查角色的 hospitalScopeEnabled
            boolean hospitalScopeEnabled = false;
            if (user.getRoleId() != null) {
                RoleEntity role = roleService.getById(user.getRoleId());
                if (role != null && role.getHospitalScopeEnabled() != null
                        && role.getHospitalScopeEnabled() == StatusConstants.YES) {
                    hospitalScopeEnabled = true;
                }
            }

            // 3. 根据 hospitalScopeEnabled 决定查询范围
            List<HospitalVO> result;
            if (hospitalScopeEnabled) {
                result = getHospitalsByUserId(userId);
            } else {
                result = new ArrayList<>();
            }

            log.info("获取当前用户可操作医院列表成功，userId={}, 数量={}", userId, result.size());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取当前用户可操作医院列表异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取可分配给用户的医院列表（管理员分配时使用）
     * 返回所有状态正常的医院，供管理员选择分配
     *
     * @param userId 用户ID（预留参数，当前返回所有正常医院）
     * @return 可分配的医院列表
     */
    @Override
    public List<HospitalVO> getHospitalOptionsByUserId(Long userId) {
        log.info("获取可分配给用户的医院列表，userId={}", userId);
        try {
            // 返回所有状态正常的医院，供管理员选择分配
            LambdaQueryWrapper<HospitalEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HospitalEntity::getStatus, StatusConstants.NORMAL)
                    .orderByAsc(HospitalEntity::getHospitalName);
            List<HospitalEntity> list = hospitalMapper.selectList(wrapper);
            List<HospitalVO> voList = list.stream()
                    .filter(Objects::nonNull)
                    .map(this::toVO)
                    .collect(Collectors.toList());
            log.info("获取可分配医院列表成功，数量={}", voList.size());
            return voList;
        } catch (Exception e) {
            log.error("获取可分配医院列表异常，userId={}", userId, e);
            throw e;
        }
    }

    /**
     * 获取用户的数据范围类型
     * 高频调用场景（如订单列表查询），建议后续引入 Redis 缓存以降低 DB 压力。
     *
     * @param userId 用户ID
     * @return 数据范围类型枚举
     */
    @Override
    public DataScopeTypeEnum getDataScopeType(Long userId) {
        if (userId == null) {
            return DataScopeTypeEnum.SELF;
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return DataScopeTypeEnum.SELF;
        }
        if (user.getRoleId() != null) {
            RoleEntity role = roleService.getById(user.getRoleId());
            if (role != null && role.getHospitalScopeEnabled() != null
                    && role.getHospitalScopeEnabled() == StatusConstants.YES) {
                return DataScopeTypeEnum.HOSPITALS;
            }
        }
        // 内部用户（accountType=1）无医院范围限制时，享有全量权限
        if (user.getAccountType() != null && user.getAccountType() == 1) {
            return DataScopeTypeEnum.ALL;
        }
        // 外部用户（如医院侧）仅看自己创建的数据
        return DataScopeTypeEnum.SELF;
    }

    /**
     * 判断用户是否有权操作指定医院
     *
     * @param userId     用户ID
     * @param hospitalId 医院ID
     * @return true 表示有权限
     */
    @Override
    public boolean hasPermissionOnHospital(Long userId, Long hospitalId) {
        if (userId == null || hospitalId == null) {
            return false;
        }
        DataScopeTypeEnum scopeType = getDataScopeType(userId);
        if (scopeType == DataScopeTypeEnum.ALL || scopeType == DataScopeTypeEnum.ORG) {
            return true;
        }
        if (scopeType == DataScopeTypeEnum.HOSPITALS) {
            List<Long> hospitalIds = getHospitalIdsByUserId(userId);
            return hospitalIds.contains(hospitalId);
        }
        return false;
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
