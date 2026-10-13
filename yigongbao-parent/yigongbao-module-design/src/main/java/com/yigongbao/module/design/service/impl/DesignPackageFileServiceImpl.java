package com.yigongbao.module.design.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.design.entity.DesignPackageFileEntity;
import com.yigongbao.module.design.mapper.DesignPackageFileMapper;
import com.yigongbao.module.design.service.DesignPackageFileService;
import org.springframework.stereotype.Service;

/**
 * 数据包内文件服务实现类
 * 用于支持批量操作
 *
 * @author hanjor
 * @date 2026-04-15
 */
@Service
public class DesignPackageFileServiceImpl
        extends ServiceImpl<DesignPackageFileMapper, DesignPackageFileEntity>
        implements DesignPackageFileService {
}
