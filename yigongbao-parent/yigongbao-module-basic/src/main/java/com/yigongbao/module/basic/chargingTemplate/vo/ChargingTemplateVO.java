package com.yigongbao.module.basic.chargingTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收费模板 VO（列表查询）
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ChargingTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 部位ID（通过明细项反查，若明细项属于同一部位则返回该部位ID，否则为null）
     */
    private Long bodyPartId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
