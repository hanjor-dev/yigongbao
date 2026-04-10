package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.util.List;

/**
 * 可分配设计师 VO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class DesignerVO {

    /**
     * 设计师用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 专业方向字典编码列表
     */
    private List<String> specialtyList;

    /**
     * 专业方向名称列表
     */
    private List<String> specialtyNameList;

    /**
     * 当前在手工单数
     */
    private Integer currentLoad;
}
