package com.yigongbao.module.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.order.dto.ClassicCaseQueryDTO;
import com.yigongbao.module.order.dto.MarkClassicCaseDTO;
import com.yigongbao.module.order.vo.ClassicCaseVO;

/**
 * 订单经典案例服务接口
 */
public interface IOrderClassicCaseService {

    /**
     * 标记订单为经典案例
     */
    void markAsClassicCase(MarkClassicCaseDTO dto);

    /**
     * 查询经典案例列表（分页）
     */
    IPage<ClassicCaseVO> listClassicCases(ClassicCaseQueryDTO dto);

    /**
     * 获取经典案例详情
     */
    ClassicCaseVO getClassicCaseDetail(Long orderId);

    /**
     * 检查订单是否为经典案例
     */
    boolean isClassicCase(Long orderId);
}
