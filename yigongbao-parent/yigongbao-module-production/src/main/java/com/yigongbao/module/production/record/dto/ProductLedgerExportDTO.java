package com.yigongbao.module.production.record.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产产品台账导出查询参数
 * <p>
 * 用于导出生产产品台账Excel，支持按流转卡编号、订单编号、产品编号、时间范围等条件查询。
 * 系统会根据当前用户的数据权限自动过滤数据（医院权限/加工中心权限/全部权限）。
 * </p>
 *
 * @author hanjor
 * @date 2026-06-22
 */
@Data
public class ProductLedgerExportDTO {
    /** 流转卡编号（模糊查询） */
    private String recordNo;

    /** 订单编号（模糊查询） */
    private String orderCode;

    /** 产品编号（模糊查询） */
    private String productNo;

    /** 创建时间起（订单创建时间） */
    private LocalDateTime startTime;

    /** 创建时间止（订单创建时间） */
    private LocalDateTime endTime;

    /**
     * 医院ID列表（数据权限过滤，由Service层根据用户权限自动填充，前端无需传递）
     */
    private List<Long> hospitalIds;

    /**
     * 加工中心ID列表（数据权限过滤，由Service层根据用户权限自动填充，前端无需传递）
     */
    private List<Long> centerIds;
}
