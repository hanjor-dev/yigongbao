package com.yigongbao.module.basic.operationlog.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 操作日志查询 DTO
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class OperationLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 模块名称（模糊查询）
     */
    private String module;

    /**
     * 操作类型
     */
    private Integer businessType;

    /**
     * 操作描述（模糊查询）
     */
    private String operation;

    /**
     * 操作用户名（模糊查询）
     */
    private String username;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 操作状态（0=失败，1=成功）
     */
    private Integer status;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;
}
