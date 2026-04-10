package com.yigongbao.module.basic.bodyPart.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 重建部位详情 VO
 * 用于返回给前端的部位详细信息
 *
 * @author hanjor
 * @date 2026-03-23
 */
@Data
public class BodyPartDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部位ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 父级名称
     */
    private String parentName;

    /**
     * 部位名称
     */
    private String name;

    /**
     * 部位编码
     */
    private String code;

    /**
     * 层级（1=身体区域，2=具体部位）
     */
    private Integer level;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态（0=禁用，1=正常）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
