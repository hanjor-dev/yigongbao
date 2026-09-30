package com.yigongbao.module.notification.announcement.mapper;

import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 系统公告发布时的有效用户解析查询。 */
@Mapper
public interface AnnouncementTargetMapper {

    List<AnnouncementTargetUserVO> selectAllActiveUsers();

    List<AnnouncementTargetUserVO> selectActiveUsersByRoleIds(@Param("roleIds") List<Long> roleIds);

    List<AnnouncementTargetUserVO> selectActiveUsersByIds(@Param("userIds") List<Long> userIds);

    String selectUserDisplayName(@Param("userId") Long userId);
}
