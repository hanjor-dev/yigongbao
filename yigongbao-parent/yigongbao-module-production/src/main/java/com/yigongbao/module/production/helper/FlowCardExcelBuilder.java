package com.yigongbao.module.production.helper;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.yigongbao.module.production.enums.ProcessTypeEnum;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 流转卡 Excel 填充器
 *
 * @author hanjor
 * @date 2026-05-29
 */
@Slf4j
@Component
public class FlowCardExcelBuilder {

    private static final String TEMPLATE_PATH = "template/流转卡模板.xlsx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BATCH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Data
    public static class BuildContext {
        private String recordNo;
        private String versionNo;
        private String designPackageCode;
        private Integer totalProductCount;
        private String productionBatchNo;
        private String material;
        private String materialBatchNo;
        private LocalDateTime printStartTime;
        private LocalDateTime printFinishTime;
        private String designerAssetNo;
        private List<ProcessInfo> processes;
        private List<ProductInfo> products;
    }

    @Data
    public static class ProcessInfo {
        private String processType;
        private String deviceNo;
        private String secondaryDeviceNo;
        private String processParams;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    public static class ProductInfo {
        private String productNo;
        private String productName;
        private String specName;
        private String materialName;
        private String colorName;
        private String fileName;
    }

    public byte[] build(BuildContext context) throws IOException {
        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            fillHeader(sheet, context);
            fillProcesses(sheet, context);
            fillProducts(sheet, context);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void fillHeader(Sheet sheet, BuildContext context) {
        setCellValue(sheet, 1, 0, "编号：QR-SC-002   版本号：" + StrUtil.blankToDefault(context.getVersionNo(), "A/0"));
        setCellValue(sheet, 2, 2, context.getDesignPackageCode());
        setCellValue(sheet, 2, 5, context.getTotalProductCount() != null ?
            String.valueOf(context.getTotalProductCount()) : "-");
        String productionBatchNo = context.getPrintStartTime() == null
            ? context.getProductionBatchNo()
            : context.getPrintStartTime().format(BATCH_DATE_FORMATTER);
        setCellValue(sheet, 3, 2, productionBatchNo);
        setCellValue(sheet, 3, 5, context.getMaterial());
        setCellValue(sheet, 4, 2, "开始时间: " + formatDateTime(context.getPrintStartTime()));
        setCellValue(sheet, 4, 5, context.getMaterialBatchNo());
        setCellValue(sheet, 5, 2, "结束时间: " + formatDateTime(context.getPrintFinishTime()));
    }

    private void fillProcesses(Sheet sheet, BuildContext context) {
        // 填充设计工序（第8行）- 设计师资产编号
        setCellValue(sheet, 7, 3, StrUtil.blankToDefault(context.getDesignerAssetNo(), "-"));
        setCellValue(sheet, 7, 4, "/");

        List<ProcessInfo> processes = context.getProcesses();
        if (processes == null || processes.isEmpty()) return;

        for (ProcessInfo process : processes) {
            if (process == null) continue;

            String processType = process.getProcessType();
            if (StrUtil.isBlank(processType)) continue;

            if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(processType)) {
                setCellValue(sheet, 11, 3, StrUtil.blankToDefault(process.getDeviceNo(), "-"));
                String params = convertProcessParams(process, processType, process.getProcessParams(), context);
                setCellValue(sheet, 11, 4, params);
                setCellValue(sheet, 12, 3, StrUtil.blankToDefault(process.getSecondaryDeviceNo(), "-"));
                setCellValue(sheet, 12, 4, "-");
            } else {
                int rowIndex = getProcessRowIndex(processType);
                if (rowIndex == -1) continue;

                setCellValue(sheet, rowIndex, 3, StrUtil.blankToDefault(process.getDeviceNo(), "-"));
                String params = convertProcessParams(process, processType, process.getProcessParams(), context);
                setCellValue(sheet, rowIndex, 4, params);
            }
        }
    }

    private void fillProducts(Sheet sheet, BuildContext context) {
        List<ProductInfo> products = context.getProducts();
        if (products == null || products.isEmpty()) {
            return;
        }

        // 第 16 行作为产品模板行
        Row templateRow = sheet.getRow(16);

        // 产品数量大于 1 时，需要给新增产品行腾出空间
        if (products.size() > 1 && sheet.getLastRowNum() >= 17) {
            sheet.shiftRows(
                    17,
                    sheet.getLastRowNum(),
                    products.size() - 1
            );
        }

        for (int i = 0; i < products.size(); i++) {
            ProductInfo product = products.get(i);

            // 产品数据从第 16 行开始
            int rowIndex = 16 + i;

            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }

            // 第二行及以后复制模板行样式
            if (templateRow != null && i > 0) {
                copyRowStyle(templateRow, row);

                // 合并当前行第 0 列和第 1 列，即 A、B 两列
                sheet.addMergedRegion(
                        new CellRangeAddress(
                                rowIndex,
                                rowIndex,
                                0,
                                1
                        )
                );
            }

            // 合并单元格后，数据写入左上角单元格，即第 0 列
            setCellValue(
                    sheet,
                    rowIndex,
                    0,
                    product.getProductNo()
            );

            // 产品名称
            setCellValue(
                    sheet,
                    rowIndex,
                    2,
                    product.getProductName()
            );

            // 规格
            setCellValue(
                    sheet,
                    rowIndex,
                    3,
                    product.getSpecName()
            );

            // 数量
            setCellValue(
                    sheet,
                    rowIndex,
                    4,
                    "1"
            );

            // 产品描述
            setCellValue(
                    sheet,
                    rowIndex,
                    5,
                    formatProductDescription(product.getFileName())
            );
        }
    }

