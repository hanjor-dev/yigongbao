package com.yigongbao.module.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.constant.DictCodeConstants;
import com.yigongbao.common.constant.StatusConstants;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.system.org.entity.OrgEntity;
import com.yigongbao.module.system.org.service.OrgService;
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
import java.util.Set;
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
    private final OrgService orgService;
    private final UserMapper userMapper;
    private final RoleService roleService;

    /**
     * 查询指定用户关联的医院ID列表
     *
     * @param userId 用户ID
     * @return 该用户已分配的医院ID列表；无记录时返回空列表
     */
    @Override
    public List<Long> getHospitalIdsByUserId(Long userId) {
        List<Long> ids = userHospitalMapper.selectHospitalIdsByUserId(userId);
        return ids != null ? ids : new ArrayList<>();
    }

    /**
     * 批量查询多个用户各自关联的医院ID列表
     * <p>
     * 一次 IN 查询替代逐用户查询，避免 N+1 问题。
     * </p>
     *
     * @param userIds 用户ID列表
     * @return Map，key 为 userId，value 为该用户关联的医院ID列表
     */
    @Override
    public Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyMap();
        // 批量查询所有用户的关联记录，再按 userId 分组
        List<UserHospitalEntity> list = userHospitalMapper.selectList(
                new LambdaQueryWrapper<UserHospitalEntity>().in(UserHospitalEntity::getUserId, userIds));
        return list.stream().collect(Collectors.groupingBy(
                UserHospitalEntity::getUserId,
                Collectors.mapping(UserHospitalEntity::getHospitalId, Collectors.toList())));
    }

    /**
     * 查询指定用户关联的医院机构VO列表
     *
     * @param userId 用户ID
     * @return 机构VO列表；用户无关联医院时返回空列表
     */
    @Override
    public List<OrgVO> getHospitalsByUserId(Long userId) {
        List<Long> ids = getHospitalIdsByUserId(userId);
        if (ids.isEmpty()) return new ArrayList<>();
        return orgService.listByIds(ids).stream()
                .filter(Objects::nonNull)
                .map(this::toOrgVO)
                .collect(Collectors.toList());
    }

    /**
     * 为用户分配医疗机构范围（全量覆盖策略）
     * <p>
     * 先删除该用户所有旧关联记录，再插入新记录，实现全量替换。
     * 传入空列表时仅清空关联，不报错。
     * </p>
     *
     * @param userId      用户ID
     * @param hospitalIds 新的医院ID列表（须为 orgType=1.3 的有效启用机构）
     * @throws BusinessException 用户不存在时抛出 USER_NOT_FOUND；
     *                           含无效机构ID时抛出 HOSPITAL_NOT_FOUND；
     *                           含已禁用机构时抛出 HOSPITAL_DISABLED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignHospitals(Long userId, List<Long> hospitalIds) {
        log.info("分配用户医疗机构范围，userId={}, 数量={}", userId, hospitalIds != null ? hospitalIds.size() : 0);
        UserEntity user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);

        // 校验 hospitalIds 均为有效医疗机构（orgType=1.3）
        if (hospitalIds != null && !hospitalIds.isEmpty()) {
            List<OrgEntity> orgs = orgService.listByIds(hospitalIds);
            List<OrgEntity> valid = orgs.stream().filter(Objects::nonNull)
                    .filter(o -> DictCodeConstants.ORG_TYPE_HOSPITAL.equals(o.getOrgType())).collect(Collectors.toList());
            // 有效机构数量不匹配，说明存在非法ID
            if (valid.size() != hospitalIds.size()) {
                throw new BusinessException(ErrorCodeEnum.HOSPITAL_NOT_FOUND);
            }
            for (OrgEntity org : valid) {
                // 禁止分配已禁用的医疗机构
                if (Integer.valueOf(StatusConstants.DISABLED).equals(org.getStatus())) {
                    throw new BusinessException(ErrorCodeEnum.HOSPITAL_DISABLED);
                }
            }
        }

        // 全量覆盖：先删除旧关联，再插入新关联
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

    /**
     * 获取当前用户可选的医院列表（依据数据权限范围）
     * <p>
     * 仅当用户角色的数据权限类型为 HOSPITALS 时，返回其已分配的医院列表；
     * 其他权限类型（ALL/ORG）返回空列表，由调用方按全量处理。
     * </p>
     *
     * @param userId 用户ID
     * @return 可选医院VO列表
     * @throws BusinessException 用户不存在时抛出 USER_NOT_FOUND
     */
    @Override
    public List<OrgVO> getMyHospitalOptions(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCodeEnum.USER_NOT_FOUND);
        boolean isHospitalsScope = false;
        if (user.getRoleId() != null) {
            RoleEntity role = roleService.getById(user.getRoleId());
            // 角色数据权限类型为 HOSPITALS 时，限定为已分配医院
            if (role != null && DataScopeTypeEnum.HOSPITALS.getCode().equals(role.getDataScopeType())) {
                isHospitalsScope = true;
            }
        }
        return isHospitalsScope ? getHospitalsByUserId(userId) : new ArrayList<>();
    }

    /**
     * 获取指定用户可见的全部医院选项（不受数据权限限制，返回所有启用医院）
     *
     * @param userId 用户ID（当前实现未使用，保留供后续权限扩展）
     * @return 所有状态正常的医疗机构VO列表，按名称升序排列
     */
    @Override
    public List<OrgVO> getHospitalOptionsByUserId(Long userId) {
        List<OrgEntity> list = orgService.list(new LambdaQueryWrapper<OrgEntity>()
                .eq(OrgEntity::getOrgType, DictCodeConstants.ORG_TYPE_HOSPITAL)
                .eq(OrgEntity::getStatus, StatusConstants.NORMAL)
                .orderByAsc(OrgEntity::getOrgName));
        return list.stream().map(this::toOrgVO).collect(Collectors.toList());
    }

    /**
     * 获取用户的数据权限范围类型
     * <p>
     * 取用户绑定角色的 dataScopeType 字段；用户或角色不存在时默认返回 ORG。
     * </p>
     *
     * @param userId 用户ID
     * @return 数据权限枚举值，默认为 DataScopeTypeEnum.ORG
     */
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

    /**
     * 判断用户是否对指定医院有操作权限
     * <p>
     * ALL/ORG 权限类型直接放行；HOSPITALS 类型需检查用户已分配列表中是否包含该医院。
     * </p>
     *
     * @param userId     用户ID
     * @param hospitalId 医院ID
     * @return true 表示有权限
     */
    @Override
    public boolean hasPermissionOnHospital(Long userId, Long hospitalId) {
        if (userId == null || hospitalId == null) return false;
        DataScopeTypeEnum scopeType = getDataScopeType(userId);
        // ALL 和 ORG 权限类型对所有医院放行
        if (scopeType == DataScopeTypeEnum.ALL || scopeType == DataScopeTypeEnum.ORG) return true;
        if (scopeType == DataScopeTypeEnum.HOSPITALS) {
            // HOSPITALS 类型需校验该医院是否在用户已分配列表中
            return getHospitalIdsByUserId(userId).contains(hospitalId);
        }
        return false;
    }

    /**
     * 从给定医院ID列表中，返回所有已被任意用户分配的医院ID集合
     * <p>
     * 用于模板明细展示时标记哪些医院已有用户覆盖（assigned 字段）。
     * 查询范围限定在传入的 hospitalIds 内，避免全表扫描。
     * </p>
     *
     * @param hospitalIds 待检查的医院ID列表
     * @return 已被任意用户分配的医院ID集合；输入为空时返回空集合
     */
    @Override
    public Set<Long> getAssignedHospitalIds(List<Long> hospitalIds) {
        if (hospitalIds == null || hospitalIds.isEmpty()) return Collections.emptySet();
        // 在传入范围内查询 sys_user_hospital，取出所有有关联记录的 hospitalId
        List<UserHospitalEntity> list = userHospitalMapper.selectList(
                new LambdaQueryWrapper<UserHospitalEntity>().in(UserHospitalEntity::getHospitalId, hospitalIds));
        return list.stream().map(UserHospitalEntity::getHospitalId).collect(Collectors.toSet());
    }

    /**
     * 将机构实体转换为机构VO
     *
     * @param entity 机构实体
     * @return 机构VO
     */
    private OrgVO toOrgVO(OrgEntity entity) {
        OrgVO vo = new OrgVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
