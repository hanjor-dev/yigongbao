package com.yigongbao.module.system.org.vo;

import lombok.Data;

import java.util.List;

/**
 * 经销商关联医院变更预检查结果 VO
 *
 * @author hanjor
 * @date 2026-04-29
 */
@Data
public class OrgHospitalChangeCheckVO {

    /**
     * 是否有受影响的用户（true=需要用户确认，false=可直接提交）
     */
    private boolean affected;

    /**
     * 被移除的医院列表
     */
    private List<RemovedHospitalVO> removedHospitals;

    /**
     * 受影响的业务员列表（拥有被移除医院权限的用户）
     */
    private List<AffectedUserVO> affectedUsers;

    /**
     * 被移除的医院信息
     */
    @Data
    public static class RemovedHospitalVO {
        /** 医院ID */
        private Long id;
        /** 医院名称 */
        private String orgName;
    }

    /**
     * 受影响的业务员信息
     */
    @Data
    public static class AffectedUserVO {
        /** 用户ID */
        private Long id;
        /** 用户姓名 */
        private String realName;
        /** 被移除的医院ID列表 */
        private List<Long> removedHospitalIds;
    }
}
