package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产流转卡分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRecordPageDTO {
    private Integer pageNum = 1;

    @Min(1)
    private Integer pageSize = 10;
    /** 关键词：模糊匹配订单号、数据包编号、患者姓名 */
    private String keyword;
    /** 流转卡状态（单值） */
    private Integer status;
    /** 流转卡状态列表（多值 IN，优先于 status） */
    private List<Integer> statuses;
    /** 加工中心ID */
    private Long processingCenterId;
    /** 订单创建时间范围-起始 */
    private LocalDateTime orderCreateTimeStart;
    /** 订单创建时间范围-结束 */
    private LocalDateTime orderCreateTimeEnd;
}
