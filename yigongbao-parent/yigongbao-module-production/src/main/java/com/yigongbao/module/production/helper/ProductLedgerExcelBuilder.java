package com.yigongbao.module.production.helper;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 生产产品台账 Excel 导出构建器。
 */
@Component
public class ProductLedgerExcelBuilder {

    private static final String SHEET_NAME = "生产产品台账";
    private static final int DEFAULT_COLUMN_WIDTH = 4500;
    private static final int PRODUCT_NAME_COLUMN_INDEX = 5;
    private static final int PRODUCT_NAME_COLUMN_WIDTH = 6000;
    private static final int EXCEL_CELL_MAX_LENGTH = 32767;
    private static final int LAST_COLUMN_INDEX = 21;
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final List<DateTimeFormatter> DATE_TIME_INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
    private static final List<DateTimeFormatter> DATE_INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    private static final String[] HEADERS = {
        "序号", "订单流水号", "时间", "产品编号", "数据文件名称", "产品名称", "型号/规格", "材质",
        "打印时长", "总重量（g）", "处理时长", "数量", "医院", "患者", "性别", "年龄", "操作人员", "医生",
        "科室", "业务员", "出库情况", "备注"
    };

    /**
     * 构建生产产品台账 Excel 文件。
     *
     * @param dataList   Mapper 返回的台账数据
     * @param totalCount 查询结果总数
     * @return Excel 文件字节数组
     * @throws IOException POI 写入失败时抛出
     */
    public byte[] build(List<Map<String, Object>> dataList, long totalCount) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            int rowIndex = 0;

