package com.yigongbao.module.order.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import com.yigongbao.module.order.vo.order.OrderDetailVO;
import com.yigongbao.module.order.vo.order.OrderListVO;

/**
 * 经典案例转换器
 */
public class ClassicCaseConvert {

    /**
     * 将 OrderDetailVO 转换为 ClassicCaseVO，并填充经典案例特有字段
     *
     * @param orderDetailVO 订单详情 VO（包含完整订单信息、订单明细、文件列表）
     * @param entity        订单实体（提供经典案例特有字段）
     * @return 经典案例 VO
     */
    public static ClassicCaseVO toClassicCaseVO(OrderDetailVO orderDetailVO, OrderMainEntity entity) {
        if (orderDetailVO == null || entity == null) {
            return null;
        }
        ClassicCaseVO vo = new ClassicCaseVO();
        // 复制 OrderDetailVO 的所有字段（包含订单明细、文件列表等完整信息）
        BeanUtil.copyProperties(orderDetailVO, vo);

        // 填充经典案例特有字段
        vo.setClassicCaseTime(entity.getClassicCaseTime());
        vo.setClassicCaseBy(entity.getClassicCaseBy());
        vo.setClassicCaseRemark(entity.getClassicCaseRemark());
        // classicCaseByName 保持 null，后续可通过 UserService 查询填充

        return vo;
    }

    /**
     * 将 OrderListVO 转换为 ClassicCaseVO（用于列表查询）
     *
     * @param orderListVO 订单列表 VO（包含基础订单信息和翻译字段）
     * @param entity      订单实体（提供经典案例特有字段）
     * @return 经典案例 VO
     */
    public static ClassicCaseVO toClassicCaseVOFromList(OrderListVO orderListVO, OrderMainEntity entity) {
        if (orderListVO == null || entity == null) {
            return null;
        }
        ClassicCaseVO vo = new ClassicCaseVO();
        // 复制 OrderListVO 的基础字段
        BeanUtil.copyProperties(orderListVO, vo);

        // 填充经典案例特有字段
        vo.setClassicCaseTime(entity.getClassicCaseTime());
        vo.setClassicCaseBy(entity.getClassicCaseBy());
        vo.setClassicCaseRemark(entity.getClassicCaseRemark());

        return vo;
    }
}
