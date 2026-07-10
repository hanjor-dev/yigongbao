package com.yigongbao.module.order.convert;

import com.yigongbao.module.order.entity.OrderCancelApplyEntity;
import com.yigongbao.module.order.vo.order.CancelApplyVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 订单取消申请 Convert
 * 提供 Entity 与 VO 之间的转换方法
 *
 * @author Claude Sonnet 4.6
 * @date 2026-07-10
 */
@Component
public class OrderCancelApplyConvert {

    /**
     * 将取消申请实体转换为 VO（基础转换）
     *
     * @param entity 取消申请实体
     * @return VO对象，entity为null时返回null
     */
    public CancelApplyVO toVO(OrderCancelApplyEntity entity) {
        if (entity == null) {
            return null;
        }
        CancelApplyVO vo = new CancelApplyVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 将取消申请实体转换为 VO（包含关联信息）
     *
     * @param entity 取消申请实体
     * @param applyByName 申请人姓名
     * @param auditByName 审核人姓名
     * @param orderCode 订单编号
     * @return VO对象，entity为null时返回null
     */
    public CancelApplyVO toVO(OrderCancelApplyEntity entity, String applyByName,
                              String auditByName, String orderCode) {
        CancelApplyVO vo = toVO(entity);
        if (vo != null) {
            vo.setApplyByName(applyByName);
            vo.setAuditByName(auditByName);
            vo.setOrderCode(orderCode);
        }
        return vo;
    }
}
