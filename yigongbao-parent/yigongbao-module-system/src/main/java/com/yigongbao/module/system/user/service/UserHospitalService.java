package com.yigongbao.module.system.user.service;

import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.module.system.org.vo.OrgVO;

import java.util.List;
import java.util.Map;

/**
 * 用户-医院关联 Service 接口
 * hospital_id 语义已变更为 sys_org.id（医疗机构类型）
 *
 * @author hanjor
 * @date 2026-03-19
 */
public interface UserHospitalService {

    List<OrgVO> getHospitalsByUserId(Long userId);

    List<Long> getHospitalIdsByUserId(Long userId);

    Map<Long, List<Long>> listHospitalIdsByUserIds(List<Long> userIds);

    void assignHospitals(Long userId, List<Long> hospitalIds);

    List<OrgVO> getMyHospitalOptions(Long userId);

    List<OrgVO> getHospitalOptionsByUserId(Long userId);

    DataScopeTypeEnum getDataScopeType(Long userId);

    boolean hasPermissionOnHospital(Long userId, Long hospitalId);
}
