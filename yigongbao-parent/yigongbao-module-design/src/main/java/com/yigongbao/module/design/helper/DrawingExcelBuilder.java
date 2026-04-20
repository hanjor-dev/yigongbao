package com.yigongbao.module.design.helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 图纸 Excel 填充器
 * <p>
 * 模板路径：classpath:template/图纸.xlsx
 * 核心逻辑：11个槽位/页，超出时复制 Sheet 分页，更新页码。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Component
public class DrawingExcelBuilder {

    private static final String TEMPLATE_PATH = "template/图纸.xlsx";
    /** 每页最多容纳的产品槽位数（右上角为二维码，共11个内容位） */
    private static final int SLOTS_PER_PAGE = 11;

    /**
     * 展开后的产品×文件行（一个文件=一行）
     */
    @Data
    public static class ProductRow {
        /** 文件名（含后缀，填充时去后缀） */
        private String packageFileName;
        /** 产品名称 */
        private String productName;
        /** 截图字节（PNG/JPG，null 表示该槽位无截图不嵌图） */
        private byte[] screenshotBytes;
    }

    /**
     * 填充上下文
     */
    @Data
    public static class BuildContext {
        private String orderCode;
        private String packageCode;
        private String remark;
        /** 生成日期（yyyy-MM-dd，填入设计/日期和审核/日期列） */
        private String generateDate;
        /** 设计师名（填入设计/日期行姓名列） */
        private String designerName;
        /** 展开后的产品×文件行列表 */
        private List<ProductRow> rows;
    }

    /**
     * 槽位坐标定义：[文件名行, 文件名列, 产品名行, 产品名列]
     * 基于模板分析（0-indexed）：每槽4列，标签列固定（不覆盖），值写入标签右侧
     * 文件名值列：D/H/L/P（col 3/7/11/15）；产品名值列：C/G/K/O（col 2/6/10/14）
     */
    private static final int[][] SLOT_COORDS = {
        // {fileNameRow, fileNameCol, productNameRow, productNameCol}
        {0,  3,  1,  2},   // 槽1：D1 / C2
        {0,  7,  1,  6},   // 槽2：H1 / G2
        {0,  11, 1,  10},  // 槽3：L1 / K2
        {12, 3,  13, 2},   // 槽4：D13 / C14
        {12, 7,  13, 6},   // 槽5：H13 / G14
        {12, 11, 13, 10},  // 槽6：L13 / K14
        {12, 15, 13, 14},  // 槽7：P13 / O14
        {24, 3,  25, 2},   // 槽8：D25 / C26
        {24, 7,  25, 6},   // 槽9：H25 / G26
        {24, 11, 25, 10},  // 槽10：L25 / K26
        {24, 15, 25, 14},  // 槽11：P25 / O26
    };

    /** footer 行（0-indexed）：原模板行37=row36 */
    private static final int FOOTER_ROW = 36;
    /** 数据包编号值列：J37=col9 */
    private static final int PKG_CODE_COL = 9;
    /** 订单编号值列：N37=col13 */
    private static final int ORDER_CODE_COL = 13;
    /** 页码文本行列：M39=row38,col12 */
    private static final int PAGE_TEXT_ROW = 38;
    private static final int PAGE_TEXT_COL = 12;
    /** 设计师名列：J39=row38,col9；设计日期列：L39=row38,col11 */
    private static final int DESIGN_NAME_ROW = 38;
    private static final int DESIGN_NAME_COL = 9;
    private static final int DESIGN_DATE_ROW = 38;
    private static final int DESIGN_DATE_COL = 11;
    /** 审核日期列：L41=row40,col11（审核人不填） */
    private static final int REVIEW_DATE_ROW = 40;
    private static final int REVIEW_DATE_COL = 11;

