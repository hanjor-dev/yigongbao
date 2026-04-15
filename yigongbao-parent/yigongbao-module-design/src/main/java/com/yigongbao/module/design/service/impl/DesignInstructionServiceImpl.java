package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignInstructionEntity;
import com.yigongbao.module.design.mapper.DesignInstructionMapper;
import com.yigongbao.module.design.service.DesignInstructionService;
import org.springframework.stereotype.Service;

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
}
