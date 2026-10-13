package com.yigongbao.module.order.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 订单导出查询参数 DTO
 * 继承分页查询参数，导出时忽略 pageSize，按服务端最终 Excel 行数上限流式导出
 *
 * @author hanjor
 * @date 2026-04-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderExportQueryDTO extends OrderPageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

}
