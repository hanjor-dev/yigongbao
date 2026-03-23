package com.yigongbao.module.system.user.service;

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
     *
     * @param userId 用户ID
     * @return 医院列表
     */
    List<HospitalVO> getHospitalOptionsByUserId(Long userId);
}
