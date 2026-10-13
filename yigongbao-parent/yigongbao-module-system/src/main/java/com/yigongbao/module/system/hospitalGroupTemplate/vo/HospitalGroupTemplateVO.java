package com.yigongbao.module.system.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 医院组合模板 VO，用于列表及详情展示
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板主键 ID */
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 模板描述 */
    private String templateDesc;

    /** 模板状态（1=启用，0=禁用） */
    private Integer status;

    /** 模板状态名称（前端展示用） */
    private String statusName;

    /** 备注信息 */
    private String remark;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;

    /** 模板关联的医院数量 */
    private Integer hospitalCount;

    /** 模板关联的医院明细列表 */
    private List<HospitalGroupTemplateDetailVO> details;
}
