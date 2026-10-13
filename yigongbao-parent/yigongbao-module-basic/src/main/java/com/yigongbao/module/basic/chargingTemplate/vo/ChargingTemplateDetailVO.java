package com.yigongbao.module.basic.chargingTemplate.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费模板详情 VO（包含明细和差异统计）
 *
 * @author hanjor
 * @date 2026-06-08
 */
@Data
public class ChargingTemplateDetailVO implements Serializable {

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
     * 模板明细列表
     */
    private List<ChargingTemplateItemVO> items;

    /**
     * 当前活跃项目总数
     */
    private Integer totalActiveProjects;

    /**
     * 缺失项目数量（活跃项目中存在但模板中未录入）
     */
    private Integer missingCount;

    /**
     * 失效项目数量（模板中存在但项目已删除）
     */
    private Integer obsoleteCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
