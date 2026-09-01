package com.yigongbao.module.production.record.dto;

import lombok.Data;

/**
 * 提交原材料批号 DTO。
 * productionBatchNo 仅为旧客户端兼容字段，服务端不再使用。
 *
 * @author hanjor
 * @date 2026-05-27
 */
@Data
public class SubmitBatchNoDTO {
    /** @deprecated 生产批号由提交打印设备时后台生成。 */
    @Deprecated
    private String productionBatchNo;
    /** 原材料批号 */
    private String materialBatchNo;
}
