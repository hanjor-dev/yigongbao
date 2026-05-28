package com.yigongbao.module.production.record.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 生产流转卡分页查询 DTO
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProductionRecordPageDTO {
    private Integer pageNum = 1;
    @Max(100)
    @Min(1)
    private Integer pageSize = 10;
    /** 关键词：模糊匹配订单号、数据包编号、指令单编号、患者姓名 */
    private String keyword;
    private Integer status;
    private Long processingCenterId;
    /** 订单创建时间范围 */
    private LocalDateTime orderCreateTimeStart;
    private LocalDateTime orderCreateTimeEnd;
}
