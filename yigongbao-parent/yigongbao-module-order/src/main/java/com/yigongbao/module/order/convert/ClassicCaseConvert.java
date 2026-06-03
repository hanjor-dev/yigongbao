package com.yigongbao.module.order.convert;

import cn.hutool.core.bean.BeanUtil;
import com.yigongbao.common.entity.OrderMainEntity;
import com.yigongbao.module.order.vo.ClassicCaseVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 经典案例转换器
 */
public class ClassicCaseConvert {

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
