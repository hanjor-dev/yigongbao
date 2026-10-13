package com.yigongbao.module.order.vo.modify;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单修改留痕记录 VO
 *
 * @author hanjor
 * @date 2026-04-08
 */
@Data
public class ModificationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderCode;
    private String publicOrderCode;
    private Long applyId;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
    private Long modifierId;
    private String modifierName;
    private LocalDateTime createTime;
}