    private void copyRowStyle(Row sourceRow, Row targetRow) {
        targetRow.setHeight(sourceRow.getHeight());
        for (int i = 0; i < 6; i++) {
            Cell sourceCell = sourceRow.getCell(i);
            if (sourceCell != null) {
                Cell targetCell = targetRow.createCell(i);
                targetCell.setCellStyle(sourceCell.getCellStyle());
            }
        }
    }

    private String formatProductDescription(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "-";
        }
        String normalized = StrUtil.trim(fileName);
        int fileNameStart = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\')) + 1;
        String baseName = normalized.substring(fileNameStart);
        if (StrUtil.isBlank(baseName)) {
            return "-";
        }
        int extensionStart = baseName.lastIndexOf('.');
        return extensionStart > 0 && extensionStart < baseName.length() - 1
            ? baseName.substring(0, extensionStart)
            : baseName;
    }

    private int getProcessRowIndex(String processType) {
        if (ProcessTypeEnum.PRINT.getCode().equals(processType)) return 8;
        if (ProcessTypeEnum.WASH.getCode().equals(processType)) return 9;
        if (ProcessTypeEnum.CURE.getCode().equals(processType)) return 10;
        if (ProcessTypeEnum.PACK.getCode().equals(processType)) return 13;
        return -1;
    }

    /**
     * 转换工序参数为显示文本
     * <p>
     * 根据工序类型解析JSON格式的工序参数，生成适合Excel显示的多行文本。
     * @param process 工序信息（包含设备编号、时间等）
     * @param processType 工序类型代码（print/wash/cure/clean_dry/pack）
     * @param processParams 工序参数JSON字符串（存储在production_process.process_params）
     * @param context 构建上下文（用于记录解析异常所属的流转卡）
     * @return 格式化的参数文本，多行用换行符分隔；解析失败或参数为空时返回"-"
     */
    private String convertProcessParams(ProcessInfo process, String processType, String processParams, BuildContext context) {
        List<String> lines = new ArrayList<>();
        try {
            if (StrUtil.isNotBlank(processParams)) {
                JSONObject p = JSONUtil.parseObj(processParams);
                if (ProcessTypeEnum.PRINT.getCode().equals(processType)) {
                    // 数据字段映射：layerThickness=层厚、laserPower=激光器功率；Excel 固定按层厚、激光器功率顺序展示
                    lines.add("层厚：" + p.getStr("layerThickness", "-") + " mm");
                    lines.add("激光器功率：" + p.getStr("laserPower", "-") + " mW");
                } else if (ProcessTypeEnum.WASH.getCode().equals(processType)) {
                    lines.add("酒精批号：" + p.getStr("alcoholBatchNo", "-"));
                    lines.add("浸泡程度：" + p.getStr("soakLevel", "-"));
                } else if (ProcessTypeEnum.CURE.getCode().equals(processType)) {
                    lines.add("固化模式：" + p.getStr("cureMode", "-"));
                } else if (ProcessTypeEnum.CLEAN_DRY.getCode().equals(processType)) {
                    lines.add("酒精批号：" + p.getStr("alcoholBatchNo", "-"));
                    lines.add("清洗模式：" + p.getStr("cleanMode", "-"));
                    lines.add("加热：" + p.getStr("heating", "-"));
                } else if (ProcessTypeEnum.PACK.getCode().equals(processType)) {
                    String sealTemperature = p.getStr("sealTemperature");
                    if (StrUtil.isNotBlank(sealTemperature)) {
                        lines.add("纸塑袋热封温度：" + sealTemperature + "℃");
                    }
                    String zipBagSealTemperature = p.getStr("zipBagSealTemperature");
                    if (StrUtil.isNotBlank(zipBagSealTemperature)) {
                        lines.add("PE复合食品包装袋热封温度：" + zipBagSealTemperature + "℃");
                    }
                    String sealTime = p.containsKey("zipBagSealTime")
                        ? p.getStr("zipBagSealTime")
                        : p.getStr("sealTime");
                    if (StrUtil.isNotBlank(sealTime)) {
                        lines.add("热封时间：" + sealTime + "秒");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析工序参数失败: recordNo={}, processType={}, processParams={}",
                context.getRecordNo(), processType, processParams, e);
        }

        // wash/cure/clean_dry 追加开始和结束时间
        if (ProcessTypeEnum.WASH.getCode().equals(processType)
                || ProcessTypeEnum.CURE.getCode().equals(processType)
                || ProcessTypeEnum.CLEAN_DRY.getCode().equals(processType)) {
            lines.add("开始：" + formatMinuteDateTime(process.getStartTime()));
            lines.add("结束：" + formatMinuteDateTime(process.getEndTime()));
        }

        return lines.isEmpty() ? "-" : String.join("\n", lines);
    }

    private void setCellValue(Sheet sheet, int rowIndex, int colIndex, String value) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        Cell cell = row.getCell(colIndex);
        CellStyle originalStyle = null;
        if (cell == null) {
            cell = row.createCell(colIndex);
        } else {
            // 保存原有样式
            originalStyle = cell.getCellStyle();
        }
        cell.setCellValue(StrUtil.blankToDefault(value, "-"));
        // 恢复原有样式
        if (originalStyle != null) {
            cell.setCellStyle(originalStyle);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_FORMATTER);
    }

    private String formatMinuteDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(MINUTE_FORMATTER);
    }
}
