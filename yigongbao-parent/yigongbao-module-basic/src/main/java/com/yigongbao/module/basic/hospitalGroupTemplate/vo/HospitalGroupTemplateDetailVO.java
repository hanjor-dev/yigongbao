package com.yigongbao.module.basic.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院组合模板明细 VO
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 医院编码
     */
    private String hospitalCode;

    /**
     * 完整地区路径
     */
    private String fullAreaName;

    /**
     * 医院等级名称
     */
    private String hospitalLevelName;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
