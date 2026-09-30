package com.yigongbao.module.notification.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementRecipientEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.yigongbao.module.notification.announcement.vo.AnnouncementPendingVO;

import java.util.List;

/** 系统公告接收记录 Mapper。 */
@Mapper
public interface SystemAnnouncementRecipientMapper extends BaseMapper<SystemAnnouncementRecipientEntity> {
    List<AnnouncementPendingVO> selectPendingByUserId(@Param("userId") Long userId);

    int acknowledge(@Param("announcementId") Long announcementId, @Param("userId") Long userId);

    int revokePending(@Param("announcementId") Long announcementId);

    int insertBatch(@Param("items") List<SystemAnnouncementRecipientEntity> items);
}
