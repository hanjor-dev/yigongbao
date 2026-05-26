package com.yigongbao.module.basic.processingCenter.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 加工中心分页查询请求 DTO
 *
 * @author hanjor
 * @date 2026-05-25
 */
@Data
public class ProcessingCenterPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，默认第1页 */
    private Integer pageNum = 1;

    /** 每页条数，默认10条 */
    private Integer pageSize = 10;

    /** 中心名称（模糊查询） */
    private String centerName;

    /** 状态筛选（0=禁用，1=启用，null=全部） */
    private Integer status;
}
