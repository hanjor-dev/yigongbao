package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignDrawingEntity;
import com.yigongbao.module.design.mapper.DesignDrawingMapper;
import com.yigongbao.module.design.service.DesignDrawingService;
import org.springframework.stereotype.Service;

/**
 * 设计图纸服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignDrawingServiceImpl
        extends ServiceImpl<DesignDrawingMapper, DesignDrawingEntity>
        implements DesignDrawingService {
}
