package com.yigongbao.module.order.enums;

import com.yigongbao.module.order.dto.modify.ExecuteModifyDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单修改类型枚举
 * 字典编码对应 sys_dict group=14：14.1（基础信息）/ 14.2（影像文件）/ 14.3（重建项目）
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Getter
@AllArgsConstructor
public enum ModifyApplyTypeEnum {

    INFO("14.1", "基础信息") {
        @Override
        public boolean isProvided(ExecuteModifyDTO dto) {
            return dto != null && dto.getInfoFields() != null && !dto.getInfoFields().isEmpty();
        }
    },
    IMAGE("14.2", "影像文件") {
        @Override
        public boolean isProvided(ExecuteModifyDTO dto) {
            return dto != null
                    && (dto.getImageDataFileIds() != null || dto.getImageReportFileIds() != null);
        }
    },
    ITEM("14.3", "重建项目") {
        @Override
        public boolean isProvided(ExecuteModifyDTO dto) {
            return dto != null && dto.getItems() != null;
        }
    };

    /**
     * 字典编码（存储到数据库，对应 sys_dict.dict_code）
     */
    private final String dictCode;

    /**
     * 类型名称（中文描述）
     */
    private final String name;

    /**
     * 判断 ExecuteModifyDTO 中是否提供了本类型对应的修改内容
     * 各枚举值覆盖此方法，定义各自的"已提交"判断规则
     *
     * @param dto 执行修改 DTO
     * @return true 表示已提供内容，false 表示缺失
     */
    public abstract boolean isProvided(ExecuteModifyDTO dto);

    /**
     * 根据字典编码获取枚举
     *
     * @param dictCode 字典编码
     * @return 枚举实例，未找到返回 null
     */
    public static ModifyApplyTypeEnum getByDictCode(String dictCode) {
        if (dictCode == null) {
            return null;
        }
        for (ModifyApplyTypeEnum type : values()) {
            if (type.getDictCode().equals(dictCode)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 将逗号分隔的字典编码转换为中文描述（顿号连接）
     * 例如："14.1,14.3" → "基础信息、重建项目"
     *
     * @param dictCodes 逗号分隔的字典编码
     * @return 中文名称拼接
     */
    public static String toNamesText(String dictCodes) {
        if (dictCodes == null || dictCodes.isBlank()) {
            return "";
        }
        return Arrays.stream(dictCodes.split(","))
                .map(String::trim)
                .map(ModifyApplyTypeEnum::getByDictCode)
                .filter(Objects::nonNull)
                .map(ModifyApplyTypeEnum::getName)
                .collect(Collectors.joining("、"));
    }
}
