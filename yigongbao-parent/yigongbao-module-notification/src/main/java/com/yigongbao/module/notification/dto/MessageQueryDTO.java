package com.yigongbao.module.notification.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 消息查询 DTO
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Data
public class MessageQueryDTO {

    private String category;
    private Integer isRead;
    private Integer isConfirmed;
    private String messageType;

    @Min(1)
    private Integer pageNum = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;
}
