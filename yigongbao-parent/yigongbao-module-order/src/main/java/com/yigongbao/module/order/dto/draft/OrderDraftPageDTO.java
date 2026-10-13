package com.yigongbao.module.order.dto.draft;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 草稿列表分页查询 DTO
 *
 * @author hanjor
 * @date 2026-04-07
 */
@Data
public class OrderDraftPageDTO {

    /**
     * 页码（默认1）
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10）
     */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize = 10;

    /**
     * 医院ID（可选）
     */
    private Long hospitalId;

    /**
     * 草稿状态（可选）：1-有效，2-已提交，3-已过期
     */
    private Integer status;

    /**
     * 患者姓名（模糊搜索，可选）
     */
    private String patientName;

    /**
     * 业务类型（可选）：11.1-业务，11.2-测试，11.3-试用，11.4-代理
     */
    private String businessType;
}
