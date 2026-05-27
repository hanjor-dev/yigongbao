package com.yigongbao.module.production.record.vo;

import lombok.Data;
import java.util.List;

/**
 * 加工中心打印机列表VO
 * 用于按加工中心分组返回打印机列表
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class ProcessingCenterPrintersVO {

    /**
     * 加工中心ID
     */
    private Long centerId;

    /**
     * 加工中心名称
     */
    private String centerName;

    /**
     * 该加工中心下的打印机列表
     */
    private List<PrinterVO> printers;
}
