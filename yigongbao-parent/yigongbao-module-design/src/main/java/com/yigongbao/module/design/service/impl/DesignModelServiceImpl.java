package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignModelEntity;
import com.yigongbao.module.design.mapper.DesignModelMapper;
import com.yigongbao.module.design.service.DesignModelService;
import org.springframework.stereotype.Service;

/**
 * 可视化模型服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignModelServiceImpl
        extends ServiceImpl<DesignModelMapper, DesignModelEntity>
        implements DesignModelService {
}
