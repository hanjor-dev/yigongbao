package com.yigongbao.module.system.hospitalGroupTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 医院组合模板 VO（列表展示）
 *
 * @author hanjor
 * @date 2026-03-19
 */
@Data
public class HospitalGroupTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateName;
    private String templateCode;
    private String templateDesc;
    private Integer status;
    private String statusName;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer hospitalCount;
    private List<HospitalGroupTemplateDetailVO> details;
}
