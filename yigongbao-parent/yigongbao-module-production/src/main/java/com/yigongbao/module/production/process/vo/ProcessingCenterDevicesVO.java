package com.yigongbao.module.production.process.vo;

import com.yigongbao.module.production.record.vo.PrinterVO;
import lombok.Data;
import java.util.List;

/**
 * 加工中心设备列表VO（按加工中心分组）
 *
 * @author hanjor
 * @date 2026-05-28
 */
@Data
public class ProcessingCenterDevicesVO {
    private Long centerId;
    private String centerName;
    private List<PrinterVO> devices;
}
