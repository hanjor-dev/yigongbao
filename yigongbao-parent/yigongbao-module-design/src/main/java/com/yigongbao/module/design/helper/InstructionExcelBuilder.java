package com.yigongbao.module.design.helper;

import com.yigongbao.module.design.entity.DesignProductEntity;
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
 * 核心逻辑：将行9-25的纵向合并大格子拆开，按产品数量动态展开多行。
 * </p>
 *
 * @author hanjor
 * @date 2026-04-16
 */
@Slf4j
@Component
public class InstructionExcelBuilder {

    private static final String TEMPLATE_PATH = "template/生产指令单.xlsx";
    /** 产品数据区起始行（0-indexed，对应模板行9） */
    private static final int DATA_ROW_START = 8;
    /** 产品数据区原始行数（模板行9-25，共17行） */
    private static final int DATA_ROW_ORIGINAL_COUNT = 17;
    /** 产品数据区结束行（0-indexed，对应模板行25） */
    private static final int DATA_ROW_END = DATA_ROW_START + DATA_ROW_ORIGINAL_COUNT - 1;

    /**
     * 填充上下文数据
     */
    @Data
    public static class BuildContext {
        private String orderCode;
        private String patientName;
        private String hospitalName;
        private String contactName;
        private String packageCode;
        private String expectedDeliveryDate;
        private String postalAddress;
        private String remark;
        private String version;
        private List<DesignProductEntity> products;
    }

    /**
     * 根据上下文填充指令单模板，返回填充后的 xlsx 字节数组
     *
     * @param ctx 填充上下文
     * @return xlsx 字节数组
     * @throws IOException 读取模板或写出失败时
     */
    public byte[] build(BuildContext ctx) throws IOException {
        log.info("开始生成生产指令单，orderCode={}, version={}, productCount={}",
                ctx.getOrderCode(), ctx.getVersion(), ctx.getProducts() == null ? 0 : ctx.getProducts().size());

        try (InputStream is = new ClassPathResource(TEMPLATE_PATH).getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            List<DesignProductEntity> products = ctx.getProducts() == null ? List.of() : ctx.getProducts();
            int n = products.size();

            // 1. 覆盖版本号（row=1, col=6，即 G2）
            setCell(sheet, 1, 6, "版本号：" + strOrEmpty(ctx.getVersion()));

            // 2. 填充基本信息区
            setCell(sheet, 3, 1, strOrEmpty(ctx.getOrderCode()));            // B4：订单编号
            setCell(sheet, 3, 4, strOrEmpty(ctx.getPatientName()));          // E4：客户名称（患者姓名）
            setCell(sheet, 3, 6, strOrEmpty(ctx.getContactName()));          // G4：联系人
            setCell(sheet, 4, 1, strOrEmpty(ctx.getPackageCode()));          // B5：数据包编号
            setCell(sheet, 4, 3, strOrEmpty(ctx.getHospitalName()));         // D5：医院
            setCell(sheet, 4, 6, strOrEmpty(ctx.getExpectedDeliveryDate())); // G5：预交货时间

            // 3. 处理产品数据区（row 8-24，共17行，全列纵向合并）
            //    先移除这17行中所有合并区域
            removeMergedRegionsInRows(sheet, DATA_ROW_START, DATA_ROW_END);

            // 4. 若产品数 > 17，需要下移后续行腾出空间
            int lastRow = sheet.getLastRowNum();
            if (n > DATA_ROW_ORIGINAL_COUNT) {
                int extra = n - DATA_ROW_ORIGINAL_COUNT;
                sheet.shiftRows(DATA_ROW_END + 1, lastRow, extra);
                // 插入新行并复制行8（0-indexed=7，数据区模板行）的样式
                CellStyle templateStyle = getRowStyle(sheet, DATA_ROW_START);
                for (int i = DATA_ROW_ORIGINAL_COUNT; i < n; i++) {
                    Row newRow = sheet.createRow(DATA_ROW_START + i);
                    copyRowStyle(newRow, templateStyle, 9); // 9列
                }
            }

            // 5. 逐行写入产品数据
            for (int i = 0; i < n; i++) {
                DesignProductEntity p = products.get(i);
                int rowIdx = DATA_ROW_START + i;
                setCell(sheet, rowIdx, 0, String.valueOf(i + 1));                   // 序号
                setCell(sheet, rowIdx, 1, strOrEmpty(p.getCertNo()));               // 注册证号
                setCell(sheet, rowIdx, 2, strOrEmpty(p.getProductName()));          // 产品名称
                setCell(sheet, rowIdx, 3, strOrEmpty(p.getPackageFileName()));      // 数据文件名称
                setCell(sheet, rowIdx, 4, strOrEmpty(p.getSpecName()));             // 型号/规格
                setCell(sheet, rowIdx, 5, strOrEmpty(p.getMaterialName()));         // 材质
                setCell(sheet, rowIdx, 6, p.getQuantity() != null ? String.valueOf(p.getQuantity()) : ""); // 数量
                setCell(sheet, rowIdx, 7, strOrEmpty(p.getTimeliness()));           // 时效
                setCell(sheet, rowIdx, 8, strOrEmpty(p.getColorName()));            // 颜色
            }

            // 6. 填充底部区域（原模板行26起，shift后位置偏移 max(0, n-17)）
            int bottomOffset = Math.max(0, n - DATA_ROW_ORIGINAL_COUNT);
            int row27 = 26 + bottomOffset; // 原行27（0-indexed=26）：患者姓名
            int row28 = 27 + bottomOffset; // 原行28：邮寄地址
            int row29 = 28 + bottomOffset; // 原行29：备注
            setCell(sheet, row27, 1, strOrEmpty(ctx.getPatientName()));   // 患者姓名
            setCell(sheet, row28, 1, strOrEmpty(ctx.getPostalAddress())); // 邮寄地址
            setCell(sheet, row29, 1, strOrEmpty(ctx.getRemark()));        // 备注

            // 7. 写出为 byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            log.info("生产指令单生成完成，size={}", baos.size());
            return baos.toByteArray();
        }
    }

    // ==================== 私有工具方法 ====================

    /** 移除指定行范围内的所有合并区域 */
    private void removeMergedRegionsInRows(Sheet sheet, int startRow, int endRow) {
        // 倒序遍历避免 remove 后下标错位
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() >= startRow && region.getLastRow() <= endRow) {
                sheet.removeMergedRegion(i);
            }
        }
    }

    /** 获取指定行的第一个单元格样式（用于后续复制） */
    private CellStyle getRowStyle(Sheet sheet, int rowIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null || row.getCell(0) == null) {
            return sheet.getWorkbook().createCellStyle();
        }
        return row.getCell(0).getCellStyle();
    }

    /** 为新插入行的每列设置样式 */
    private void copyRowStyle(Row row, CellStyle style, int colCount) {
        for (int c = 0; c < colCount; c++) {
            Cell cell = row.createCell(c);
            cell.setCellStyle(style);
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

    private String strOrEmpty(String s) {
        return s != null ? s : "";
    }
}
