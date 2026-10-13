package com.yigongbao.module.design.helper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
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
        /** 备注（来自 design_package.remark，允许为 null） */
        private String remark;
        /** 生成日期（yyyy-MM-dd，填入设计/日期和审核/日期列） */
        private String generateDate;
        /** 设计师名（填入设计/日期行姓名列） */
        private String designerName;
        /** 展开后的产品×文件行列表 */
        private List<ProductRow> rows;
        /** 前端图片或后端兜底二维码的 PNG 字节（null 时跳过二维码嵌入） */
        private byte[] qrBytes;
        /** 二维码来源：FRONTEND_FILE 或 BACKEND_FALLBACK。 */
        private String qrSource;
        /** 服务层用于保存图纸版本快照的前端二维码文件 ID，后端兜底时为空。 */
        private String qrFileId;
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

    /** 二维码槽位坐标：{col1, row1, col2, row2}，对应 M1:P10 */
    private static final int[] QR_COORDS = {12, 0, 16, 10};
    /** 原二维码字节无法被 POI 识别时的最小有效 PNG，保证工作簿仍有图片对象。 */
    private static final byte[] FALLBACK_QR_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    /** footer 行（0-indexed）：原模板行37=row36 */
    private static final int FOOTER_ROW = 36;
    /** 备注值列：B37=col1（B37:H42 合并区域，写首格） */
    private static final int REMARK_COL = 1;
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

        log.info("开始生成图纸，orderCode={}, rowCount={}, totalPages={}, qrSource={}, qrFileId={}, qrBytes={}",
                ctx.getOrderCode(), n, totalPages, ctx.getQrSource(), ctx.getQrFileId(),
                ctx.getQrBytes() == null ? 0 : ctx.getQrBytes().length);

        // 前端图片或后端兜底二维码所有分页复用同一份图片字节
        byte[] qrBytes = ctx.getQrBytes();

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            // 多页时保留一个永不参与填充的干净模板，避免后续页面继承
            // 前一页已经插入的截图、二维码和动态字段。单页继续直接使用原模板。
            Sheet templateSheet = wb.getSheetAt(0);
            Sheet cleanTemplateSheet = totalPages > 1 ? wb.cloneSheet(0) : null;
            if (cleanTemplateSheet != null) {
                wb.setSheetName(wb.getSheetIndex(cleanTemplateSheet), "__drawing_template__");
            }

            for (int page = 0; page < totalPages; page++) {
                Sheet sheet;
                if (page == 0) {
                    sheet = templateSheet;
                } else {
                    sheet = wb.cloneSheet(wb.getSheetIndex(cleanTemplateSheet));
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
                        // 槽2+ 的产品名单元格在模板中没有预设合并区域，需手动补充（与下一列合并，对齐槽1布局）
                        if (slot > 0) {
                            ensureCellCentered((XSSFWorkbook) wb, sheet, coord[2], coord[3]);
                            mergeIfAbsent(sheet, coord[2], coord[3], coord[2], coord[3] + 1);
                        }
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

                // 嵌入二维码（右上角，每页相同）
                if (qrBytes != null) {
                    insertQrCode((XSSFSheet) sheet, (XSSFWorkbook) wb, qrBytes, ctx.getQrSource(), page + 1);
                } else {
                    log.warn("图纸二维码未嵌入，二维码字节为空，orderCode={}, page={}, qrSource={}",
                            ctx.getOrderCode(), page + 1, ctx.getQrSource());
                }

                // 填充 footer
                setCellWithWrap(sheet, FOOTER_ROW, REMARK_COL, strOrEmpty(ctx.getRemark()));
                setCell(sheet, FOOTER_ROW, PKG_CODE_COL, strOrEmpty(ctx.getPackageCode()));
                setCell(sheet, FOOTER_ROW, ORDER_CODE_COL, strOrEmpty(ctx.getOrderCode()));
                setCell(sheet, PAGE_TEXT_ROW, PAGE_TEXT_COL,
                        "第" + toChinese(page + 1) + "页/共" + toChinese(totalPages) + "页");
                // 设计师名留空，由设计师手动签名
                setCell(sheet, DESIGN_DATE_ROW, DESIGN_DATE_COL, strOrEmpty(ctx.getGenerateDate()));
                // 审核日期留空，由审核人手动填写

                // 更新文本框中的页码水印（如果存在）
                updatePageWatermark((XSSFSheet) sheet, page + 1, totalPages);
            }

            // 干净模板只用于复制，不作为最终输出页保留。
            if (cleanTemplateSheet != null) {
                wb.removeSheetAt(wb.getSheetIndex(cleanTemplateSheet));
            }

            // 写出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("图纸生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    private void insertQrCode(XSSFSheet sheet, XSSFWorkbook wb, byte[] qrBytes,
                              String qrSource, int page) {
        try {
            createQrPicture(sheet, wb, qrBytes);
            log.info("图纸二维码插入Excel成功，source={}, page={}, bytes={}, slot=M1:P10",
                    qrSource, page, qrBytes.length);
        } catch (Exception e) {
            log.warn("图纸二维码原图插入Excel失败，改用构建器最小PNG兜底，source={}, page={}, bytes={}, error={}",
                    qrSource, page, qrBytes.length, e.getMessage(), e);
            try {
                createQrPicture(sheet, wb, FALLBACK_QR_PNG);
                log.info("图纸二维码构建器兜底图片插入Excel成功，source=BUILDER_EMBED_FALLBACK, page={}, bytes={}, slot=M1:P10",
                        page, FALLBACK_QR_PNG.length);
            } catch (Exception fallbackException) {
                log.error("图纸二维码原图和构建器兜底图片均插入Excel失败，source={}, page={}, error={}",
                        qrSource, page, fallbackException.getMessage(), fallbackException);
            }
        }
    }

    private void createQrPicture(XSSFSheet sheet, XSSFWorkbook wb, byte[] imageBytes) {
        int pictureIdx = wb.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
        XSSFDrawing drawing = sheet.getDrawingPatriarch() != null
                ? sheet.getDrawingPatriarch() : sheet.createDrawingPatriarch();

        int[] imageSize = getImageDimensions(imageBytes);
        XSSFClientAnchor anchor = imageSize == null
                ? createFallbackAnchor(QR_COORDS[0], QR_COORDS[2], QR_COORDS[1], QR_COORDS[3], 10)
                : createContainAnchor(sheet, QR_COORDS[0], QR_COORDS[2], QR_COORDS[1], QR_COORDS[3],
                imageSize[0], imageSize[1], 10);
        anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
        drawing.createPicture(anchor, pictureIdx);
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
            int[] imageSize = getImageDimensions(screenshotBytes);

            // 图片区域：产品名行下方，占槽位主体（高度10行，宽度4列：标签2列+值2列）
            int colStart = coord[3] - 2;      // 标签列起始位置（产品名称值列向左2列）
            int colEnd = coord[3] + 2;        // 值列结束位置（产品名称值列向右2列）
            int rowStart = coord[2] + 1;      // productNameRow + 1
            int rowEnd = rowStart + 10;       // 向下10行

            // 根据原图尺寸按比例缩放，避免将任意比例图片强制拉伸到固定槽位。
            XSSFClientAnchor anchor;
            if (imageSize == null) {
                // 尺寸解析失败时仍然插入图片，退回固定槽位矩形，避免生成空白槽位。
                log.warn("截图尺寸无法解析，使用槽位矩形兜底嵌图，coord={}", coord);
                anchor = createFallbackAnchor(colStart, colEnd, rowStart, rowEnd, 10);
            } else {
                anchor = createContainAnchor(
                        sheet, colStart, colEnd, rowStart, rowEnd, imageSize[0], imageSize[1], 10);
            }
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);

            XSSFDrawing drawing = sheet.getDrawingPatriarch() != null
                    ? sheet.getDrawingPatriarch() : sheet.createDrawingPatriarch();
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception e) {
            log.warn("截图嵌入失败，coord={}, error={}", coord, e.getMessage());
        }
    }

    /** 创建旧逻辑的固定槽位锚点，作为图片尺寸无法解析时的兜底方案。 */
    private XSSFClientAnchor createFallbackAnchor(
            int colStart, int colEnd, int rowStart, int rowEnd, int paddingPixels) {
        int padding = paddingPixels * Units.EMU_PER_PIXEL;
        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(colStart);
        anchor.setRow1(rowStart);
        anchor.setDx1(padding);
        anchor.setDy1(padding);
        anchor.setCol2(colEnd);
        anchor.setRow2(rowEnd);
        anchor.setDx2(-padding);
        anchor.setDy2(-padding);
        return anchor;
    }

    /**
     * 创建等比例缩放并居中的图片锚点，图片边界不会超出指定单元格区域。
     * XSSF 锚点的偏移量使用 EMU，列宽和行高也统一换算到 EMU 后计算。
     */
    private XSSFClientAnchor createContainAnchor(
            XSSFSheet sheet, int colStart, int colEnd, int rowStart, int rowEnd,
            int imageWidth, int imageHeight, int paddingPixels) {
        double padding = (double) paddingPixels * Units.EMU_PER_PIXEL;
        double slotLeft = columnPositionInEmu(sheet, colStart);
        double slotRight = columnPositionInEmu(sheet, colEnd);
        double slotTop = rowPositionInEmu(sheet, rowStart);
        double slotBottom = rowPositionInEmu(sheet, rowEnd);

        double availableWidth = slotRight - slotLeft - padding * 2;
        double availableHeight = slotBottom - slotTop - padding * 2;
        double imageWidthInEmu = imageWidth * (double) Units.EMU_PER_PIXEL;
        double imageHeightInEmu = imageHeight * (double) Units.EMU_PER_PIXEL;
        double scale = Math.min(availableWidth / imageWidthInEmu, availableHeight / imageHeightInEmu);

        double displayWidth = imageWidthInEmu * scale;
        double displayHeight = imageHeightInEmu * scale;
        double imageLeft = slotLeft + padding + (availableWidth - displayWidth) / 2;
        double imageTop = slotTop + padding + (availableHeight - displayHeight) / 2;

        int[] fromColumn = locateColumn(sheet, colEnd, imageLeft);
        int[] toColumn = locateColumn(sheet, colEnd, imageLeft + displayWidth);
        int[] fromRow = locateRow(sheet, rowEnd, imageTop);
        int[] toRow = locateRow(sheet, rowEnd, imageTop + displayHeight);

        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(fromColumn[0]);
        anchor.setDx1(fromColumn[1]);
        anchor.setCol2(toColumn[0]);
        anchor.setDx2(toColumn[1]);
        anchor.setRow1(fromRow[0]);
        anchor.setDy1(fromRow[1]);
        anchor.setRow2(toRow[0]);
        anchor.setDy2(toRow[1]);
        return anchor;
    }

    private double columnPositionInEmu(XSSFSheet sheet, int colIndex) {
        double position = 0;
        for (int i = 0; i < colIndex; i++) {
            position += Units.columnWidthToEMU(sheet.getColumnWidth(i));
        }
        return position;
    }

    private double rowPositionInEmu(XSSFSheet sheet, int rowIndex) {
        double position = 0;
        for (int i = 0; i < rowIndex; i++) {
            position += rowHeightInEmu(sheet, i);
        }
        return position;
    }

    private double rowHeightInEmu(XSSFSheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        double points = row == null || row.getHeightInPoints() < 0
                ? sheet.getDefaultRowHeightInPoints()
                : row.getHeightInPoints();
        return points * Units.EMU_PER_POINT;
    }

    /** 将绝对 EMU 坐标转换为 XSSF 的 {cellIndex, cellOffset} 锚点坐标。 */
    private int[] locateColumn(XSSFSheet sheet, int endColumn, double position) {
        double current = 0;
        for (int col = 0; col < endColumn; col++) {
            double next = current + Units.columnWidthToEMU(sheet.getColumnWidth(col));
            if (position < next - 0.5) {
                return new int[]{col, (int) Math.round(position - current)};
            }
            current = next;
        }
        return new int[]{endColumn, 0};
    }

    /** 将绝对 EMU 坐标转换为 XSSF 的 {rowIndex, rowOffset} 锚点坐标。 */
    private int[] locateRow(XSSFSheet sheet, int endRow, double position) {
        double current = 0;
        for (int row = 0; row < endRow; row++) {
            double next = current + rowHeightInEmu(sheet, row);
            if (position < next - 0.5) {
                return new int[]{row, (int) Math.round(position - current)};
            }
            current = next;
        }
        return new int[]{endRow, 0};
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

    /**
     * 读取图片尺寸 [width, height]
     */
    private int[] getImageDimensions(byte[] imageBytes) {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(imageBytes));
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.warn("读取图片尺寸失败: {}", e.getMessage());
        }
        return null;
    }

    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
            // 新单元格从模板页（第0页）的对应位置复制样式（包括边框）
            Sheet templateSheet = sheet.getWorkbook().getSheetAt(0);
            Row templateRow = templateSheet.getRow(rowIdx);
            if (templateRow != null) {
                Cell templateCell = templateRow.getCell(colIdx);
                if (templateCell != null && templateCell.getCellStyle() != null) {
                    cell.setCellStyle(templateCell.getCellStyle());
                }
            }
        }
        cell.setCellValue(value != null ? value : "");
    }

    /** 向指定行列写入字符串值，启用自动换行并调整行高（用于长文本如备注） */
    private void setCellWithWrap(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }

        Workbook wb = sheet.getWorkbook();
        CellStyle existingStyle = cell.getCellStyle();
        CellStyle newStyle = wb.createCellStyle();

        if (existingStyle != null && existingStyle.getIndex() != 0) {
            newStyle.cloneStyleFrom(existingStyle);
        }
        newStyle.setWrapText(true);
        cell.setCellStyle(newStyle);
        cell.setCellValue(value != null ? value : "");

        if (value != null && !value.isEmpty()) {
            int charCount = value.length();
            int lines = (charCount / 20) + 1;
            if (lines > 1) {
                row.setHeightInPoints(20 * lines);
            }
        }
    }

    /** 确保单元格有居中样式（水平和垂直居中） */
    private void ensureCellCentered(XSSFWorkbook wb, Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return;

        CellStyle currentStyle = cell.getCellStyle();
        if (currentStyle == null ||
            currentStyle.getAlignment() != HorizontalAlignment.CENTER ||
            currentStyle.getVerticalAlignment() != VerticalAlignment.CENTER) {
            CellStyle newStyle = wb.createCellStyle();
            if (currentStyle != null) {
                newStyle.cloneStyleFrom(currentStyle);
            }
            newStyle.setAlignment(HorizontalAlignment.CENTER);
            newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cell.setCellStyle(newStyle);
        }
    }

    /** 若指定区域尚未有合并区域则添加，避免重复合并报错（cloneSheet 会保留已有合并） */
    private void mergeIfAbsent(Sheet sheet, int firstRow, int firstCol, int lastRow, int lastCol) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() == firstRow && r.getFirstColumn() == firstCol
                    && r.getLastRow() == lastRow && r.getLastColumn() == lastCol) {
                return; // 已存在
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    /**
     * 更新文本框中的页码水印（如果存在）
     * 查找包含"第X页"文本的文本框，并更新为当前页码
     *
     * @param sheet       目标 Sheet
     * @param currentPage 当前页码（1-based）
     * @param totalPages  总页数
     */
    private void updatePageWatermark(XSSFSheet sheet, int currentPage, int totalPages) {
        try {
            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                return;
            }
            // 遍历所有形状，查找包含页码文本的文本框
            for (XSSFShape shape : drawing.getShapes()) {
                if (shape instanceof org.apache.poi.xssf.usermodel.XSSFSimpleShape) {
                    org.apache.poi.xssf.usermodel.XSSFSimpleShape textBox =
                        (org.apache.poi.xssf.usermodel.XSSFSimpleShape) shape;
                    String text = textBox.getText();
                    // 检查是否包含"第"和"页"，表示这是页码水印
                    if (text != null && text.contains("第") && text.contains("页")) {
                        // 更新为当前页码
                        String newText = "第 " + currentPage + " 页";
                        textBox.setText(newText);
                        log.debug("更新页码水印：{} -> {}", text, newText);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("更新页码水印失败：{}", e.getMessage());
        }
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
