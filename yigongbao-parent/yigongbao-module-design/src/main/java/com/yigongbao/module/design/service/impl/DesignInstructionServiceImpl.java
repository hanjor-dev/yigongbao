package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.service.DesignInstructionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设计指令单服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignInstructionServiceImpl
        extends ServiceImpl<DesignInstructionMapper, DesignInstructionEntity>
        implements DesignInstructionService {

    @Override
    public int getMaxVersionSeq(Long packageId) {
        return lambdaQuery()
                .eq(DesignInstructionEntity::getPackageId, packageId)
                .orderByDesc(DesignInstructionEntity::getVersionSeq)
                .last("LIMIT 1")
                .oneOpt()
                .map(DesignInstructionEntity::getVersionSeq)
                .orElse(0);
    }

    @Override
    public List<DesignInstructionEntity> listVersions(Long packageId) {
        return lambdaQuery()
                .eq(DesignInstructionEntity::getPackageId, packageId)
                .orderByDesc(DesignInstructionEntity::getVersionSeq)
                .list();
    }
}
