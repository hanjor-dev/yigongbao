package com.yigongbao.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.notification.entity.NotificationMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