            if (totalCount > 10000) {
                Row warningRow = sheet.createRow(rowIndex++);
                Cell warningCell = warningRow.createCell(0);
                setStringValue(warningCell,
                        "⚠️ 警告：查询结果共 " + totalCount
                                + " 条，已超过1万条上限，当前仅导出前10000条数据，请缩小查询条件范围！");
                warningCell.setCellStyle(createWarningStyle(workbook));
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, LAST_COLUMN_INDEX));
                rowIndex++;
            }

            Row headerRow = sheet.createRow(rowIndex++);
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i,
                        i == PRODUCT_NAME_COLUMN_INDEX ? PRODUCT_NAME_COLUMN_WIDTH : DEFAULT_COLUMN_WIDTH);
            }

            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle weightStyle = createWeightStyle(workbook, dataStyle);
            int sequence = 1;
            for (Map<String, Object> data : dataList) {
                Row row = sheet.createRow(rowIndex++);
                setNumericCell(row, 0, sequence++, dataStyle);
                setStringCell(row, 1, data.get("order_code"), dataStyle);
                setStringCell(row, 2, formatOrderDate(data.get("order_create_time")), dataStyle);
                setStringCell(row, 3, data.get("product_no"), dataStyle);
                setStringCell(row, 4, formatFileName(data.get("file_name")), dataStyle);
                setStringCell(row, 5, data.get("product_name"), dataStyle);
                setStringCell(row, 6, data.get("spec_name"), dataStyle);
                setStringCell(row, 7,
                        combineMaterial(data.get("color_name"), data.get("material_name")), dataStyle);
                setStringCell(row, 8, formatPrintDuration(data.get("print_duration_seconds")), dataStyle);
                setNullableNumericCell(row, 9, data.get("weight"), weightStyle);
                setStringCell(row, 10,
                        formatProcessingDuration(data.get("processing_duration_seconds")), dataStyle);
                setNumericCell(row, 11, 1, dataStyle);
                setStringCell(row, 12, data.get("hospital_name"), dataStyle);
                setStringCell(row, 13, data.get("patient_name"), dataStyle);
                setStringCell(row, 14, formatGender(data.get("patient_gender")), dataStyle);
                setNullableNumericCell(row, 15, data.get("patient_age"), dataStyle);
                setStringCell(row, 16, data.get("producer_name"), dataStyle);
                setStringCell(row, 17, data.get("doctor_name"), dataStyle);
                setStringCell(row, 18, data.get("hospital_dept_name"), dataStyle);
                setStringCell(row, 19, data.get("business_operator"), dataStyle);
                setStringCell(row, 20,
                        data.get("warehouse_out_time") == null ? "未出库" : "已出库", dataStyle);
                setStringCell(row, 21, "", dataStyle);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

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

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        applyBorders(style);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        applyBorders(style);
        return style;
    }

    private CellStyle createWeightStyle(Workbook workbook, CellStyle dataStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(dataStyle);
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("0.00"));
        return style;
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void setStringCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        setStringValue(cell, value == null ? "" : String.valueOf(value));
        cell.setCellStyle(style);
    }

    private void setStringValue(Cell cell, String value) {
        String safeValue = value.length() > EXCEL_CELL_MAX_LENGTH
                ? value.substring(0, EXCEL_CELL_MAX_LENGTH)
                : value;
        cell.setCellValue(safeValue);
    }

    private void setNumericCell(Row row, int colIndex, double value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setNullableNumericCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        Double numericValue = toDouble(value);
        if (numericValue != null) {
            cell.setCellValue(numericValue);
        }
        cell.setCellStyle(style);
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        Double numericValue;
        if (value instanceof Number number) {
            numericValue = number.doubleValue();
        } else {
            try {
                numericValue = Double.valueOf(String.valueOf(value).trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return Double.isFinite(numericValue) ? numericValue : null;
    }

    private String formatOrderDate(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate().format(OUTPUT_DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate().format(OUTPUT_DATE_FORMATTER);
        }
        if (value instanceof LocalDate date) {
            return date.format(OUTPUT_DATE_FORMATTER);
        }

        String text = String.valueOf(value).trim();
        for (DateTimeFormatter formatter : DATE_TIME_INPUT_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter).toLocalDate().format(OUTPUT_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                // Try the next supported representation.
            }
        }
        try {
            return OffsetDateTime.parse(text).toLocalDate().format(OUTPUT_DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // Try date-only representations next.
        }
        for (DateTimeFormatter formatter : DATE_INPUT_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter).format(OUTPUT_DATE_FORMATTER);
            } catch (DateTimeParseException ignored) {
                // Try the next supported representation.
            }
        }
        return text;
    }

    private String formatFileName(Object value) {
        if (value == null) {
            return "";
        }
        String fileName = String.valueOf(value);
        int separatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String baseName = fileName.substring(separatorIndex + 1);
        int extensionIndex = baseName.lastIndexOf('.');
        if (extensionIndex > 0 && extensionIndex < baseName.length() - 1) {
            return baseName.substring(0, extensionIndex);
        }
        return baseName;
    }

    private String combineMaterial(Object color, Object material) {
        String colorText = color == null ? "" : String.valueOf(color).trim();
        String materialText = material == null ? "" : String.valueOf(material).trim();
        return colorText + materialText;
    }

    private String formatPrintDuration(Object value) {
        Long seconds = toLong(value);
        if (seconds == null || seconds < 0) {
            return "";
        }
        long hours = seconds / 3600;
        long minutes = seconds % 3600 / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String formatProcessingDuration(Object value) {
        Long seconds = toLong(value);
        if (seconds == null || seconds < 0) {
            return "";
        }
        long totalMinutes = seconds / 60;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "分钟";
        }
        return minutes == 0 ? hours + "小时" : hours + "小时" + minutes + "分钟";
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String formatGender(Object value) {
        if (value == null) {
            return "";
        }
        String gender = String.valueOf(value);
        String normalizedGender = gender.trim();
        if ("12.1".equals(normalizedGender) || "男".equals(normalizedGender)) {
            return "男";
        }
        if ("12.2".equals(normalizedGender) || "女".equals(normalizedGender)) {
            return "女";
        }
        return gender;
    }
}
