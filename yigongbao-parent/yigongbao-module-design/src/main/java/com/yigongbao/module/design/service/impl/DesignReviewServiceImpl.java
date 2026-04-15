package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignReviewEntity;
import com.yigongbao.module.design.mapper.DesignReviewMapper;
import com.yigongbao.module.design.service.DesignReviewService;
import org.springframework.stereotype.Service;

/**
 * 设计审核服务实现类
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignReviewServiceImpl
        extends ServiceImpl<DesignReviewMapper, DesignReviewEntity>
        implements DesignReviewService {
}
