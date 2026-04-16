package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
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
     * 填充上下文
     */
    @Data
    public static class BuildContext {
        private String orderCode;
        private String packageCode;
        private String remark;
        private List<DesignProductEntity> products;
    }

    /**
     * 槽位坐标定义：[文件名行, 文件名列, 产品名行, 产品名列]
     * 基于模板分析（0-indexed），共11个内容槽位
     */
    private static final int[][] SLOT_COORDS = {
        // {fileNameRow, fileNameCol, productNameRow, productNameCol}
        {0,  2,  1,  0},   // 槽1：A-D区
        {0,  6,  1,  4},   // 槽2：E-H区
        {0,  10, 1,  8},   // 槽3：I-L区
        {12, 2,  13, 0},   // 槽4：A-D区 第2行组
        {12, 6,  13, 4},   // 槽5：E-H区
        {12, 10, 13, 8},   // 槽6：I-L区
        {12, 14, 13, 12},  // 槽7：M-P区
        {24, 2,  25, 0},   // 槽8：A-D区 第3行组
        {24, 6,  25, 4},   // 槽9：E-H区
        {24, 10, 25, 8},   // 槽10：I-L区
        {24, 14, 25, 12},  // 槽11：M-P区
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

    /**
     * 根据上下文填充图纸模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        List<DesignProductEntity> products = ctx.getProducts() == null ? List.of() : ctx.getProducts();
        int n = products.size();
        // 计算总页数：至少1页
        int totalPages = Math.max(1, (int) Math.ceil((double) n / SLOTS_PER_PAGE));

        log.info("开始生成图纸，orderCode={}, productCount={}, totalPages={}",
                ctx.getOrderCode(), n, totalPages);

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet templateSheet = wb.getSheetAt(0);

            // 对每一页处理
            for (int page = 0; page < totalPages; page++) {
                Sheet sheet;
                if (page == 0) {
                    // 第一页直接使用模板 Sheet
                    sheet = templateSheet;
                } else {
                    // 复制模板 Sheet 作为新页
                    sheet = wb.cloneSheet(0);
                    wb.setSheetName(wb.getSheetIndex(sheet), "图纸-" + (page + 1));
                }

                // 计算本页产品范围
                int from = page * SLOTS_PER_PAGE;
                int to = Math.min(from + SLOTS_PER_PAGE, n);

                // 填充槽位
                for (int slot = 0; slot < SLOTS_PER_PAGE; slot++) {
                    int productIdx = from + slot;
                    int[] coord = SLOT_COORDS[slot];
                    if (productIdx < to) {
                        DesignProductEntity p = products.get(productIdx);
                        setCell(sheet, coord[0], coord[1], strOrEmpty(p.getPackageFileName())); // 文件名
                        setCell(sheet, coord[2], coord[3], strOrEmpty(p.getProductName()));     // 产品名
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
                        "第" + (page + 1) + "页/共" + totalPages + "页");
            }

            // 写出
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("图纸生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
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
}
