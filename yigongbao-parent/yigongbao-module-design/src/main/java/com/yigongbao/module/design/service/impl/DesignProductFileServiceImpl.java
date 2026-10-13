package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignProductEntity;
import com.yigongbao.module.design.entity.DesignProductFileEntity;
import com.yigongbao.module.design.mapper.DesignProductFileMapper;
import com.yigongbao.module.design.mapper.DesignProductMapper;
import com.yigongbao.module.design.service.DesignProductFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 打印产品关联文件 Service 实现类
 *
 * @author hanjor
 * @date 2026-04-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DesignProductFileServiceImpl
        extends ServiceImpl<DesignProductFileMapper, DesignProductFileEntity>
        implements DesignProductFileService {

    private final DesignProductMapper designProductMapper;
    @Override
    public List<DesignProductFileEntity> listByProductId(Long designProductId) {
        return list(new LambdaQueryWrapper<DesignProductFileEntity>()
                .eq(DesignProductFileEntity::getDesignProductId, designProductId)
                .orderByAsc(DesignProductFileEntity::getSortOrder));
    }

    @Override
    public List<DesignProductFileEntity> listByProductIds(List<Long> designProductIds) {
        if (designProductIds == null || designProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<DesignProductFileEntity>()
                .in(DesignProductFileEntity::getDesignProductId, designProductIds)
                .orderByAsc(DesignProductFileEntity::getDesignProductId)
                .orderByAsc(DesignProductFileEntity::getSortOrder));
    }

    @Override
    public void removeByProductId(Long designProductId) {
        remove(new LambdaQueryWrapper<DesignProductFileEntity>()
                .eq(DesignProductFileEntity::getDesignProductId, designProductId));
    }

    @Override
    public void removeByProductIds(List<Long> designProductIds) {
        if (designProductIds == null || designProductIds.isEmpty()) {
            return;
        }
        remove(new LambdaQueryWrapper<DesignProductFileEntity>()
                .in(DesignProductFileEntity::getDesignProductId, designProductIds));
    }

    @Override
    public Set<Long> getFilledPackageFileIds(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 第一步：查询这些数据包下的所有 design_product.id
        List<Long> productIds = designProductMapper.selectList(
                new LambdaQueryWrapper<DesignProductEntity>()
                        .in(DesignProductEntity::getPackageId, packageIds)
                        .select(DesignProductEntity::getId))
                .stream().map(DesignProductEntity::getId).toList();
        if (productIds.isEmpty()) {
            return Collections.emptySet();
        }
        // 第二步：查询这些产品行关联的所有 package_file_id
        return list(new LambdaQueryWrapper<DesignProductFileEntity>()
                .in(DesignProductFileEntity::getDesignProductId, productIds)
                .select(DesignProductFileEntity::getPackageFileId))
                .stream()
                .map(DesignProductFileEntity::getPackageFileId)
                .collect(Collectors.toSet());
    }
}
