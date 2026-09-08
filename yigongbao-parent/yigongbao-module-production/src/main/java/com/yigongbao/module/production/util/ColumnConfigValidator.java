package com.yigongbao.module.production.util;

import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 列配置业务规则校验。
 */
public final class ColumnConfigValidator {

    private static final Map<String, Set<String>> ALLOWED_FIELDS = Map.of(
            "quality", Set.of(
                    "recordNo", "designPackageCode", "productionBatchNo", "orderCode", "publicOrderCode",
                    "hospitalName", "hospitalDeptName", "doctorName", "patientName",
                    "isUrgent", "isPostal", "expectedDeliveryDate", "orgName",
                    "totalProductCount", "qualifiedCount", "unqualifiedCount", "pendingCount",
                    "status", "createTime", "action"),
            "warehouse", Set.of(
                    "recordNo", "designPackageCode", "status", "productionBatchNo", "orderNo",
                    "hospitalName", "hospitalDeptName", "doctorName", "patientName",
                    "isUrgent", "isPostal", "expectedDeliveryDate", "totalCount",
                    "warehouseCountSummary", "earliestInTime", "latestOutTime", "action")
    );

    private ColumnConfigValidator() {
    }

    public static void validate(String module, List<? extends ColumnConfigItem> columns) {
        if (columns == null) {
            throw invalid("列配置不能为空");
        }

        Set<String> allowedFields = ALLOWED_FIELDS.get(module);
        if (allowedFields == null) {
            throw invalid("不支持的列配置模块");
        }

        Set<String> fields = new HashSet<>();
        Set<Integer> sorts = new HashSet<>();
        for (ColumnConfigItem item : columns) {
            if (item == null) {
                throw invalid("列配置项不能为空");
            }
            if (isBlank(item.getField())) {
                throw invalid("字段名不能为空");
            }
            if (!allowedFields.contains(item.getField())) {
                throw invalid("不支持的字段：" + item.getField());
            }
            if (!fields.add(item.getField())) {
                throw invalid("字段重复：" + item.getField());
            }
            if (isBlank(item.getLabel())) {
                throw invalid("列标题不能为空");
            }
            if (item.getVisible() == null) {
                throw invalid("是否可见不能为空");
            }
            if (item.getSort() == null || item.getSort() <= 0) {
                throw invalid("排序序号必须为正数");
            }
            if (!sorts.add(item.getSort())) {
                throw invalid("排序序号重复：" + item.getSort());
            }
            if (item.getWidth() != null && item.getWidth() <= 0) {
                throw invalid("列宽度必须为正数");
            }
            if (item.getFixed() != null
                    && !"left".equals(item.getFixed())
                    && !"right".equals(item.getFixed())) {
                throw invalid("固定位置只能是 left 或 right");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCodeEnum.INVALID_PARAMETER.getCode(), message);
    }
}
