package com.yigongbao.module.order.service.legacy;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yigongbao.module.order.entity.legacy.LegacyOrderListEntity;
import com.yigongbao.module.order.mapper.legacy.LegacyOrderListMapper;
import org.springframework.stereotype.Service;

@Service
public class LegacyOrderArchiveService extends ServiceImpl<LegacyOrderListMapper, LegacyOrderListEntity> {
}
