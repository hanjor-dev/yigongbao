package com.yigongbao.module.notification.announcement.mapper;

import com.yigongbao.module.notification.announcement.vo.AnnouncementTargetUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 系统公告发布时的有效用户解析查询。 */
@Mapper
public interface AnnouncementTargetMapper {

    @Select("SELECT u.id AS userId, u.username AS username, u.real_name AS realName, u.role_name AS roleName FROM sys_user u WHERE u.status = 1 AND u.is_deleted = 0 ORDER BY u.id")
    List<AnnouncementTargetUserVO> selectAllActiveUsers();

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.username AS username, u.real_name AS realName, u.role_name AS roleName FROM sys_user u",
            "WHERE u.status = 1 AND u.is_deleted = 0 AND u.role_id IN <foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach>",
            "ORDER BY u.id",
            "</script>"
    })
    List<AnnouncementTargetUserVO> selectActiveUsersByRoleIds(@Param("roleIds") List<Long> roleIds);

    @Select({
            "<script>",
            "SELECT u.id AS userId, u.username AS username, u.real_name AS realName, u.role_name AS roleName FROM sys_user u",
            "WHERE u.status = 1 AND u.is_deleted = 0 AND u.id IN <foreach collection='userIds' item='userId' open='(' separator=',' close=')'>#{userId}</foreach>",
            "ORDER BY u.id",
            "</script>"
    })
    List<AnnouncementTargetUserVO> selectActiveUsersByIds(@Param("userIds") List<Long> userIds);

    @Select("SELECT COALESCE(NULLIF(real_name, ''), username) FROM sys_user WHERE id = #{userId} AND is_deleted = 0 LIMIT 1")
    String selectUserDisplayName(@Param("userId") Long userId);
}
