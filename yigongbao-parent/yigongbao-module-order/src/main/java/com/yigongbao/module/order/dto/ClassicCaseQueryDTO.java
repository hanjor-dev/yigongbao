package com.yigongbao.module.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 经典案例查询请求DTO
 */
@Data
public class ClassicCaseQueryDTO {

    private String orderCode;
    private String patientName;
    private Long hospitalId;
    private Long bodyPartId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
