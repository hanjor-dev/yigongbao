package com.yigongbao.module.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.common.enums.DataScopeTypeEnum;
import com.yigongbao.common.enums.ErrorCodeEnum;
import com.yigongbao.common.exception.BusinessException;
import com.yigongbao.module.order.dto.order.OrderExportQueryDTO;
import com.yigongbao.module.order.helper.OrderQueryHelper;
import com.yigongbao.module.order.mapper.OrderMainMapper;
import com.yigongbao.module.order.service.OrderExportService;
import com.yigongbao.module.order.vo.order.OrderColumnConfigVO;
import com.yigongbao.module.order.vo.order.OrderListVO;
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
            case "hospitalName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getHospitalName()));
                break;
            case "areaName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getAreaName()));
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
            case "designerName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getDesignerName()));
                break;
            case "phase":
                cell.setCellValue(StrUtil.nullToEmpty(orderQueryHelper.getPhaseName(order.getPhase())));
                break;
            case "status":
            case "statusName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getStatusName()));
                break;
            case "operatorName":
                cell.setCellValue(StrUtil.nullToEmpty(order.getOperatorName()));
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

}

