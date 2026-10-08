package com.yigongbao.module.notification.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 系统公告主表 Mapper。 */
@Mapper
public interface SystemAnnouncementMapper extends BaseMapper<SystemAnnouncementEntity> {
    @Select("SELECT * FROM system_announcement WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    SystemAnnouncementEntity selectForUpdate(@Param("id") Long id);
}
