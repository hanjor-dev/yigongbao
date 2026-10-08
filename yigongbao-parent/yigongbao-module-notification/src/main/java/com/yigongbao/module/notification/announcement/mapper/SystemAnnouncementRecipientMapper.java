package com.yigongbao.module.notification.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yigongbao.module.notification.announcement.entity.SystemAnnouncementRecipientEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.yigongbao.module.notification.announcement.vo.AnnouncementPendingVO;
import com.yigongbao.module.notification.announcement.vo.AnnouncementRecipientVO;

import java.util.List;

/** 系统公告接收记录 Mapper。 */
@Mapper
public interface SystemAnnouncementRecipientMapper extends BaseMapper<SystemAnnouncementRecipientEntity> {
    @Select({
            "SELECT a.id AS id, a.title, a.content_html AS contentHtml, a.force_confirm AS forceConfirm, a.published_at AS publishedAt",
            "FROM system_announcement_recipient r INNER JOIN system_announcement a ON a.id = r.announcement_id",
            "WHERE r.user_id = #{userId} AND r.delivery_status = 'PENDING' AND r.is_deleted = 0",
            "  AND a.status = 'PUBLISHED' AND a.is_deleted = 0",
            "ORDER BY a.published_at DESC, a.id DESC"
    })
    List<AnnouncementPendingVO> selectPendingByUserId(@Param("userId") Long userId);

    @Select({
            "<script>",
            "SELECT r.id,",
            "       r.announcement_id AS announcementId,",
            "       r.user_id AS userId,",
            "       r.user_name_snapshot AS realName,",
            "       r.username_snapshot AS userNameSnapshot,",
            "       r.role_snapshot AS roleSnapshot,",
            "       r.delivery_status AS deliveryStatus,",
            "       r.acknowledged_at AS acknowledgedAt,",
            "       r.revoked_at AS revokedAt",
            "FROM system_announcement_recipient r",
            "WHERE r.announcement_id = #{announcementId}",
            "  AND r.is_deleted = 0",
            "<if test=\"deliveryStatus != null and deliveryStatus != ''\">",
            "  AND r.delivery_status = #{deliveryStatus}",
            "</if>",
            "<if test=\"keyword != null and keyword != ''\">",
            "  AND (r.user_name_snapshot LIKE CONCAT('%', #{keyword}, '%')",
            "       OR r.username_snapshot LIKE CONCAT('%', #{keyword}, '%'))",
            "</if>",
            "ORDER BY CASE r.delivery_status",
            "             WHEN 'ACKNOWLEDGED' THEN 0",
            "             WHEN 'PENDING' THEN 1",
            "             WHEN 'REVOKED' THEN 2",
            "             ELSE 3",
            "         END,",
            "         r.create_time DESC,",
            "         r.user_name_snapshot ASC,",
            "         r.id ASC",
            "</script>"
    })
    IPage<AnnouncementRecipientVO> selectRecipientPage(IPage<?> page,
                                                        @Param("announcementId") Long announcementId,
                                                        @Param("deliveryStatus") String deliveryStatus,
                                                        @Param("keyword") String keyword);

    @Update("UPDATE system_announcement_recipient SET delivery_status = 'ACKNOWLEDGED', acknowledged_at = NOW(), update_time = NOW() WHERE announcement_id = #{announcementId} AND user_id = #{userId} AND delivery_status = 'PENDING' AND is_deleted = 0")
    int acknowledge(@Param("announcementId") Long announcementId, @Param("userId") Long userId);

    @Update("UPDATE system_announcement_recipient SET delivery_status = 'REVOKED', revoked_at = NOW(), update_time = NOW() WHERE announcement_id = #{announcementId} AND delivery_status = 'PENDING' AND is_deleted = 0")
    int revokePending(@Param("announcementId") Long announcementId);

    @Insert({
            "<script>",
            "INSERT INTO system_announcement_recipient (announcement_id, user_id, user_name_snapshot, username_snapshot, role_snapshot, delivery_status, create_time, update_time, create_by, update_by, is_deleted)",
            "VALUES <foreach collection='items' item='item' separator=','>",
            "(#{item.announcementId}, #{item.userId}, #{item.userNameSnapshot}, #{item.usernameSnapshot}, #{item.roleSnapshot}, #{item.deliveryStatus}, #{item.createTime}, #{item.updateTime}, #{item.createBy}, #{item.updateBy}, #{item.isDeleted})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("items") List<SystemAnnouncementRecipientEntity> items);
}
