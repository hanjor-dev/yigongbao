package com.yigongbao.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置兜底默认值属性类
 * 当数据库配置不存在或已禁用时，使用此处配置的默认值
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "yigongbao.config.default-values")
public class DefaultConfigProperties {

    /**
     * 默认密码
     * 新用户初始密码
     */
    private String defaultPassword = "123456";

    /**
     * 最大连续登录失败次数
     * 连续失败后锁定账号
     */
    private Integer loginMaxFailures = 5;

    /**
     * 登录锁定时长（分钟）
     * 自动解锁时间
     */
    private Integer loginLockDuration = 15;

    /**
     * 短信发送间隔（秒）
     * 同一手机号发送间隔
     */
    private Integer smsSendInterval = 60;

    /**
     * 文件上传最大大小（字节）
     * 默认10MB
     */
    private Long maxUploadSize = 10485760L;
}
