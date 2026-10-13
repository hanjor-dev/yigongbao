package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.NotificationMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 通知消息 Mapper
 *
 * @author hanjor
 * @date 2026-06-18
 */
@Mapper
public interface NotificationMessageMapper extends BaseMapper<NotificationMessageEntity> {

    @Select("SELECT category, COUNT(*) AS count FROM notification_message WHERE receiver_id = #{receiverId} AND is_read = 0 AND is_deleted = 0 GROUP BY category")
    List<Map<String, Object>> selectUnreadCountByCategory(@Param("receiverId") Long receiverId);

    @Update({
            "<script>",
            "UPDATE notification_message SET is_read = 1, read_time = NOW(), update_time = NOW()",
            "WHERE receiver_id = #{receiverId}",
            "  AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "  AND is_deleted = 0",
            "</script>"
    })
    void batchMarkRead(@Param("ids") List<Long> ids, @Param("receiverId") Long receiverId);

    @Update({
            "<script>",
            "UPDATE notification_message SET is_read = 1, read_time = NOW(), update_time = NOW()",
            "WHERE receiver_id = #{receiverId}",
            "  AND is_read = 0",
            "  AND is_deleted = 0",
            "<if test='category != null and category != \"\"'>AND category = #{category}</if>",
            "</script>"
    })
    void markAllRead(@Param("receiverId") Long receiverId, @Param("category") String category);

    @Update("UPDATE notification_message SET biz_status = 'CLAIMED', is_confirmed = 1, confirmed_time = NOW(), update_time = NOW() WHERE biz_type = 'PRODUCTION_CARD' AND biz_id = #{recordId} AND receiver_id != #{claimedByUserId} AND is_confirmed = 0 AND is_deleted = 0")
    void batchMarkClaimed(@Param("recordId") Long recordId, @Param("claimedByUserId") Long claimedByUserId);

    @Update("UPDATE notification_message SET content = JSON_SET(content, '$.remark', #{remark}), " +
            "biz_status = 'PROCESSED' " +
            "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND category = #{category} AND biz_status = 'PENDING' AND is_deleted = 0")
    void updateRemark(@Param("bizType") String bizType, @Param("bizId") Long bizId,
                      @Param("category") String category, @Param("remark") String remark);
}
