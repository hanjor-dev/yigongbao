package com.yigongbao.module.basic.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志 Entity
 *
 * @author hanjor
 * @date 2026-03-24
 */
@Data
@TableName("sys_operation_log")
public class OperationLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 业务类型（关联字典或枚举）
     */
    private Integer businessType;

    /**
     * 业务类型描述
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
     * 请求参数
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
     * User-Agent
     */
    private String userAgent;

    /**
     * 响应状态（0-失败，1-成功）
     */
    private Integer status;

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
