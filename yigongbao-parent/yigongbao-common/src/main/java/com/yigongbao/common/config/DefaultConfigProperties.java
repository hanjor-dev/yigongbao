package com.yigongbao.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置兜底默认值属性类
 * 从 application.yml 的 yigongbao.config 节点读取默认值
 * 当数据库配置不存在或已禁用时，使用此处配置的值
 *
 * 字段命名规则：configKey 去除点号转驼峰后加 config 前缀
 * 例如：default.password → configDefaultPassword
 *
 * @author hanjor
 * @date 2026-03-18
 */
@Data
@Component
@ConfigurationProperties(prefix = "yigongbao.config")
public class DefaultConfigProperties {

    // ==================== 安全配置 ====================
    /**
     * 默认密码
     * 新用户初始密码
     */
    private String configDefaultPassword = "123456";

    /**
     * 最大连续登录失败次数
     * 连续失败后锁定账号
     */
    private Integer configLoginMaxFailures = 5;

    /**
     * 登录锁定时长（分钟）
     * 自动解锁时间
     */
    private Integer configLoginLockDuration = 15;

    /**
     * 短信发送间隔（秒）
     * 同一手机号发送间隔
     */
    private Integer configSmsSendInterval = 60;

    // ==================== 系统配置 ====================
    /**
     * 系统名称
     */
    private String configSystemName = "医工宝";

    /**
     * 文件上传最大大小（字节）
     * 默认 500MB
     */
    private Long configMaxUploadSize = 524288000L;

    // ==================== 订单配置 ====================
    /**
     * 提交订单是否必须上传影像文件
     * true - 必须上传（默认）
     * false - 非必填
     */
    private Boolean configOrderImageRequired = true;

    /**
     * 草稿自动过期天数
     * 默认 30 天
     */
    private Integer configOrderDraftExpireDays = 30;

    /**
     * 订单提交后修改窗口期（分钟）
     * 默认 10 分钟
     */
    private Integer configOrderModifyWindowMinutes = 10;

    /**
     * 订单列表默认列配置（JSON 格式）
     */
    private String configOrderColumnConfig = "{\"id\":true,\"orderNo\":true,\"hospitalName\":true,\"patientName\":true,\"statusName\":true,\"createTime\":true}";

    // ==================== 流程状态机配置 ====================
    /**
     * 最大允许的审核驳回次数
     */
    private Integer configFlowMaxAuditReject = 3;

    /**
     * 最大允许的返工次数
     */
    private Integer configFlowMaxRework = 2;

    /**
     * 最大允许的设计审核驳回次数
     */
    private Integer configFlowMaxDesignReject = 3;
}
