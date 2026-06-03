package com.yigongbao.module.order.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.ClassicCaseVO;
import com.yigongbao.module.order.vo.order.OrderListVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 经典案例转换器
 */
public class ClassicCaseConvert {

    /**
     * 将 OrderListVO 转换为 ClassicCaseVO，并填充经典案例特有字段
     *
     * @param orderListVO 订单列表 VO（包含完整订单信息和翻译字段）
     * @param entity      订单实体（提供经典案例特有字段）
     * @return 经典案例 VO
     */
    public static ClassicCaseVO toClassicCaseVO(OrderListVO orderListVO, OrderMainEntity entity) {
        if (orderListVO == null || entity == null) {
            return null;
        }
        ClassicCaseVO vo = new ClassicCaseVO();
        // 复制 OrderListVO 的所有字段（包括翻译后的 xxxName 字段）
        BeanUtil.copyProperties(orderListVO, vo);

        // 填充经典案例特有字段（OrderMainEntity 中只有 classicCaseBy ID，没有 Name）
        vo.setClassicCaseTime(entity.getClassicCaseTime());
        vo.setClassicCaseBy(entity.getClassicCaseBy());
        vo.setClassicCaseRemark(entity.getClassicCaseRemark());
        // classicCaseByName 保持 null，后续可通过 UserService 查询填充

        return vo;
    }

    public static ClassicCaseVO toVO(OrderMainEntity entity) {
        if (entity == null) {
            return null;
        }
        ClassicCaseVO vo = new ClassicCaseVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    public static List<ClassicCaseVO> toVOList(List<OrderMainEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(ClassicCaseConvert::toVO)
                .collect(Collectors.toList());
    }
}
