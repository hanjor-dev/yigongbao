package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yigongbao.common.service.SpecReferenceChecker;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 规格引用检查器实现
 * 检查 design_product 表中是否有未删除的记录引用了该规格
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Component
@RequiredArgsConstructor
public class SpecReferenceCheckerImpl implements SpecReferenceChecker {

    private final DesignProductMapper designProductMapper;

    @Override
    public boolean isSpecInUse(Long specId) {
        return designProductMapper.selectCount(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .eq(DesignProductEntity::getSpecId, specId)) > 0;
    }
}
