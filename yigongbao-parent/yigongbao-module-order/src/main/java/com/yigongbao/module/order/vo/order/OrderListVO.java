package com.yigongbao.module.order.vo.order;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单列表项 VO
 * 用于订单列表分页查询返回的数据结构
 *
 * @author hanjor
 * @date 2026-03-31
 */
@Data
public class OrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderCode;

    /**
     * 订单类型：1-医疗器械，2-非医疗器械
     */
    private Integer orderType;

    /**
     * 订单类型名称（补充显示）
     */
    private String orderTypeName;

    /**
     * 是否需要实体交付：0-不需要，1-需要
     */
    private Integer needsPhysicalDelivery;

    /**
     * 是否需要实体交付名称（补充显示）
     */
    private String needsPhysicalDeliveryName;

    /**
     * 业务类型（字典 dict_code：11.1-业务，11.2-测试，11.3-试用，11.4-代理）
     */
    private String businessType;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称（冗余）
     */
    private String hospitalName;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 当前阶段：1-订单，2-设计，3-打印，4-后处理，5-质检，6-仓储，7-确认，8-完成
     */
    private Integer phase;

    /**
     * 当前状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
