package com.yigongbao.module.order.dto.modify;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 执行订单修改 DTO
 * <p>
 * 按申请类型分三个子结构，只需传入本次申请对应类型的字段：
 * <ul>
 *   <li><b>infoFields</b>（14.1 基础信息）：差量列表，只传要修改的字段；
 *       field 取值见 sys_config key=order.modify.field.config 的 "14.1".fields[].field</li>
 *   <li><b>items</b>（14.3 重建项目）：全量替换，列表即为最终状态；
 *       有 orderItemId 表示修改已有项目，null 表示新增；不在列表内的旧项目自动删除；
 *       item 内 field 取值见配置 "14.3".fields[].field</li>
 *   <li><b>imageDataFileIds / imageReportFileIds</b>（14.2 影像文件）：全量替换文件ID列表；
 *       两者共用 14.2 申请类型控制，各自传 null 表示不修改该类别</li>
 * </ul>
 *
 * @author hanjor
 * @date 2026-04-12
 */
@Data
public class ExecuteModifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 基础信息修改字段列表（14.1）
     * <p>
     * 仅传需要修改的字段，field 字段名须与 sys_config 配置中 "14.1".fields[].field 匹配，
     * 白名单外的字段名会被忽略。
     * <p>
     * 示例：[{"field":"patientName","value":"李四"},{"field":"isUrgent","value":1}]
     */
    private List<ModifyField> infoFields;

    /**
     * 重建项目列表（14.3）
     * <p>
     * 全量替换：传入即为最终状态。orderItemId 不为 null 时修改已有项目，null 时新增。
     * 不在本列表内的旧项目将被删除。item 内 field 须与配置 "14.3".fields[].field 匹配。
     * <p>
     * 示例：[{"orderItemId":123,"fields":[{"field":"projectDesc","value":"新描述"}]}]
     */
    private List<ModifyItem> items;

    /**
     * 影像数据文件ID列表（14.2，IMAGE_DATA 类别）
     * <p>
     * 申请类型包含 14.2 时生效；全量替换；传空列表表示清空；传 null 表示不修改。
     */
    private List<String> imageDataFileIds;

    /**
     * 影像报告文件ID列表（14.2，IMAGE_REPORT 类别）
     * <p>
     * 申请类型包含 14.2 时生效；全量替换；传空列表表示清空；传 null 表示不修改。
     */
    private List<String> imageReportFileIds;

    // ==================== 内部类 ====================

    /**
     * 单个字段修改条目
     */
    @Data
    public static class ModifyField {

        /**
         * 字段名（须与 sys_config order.modify.field.config 对应类型的 fields[].field 一致）
         */
        @NotBlank(message = "字段名不能为空")
        private String field;

        /**
         * 字段新值
         */
        private Object value;
    }

    /**
     * 重建项目条目（全量替换列表中的一项）
     */
    @Data
    public static class ModifyItem {

        /**
         * 已有项目ID（null 表示新增，非 null 表示修改）
         */
        private Long orderItemId;

        /**
         * 该项目的字段列表
         */
        private List<ModifyField> fields;
    }
}
