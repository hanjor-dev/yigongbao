package com.yigongbao.module.production.helper;

import cn.hutool.core.util.StrUtil;
import com.yigongbao.module.production.enums.ProcessTypeEnum;
import com.yigongbao.module.production.enums.ProductStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 生产产品台账Excel导出构建器
 * <p>
 * 负责将生产产品台账数据转换为Excel格式，支持：
 * 1. 产品级别的详细数据导出（41个字段）
 * 2. 超过1万条数据时顶部显示红色警告
 * 3. 枚举值自动转换为中文描述
 * 4. NULL值和超长文本的安全处理
 * </p>
 *
 * @author hanjor
 * @date 2026-06-22
 */
@Slf4j
@Component
public class ProductLedgerExcelBuilder {

    /** 日期时间格式化器：yyyy-MM-dd HH:mm:ss */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 日期格式化器：yyyy-MM-dd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** Excel单元格最大字符数限制 */
    private static final int EXCEL_CELL_MAX_LENGTH = 32767;

    /**
     * 构建生产产品台账Excel文件
     *
     * @param dataList   产品台账数据列表（来自Mapper查询结果）
     * @param totalCount 查询结果总数（用于判断是否超过1万条限制）
     * @return Excel文件字节数组
     * @throws IOException POI生成Excel时可能抛出的IO异常
     */
    public byte[] build(List<Map<String, Object>> dataList, long totalCount) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("生产产品台账");
            int rowIndex = 0;

