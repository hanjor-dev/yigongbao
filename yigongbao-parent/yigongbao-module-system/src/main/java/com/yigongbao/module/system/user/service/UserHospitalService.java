package com.yigongbao.module.system.user.service;

import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.system.org.vo.OrgVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户-医院关联 Service 接口
 * hospital_id 语义已变更为 sys_org.id（医疗机构类型）
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface UserHospitalService {

    /**
     * 根据用户ID查询其关联的医院列表
     *
     * @param userId 用户ID
     * @return 医院VO列表
     */
    List<OrgVO> getHospitalsByUserId(Long userId);

    /**
     * 根据用户ID查询其关联的医院ID列表
     *
     * @param userId 用户ID
     * @return 医院ID列表
     */
    List<Long> getHospitalIdsByUserId(Long userId);

    /**
     * 批量查询多个用户各自关联的医院ID列表
     *
     * @param userIds 用户ID列表
     * @return 用户ID -> 医院ID列表 的映射
     */
    Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds);

    /**
     * 为指定用户分配医院范围（全量覆盖）
     *
     * @param userId      用户ID
     * @param hospitalIds 要分配的医院ID列表
     */
    void assignHospitals(Long userId, List<Long> hospitalIds);

    /**
     * 获取当前用户可选的医院列表（用于下拉选项）
     *
     * @param userId 当前用户ID
     * @return 可选医院VO列表
     */
    List<OrgVO> getMyHospitalOptions(Long userId);

    /**
     * 根据用户ID获取其可选的医院列表（管理员视角）
     *
     * @param userId 用户ID
     * @return 可选医院VO列表
     */
    List<OrgVO> getHospitalOptionsByUserId(Long userId);

    /**
     * 获取指定用户的数据权限范围类型
     *
     * @param userId 用户ID
     * @return 数据权限范围枚举（ALL / ORG / DEPT / SELF）
     */
    DataScopeTypeEnum getDataScopeType(Long userId);

    /**
     * 判断指定用户是否对某医院有访问权限
     *
     * @param userId     用户ID
     * @param hospitalId 医院ID（sys_org.id）
     * @return true=有权限，false=无权限
     */
    boolean hasPermissionOnHospital(Long userId, Long hospitalId);

    /**
     * 从给定医院ID列表中，筛选出已被任意用户分配过的医院ID集合
     *
     * @param hospitalIds 待检查的医院ID列表
     * @return 已被分配的医院ID集合
     */
    Set<Long> getAssignedHospitalIds(List<Long> hospitalIds);
}
