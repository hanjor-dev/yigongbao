package com.yigongbao.module.design.helper;

import com.yigongbao.common.constant.StatusConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 生产指令单 Excel 填充器
 * <p>
 * 模板路径：classpath:template/生产指令单.xlsx
 * <p>
 * 模板布局（0-indexed）：
 * <pre>
 *   row0  : 标题行（生产指令单）
 *   row1  : 单号(A2:B2) / 版本号(G2:I2)
 *   row2  : 合并"基本信息"(A3:I3)
 *   row3  : 订单编号标签A4, 值B4:C4 | 客户名称标签D4, 值E4:F4 | 联系人标签G4:H4, 值I4
 *   row4  : 数据包编号标签A5, 值B5:C5 | 医院标签D5, 值E5:F5 | 预交货时间标签G5:H5, 值I5
 *   row5  : 合并"生产产品信息"(A6:I6)
 *   row6  : 表头行（序号/注册证号/产品名称/…）
 *   row7  : 第1条产品数据行（DATA_ROW_START）
 *   row8-24: 产品数据区余量行（模板中合并占位，共17行）
 *   row25 : 产品标识/包装数量/开始时间
 *   row26 : 患者姓名/是否邮寄/结束时间
 *   row27 : 邮寄地址（B28:I28 合并）
 *   row28 : 备注（B29:I29 合并）
 *   row29 : 指令/日期 行
 * </pre>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Component
public class InstructionExcelBuilder {

    private static final String TEMPLATE_PATH = "template/生产指令单.xlsx";

    /**
     * 产品数据区起始行（0-indexed，对应模板第8行 row7）
     */
    private static final int DATA_ROW_START = 7;

    /**
     * 产品数据区原始预留行数（模板 row7-24，共18行）
     */
    private static final int DATA_ROW_ORIGINAL_COUNT = 18;

    /**
     * 产品数据区末尾行（0-indexed）
     */
    private static final int DATA_ROW_END = DATA_ROW_START + DATA_ROW_ORIGINAL_COUNT - 1;

    // ==================== 填充上下文 ====================

    /**
     * 展开后的产品×文件行（一个文件=一行）
     */
    @Data
    public static class ProductRow {
        /** 注册证号 */
        private String certNo;
        /** 产品名称 */
        private String productName;
        /** 数据文件名（含后缀，填充时去后缀） */
        private String packageFileName;
        /** 型号规格名称 */
        private String specName;
        /** 材质名称 */
        private String materialName;
        /** 数量 */
        private Integer quantity;
        /** 是否加急（0=普通，1=加急），行级 */
        private Integer isUrgent;
        /** 颜色名称 */
        private String colorName;
    }

    /**
     * 填充上下文数据
     */
    @Data
    public static class BuildContext {
        /** 订单编号 */
        private String orderCode;
        /** 患者姓名 */
        private String patientName;
        /** 医院名称 */
        private String hospitalName;
        /** 联系人（医生姓名） */
        private String contactName;
        /** 数据包编号 */
        private String packageCode;
        /** 预交货时间（格式化字符串） */
        private String expectedDeliveryDate;
        /** 邮寄地址 */
        private String postalAddress;
        /** 是否邮寄（"是"/"否"） */
        private String isPostal;
        /** 备注（来自 design_package） */
        private String remark;
        /** 产品标识（来自 design_package） */
        private String productMark;
        /** 包装数量（来自 design_package） */
        private Integer packQuantity;
        /** 版本号 */
        private String version;
        /** 设计师名（填入"指令"列） */
        private String designerName;
        /** 生成日期（yyyy-MM-dd，填入日期列） */
        private String generateDate;
        /** 设计开始时间（格式化字符串） */
        private String designStartTime;
        /** 生成时间（填入"结束时间"列） */
        private String generateTime;
        /** 展开后的产品×文件行列表 */
        private List<ProductRow> rows;
    }

    // ==================== 主方法 ====================

