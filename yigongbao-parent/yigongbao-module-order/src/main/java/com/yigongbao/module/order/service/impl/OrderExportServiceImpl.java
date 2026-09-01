package com.yigongbao.module.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.order.dto.order.OrderCustomExportDTO;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.dto.workload.DesignerWorkloadExportDTO;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.OrderExportService;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderExportFieldVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
import com.yigongbao.module.order.vo.workload.DesignerWorkloadVO;
import com.yigongbao.module.system.user.service.UserHospitalService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单导出 Service 实现类
 * 使用 SXSSFWorkbook 流式写入，避免大数据量导出时的 OOM
 *
 * @author hanjor
 * @date 2026-04-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExportServiceImpl implements OrderExportService {

    private static final int MAX_EXPORT_COUNT = 10000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderMainMapper orderMainMapper;
    private final UserHospitalService userHospitalService;
    private final OrderQueryHelper orderQueryHelper;

    @Override
    public void exportOrders(OrderExportQueryDTO dto, HttpServletResponse response) {
        // Step 1：获取当前用户的列配置（用户个人配置 > 系统默认配置）
        OrderColumnConfigVO columnConfig = orderQueryHelper.getColumnConfig();
        if (columnConfig == null || columnConfig.getColumns() == null || columnConfig.getColumns().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.MISSING_PARAMETER, "列配置");
        }

        // 过滤出 visible=true 的列，按 sort 排序
        List<OrderColumnConfigVO.ColumnItemVO> visibleColumns = columnConfig.getColumns().stream()
                .filter(col -> Boolean.TRUE.equals(col.getVisible()))
                .sorted((a, b) -> {
                    int sortA = a.getSort() != null ? a.getSort() : 0;
                    int sortB = b.getSort() != null ? b.getSort() : 0;
                    return Integer.compare(sortA, sortB);
                })
                .collect(Collectors.toList());
        if (visibleColumns.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.INVALID_PARAMETER, "列配置无可见列");
        }

        // Step 2：按数据权限查询订单（最多10000条）
        List<OrderListVO> orderList = queryOrdersForExport(dto);

        // Step 3：构建 Excel（SXSSF 流式写入）
        try {
            buildExcel(visibleColumns, orderList, response);
            // 在响应头中标注实际导出数量及是否被截断，前端可据此提示用户
            response.setHeader("X-Export-Total", String.valueOf(orderList.size()));
            response.setHeader("X-Export-Truncated", String.valueOf(orderList.size() >= MAX_EXPORT_COUNT));
            log.info("导出订单: 总数={}, 截断={}", orderList.size(), orderList.size() >= MAX_EXPORT_COUNT);
        } catch (Exception e) {
            log.error("导出订单异常", e);
            throw new BusinessException(ErrorCodeEnum.ORDER_EXPORT_FAILED);
        }
    }

    /**
     * 按数据权限查询订单列表
     */
    private List<OrderListVO> queryOrdersForExport(OrderExportQueryDTO dto) {
        Long currentUserId = orderQueryHelper.getCurrentUserId();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();

        // 根据 dataScopeType 注入数据过滤条件
        orderQueryHelper.buildDataScopeCondition(wrapper, currentUserId, scopeType);

        // hospitalId 参数校验（HOSPITALS 类型）
        if (dto.getHospitalId() != null) {
            if (scopeType == DataScopeTypeEnum.HOSPITALS) {
                List<Long> userHospitalIds = userHospitalService.getHospitalIdsByUserId(currentUserId);
                if (!userHospitalIds.contains(dto.getHospitalId())) {
                    log.warn("传入的医院ID不在用户权限范围内，返回空列表");
                    return List.of();
                }
            }
            wrapper.eq(OrderMainEntity::getHospitalId, dto.getHospitalId());
        }

        // 其他查询条件
        wrapper.like(StrUtil.isNotBlank(dto.getOrderCode()), OrderMainEntity::getOrderCode, dto.getOrderCode())
                .eq(Objects.nonNull(dto.getPhase()), OrderMainEntity::getPhase, dto.getPhase())
                .eq(Objects.nonNull(dto.getStatus()), OrderMainEntity::getStatus, dto.getStatus());

        orderQueryHelper.applySort(wrapper, dto.getSortField(), dto.getSortOrder());
        wrapper.last("LIMIT " + MAX_EXPORT_COUNT);

        // 执行查询并转换为 VO
        List<OrderListVO> voList = orderMainMapper.selectList(wrapper).stream()
                .map(orderQueryHelper::toOrderListVO)
                .collect(Collectors.toList());

        // 补充重建项目列表
        orderQueryHelper.fillRebuildProjectList(voList);

        return voList;
    }

    /**
     * 构建 Excel 文件
     */
    private void buildExcel(List<OrderColumnConfigVO.ColumnItemVO> columns,
                            List<OrderListVO> orderList,
                            HttpServletResponse response) throws IOException {
        // 使用 SXSSFWorkbook，流式写入避免 OOM
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("订单列表");

            // 设置列宽
            for (int i = 0; i < columns.size(); i++) {
                OrderColumnConfigVO.ColumnItemVO col = columns.get(i);
                int width = col.getWidth() != null ? col.getWidth() / 6 : 20;
                sheet.setColumnWidth(i, Math.min(width * 256, 255 * 256));
            }

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 构建表头行
            Row headerRow = sheet.createRow(0);
            int unsupportedColumnCount = 0;
            for (int i = 0; i < columns.size(); i++) {
                OrderColumnConfigVO.ColumnItemVO col = columns.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(col.getLabel());
                cell.setCellStyle(headerStyle);
            }

            // 填充数据行
            int rowNum = 1;
            for (OrderListVO order : orderList) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.size(); i++) {
                    OrderColumnConfigVO.ColumnItemVO col = columns.get(i);
                    Cell cell = row.createCell(i);
                    if (!setCellValue(cell, order, col.getField())) {
                        unsupportedColumnCount++;
                    }
                }
            }

            // 检测并记录未匹配到的列（辅助定位 Excel 导出字段遗漏问题）
            if (unsupportedColumnCount > 0) {
                log.warn("Excel 导出发现 {} 个字段未匹配到对应处理逻辑，请检查 setCellValue 方法",
                        unsupportedColumnCount);
            }

            // 设置响应头
            String fileName = "订单列表_" + java.time.LocalDate.now().format(DATE_FORMATTER) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            workbook.write(response.getOutputStream());
            workbook.dispose();
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * 根据字段名设置单元格值（带重建项目参数）
     */
    private boolean setCellValue(Cell cell, OrderListVO order, OrderListVO.RebuildProjectItemVO project, String field) {
        // 重建项目相关字段
        if (project != null) {
            switch (field) {
                case "projectName":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getProjectName()));
                    return true;
                case "bodyPartName":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getBodyPartName()));
                    return true;
                case "categoryName":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getCategoryName()));
                    return true;
                case "projectDesc":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getProjectDesc()));
                    return true;
                case "formingRequirement":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getFormingRequirement()));
                    return true;
                case "otherRequirement":
                    cell.setCellValue(StrUtil.nullToEmpty(project.getOtherRequirement()));
                    return true;
                case "rebuildProjectList":
                    cell.setCellValue(formatSingleRebuildProject(project));
                    return true;
            }
        }

        // 订单基础字段（调用原有逻辑）
        return setCellValue(cell, order, field);
    }

    /**
     * 格式化单个重建项目
     */
    private String formatSingleRebuildProject(OrderListVO.RebuildProjectItemVO item) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(item.getProjectName())) {
            sb.append(item.getProjectName());
        }
        if (StrUtil.isNotBlank(item.getBodyPartName())) {
            if (sb.length() > 0) sb.append("-");
            sb.append(item.getBodyPartName());
        }
        if (StrUtil.isNotBlank(item.getCategoryName())) {
            if (sb.length() > 0) {
                sb.append("(").append(item.getCategoryName()).append(")");
            } else {
                sb.append(item.getCategoryName());
            }
        }
        if (item.getCount() != null && item.getCount() > 1) {
            if (sb.length() > 0) sb.append("×").append(item.getCount());
        }
        return sb.toString();
    }

    /**
     * 根据字段名设置单元格值
     */
    /** @return true 表示字段已匹配处理，false 表示未匹配 */
    private boolean setCellValue(Cell cell, OrderListVO order, String field) {
        if (field == null) {
            return true;
        }
        switch (field) {
            case "orderCode":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOrderCode()));
                break;
            case "orderType":
            case "orderTypeName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOrderTypeName()));
                break;
            case "needsPhysicalDeliveryName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getNeedsPhysicalDeliveryName()));
                break;
            case "hospitalName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getHospitalName()));
                break;
            case "areaName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getAreaName()));
                break;
            case "fullAreaName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getFullAreaName()));
                break;
            case "hospitalDeptName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getHospitalDeptName()));
                break;
            case "doctorName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getDoctorName()));
                break;
            case "doctorPhone":
                cell.setCellValue(StrUtil.nullToEmpty(order.getDoctorPhone()));
                break;
            case "patientName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getPatientName()));
                break;
            case "patientAge":
                cell.setCellValue(order.getPatientAge() != null ? order.getPatientAge() : 0);
                break;
            case "patientGenderName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getPatientGenderName()));
                break;
            case "businessTypeName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getBusinessTypeName()));
                break;
            case "isUrgent":
                cell.setCellValue(order.getIsUrgent() != null && order.getIsUrgent() == 1 ? "是" : "否");
                break;
            case "isPostal":
                cell.setCellValue(order.getIsPostal() != null && order.getIsPostal() == 1 ? "是" : "否");
                break;
            case "isClassicCase":
                cell.setCellValue(order.getIsClassicCase() != null && order.getIsClassicCase() == 1 ? "是" : "否");
                break;
            case "postalAddress":
                cell.setCellValue(StrUtil.nullToEmpty(order.getPostalAddress()));
                break;
            case "expectedDeliveryDate":
                cell.setCellValue(order.getExpectedDeliveryDate() != null
                        ? order.getExpectedDeliveryDate().format(DATE_TIME_FORMATTER) : "");
                break;
            case "estimatedCost":
                BigDecimal cost = order.getEstimatedCost();
                cell.setCellValue(cost != null ? cost.toString() : "");
                break;
            case "dataEvaluationOpinion":
                cell.setCellValue(StrUtil.nullToEmpty(order.getDataEvaluationOpinion()));
                break;
            case "designerName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getDesignerName()));
                break;
            case "designStartTime":
                cell.setCellValue(order.getDesignStartTime() != null
                        ? order.getDesignStartTime().format(DATE_TIME_FORMATTER) : "");
                break;
            case "designSubmitTime":
                cell.setCellValue(order.getDesignSubmitTime() != null
                        ? order.getDesignSubmitTime().format(DATE_TIME_FORMATTER) : "");
                break;
            case "productionStartTime":
                cell.setCellValue(order.getProductionStartTime() != null
                        ? order.getProductionStartTime().format(DATE_TIME_FORMATTER) : "");
                break;
            case "productionEndTime":
                cell.setCellValue(order.getProductionEndTime() != null
                        ? order.getProductionEndTime().format(DATE_TIME_FORMATTER) : "");
                break;
            case "phase":
            case "phaseName":
                cell.setCellValue(StrUtil.nullToEmpty(orderQueryHelper.getPhaseName(order.getPhase())));
                break;
            case "status":
            case "statusName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getStatusName()));
                break;
            case "operatorName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOperatorName()));
                break;
            case "operatorPhone":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOperatorPhone()));
                break;
            case "operatorDeptName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOperatorDeptName()));
                break;
            case "orgName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOrgName()));
                break;
            case "createTime":
                cell.setCellValue(order.getCreateTime() != null
                        ? order.getCreateTime().format(DATE_TIME_FORMATTER) : "");
                break;
            case "rebuildProjectList":
                String projects = formatRebuildProjectList(order);
                cell.setCellValue(projects);
                break;
            default:
                cell.setCellValue("");
                return false;
        }
        return true;
    }

    /**
     * 格式化重建项目列表
     */
    private String formatRebuildProjectList(OrderListVO order) {
        if (order.getRebuildProjectList() == null || order.getRebuildProjectList().isEmpty()) {
            return "";
        }
        return order.getRebuildProjectList().stream()
                .map(item -> {
                    StringBuilder sb = new StringBuilder();
                    if (StrUtil.isNotBlank(item.getProjectName())) {
                        sb.append(item.getProjectName());
                    }
                    if (StrUtil.isNotBlank(item.getBodyPartName())) {
                        if (sb.length() > 0) {
                            sb.append("-");
                        }
                        sb.append(item.getBodyPartName());
                    }
                    if (StrUtil.isNotBlank(item.getCategoryName())) {
                        if (sb.length() > 0) {
                            sb.append("(").append(item.getCategoryName()).append(")");
                        } else {
                            sb.append(item.getCategoryName());
                        }
                    }
                    if (item.getCount() != null && item.getCount() > 1) {
                        if (sb.length() > 0) {
                            sb.append("×").append(item.getCount());
                        }
                    }
                    return sb.toString();
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("；"));
    }

    @Override
    public void customExportOrders(OrderCustomExportDTO dto, HttpServletResponse response) {
        Long currentUserId = orderQueryHelper.getCurrentUserId();
        DataScopeTypeEnum scopeType = userHospitalService.getDataScopeType(currentUserId);

        // 先注入统一数据权限条件，再叠加自定义导出的时间范围和数量限制
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<>();
        orderQueryHelper.buildDataScopeCondition(wrapper, currentUserId, scopeType);
        wrapper
                .ge(dto.getCreateTimeStart() != null, OrderMainEntity::getCreateTime, dto.getCreateTimeStart())
                .le(dto.getCreateTimeEnd() != null, OrderMainEntity::getCreateTime, dto.getCreateTimeEnd())
                .orderByDesc(OrderMainEntity::getCreateTime)
                .last("LIMIT " + MAX_EXPORT_COUNT);

        List<OrderMainEntity> orderEntities = orderMainMapper.selectList(wrapper);
        List<OrderListVO> orderList = orderEntities.stream()
                .map(orderQueryHelper::toOrderListVO)
                .collect(Collectors.toList());

        // 填充重建项目列表
        orderQueryHelper.fillRebuildProjectList(orderList);

        // 构建字段标签映射
        java.util.Map<String, String> fieldLabels = dto.getFieldLabels() != null
                ? dto.getFieldLabels()
                : getDefaultFieldLabels();

        // 构建Excel
        try {
            buildCustomExcel(dto.getExportFields(), fieldLabels, orderList, response);
        } catch (IOException e) {
            log.error("自定义导出订单失败", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private void buildCustomExcel(List<String> exportFields, java.util.Map<String, String> fieldLabels,
                                   List<OrderListVO> orderList, HttpServletResponse response) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("订单列表");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 构建表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < exportFields.size(); i++) {
                String field = exportFields.get(i);
                String label = fieldLabels.getOrDefault(field, field);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(label);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // 填充数据（按重建项目拆分行）
            int rowNum = 1;
            for (OrderListVO order : orderList) {
                if (order.getRebuildProjectList() == null || order.getRebuildProjectList().isEmpty()) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < exportFields.size(); i++) {
                        Cell cell = row.createCell(i);
                        setCellValue(cell, order, null, exportFields.get(i));
                    }
                } else {
                    for (OrderListVO.RebuildProjectItemVO project : order.getRebuildProjectList()) {
                        Row row = sheet.createRow(rowNum++);
                        for (int i = 0; i < exportFields.size(); i++) {
                            Cell cell = row.createCell(i);
                            setCellValue(cell, order, project, exportFields.get(i));
                        }
                    }
                }
            }

            // 设置响应头
            String fileName = "订单列表_" + java.time.LocalDate.now().format(DATE_FORMATTER) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            workbook.write(response.getOutputStream());
            workbook.dispose();
        }
    }

    private java.util.Map<String, String> getDefaultFieldLabels() {
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        labels.put("orderCode", "订单编号");
        labels.put("orderTypeName", "订单类型");
        labels.put("needsPhysicalDeliveryName", "是否需要实体交付");
        labels.put("hospitalName", "医院名称");
        labels.put("areaName", "地区");
        labels.put("fullAreaName", "完整地区路径");
        labels.put("hospitalDeptName", "医院科室");
        labels.put("doctorName", "医生姓名");
        labels.put("doctorPhone", "医生电话");
        labels.put("patientName", "患者姓名");
        labels.put("patientAge", "患者年龄");
        labels.put("patientGenderName", "患者性别");
        labels.put("businessTypeName", "业务类型");
        labels.put("isUrgent", "是否加急");
        labels.put("isPostal", "是否邮寄");
        labels.put("isClassicCase", "是否经典案例");
        labels.put("postalAddress", "邮寄地址");
        labels.put("expectedDeliveryDate", "期望交付时间");
        labels.put("estimatedCost", "预计费用");
        labels.put("dataEvaluationOpinion", "影像评估意见");
        labels.put("designerName", "设计师");
        labels.put("designStartTime", "设计开始时间");
        labels.put("designSubmitTime", "设计提交时间");
        labels.put("productionStartTime", "生产开始时间");
        labels.put("productionEndTime", "生产结束时间");
        labels.put("phase", "阶段");
        labels.put("phaseName", "阶段");
        labels.put("statusName", "状态");
        labels.put("operatorName", "业务员");
        labels.put("operatorPhone", "业务员电话");
        labels.put("operatorDeptName", "业务员部门");
        labels.put("orgName", "提单机构");
        labels.put("createTime", "创建时间");
        labels.put("rebuildProjectList", "重建项目");
        labels.put("projectName", "重建项目名称");
        labels.put("bodyPartName", "重建部位");
        labels.put("categoryName", "项目分类");
        labels.put("projectDesc", "项目说明");
        labels.put("formingRequirement", "成形需求");
        labels.put("otherRequirement", "其他要求");
        return labels;
    }

    @Override
    public List<OrderExportFieldVO> getAvailableExportFields() {
        List<OrderExportFieldVO> fields = new java.util.ArrayList<>();
        fields.add(new OrderExportFieldVO("orderCode", "订单编号"));
        fields.add(new OrderExportFieldVO("orderTypeName", "订单类型"));
        fields.add(new OrderExportFieldVO("needsPhysicalDeliveryName", "是否需要实体交付"));
        fields.add(new OrderExportFieldVO("hospitalName", "医院名称"));
        fields.add(new OrderExportFieldVO("areaName", "地区"));
        fields.add(new OrderExportFieldVO("fullAreaName", "完整地区路径"));
        fields.add(new OrderExportFieldVO("hospitalDeptName", "医院科室"));
        fields.add(new OrderExportFieldVO("doctorName", "医生姓名"));
        fields.add(new OrderExportFieldVO("doctorPhone", "医生电话"));
        fields.add(new OrderExportFieldVO("patientName", "患者姓名"));
        fields.add(new OrderExportFieldVO("patientAge", "患者年龄"));
        fields.add(new OrderExportFieldVO("patientGenderName", "患者性别"));
        fields.add(new OrderExportFieldVO("businessTypeName", "业务类型"));
        fields.add(new OrderExportFieldVO("isUrgent", "是否加急"));
        fields.add(new OrderExportFieldVO("isPostal", "是否邮寄"));
        fields.add(new OrderExportFieldVO("isClassicCase", "是否经典案例"));
        fields.add(new OrderExportFieldVO("postalAddress", "邮寄地址"));
        fields.add(new OrderExportFieldVO("expectedDeliveryDate", "期望交付时间"));
        fields.add(new OrderExportFieldVO("estimatedCost", "预计费用"));
        fields.add(new OrderExportFieldVO("dataEvaluationOpinion", "影像评估意见"));
        fields.add(new OrderExportFieldVO("designerName", "设计师"));
        fields.add(new OrderExportFieldVO("designStartTime", "设计开始时间"));
        fields.add(new OrderExportFieldVO("designSubmitTime", "设计提交时间"));
        fields.add(new OrderExportFieldVO("productionStartTime", "生产开始时间"));
        fields.add(new OrderExportFieldVO("productionEndTime", "生产结束时间"));
        fields.add(new OrderExportFieldVO("phaseName", "阶段"));
        fields.add(new OrderExportFieldVO("statusName", "状态"));
        fields.add(new OrderExportFieldVO("operatorName", "业务员"));
        fields.add(new OrderExportFieldVO("operatorPhone", "业务员电话"));
        fields.add(new OrderExportFieldVO("operatorDeptName", "业务员部门"));
        fields.add(new OrderExportFieldVO("orgName", "提单机构"));
        fields.add(new OrderExportFieldVO("createTime", "创建时间"));
        fields.add(new OrderExportFieldVO("rebuildProjectList", "重建项目"));
        fields.add(new OrderExportFieldVO("projectName", "重建项目名称"));
        fields.add(new OrderExportFieldVO("bodyPartName", "重建部位"));
        fields.add(new OrderExportFieldVO("categoryName", "项目分类"));
        fields.add(new OrderExportFieldVO("projectDesc", "项目说明"));
        fields.add(new OrderExportFieldVO("formingRequirement", "成形需求"));
        fields.add(new OrderExportFieldVO("otherRequirement", "其他要求"));
        return fields;
    }

    @Override
    public void exportDesignerWorkload(DesignerWorkloadExportDTO dto, HttpServletResponse response) {
        List<DesignerWorkloadVO> workloadList =
            orderMainMapper.statisticsDesignerWorkload(dto.getCreateTimeStart(), dto.getCreateTimeEnd());

        if (workloadList == null || workloadList.isEmpty()) {
            log.warn("设计师工作量统计为空: startTime={}, endTime={}", dto.getCreateTimeStart(), dto.getCreateTimeEnd());
            throw new BusinessException(ErrorCodeEnum.DATA_NOT_FOUND, "统计数据");
        }

        int totalCaseCount = workloadList.stream().mapToInt(DesignerWorkloadVO::getCaseCount).sum();
        int totalPoints = workloadList.stream().mapToInt(DesignerWorkloadVO::getTotalPoints).sum();

        if (totalCaseCount == 0) {
            log.error("设计师工作量统计案例数为0，数据异常");
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR, "统计数据异常");
        }

        workloadList.forEach(vo -> {
            vo.setCaseCountRate(BigDecimal.valueOf(vo.getCaseCount() * 100.0 / totalCaseCount)
                .setScale(2, RoundingMode.HALF_UP));

            if (totalPoints > 0) {
                vo.setTotalPointsRate(BigDecimal.valueOf(vo.getTotalPoints() * 100.0 / totalPoints)
                    .setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setTotalPointsRate(BigDecimal.ZERO);
            }
        });

        try {
            buildWorkloadExcel(workloadList, totalCaseCount, totalPoints, response);
            log.info("导出设计师工作量统计: 设计师数量={}, 总案例数={}, 总分值={}",
                workloadList.size(), totalCaseCount, totalPoints);
        } catch (IOException e) {
            log.error("导出设计师工作量统计失败", e);
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
    }

    private void buildWorkloadExcel(List<DesignerWorkloadVO> workloadList,
                                     int totalCaseCount, int totalPoints,
                                     HttpServletResponse response) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            SXSSFSheet sheet = workbook.createSheet("设计师工作量统计");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"设计师", "案例数", "案例数占比", "分值总数", "分值占比"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowNum = 1;
            for (DesignerWorkloadVO vo : workloadList) {
                Row row = sheet.createRow(rowNum++);
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(StrUtil.nullToEmpty(vo.getDesignerName()));
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(vo.getCaseCount());
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(vo.getCaseCountRate().toString() + "%");
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(vo.getTotalPoints());
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(vo.getTotalPointsRate().toString() + "%");
                cell4.setCellStyle(dataStyle);
            }

            Row totalRow = sheet.createRow(rowNum);
            Cell totalCell = totalRow.createCell(0);
            totalCell.setCellValue("合计");
            totalCell.setCellStyle(headerStyle);
            Cell totalCaseCell = totalRow.createCell(1);
            totalCaseCell.setCellValue(totalCaseCount);
            totalCaseCell.setCellStyle(headerStyle);
            Cell totalCaseRateCell = totalRow.createCell(2);
            totalCaseRateCell.setCellValue("100.00%");
            totalCaseRateCell.setCellStyle(headerStyle);
            Cell totalPointsCell = totalRow.createCell(3);
            totalPointsCell.setCellValue(totalPoints);
            totalPointsCell.setCellStyle(headerStyle);
            Cell totalPointsRateCell = totalRow.createCell(4);
            totalPointsRateCell.setCellValue("100.00%");
            totalPointsRateCell.setCellStyle(headerStyle);

            String fileName = "设计师工作量统计_" + java.time.LocalDate.now().format(DATE_FORMATTER) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

            workbook.write(response.getOutputStream());
            workbook.dispose();
        }
    }

    /**
     * 创建数据行样式（居中）
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

}

