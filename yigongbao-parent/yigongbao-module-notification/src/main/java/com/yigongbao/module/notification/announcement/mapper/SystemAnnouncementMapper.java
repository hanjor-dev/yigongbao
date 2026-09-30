package com.yigongbao.module.notification.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 系统公告主表 Mapper。 */
@Mapper
public interface SystemAnnouncementMapper extends BaseMapper<SystemAnnouncementEntity> {
    SystemAnnouncementEntity selectForUpdate(@Param("id") Long id);
}