    /**
     * 根据上下文填充指令单模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        List<ProductRow> rows = ctx.getRows() == null ? List.of() : ctx.getRows();
        int n = rows.size();
        log.info("开始生成生产指令单，orderCode={}, version={}, rowCount={}",
                ctx.getOrderCode(), ctx.getVersion(), n);

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);

            // 1. 填充版本号（G2:I2 合并区左上角 → row1, col6）
            setCell(sheet, 1, 6, "版本号：" + strOrEmpty(ctx.getVersion()));

            // 2. 填充基本信息区
            setCell(sheet, 3, 1, strOrEmpty(ctx.getOrderCode()));            // B4：订单编号
            setCell(sheet, 3, 4, strOrEmpty(ctx.getHospitalName()));         // E4：客户名称（医院名）
            setCell(sheet, 3, 8, strOrEmpty(ctx.getContactName()));          // I4：联系人
            setCell(sheet, 4, 1, strOrEmpty(ctx.getPackageCode()));          // B5：数据包编号
            setCell(sheet, 4, 4, strOrEmpty(ctx.getHospitalName()));         // E5：医院名称
            setCell(sheet, 4, 8, strOrEmpty(ctx.getExpectedDeliveryDate())); // I5：预交货时间

            // 3. 清除产品数据区合并（row7-24，共18行）
            removeMergedRegionsInRows(sheet, DATA_ROW_START, DATA_ROW_END);

            // 4. 行样式处理：超出原始18行时下移后续行，新建行复制模板样式；
            //    原始区域内 row8-24（index 1-17）是模板合并占位行，也需应用 row7（index 0）的样式
            int lastRow = sheet.getLastRowNum();
            Row templateRow = sheet.getRow(DATA_ROW_START);
            if (n > DATA_ROW_ORIGINAL_COUNT) {
                int extra = n - DATA_ROW_ORIGINAL_COUNT;
                sheet.shiftRows(DATA_ROW_END + 1, lastRow, extra);
                for (int i = DATA_ROW_ORIGINAL_COUNT; i < n; i++) {
                    Row newRow = sheet.createRow(DATA_ROW_START + i);
                    copyRowFull(newRow, templateRow, 9);
                }
            }
            // 原始区域内非首行（index 1 ~ min(n,18)-1）是合并占位行，无独立单元格样式，需应用首行样式
            for (int i = 1; i < Math.min(n, DATA_ROW_ORIGINAL_COUNT); i++) {
                Row r = sheet.getRow(DATA_ROW_START + i);
                if (r == null) r = sheet.createRow(DATA_ROW_START + i);
                copyRowFull(r, templateRow, 9);
            }

            // 5. 逐行写入产品数据（从 DATA_ROW_START 开始）
            for (int i = 0; i < n; i++) {
                ProductRow row = rows.get(i);
                int rowIdx = DATA_ROW_START + i;
                setCell(sheet, rowIdx, 0, String.valueOf(i + 1));                    // A：序号
                setCell(sheet, rowIdx, 1, strOrEmpty(row.getCertNo()));              // B：注册证号
                setCell(sheet, rowIdx, 2, strOrEmpty(row.getProductName()));         // C：产品名称
                setCell(sheet, rowIdx, 3, stripExtension(row.getPackageFileName())); // D：数据文件名称
                setCell(sheet, rowIdx, 4, strOrEmpty(row.getSpecName()));            // E：型号/规格
                setCell(sheet, rowIdx, 5, strOrEmpty(row.getMaterialName()));        // F：材质
                setCell(sheet, rowIdx, 6, row.getQuantity() != null
                        ? String.valueOf(row.getQuantity()) : "");                   // G：数量
                // H：时效（0=普通，1=加急）
                int urgentVal = row.getIsUrgent() != null ? row.getIsUrgent() : StatusConstants.NO;
                setCell(sheet, rowIdx, 7, urgentVal == StatusConstants.YES ? "加急" : "普通");
                if (urgentVal == StatusConstants.YES) {
                    setBold(wb, sheet, rowIdx, 7);
                }
                setCell(sheet, rowIdx, 8, strOrEmpty(row.getColorName()));           // I：颜色
            }

            // 6. 未使用的余量行重新合并（产品行之后 ~ DATA_ROW_END）
            int mergeStart = DATA_ROW_START + Math.max(n, 1);
            if (mergeStart <= DATA_ROW_END) {
                for (int col = 0; col < 9; col++) {
                    sheet.addMergedRegion(new CellRangeAddress(mergeStart, DATA_ROW_END, col, col));
                    for (int r = mergeStart; r <= DATA_ROW_END; r++) {
                        setCell(sheet, r, col, "");
                    }
                }
            }

            // 7. 填充底部区域（偏移量 = max(0, n - DATA_ROW_ORIGINAL_COUNT)）
            int offset = Math.max(0, n - DATA_ROW_ORIGINAL_COUNT);

            // row26（原 row25）：产品标识/包装数量/开始时间
            int row26 = 25 + offset;
            setCell(sheet, row26, 1, strOrEmpty(ctx.getProductMark()));              // B26：产品标识
            setCell(sheet, row26, 4, ctx.getPackQuantity() != null
                    ? String.valueOf(ctx.getPackQuantity()) : "");                   // E26：包装数量
            setCell(sheet, row26, 7, strOrEmpty(ctx.getDesignStartTime()));          // H26：开始时间

            // row27（原 row26）：患者姓名/是否邮寄/结束时间
            int row27 = 26 + offset;
            setCell(sheet, row27, 1, strOrEmpty(ctx.getPatientName()));              // B27：患者姓名
            setCell(sheet, row27, 4, strOrEmpty(ctx.getIsPostal()));                 // E27：是否邮寄
            setCell(sheet, row27, 7, strOrEmpty(ctx.getGenerateTime()));             // H27：结束时间

            // row28（原 row27）：邮寄地址
            int row28 = 27 + offset;
            setCell(sheet, row28, 1, strOrEmpty(ctx.getPostalAddress()));            // B28：邮寄地址

            // row29（原 row28）：备注
            int row29 = 28 + offset;
            setCell(sheet, row29, 1, strOrEmpty(ctx.getRemark()));                   // B29：备注

            // row30（原 row29）：指令人/日期 | 复核/日期
            int row30 = 29 + offset;
            setCell(sheet, row30, 1, strOrEmpty(ctx.getDesignerName()));             // B30：指令人
            setCell(sheet, row30, 2, strOrEmpty(ctx.getGenerateDate()));             // C30：指令日期
            setCell(sheet, row30, 8, strOrEmpty(ctx.getGenerateDate()));             // I30：复核日期

            // 8. 写出为 byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("生产指令单生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== 私有工具方法 ====================

    /** 移除指定行范围内的所有合并区域 */
    private void removeMergedRegionsInRows(Sheet sheet, int startRow, int endRow) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() >= startRow && region.getLastRow() <= endRow) {
                sheet.removeMergedRegion(i);
            }
        }
    }

    /**
     * 完整复制模板行到新行：逐列复制各列独立样式，并同步行高
     */
    private void copyRowFull(Row newRow, Row templateRow, int colCount) {
        newRow.setHeight(templateRow.getHeight());
        for (int c = 0; c < colCount; c++) {
            Cell templateCell = templateRow.getCell(c);
            Cell newCell = newRow.createCell(c);
            if (templateCell != null) {
                newCell.setCellStyle(templateCell.getCellStyle());
            }
        }
    }

    /** 向指定行列写入字符串值 */
    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        cell.setCellValue(value != null ? value : "");
    }

    /** 将指定单元格设置为加粗（基于原有样式克隆，避免影响其他单元格） */
    private void setBold(Workbook wb, Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return;
        CellStyle boldStyle = wb.createCellStyle();
        boldStyle.cloneStyleFrom(cell.getCellStyle());
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        cell.setCellStyle(boldStyle);
    }

    private String strOrEmpty(String s) {
        return s != null ? s : "";
    }

    /** 去除文件名后缀（如 "左髋骨.stl" → "左髋骨"） */
    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
