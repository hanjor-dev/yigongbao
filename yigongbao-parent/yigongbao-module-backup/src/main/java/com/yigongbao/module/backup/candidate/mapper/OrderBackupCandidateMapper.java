package com.yigongbao.module.backup.candidate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.candidate.entity.OrderBackupCandidateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/** 候选事实 Mapper，仅使用 MyBatis-Plus 和注解 SQL，不使用 XML。 */
@Mapper
public interface OrderBackupCandidateMapper extends BaseMapper<OrderBackupCandidateEntity> {

    @Select("SELECT COALESCE(MAX(generation), 0) FROM order_backup_candidate "
            + "WHERE order_id = #{orderId} AND is_deleted = 0")
    Integer selectMaxGeneration(@Param("orderId") Long orderId);

    @Select("SELECT COALESCE(MAX(event_id), 0) FROM order_backup_candidate WHERE is_deleted = 0")
    Long selectMaxEventId();

    @Select("SELECT * FROM order_backup_candidate WHERE is_deleted = 0 "
            + "ORDER BY eligible_at DESC, event_id DESC LIMIT 1")
    OrderBackupCandidateEntity selectHighWatermark();

    @Select("SELECT * FROM order_backup_candidate WHERE order_id = #{orderId} "
            + "AND generation = #{generation} AND is_deleted = 0 LIMIT 1")
    OrderBackupCandidateEntity selectByOrderGeneration(@Param("orderId") Long orderId,
                                                        @Param("generation") Integer generation);

    @Update("UPDATE order_backup_candidate SET event_status = #{status}, update_time = CURRENT_TIMESTAMP(3) "
            + "WHERE order_id = #{orderId} AND event_status = 'ACTIVE' AND is_deleted = 0")
    int updateActiveStatus(@Param("orderId") Long orderId, @Param("status") String status);

    @Select("SELECT * FROM order_backup_candidate "
            + "WHERE is_deleted = 0 "
            + "AND (#{eligibleFrom} IS NULL OR eligible_at >= #{eligibleFrom}) "
            + "AND (#{eligibleTo} IS NULL OR eligible_at < #{eligibleTo}) "
            + "AND (#{cursorAt} IS NULL OR eligible_at > #{cursorAt} "
            + "OR (eligible_at = #{cursorAt} AND event_id > #{cursorEventId})) "
            + "AND (#{highWaterAt} IS NULL OR eligible_at < #{highWaterAt} "
            + "OR (eligible_at = #{highWaterAt} AND event_id <= #{highWaterEventId})) "
            + "ORDER BY eligible_at ASC, event_id ASC LIMIT #{limit}")
    List<OrderBackupCandidateEntity> search(@Param("eligibleFrom") LocalDateTime eligibleFrom,
                                            @Param("eligibleTo") LocalDateTime eligibleTo,
                                            @Param("cursorAt") LocalDateTime cursorAt,
                                            @Param("cursorEventId") Long cursorEventId,
                                            @Param("highWaterAt") LocalDateTime highWaterAt,
                                            @Param("highWaterEventId") Long highWaterEventId,
                                            @Param("limit") int limit);

    @Select("SELECT * FROM order_backup_candidate WHERE is_deleted = 0 "
            + "AND order_id = #{orderId} ORDER BY generation DESC LIMIT 1")
    OrderBackupCandidateEntity selectLatestByOrder(@Param("orderId") Long orderId);

    @Select("SELECT * FROM order_backup_candidate WHERE is_deleted = 0 "
            + "AND order_id BETWEEN #{orderIdFrom} AND #{orderIdTo} "
            + "ORDER BY order_id ASC, generation DESC")
    List<OrderBackupCandidateEntity> selectByOrderRange(@Param("orderIdFrom") Long orderIdFrom,
                                                         @Param("orderIdTo") Long orderIdTo);
}
