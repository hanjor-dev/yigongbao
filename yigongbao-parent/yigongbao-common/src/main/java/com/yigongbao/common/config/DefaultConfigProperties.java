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
     * 默认 2GB
     */
    private Long configMaxUploadSize = 2147483648L;

    // ==================== 订单配置 ====================
    /**
     * 提交订单是否必须上传影像文件
     * true - 必须上传（默认）
     * false - 非必填
     */
    private Boolean configOrderImageRequired = true;

    /**
     * 影像数据包允许的文件扩展名（逗号分隔）
     * 默认：.zip,.rar,.7z
     */
    private String configOrderImageDataAllowedExtensions = ".zip,.rar,.7z";

    /**
     * 影像报告允许的文件扩展名（逗号分隔）
     * 默认：.pdf,.doc,.docx,.xls,.xlsx
     */
    private String configOrderImageReportAllowedExtensions = ".pdf,.doc,.docx,.xls,.xlsx";

    /**
     * 影像数据包最大文件大小（MB）
     * 默认：500 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，此处仅作兜底
     */
    private Integer configOrderImageDataMaxSizeMb = 500;

    /**
     * 影像报告最大文件大小（MB）
     * 默认：50 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，此处仅作兜底
     */
    private Integer configOrderImageReportMaxSizeMb = 50;

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
    private String configOrderColumnConfig = "{\"module\":\"order\",\"columns\":[{\"field\":\"orderCode\",\"label\":\"订单编号\",\"visible\":true,\"sort\":1,\"width\":160,\"fixed\":null},{\"field\":\"phaseName\",\"label\":\"当前阶段\",\"visible\":true,\"sort\":2,\"width\":100,\"fixed\":null},{\"field\":\"statusName\",\"label\":\"当前状态\",\"visible\":true,\"sort\":3,\"width\":120,\"fixed\":null},{\"field\":\"isUrgent\",\"label\":\"加急\",\"visible\":true,\"sort\":4,\"width\":70,\"fixed\":null},{\"field\":\"businessTypeName\",\"label\":\"业务类型\",\"visible\":true,\"sort\":5,\"width\":90,\"fixed\":null},{\"field\":\"orderTypeName\",\"label\":\"订单类型\",\"visible\":true,\"sort\":6,\"width\":110,\"fixed\":null},{\"field\":\"needsPhysicalDeliveryName\",\"label\":\"实体交付\",\"visible\":true,\"sort\":7,\"width\":90,\"fixed\":null},{\"field\":\"orgName\",\"label\":\"提单机构\",\"visible\":true,\"sort\":8,\"width\":150,\"fixed\":null},{\"field\":\"operatorName\",\"label\":\"操作员\",\"visible\":true,\"sort\":9,\"width\":100,\"fixed\":null},{\"field\":\"operatorPhone\",\"label\":\"操作员电话\",\"visible\":true,\"sort\":10,\"width\":120,\"fixed\":null},{\"field\":\"operatorDeptName\",\"label\":\"所属部门\",\"visible\":true,\"sort\":11,\"width\":120,\"fixed\":null},{\"field\":\"hospitalName\",\"label\":\"医院\",\"visible\":true,\"sort\":12,\"width\":180,\"fixed\":null},{\"field\":\"areaName\",\"label\":\"地区\",\"visible\":true,\"sort\":13,\"width\":100,\"fixed\":null},{\"field\":\"fullAreaName\",\"label\":\"完整地区\",\"visible\":true,\"sort\":14,\"width\":160,\"fixed\":null},{\"field\":\"hospitalDeptName\",\"label\":\"科室\",\"visible\":true,\"sort\":15,\"width\":100,\"fixed\":null},{\"field\":\"doctorName\",\"label\":\"医生姓名\",\"visible\":true,\"sort\":16,\"width\":100,\"fixed\":null},{\"field\":\"doctorPhone\",\"label\":\"医生电话\",\"visible\":true,\"sort\":17,\"width\":120,\"fixed\":null},{\"field\":\"patientName\",\"label\":\"患者姓名\",\"visible\":true,\"sort\":18,\"width\":100,\"fixed\":null},{\"field\":\"patientAge\",\"label\":\"患者年龄\",\"visible\":true,\"sort\":19,\"width\":80,\"fixed\":null},{\"field\":\"patientGenderName\",\"label\":\"患者性别\",\"visible\":true,\"sort\":20,\"width\":80,\"fixed\":null},{\"field\":\"isPostal\",\"label\":\"是否邮寄\",\"visible\":true,\"sort\":21,\"width\":80,\"fixed\":null},{\"field\":\"postalAddress\",\"label\":\"邮寄地址\",\"visible\":true,\"sort\":22,\"width\":160,\"fixed\":null},{\"field\":\"designerName\",\"label\":\"设计师\",\"visible\":true,\"sort\":23,\"width\":100,\"fixed\":null},{\"field\":\"expectedDeliveryDate\",\"label\":\"期望交付时间\",\"visible\":true,\"sort\":24,\"width\":160,\"fixed\":null},{\"field\":\"estimatedCost\",\"label\":\"预估费用\",\"visible\":true,\"sort\":25,\"width\":100,\"fixed\":null},{\"field\":\"dataEvaluationOpinion\",\"label\":\"影像评估意见\",\"visible\":true,\"sort\":26,\"width\":160,\"fixed\":null},{\"field\":\"rebuildProjectList\",\"label\":\"重建项目\",\"visible\":true,\"sort\":27,\"width\":200,\"fixed\":null},{\"field\":\"createTime\",\"label\":\"创建时间\",\"visible\":true,\"sort\":28,\"width\":160,\"fixed\":null},{\"field\":\"action\",\"label\":\"操作\",\"visible\":true,\"sort\":29,\"width\":150,\"fixed\":null}]}";
    /**
     * 订单修改申请字段配置（JSON 格式）
     * 顶层 key 为申请类型字典编码（14.1/14.2/14.3），value 为该类型允许修改的字段列表
     */
    private String configOrderModifyFieldConfig = "{\"14.1\":{\"name\":\"基础信息\",\"fields\":[{\"field\":\"hospitalId\",\"label\":\"医院\",\"type\":\"autocomplete\",\"required\":false,\"group\":\"hospital_doctor\"},{\"field\":\"hospitalDeptId\",\"label\":\"科室\",\"type\":\"autocomplete\",\"required\":false,\"group\":\"hospital_doctor\"},{\"field\":\"doctorId\",\"label\":\"关联医生\",\"type\":\"autocomplete\",\"required\":false,\"group\":\"hospital_doctor\"},{\"field\":\"doctorName\",\"label\":\"医生姓名\",\"type\":\"text\",\"required\":false,\"group\":\"hospital_doctor\"},{\"field\":\"doctorPhone\",\"label\":\"医生电话\",\"type\":\"text\",\"required\":false,\"group\":\"hospital_doctor\"},{\"field\":\"patientName\",\"label\":\"患者姓名\",\"type\":\"text\",\"required\":false},{\"field\":\"patientAge\",\"label\":\"患者年龄\",\"type\":\"number\",\"required\":false},{\"field\":\"patientGender\",\"label\":\"患者性别\",\"type\":\"select\",\"required\":false,\"options\":[{\"value\":\"12.1\",\"label\":\"男\"},{\"value\":\"12.2\",\"label\":\"女\"}]},{\"field\":\"isUrgent\",\"label\":\"是否加急\",\"type\":\"switch\",\"required\":false},{\"field\":\"isPostal\",\"label\":\"是否邮寄\",\"type\":\"switch\",\"required\":false},{\"field\":\"postalAddress\",\"label\":\"邮寄地址\",\"type\":\"textarea\",\"required\":false},{\"field\":\"expectedDeliveryDate\",\"label\":\"期望交付时间\",\"type\":\"datetime\",\"required\":false}]},\"14.2\":{\"name\":\"影像文件\",\"fields\":[{\"field\":\"imageDataFileIds\",\"label\":\"影像数据文件\",\"type\":\"file\",\"required\":false},{\"field\":\"imageReportFileIds\",\"label\":\"影像报告文件\",\"type\":\"file\",\"required\":false}]},\"14.3\":{\"name\":\"重建项目\",\"fields\":[{\"field\":\"items\",\"label\":\"重建项目明细\",\"type\":\"array\",\"required\":false,\"subFields\":[{\"field\":\"bodyPartId\",\"label\":\"部位\",\"type\":\"select\"},{\"field\":\"projectId\",\"label\":\"重建项目\",\"type\":\"select\"},{\"field\":\"projectDesc\",\"label\":\"项目说明\",\"type\":\"textarea\"},{\"field\":\"formingRequirement\",\"label\":\"成形需求\",\"type\":\"textarea\"},{\"field\":\"otherRequirement\",\"label\":\"其他要求\",\"type\":\"textarea\"}]}]}}";

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

    // ==================== 设计师分配配置 ====================
    /**
     * 设计师分配模式
     * auto - 自动分配（默认）
     * manual - 手动分配
     */
    private String configDesignAssignMode = "auto";

    // ==================== 设计文件配置 ====================
    /**
     * 设计文件数据包容器格式（压缩包本身允许的扩展名，逗号分隔）
     * 默认：.zip,.rar,.7z
     */
    private String configDesignPackageArchiveExtensions = ".zip,.rar,.7z,.tar";

    /**
     * 数据包内部允许的文件扩展名（逗号分隔，用于解析压缩包内容）
     * 默认：.stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp
     */
    private String configDesignPackageAllowedExtensions = ".stl,.obj,.ply,.3mf,.gcode,.ctb,.cbddlp";

    /**
     * 设计报告允许的文件扩展名（逗号分隔）
     * 默认：.pdf,.doc,.docx,.xls,.xlsx
     */
    private String configDesignReportAllowedExtensions = ".pdf,.doc,.docx,.xls,.xlsx";

    /**
     * 可视化模型允许的文件扩展名（逗号分隔）
     * 默认：.stl,.obj,.ply,.3mf
     */
    private String configDesignModelAllowedExtensions = ".stl,.obj,.ply,.3mf";

    /**
     * 设计文件数据包最大文件大小（MB）
     * 默认：500 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，此处仅作兜底
     */
    private Integer configDesignPackageMaxSizeMb = 500;

    /**
     * 设计报告最大文件大小（MB）
     * 默认：50 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，此处仅作兜底
     */
    private Integer configDesignReportMaxSizeMb = 50;

    /**
     * 可视化模型最大文件大小（MB）
     * 默认：200 MB；由 FileUploadConfigProvider 通过 configPrefix 读取，此处仅作兜底
     */
    private Integer configDesignModelMaxSizeMb = 200;

    /**
     * 设计模式（1=线下修改，2=在线编辑）
     */
    private Integer configDesignMode = 1;

    // ==================== 设计工单列配置 ====================
    /**
     * 设计工单列表默认列配置（JSON 格式）
     * 字段命名规则：design.column.config → configDesignColumnConfig
     */
    private String configDesignColumnConfig = "{\"module\":\"design\",\"columns\":[{\"field\":\"isUrgent\",\"label\":\"加急\",\"visible\":true,\"sort\":1,\"width\":70,\"fixed\":null},{\"field\":\"orderCode\",\"label\":\"订单编号\",\"visible\":true,\"sort\":2,\"width\":160,\"fixed\":null},{\"field\":\"statusName\",\"label\":\"当前状态\",\"visible\":true,\"sort\":3,\"width\":120,\"fixed\":null},{\"field\":\"businessTypeName\",\"label\":\"业务类型\",\"visible\":true,\"sort\":4,\"width\":100,\"fixed\":null},{\"field\":\"orderTypeName\",\"label\":\"订单类型\",\"visible\":true,\"sort\":5,\"width\":110,\"fixed\":null},{\"field\":\"needsPhysicalDeliveryName\",\"label\":\"实体交付\",\"visible\":true,\"sort\":6,\"width\":90,\"fixed\":null},{\"field\":\"patientName\",\"label\":\"患者姓名\",\"visible\":true,\"sort\":7,\"width\":100,\"fixed\":null},{\"field\":\"hospitalName\",\"label\":\"医院\",\"visible\":true,\"sort\":8,\"width\":180,\"fixed\":null},{\"field\":\"hospitalDeptName\",\"label\":\"科室\",\"visible\":true,\"sort\":9,\"width\":100,\"fixed\":null},{\"field\":\"doctorName\",\"label\":\"医生姓名\",\"visible\":true,\"sort\":10,\"width\":100,\"fixed\":null},{\"field\":\"areaName\",\"label\":\"地区\",\"visible\":true,\"sort\":11,\"width\":100,\"fixed\":null},{\"field\":\"rebuildProjectSummary\",\"label\":\"重建项目\",\"visible\":true,\"sort\":12,\"width\":200,\"fixed\":null},{\"field\":\"designerName\",\"label\":\"设计师\",\"visible\":true,\"sort\":13,\"width\":100,\"fixed\":null},{\"field\":\"packageCount\",\"label\":\"数据包数\",\"visible\":true,\"sort\":14,\"width\":90,\"fixed\":null},{\"field\":\"designStartTime\",\"label\":\"开始设计时间\",\"visible\":true,\"sort\":15,\"width\":160,\"fixed\":null},{\"field\":\"expectedDeliveryDate\",\"label\":\"期望交付\",\"visible\":true,\"sort\":16,\"width\":120,\"fixed\":null},{\"field\":\"createTime\",\"label\":\"创建时间\",\"visible\":true,\"sort\":17,\"width\":160,\"fixed\":null},{\"field\":\"rejectReason\",\"label\":\"驳回原因\",\"visible\":false,\"sort\":18,\"width\":160,\"fixed\":null},{\"field\":\"action\",\"label\":\"操作\",\"visible\":true,\"sort\":19,\"width\":150,\"fixed\":\"right\"}]}";
}
