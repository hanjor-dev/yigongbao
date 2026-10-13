package com.yigongbao.module.notification.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchReadDTO {
    private List<Long> ids;
    private String category;
    private Boolean markAll;
}
