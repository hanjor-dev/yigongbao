package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignPackageEntity;
import com.yigongbao.module.design.mapper.DesignPackageMapper;
import com.yigongbao.module.design.service.DesignPackageService;
import org.springframework.stereotype.Service;

/**
 * 打印文件数据包服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignPackageServiceImpl
        extends ServiceImpl<DesignPackageMapper, DesignPackageEntity>
        implements DesignPackageService {

    @Override
    public Integer getNextPackageSeq(Long orderId) {
        Integer maxSeq = list(
                new LambdaQueryWrapper<DesignPackageEntity>()
                        .eq(DesignPackageEntity::getOrderId, orderId)
                        .select(DesignPackageEntity::getPackageSeq))
                .stream()
                .map(DesignPackageEntity::getPackageSeq)
                .max(Integer::compareTo)
                .orElse(0);
        return maxSeq + 1;
    }
}
