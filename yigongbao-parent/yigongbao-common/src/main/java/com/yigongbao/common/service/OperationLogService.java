package com.yigongbao.common.service;

import com.yigongbao.common.enums.OperationTypeEnum;

/**
 * 操作日志 Service 接口（common 层）
 * 仅定义 AOP 切面所需的基础方法，避免 framework 层依赖 system 模块
 *
 * @author hanjor
 * @date 2026-03-24
 */
public interface OperationLogService {

    /**
     * 异步保存操作日志
     *
     * @param operationType 操作类型
     * @param module 模块名称
     * @param businessNo 业务编号
     * @param content 操作内容
     * @param operatorId 操作人ID
     * @param operatorName 操作人姓名
     * @param operatorUsername 操作人用户名
     * @param ipAddress IP地址
     * @param ipLocation IP归属地
     * @param userAgent 用户代理
     * @param requestMethod 请求方法（GET/POST/PUT/DELETE）
     * @param duration 执行时长（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误信息（成功时为null）
     * @param requestParams 请求参数
     */
    void saveLog(OperationTypeEnum operationType, String module, String businessNo,
                  String content, Long operatorId, String operatorName, String operatorUsername,
                  String ipAddress, String ipLocation, String userAgent, String requestMethod,
                  Long duration, boolean success, String errorMessage, String requestParams);
}
