package com.yigongbao.module.design.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yigongbao.module.design.entity.DesignPackageEntity;

/**
 * 打印文件数据包服务接口
 *
 * @author hanjor
 * @date 2026-04-15
 */
public interface DesignPackageService extends IService<DesignPackageEntity> {

    /**
     * 获取订单下一个数据包序号
     *
     * @param orderId 订单ID
     * @return 下一个序号
     */
    Integer getNextPackageSeq(Long orderId);
}
