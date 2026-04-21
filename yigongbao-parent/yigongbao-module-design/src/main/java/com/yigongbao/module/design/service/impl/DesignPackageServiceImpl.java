package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.service.DesignPackageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 打印文件数据包服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Slf4j
@Service
public class DesignPackageServiceImpl
        extends ServiceImpl<DesignPackageMapper, DesignPackageEntity>
        implements DesignPackageService {

    /**
     * 获取订单下下一个数据包序号
     *
     * @param orderId 订单ID
     * @return 下一个序号（订单下无数据包时返回 1）
     */
    @Override
    public Integer getNextPackageSeq(Long orderId) {
        log.info("获取数据包下一个序号，orderId={}", orderId);

        // 只查询 package_seq 非空的记录，避免 Integer.compareTo 对 null 比较时抛出 NPE
        Integer maxSeq = list(new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .isNotNull(DesignPackageEntity::getPackageSeq)
                        .select(DesignPackageEntity::getPackageSeq))
                .stream()
                .map(DesignPackageEntity::getPackageSeq)
                .max(Integer::compareTo)
                .orElse(0);

        log.info("获取数据包下一个序号完成，orderId={}, maxSeq={}, nextSeq={}", orderId, maxSeq, maxSeq + 1);
        return maxSeq + 1;
    }
}
