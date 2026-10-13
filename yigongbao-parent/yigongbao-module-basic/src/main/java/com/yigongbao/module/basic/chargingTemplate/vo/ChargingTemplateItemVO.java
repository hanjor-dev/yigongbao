package com.yigongbao.module.basic.chargingTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 收费模板明细 VO
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ChargingTemplateItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    private Long id;

    /**
     * 重建项目ID
     */
    private Long rebuildProjectId;

    /**
     * 重建项目名称
     */
    private String projectName;

    /**
     * 收费价格（元）
     */
    private BigDecimal price;

    /**
     * 项目是否已失效（已删除）
     */
    private Boolean isObsolete;
}