    /**
     * 根据上下文填充图纸模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        List<ProductRow> rows = ctx.getRows() == null ? List.of() : ctx.getRows();
        int n = rows.size();
        // 计算总页数：至少1页
        int totalPages = Math.max(1, (int) Math.ceil((double) n / SLOTS_PER_PAGE));

        log.info("开始生成图纸，orderCode={}, rowCount={}, totalPages={}",
                ctx.getOrderCode(), n, totalPages);

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet templateSheet = wb.getSheetAt(0);

            for (int page = 0; page < totalPages; page++) {
                Sheet sheet;
                if (page == 0) {
                    sheet = templateSheet;
                } else {
                    sheet = wb.cloneSheet(0);
                    wb.setSheetName(wb.getSheetIndex(sheet), "图纸-" + (page + 1));
                }

                // 计算本页行范围
                int from = page * SLOTS_PER_PAGE;
                int to = Math.min(from + SLOTS_PER_PAGE, n);

                // 填充槽位（文字 + 截图）
                for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
                    int rowIdx = from + slot;
                    int[] coord = SLOT_COORDS[slot];
                    if (rowIdx < to) {
                        ProductRow row = rows.get(rowIdx);
                        setCell(sheet, coord[0], coord[1], stripExtension(row.getPackageFileName())); // 文件名（去后缀）
                        setCell(sheet, coord[2], coord[3], strOrEmpty(row.getProductName()));         // 产品名
                        // 嵌入截图（如果有）
                        if (row.getScreenshotBytes() != null && row.getScreenshotBytes().length > 0) {
                            insertSlotImage((XSSFSheet) sheet, (XSSFWorkbook) wb, coord, row.getScreenshotBytes());
                        }
                    } else {
                        // 清空多余槽位
                        setCell(sheet, coord[0], coord[1], "");
                        setCell(sheet, coord[2], coord[3], "");
                    }
                }

                // 填充 footer
                setCell(sheet, FOOTER_ROW, PKG_CODE_COL, strOrEmpty(ctx.getPackageCode()));
                setCell(sheet, FOOTER_ROW, ORDER_CODE_COL, strOrEmpty(ctx.getOrderCode()));
                setCell(sheet, PAGE_TEXT_ROW, PAGE_TEXT_COL,
                        "第" + toChinese(page + 1) + "页/共" + toChinese(totalPages) + "页");
                setCell(sheet, DESIGN_NAME_ROW, DESIGN_NAME_COL, strOrEmpty(ctx.getDesignerName()));
                setCell(sheet, DESIGN_DATE_ROW, DESIGN_DATE_COL, strOrEmpty(ctx.getGenerateDate()));
                setCell(sheet, REVIEW_DATE_ROW, REVIEW_DATE_COL, strOrEmpty(ctx.getGenerateDate()));
            }

            // 写出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("图纸生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    /**
     * 在槽位主体区域嵌入截图
     * 图片占据槽位的列范围（productNameCol ~ productNameCol+3），
     * 行范围从 productNameRow+1 到 productNameRow+10（槽位主体区域）
     *
     * @param sheet          目标 Sheet
     * @param wb             工作簿（用于添加图片）
     * @param coord          槽位坐标 {fileNameRow, fileNameCol, productNameRow, productNameCol}
     * @param screenshotBytes 截图字节（PNG 或 JPG）
     */
    private void insertSlotImage(XSSFSheet sheet, XSSFWorkbook wb, int[] coord, byte[] screenshotBytes) {
        try {
            // 判断图片格式（PNG/JPG）
            int pictureType = detectPictureType(screenshotBytes);
            int pictureIdx = wb.addPicture(screenshotBytes, pictureType);

            // 图片区域：产品名行下方，占槽位主体（高度约10行，宽度4列）
            int colStart = coord[3];          // productNameCol
            int colEnd = coord[1] + 1;        // fileNameCol + 1（含右边列）
            int rowStart = coord[2] + 1;      // productNameRow + 1
            int rowEnd = rowStart + 10;       // 向下10行（槽位主体）

            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0,
                    colStart, rowStart, colEnd, rowEnd);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception e) {
            log.warn("截图嵌入失败，coord={}, error={}", coord, e.getMessage());
        }
    }

    /**
     * 根据文件头字节判断图片格式（PNG 或 JPG）
     */
    private int detectPictureType(byte[] bytes) {
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50
                && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }

    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value != null ? value : "");
    }

    private String strOrEmpty(String s) {
        return s != null ? s : "";
    }

    /** 将正整数转为中文数字（1-99，超出范围退回阿拉伯数字） */
    private String toChinese(int n) {
        String[] units = {"", "十", "百"};
        String[] digits = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        if (n <= 0 || n >= 100) return String.valueOf(n);
        if (n < 10) return digits[n];
        int tens = n / 10, ones = n % 10;
        String result = (tens == 1 ? "" : digits[tens]) + units[1];
        if (ones > 0) result += digits[ones];
        return result;
    }

    /** 去除文件名后缀（如 "左髋骨.stl" → "左髋骨"） */
    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
