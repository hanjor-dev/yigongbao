package com.yigongbao.module.basic.operationlog.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志 VO（视图对象）
 * 用于返回给前端的操作日志数据
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作用户真实姓名
     */
    private String realName;

    /**
     * 请求模块
     */
    private String module;

    /**
     * 业务类型
     */
    private Integer businessType;

    /**
     * 业务类型名称
     */
    private String businessTypeName;

    /**
     * 操作描述
     */
    private String operation;

    /**
     * 业务描述
     */
    private String description;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数（脱敏处理）
     */
    private String requestParams;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 操作地点
     */
    private String location;

    /**
     * 响应状态（0-失败，1-成功）
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时长（毫秒）
     */
    private Long duration;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
}
