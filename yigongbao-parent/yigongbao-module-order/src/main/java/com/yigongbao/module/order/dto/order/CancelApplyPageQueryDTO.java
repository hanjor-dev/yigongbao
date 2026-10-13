package com.yigongbao.module.order.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 取消申请分页查询参数。
 */
@Data
public class CancelApplyPageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 100, message = "每页条数最大值为100")
    private Integer pageSize = 10;

    /** 订单编号，模糊匹配。 */
    private String orderCode;

    /** 申请人姓名，模糊匹配。 */
    private String applyByName;

    /** 申请人 ID，仅供服务层设置当前用户范围。 */
    private Long applyBy;

    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;
}
