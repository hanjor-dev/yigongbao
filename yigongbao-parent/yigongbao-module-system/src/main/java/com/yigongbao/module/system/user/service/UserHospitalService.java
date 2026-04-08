package com.yigongbao.module.system.user.service;

import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.basic.hospital.vo.HospitalVO;

import java.util.List;
import java.util.Map;

/**
 * 用户-医院关联 Service 接口
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface UserHospitalService {

    /**
     * 查询用户的医院列表
     *
     * @param userId 用户ID
     * @return 医院列表
     */
    List<HospitalVO> getHospitalsByUserId(Long userId);

    /**
     * 查询用户的医院ID列表
     *
     * @param userId 用户ID
     * @return 医院ID列表
     */
    List<Long> getHospitalIdsByUserId(Long userId);

    /**
     * 批量查询用户的医院ID列表
     *
     * @param userIds 用户ID列表
     * @return Map<用户ID, 医院ID列表>
     */
    Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds);

    /**
     * 分配用户医院范围（覆盖式）
     *
     * @param userId      用户ID
     * @param hospitalIds 医院ID列表
     */
    void assignHospitals(Long userId, List<Long> hospitalIds);

    /**
     * 获取当前用户可操作医院（下拉选项）
     * 根据用户角色的 dataScopeType 配置决定返回范围：
     * - dataScopeType == 'hospitals'：返回用户关联的医院列表
     * - 其他类型或无角色：返回空列表
     *
     * @param userId 用户ID
     * @return 医院列表
     * @throws BusinessException 用户不存在
     */
    List<HospitalVO> getMyHospitalOptions(Long userId);

    /**
     * 获取可分配给用户的医院列表（管理员分配时使用）
     * 返回所有状态正常的医院，供管理员选择分配
     *
     * @param userId 用户ID（预留参数，当前返回所有正常医院）
     * @return 可分配的医院列表
     */
    List<HospitalVO> getHospitalOptionsByUserId(Long userId);

    /**
     * 获取用户的数据范围类型
     * 直接读取用户关联角色的 dataScopeType 字段：
     * - 角色 dataScopeType=hospitals → HOSPITALS（仅关联医院范围）
     * - 角色 dataScopeType=org → ORG（本机构全部数据）
     * - 角色 dataScopeType=all → ALL（全部数据）
     * - 角色 dataScopeType=self → SELF（仅自己创建的数据）
     * - 用户无角色或角色未配置 dataScopeType → ORG（最保守权限）
     *
     * @param userId 用户ID
     * @return 数据范围类型枚举
     */
    DataScopeTypeEnum getDataScopeType(Long userId);

    /**
     * 判断用户是否有权操作指定医院
     * - ALL/ORG 范围：允许所有医院
     * - HOSPITALS 范围：仅允许用户关联的医院
     * - SELF 范围：拒绝（不以医院维度授权）
     *
     * @param userId     用户ID
     * @param hospitalId 医院ID
     * @return true 表示有权限
     */
    boolean hasPermissionOnHospital(Long userId, Long hospitalId);
}
