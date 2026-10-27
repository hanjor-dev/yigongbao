package com.yigongbao.module.backup.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yigongbao.module.backup.receipt.entity.OrderBackupReceiptEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderBackupReceiptMapper extends BaseMapper<OrderBackupReceiptEntity> {
    @Select("SELECT * FROM order_backup_receipt WHERE local_idempotency_key = #{key} AND is_deleted = 0 LIMIT 1")
    OrderBackupReceiptEntity selectByIdempotencyKey(@Param("key") String key);

    /** 在同一事务内锁定当前凭证，保证切换 current 标识时不会并发产生两个当前凭证。 */
    @Select("SELECT * FROM order_backup_receipt WHERE order_id = #{orderId} "
            + "AND is_current = 1 AND is_deleted = 0 LIMIT 1 FOR UPDATE")
    OrderBackupReceiptEntity selectCurrent(@Param("orderId") Long orderId);

    @Update("UPDATE order_backup_receipt SET is_current = 0, update_time = CURRENT_TIMESTAMP(3) "
            + "WHERE order_id = #{orderId} AND is_current = 1 AND is_deleted = 0")
    int clearCurrent(@Param("orderId") Long orderId);

    @Update("UPDATE order_backup_receipt SET receipt_status = #{status}, state_version = state_version + 1, "
            + "update_time = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND state_version = #{version} "
            + "AND is_deleted = 0")
    int updateStatusCas(@Param("id") Long id, @Param("version") Integer version,
                        @Param("status") String status);
}
