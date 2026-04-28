package com.yigongbao.module.system.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院组合模板明细 VO，用于展示模板中每家医院的详细信息及分配状态
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细记录主键 ID */
    private Long id;

    /** 所属模板 ID */
    private Long templateId;

    /** 医院 ID */
    private Long hospitalId;

    /** 医院名称 */
    private String hospitalName;

    /** 医院编码 */
    private String hospitalCode;

    /** 完整行政区域名称（省/市/区） */
    private String fullAreaName;

    /** 医院等级名称（如三甲、二甲等） */
    private String hospitalLevelName;

    /** 联系人姓名 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /**
     * 该医院是否已被系统中任意用户分配。
     * true=已有用户分配，false=无用户分配，用于前端展示分配状态
     */
    private Boolean assigned;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
