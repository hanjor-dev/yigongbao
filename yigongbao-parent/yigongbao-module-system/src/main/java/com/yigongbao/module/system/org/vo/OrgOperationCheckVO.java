package com.yigongbao.module.system.org.vo;

import lombok.Data;

import java.util.List;

/**
 * 机构删除/禁用操作预检查结果 VO
 *
 * @author hanjor
 * @date 2026-04-29
 */
@Data
public class OrgOperationCheckVO {

    /**
     * 是否有受影响数据（true=需用户确认，false=可直接执行）
     */
    private boolean affected;

    /**
     * 提示文案
     */
    private String message;

    /**
     * 受影响的用户列表（机构下的用户）
     */
    private List<AffectedUserVO> affectedUsers;

    /**
     * 受影响的医生列表（仅删除医疗机构时有值）
     */
    private List<AffectedDoctorVO> affectedDoctors;

    /**
     * 关联该机构的部门列表（删除时会解除关联）
     */
    private List<AffectedDeptVO> affectedDepts;

    /**
     * 包含该医院的组合模板列表（禁用/删除时会从模板中移除）
     */
    private List<AffectedTemplateVO> affectedTemplates;

    @Data
    public static class AffectedUserVO {
        private Long id;
        private String realName;
    }

    @Data
    public static class AffectedDoctorVO {
        private Long id;
        private String doctorName;
    }

    @Data
    public static class AffectedDeptVO {
        private Long id;
        private String deptName;
    }

    @Data
    public static class AffectedTemplateVO {
        private Long id;
        private String templateName;
    }
}