            // 如果查询结果超过1万条，在顶部插入红色警告行
            if (totalCount > 10000) {
                Row warningRow = sheet.createRow(rowIndex++);
                Cell warningCell = warningRow.createCell(0);
                warningCell.setCellValue("⚠️ 警告：查询结果共 " + totalCount + " 条，已超过1万条上限，当前仅导出前10000条数据，请缩小查询条件范围！");
                CellStyle warningStyle = createWarningStyle(workbook);
                warningCell.setCellStyle(warningStyle);
                // 合并单元格，跨越所有列（0-40，共41列）
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 40));
                rowIndex++; // 空一行
            }

            // 创建表头行
            Row headerRow = sheet.createRow(rowIndex++);
            CellStyle headerStyle = createHeaderStyle(workbook);
            String[] headers = {
                "产品编号", "产品名称", "型号规格", "材质", "颜色", "注册证号", "打印文件名", "UDI码",
                "产品状态", "当前工序", "订单编号", "订单类型", "医院", "科室", "医生", "患者",
                "是否加急", "是否邮寄", "期望交付时间", "流转卡编号", "生产批号",
                "设计数据包编号", "加工中心", "打印机编号", "打印开始时间", "打印完成时间", "原材料批号",
                "打印操作员", "清洗操作员", "固化操作员", "包装操作员", "包装时间",
                "质检结果", "质检员", "质检时间", "质检备注", "入库时间", "入库人", "出库时间", "出库人", "创建时间"
            };
            // 填充表头并设置列宽（统一4000，约20个汉字）
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4500);
            }

            // 填充数据行
            CellStyle dataStyle = createDataStyle(workbook);
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowIndex++);
                int colIndex = 0;
                // 按表头顺序填充各字段，注意枚举转换和格式化
                setCell(row, colIndex++, data.get("product_no"), dataStyle);
                setCell(row, colIndex++, data.get("product_name"), dataStyle);
                setCell(row, colIndex++, data.get("spec_name"), dataStyle);
                setCell(row, colIndex++, data.get("material_name"), dataStyle);
                setCell(row, colIndex++, data.get("color_name"), dataStyle);
                setCell(row, colIndex++, data.get("cert_no"), dataStyle);
                setCell(row, colIndex++, data.get("file_name"), dataStyle);
                setCell(row, colIndex++, data.get("udi_code"), dataStyle);
                setCell(row, colIndex++, convertProductStatus(data.get("product_status")), dataStyle); // 枚举转中文
                setCell(row, colIndex++, convertProcessType(data.get("current_process_type")), dataStyle); // 枚举转中文
                setCell(row, colIndex++, data.get("order_code"), dataStyle);
                setCell(row, colIndex++, convertOrderType(data.get("order_type")), dataStyle); // 1=医疗器械/2=非医疗器械
                setCell(row, colIndex++, data.get("hospital_name"), dataStyle);
                setCell(row, colIndex++, data.get("hospital_dept_name"), dataStyle);
                setCell(row, colIndex++, data.get("doctor_name"), dataStyle);
                setCell(row, colIndex++, data.get("patient_name"), dataStyle);
                setCell(row, colIndex++, convertYesNo(data.get("is_urgent")), dataStyle); // 0=否/1=是
                setCell(row, colIndex++, convertYesNo(data.get("is_postal")), dataStyle); // 0=否/1=是
                setCell(row, colIndex++, formatDateTime(data.get("expected_delivery_date")), dataStyle);
                setCell(row, colIndex++, data.get("record_no"), dataStyle);
                setCell(row, colIndex++, data.get("production_batch_no"), dataStyle);
                setCell(row, colIndex++, data.get("design_package_code"), dataStyle);
                setCell(row, colIndex++, data.get("processing_center_name"), dataStyle);
                setCell(row, colIndex++, data.get("print_device_code"), dataStyle);
                setCell(row, colIndex++, formatDateTime(data.get("print_start_time")), dataStyle);
                setCell(row, colIndex++, formatDateTime(data.get("print_finish_time")), dataStyle);
                setCell(row, colIndex++, data.get("material_batch_no"), dataStyle);
                setCell(row, colIndex++, data.get("print_operator"), dataStyle); // 子查询结果
                setCell(row, colIndex++, data.get("wash_operator"), dataStyle); // 子查询结果
                setCell(row, colIndex++, data.get("cure_operator"), dataStyle); // 子查询结果
                setCell(row, colIndex++, data.get("pack_operator_name"), dataStyle);
                setCell(row, colIndex++, formatDateTime(data.get("pack_time")), dataStyle);
                setCell(row, colIndex++, convertQcResult(data.get("qc_result")), dataStyle); // pass=合格/fail=不合格
                setCell(row, colIndex++, data.get("qc_user_name"), dataStyle); // LEFT JOIN获取
                setCell(row, colIndex++, formatDateTime(data.get("qc_time")), dataStyle);
                setCell(row, colIndex++, data.get("qc_remark"), dataStyle);
                setCell(row, colIndex++, formatDateTime(data.get("warehouse_in_time")), dataStyle);
                setCell(row, colIndex++, data.get("warehouse_in_user_name"), dataStyle); // LEFT JOIN获取
                setCell(row, colIndex++, formatDateTime(data.get("warehouse_out_time")), dataStyle);
                setCell(row, colIndex++, data.get("warehouse_out_user_name"), dataStyle); // LEFT JOIN获取
                setCell(row, colIndex++, formatDateTime(data.get("create_time")), dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建警告样式（红色加粗字体）
     */
    private CellStyle createWarningStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建表头样式（加粗、灰色背景、边框）
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 创建数据样式（左对齐、边框）
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 设置单元格值（带NULL和超长文本防护）
     * <p>
     * 安全处理：
     * 1. NULL值转空字符串
     * 2. 超过32767字符自动截断（Excel单元格限制）
     * </p>
     */
    private void setCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        String cellValue = "";
        if (value != null) {
            String strValue = String.valueOf(value);
            // Excel单元格最大32767字符，超出部分截断
            cellValue = strValue.length() > EXCEL_CELL_MAX_LENGTH ? strValue.substring(0, EXCEL_CELL_MAX_LENGTH) : strValue;
        }
        cell.setCellValue(cellValue);
        cell.setCellStyle(style);
    }

    /**
     * 转换产品状态枚举为中文描述
     * <p>
     * in_process=生产中, fail=不合格, pass=合格, completed=已完成, cancelled=已取消
     * </p>
     */
    private String convertProductStatus(Object status) {
        if (status == null) return "";
        try {
            ProductStatusEnum statusEnum = ProductStatusEnum.getByCode(String.valueOf(status));
            return statusEnum != null ? statusEnum.getDesc() : String.valueOf(status);
        } catch (Exception e) {
            // 枚举转换失败时返回原始值，不中断流程
            return String.valueOf(status);
        }
    }

    /**
     * 转换工序类型枚举为中文描述
     * <p>
     * print=打印, wash=清洗, cure=固化, clean_dry=清洁干燥, pack=包装
     * </p>
     */
    private String convertProcessType(Object processType) {
        if (processType == null) return "";
        try {
            ProcessTypeEnum typeEnum = ProcessTypeEnum.getByCode(String.valueOf(processType));
            return typeEnum != null ? typeEnum.getDesc() : String.valueOf(processType);
        } catch (Exception e) {
            // 枚举转换失败时返回原始值，不中断流程
            return String.valueOf(processType);
        }
    }

    /** 转换订单类型：1=医疗器械，2=非医疗器械 */
    private String convertOrderType(Object orderType) {
        if (orderType == null) return "";
        return "1".equals(String.valueOf(orderType)) ? "医疗器械" : "非医疗器械";
    }

    /** 转换是/否标识：1=是，0=否 */
    private String convertYesNo(Object value) {
        if (value == null) return "否";
        return "1".equals(String.valueOf(value)) ? "是" : "否";
    }

    /** 转换质检结果：pass=合格，fail=不合格 */
    private String convertQcResult(Object qcResult) {
        if (qcResult == null) return "";
        return "pass".equals(String.valueOf(qcResult)) ? "合格" : "不合格";
    }

    /**
     * 格式化日期时间为字符串
     * <p>
     * 格式：yyyy-MM-dd HH:mm:ss
     * </p>
     */
    private String formatDateTime(Object datetime) {
        if (datetime == null) return "";
        if (datetime instanceof LocalDateTime) {
            return ((LocalDateTime) datetime).format(DATETIME_FORMATTER);
        }
        return String.valueOf(datetime);
    }
}
