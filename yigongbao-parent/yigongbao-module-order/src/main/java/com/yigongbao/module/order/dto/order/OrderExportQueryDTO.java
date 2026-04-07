package com.yigongbao.module.order.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 订单导出查询参数 DTO
 * 继承分页查询参数，导出时不限制 pageSize，固定最多10000条
 *
 * @author hanjor
 * @date 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderExportQueryDTO extends OrderPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 导出时忽略分页，固定导出最多10000条
     */
    private static final int MAX_EXPORT_COUNT = 10000;
}
