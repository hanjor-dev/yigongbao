package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.NotificationMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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

    List<Map<String, Object>> selectUnreadCountByCategory(@Param("receiverId") Long receiverId);

    void batchMarkRead(@Param("ids") List<Long> ids, @Param("receiverId") Long receiverId);

    void markAllRead(@Param("receiverId") Long receiverId, @Param("category") String category);

    void batchMarkClaimed(@Param("recordId") Long recordId, @Param("claimedByUserId") Long claimedByUserId);

    @Update("UPDATE notification_message SET content = JSON_SET(content, '$.remark', #{remark}), " +
            "biz_status = 'PROCESSED' " +
            "WHERE biz_type = #{bizType} AND biz_id = #{bizId} AND category = #{category} AND biz_status = 'PENDING' AND is_deleted = 0")
    void updateRemark(@Param("bizType") String bizType, @Param("bizId") Long bizId,
                      @Param("category") String category, @Param("remark") String remark);
}

