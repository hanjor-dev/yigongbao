package com.yigongbao.module.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.service.DesignProductService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打印产品信息服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignProductServiceImpl
        extends ServiceImpl<DesignProductMapper, DesignProductEntity>
        implements DesignProductService {

    @Override
    public long countByPackageId(Long packageId) {
        return count(new LambdaQueryWrapper<DesignProductEntity>()
                .eq(DesignProductEntity::getPackageId, packageId));
    }

    @Override
    public Set<Long> getFilledFileIds(List<Long> packageIds) {
        if (CollUtil.isEmpty(packageIds)) {
            return Collections.emptySet();
        }
        return list(new LambdaQueryWrapper<DesignProductEntity>()
                .in(DesignProductEntity::getPackageId, packageIds)
                .select(DesignProductEntity::getPackageFileId))
                .stream()
                .map(DesignProductEntity::getPackageFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
