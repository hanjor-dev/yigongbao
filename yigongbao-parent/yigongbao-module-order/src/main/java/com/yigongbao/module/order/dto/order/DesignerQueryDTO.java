package com.yigongbao.module.order.dto.order;

import lombok.Data;

import java.util.List;

/**
 * 查询可分配设计师 DTO
 *
 * @author hanjor
 * @date 2026-04-10
 */
@Data
public class DesignerQueryDTO {

    /**
     * 订单涉及的专业方向字典编码列表，如 ["7.1"]
     */
    private List<String> specialties;
}
